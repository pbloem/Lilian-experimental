package org.lilian.data.real.fractal.fin;

import static java.lang.String.format;
import static org.data2semantics.platform.util.Statistics.toArray;
import static org.lilian.util.Functions.log2;
import static org.lilian.util.Series.series;

import java.awt.image.BufferedImage;
import java.awt.image.RenderedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.imageio.ImageIO;

import org.apache.commons.math3.stat.inference.MannWhitneyUTest;
import org.apache.commons.math3.stat.inference.TTest;
import org.data2semantics.platform.annotation.In;
import org.data2semantics.platform.annotation.Main;
import org.data2semantics.platform.annotation.Module;
import org.data2semantics.platform.annotation.Out;
import org.data2semantics.platform.util.Statistics;
import org.lilian.Global;
import org.lilian.data.real.Datasets;
import org.lilian.data.real.Draw;
import org.lilian.data.real.MVN;
import org.lilian.data.real.MogEM;
import org.lilian.data.real.Point;
import org.lilian.data.real.Similitude;
import org.lilian.data.real.fractal.EMOld;
import org.lilian.data.real.fractal.IFS;
import org.lilian.data.real.fractal.IFSs;
import org.lilian.data.real.fractal.SimEM;
import org.lilian.experiment.Result;
import org.lilian.util.Functions;
import org.nodes.util.Series;

@Module(name="IFS vs MOG")
public class IFSvsMOG
{
	private static final double RADIUS = 0.7;
	private static final double SCALE = 0.1;	
	private static final int NUM_SOURCES = 1;
	
	private List<Point> data;
	private int iterations, repeats;
	private double depth;
	private int sampleSize;
	
	private List<Double> mogLLs, ifsLLs, bestDepths; 
	
	private int numComponents;
	
	private int dataSize;
	private int dataDim;
	
	private double testRatio;
	
	private List<Point> test, train;

	public IFSvsMOG(
			@In(name="data", print=false) List<Point> data, 
			@In(name="iterations") int iterations, 
			@In(name="depth") double depth,
			@In(name="sample size") int sampleSize, 
			@In(name="num components") int numComponents,
			@In(name="test ratio") double testRatio,
			@In(name="reps") int repeats) 
	{
		this.data = data;
		this.iterations = iterations;
		this.depth = depth;
		this.sampleSize = sampleSize;
		this.numComponents = numComponents;
		this.testRatio = testRatio;
		
		this.dataSize = data.size();
		this.dataDim = data.get(0).dimensionality();
		this.repeats = repeats;
	}
	
	@Main
	public void main()
		throws IOException
	{
		int dim = data.get(0).dimensionality();
		
		ifsLLs = new ArrayList<Double>(repeats);
		mogLLs = new ArrayList<Double>(repeats);
		bestDepths = new ArrayList<Double>(repeats);
		
		if(dim == 2) // draw the data
		{
			BufferedImage im = Draw.draw(data, 1000, true);
			ImageIO.write(im, "PNG", new File(org.data2semantics.platform.Global.getWorkingDir(), "data.png"));
		}
		
		// * Split the data in test and training sets		
		train = new ArrayList<Point>(data);
		test  = new ArrayList<Point>((int)(data.size() * testRatio + 1));
		
		for(int i : series((int)(data.size() * testRatio)))
		{
			int draw = Global.random.nextInt(train.size());
			test.add(train.remove(draw));
		}
				
		// * We use the "sphere" initialization strategy
		IFS<Similitude> model = null;
		model = IFSs.initialSphere(dim, numComponents, RADIUS, SCALE);
			
		// * IFS EM model
		EMOld<Similitude> ifsEM = new SimEM(model, train, NUM_SOURCES, 
					Similitude.similitudeBuilder(dim), 1.0);
		// * MOG EM model
		MogEM mogEM = new MogEM(train, numComponents);
		
		for(int repeat: series(repeats))
		{
			File dir = new File(org.data2semantics.platform.Global.getWorkingDir(), format("%02d/", repeat));
			dir.mkdirs();
			
			for(int iteration : Series.series(iterations))
			{
				Global.log().info("Starting iteration " + iteration);
				
				ifsEM.iterate(sampleSize, depth);
				Global.log().info("ifs done");
				
				mogEM.iterate();
				Global.log().info("mog done");
				
				double mogLL =  mogEM.model().logDensity(test);
				Global.log().info("likelihood MOG: " + mogLL);
				if(Double.isNaN(mogLL))
					System.out.println(mogEM.model());
				
				if(dim == 2) // draw the data
				{
					BufferedImage im;
					
					im = Draw.draw(ifsEM.model().generator(depth, ifsEM.basis()), 100000, 1000, true);
					ImageIO.write(im, "PNG", new File(dir, format("ifs.%04d.png", iteration)));
					
					im = Draw.draw(mogEM.model(), 100000, 1000, true);
					ImageIO.write(im, "PNG", new File(dir, format("mog.%04d.png", iteration)));
				}
				
			}
			
			Global.log().info("Calculating MOG likelihood");
			double mogLikelihood = mogEM.model().logDensity(test);
			
			Global.log().info("Calculating IFS likelihood");
			double bestDepth = Double.NaN;
			double ifsLikelihood = Double.NEGATIVE_INFINITY;
			
			for(double d : Series.series(0.0, 0.5, depth+0.1))
			{
				double ll = 0.0;
				
				for(Point p : test)
					ll += log2(IFS.density(ifsEM.model(), p, d, ifsEM.basis()));
				
				if(ll > ifsLikelihood)
				{
					ifsLikelihood = ll;
					bestDepth = d;
				}
				
				System.out.print(d + " ("+format("%.1f", ll)+") ");
			}
			
			mogLLs.add(mogLikelihood);
			ifsLLs.add(ifsLikelihood);
			bestDepths.add(bestDepth);
		}
	}
	
	@Out(name="data size")
	public int dataSize()
	{
		return dataSize;
	}
	
	@Out(name="data dim")
	public int dataDim()
	{
		return dataDim;
	}
	
	@Out(name="mog likelihoods")
	public List<Double> mogLLs()
	{
		return mogLLs;
	}
	
	@Out(name="ifs likelihoods")
	public List<Double> ifsLLs()
	{
		return ifsLLs;
	}
	
	@Out(name="ifs depths")
	public List<Double> depths()
	{
		return bestDepths;
	}
	
	@Out(name="p-value (t test)", description="T test p-value for the null hypothesis that the MOG and IFS likelihoods come from different distributions")
	public double pValueTTest()
	{
		TTest test = new TTest();
		
		return test.tTest(toArray(mogLLs), toArray(ifsLLs));
	}
	
	@Out(name="p-value (mwu)", description="MWU p-value for the null hypothesis that the MOG and IFS likelihoods come from different distributions")
	public double pValueMWU()
	{
		MannWhitneyUTest test = new MannWhitneyUTest();
		
		return test.mannWhitneyUTest(toArray(mogLLs), toArray(ifsLLs));
	}
}
