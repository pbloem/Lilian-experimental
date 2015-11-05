package org.lilian.data.real.fractal.fin;

import static java.lang.Math.ceil;
import static java.lang.Math.max;
import static java.lang.Math.min;
import static org.lilian.util.Functions.tic;
import static org.lilian.util.Functions.toc;

import java.awt.image.BufferedImage;
import java.awt.image.RenderedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.imageio.ImageIO;

import org.data2semantics.platform.Global;
import org.data2semantics.platform.annotation.In;
import org.data2semantics.platform.annotation.Main;
import org.data2semantics.platform.annotation.Module;
import org.data2semantics.platform.annotation.Out;
import org.data2semantics.platform.util.Series;
import org.lilian.data.real.AffineMap;
import org.lilian.data.real.Datasets;
import org.lilian.data.real.Draw;
import org.lilian.data.real.MVN;
import org.lilian.data.real.Map;
import org.lilian.data.real.MappedList;
import org.lilian.data.real.Maps;
import org.lilian.data.real.Point;
import org.lilian.data.real.Similitude;
import org.lilian.data.real.fractal.old.AffineEM;
import org.lilian.data.real.fractal.old.EMOld;
import org.lilian.data.real.fractal.old.IFS;
import org.lilian.data.real.fractal.old.IFSs;
import org.lilian.data.real.fractal.old.SimEM;
import org.lilian.experiment.Parameter;
import org.lilian.experiment.Reportable;
import org.lilian.search.Parametrizable;
import org.lilian.util.Functions;

@Module(name="Visual")
public class AffinevsSim
{
	private static final boolean CENTER_UNIFORM = true;
	private static final boolean HIGH_QUALITY = true;

	private static final double VAR = 0.1;
	private static final double RADIUS = 0.7;
	private static final double SCALE = 0.1;
	private static final double IDENTITY_INIT_VAR = 0.1;
	private static final int NUM_SOURCES = 1;

	private static final double DEPTH_STEP = 0.5;
	
	@In(name="data", print=false)
	public List<Point> data;	
	
	@In(name="generations")
	public int generations;
	
	@In(name="num components", description="The number of components in the IFS model")
	public int numComponents;
	
	@In(name="sample size")
	public int sampleSize;	
	
	@In(name="spanning variance")
	public double spanningVariance;
	
	@In(name="high quality")
	public boolean highQuality;
	
	@Out(name="best depth")
	public double bestDepth;
	
	private List<RenderedImage> images;
	@Out(name="images")
	public List<RenderedImage> images()
	{
		return images;
	}
	
	private List<RenderedImage> imagesDeep;
	@Out(name="images deep")
	public List<RenderedImage> imagesDeep()
	{
		return imagesDeep;
	}
	
	@Main()
	public void main() throws IOException
	{
		// * SETUP
		AffineMap map = CENTER_UNIFORM ? 
			Maps.centerUniform(data) :
			Maps.centered(data) ;
			
		data = new MappedList(data, map);

		BufferedImage image= Draw.draw(data, 1000, true);
		// imagesDeep.add(image);
		ImageIO.write(image, "PNG", new File(Global.getWorkingDir(), "data.png"));
		
		Global.log().info("Data size: " + data.size());
		
		int dim = data.get(0).dimensionality();
		
		// * We use the "sphere" initialization strategy
		IFS<Similitude> modelSim = IFSs.initialSphere(dim, numComponents, RADIUS, SCALE);
		IFS<AffineMap> modelAffine = IFSs.toAffine(modelSim);
			
		EMOld<Similitude> simEM = new SimEM(modelSim, data, NUM_SOURCES, 
					Similitude.similitudeBuilder(dim), spanningVariance);
		
		EMOld<AffineMap> affEM = new AffineEM(modelAffine, data, NUM_SOURCES, 
				AffineMap.affineMapBuilder(dim), spanningVariance);
				
		images = new ArrayList<RenderedImage>(generations);
		imagesDeep = new ArrayList<RenderedImage>(generations);
						
		double simDepth = 1.0, affDepth = 1.0;
		
		for(int generation : Series.series(generations))
		{			
			if(dim == 2)
				write(simEM.model(), new File(Global.getWorkingDir(), "sim/"), String.format("generation%04d", generation), simDepth, simEM.basis());
			if(dim == 2)
				write(affEM.model(), new File(Global.getWorkingDir(), "aff/"), String.format("generation%04d", generation), affDepth, affEM.basis());
			
			tic();
			simEM.iterate(sampleSize, simDepth);
			Global.log().info(generation + ") Sim finished ("+toc() +" seconds)");

			tic();
			affEM.iterate(sampleSize, affDepth);
			Global.log().info(generation + ") Aff finished ("+toc() +" seconds)");		
			
			simDepth = EMOld.depth(simEM, max(0.5, simDepth - 0.5), 0.5, simDepth + 0.51, 4000, data);
			Global.log().info("new sim depth: " + simDepth);
			
			affDepth = EMOld.depth(affEM, max(0.5, affDepth - 0.5), 0.5, affDepth + 0.51, 4000, data);
			Global.log().info("new aff depth: " + affDepth);
		}
	}
	
	private <M extends Map & Parametrizable> void write(IFS<M> ifs, File dir, String name, double currentDepth, MVN basis) throws IOException
	{		
		int div = highQuality ? 1 : 4;
		int its = highQuality ? (int) 10000000 : 10000;
		
		dir.mkdirs();
		
		BufferedImage image;
		
		image= Draw.draw(ifs, its, new double[]{-1.1, 1.1}, new double[]{-1.1, 1.1}, 1000/div, 1000/div, true, currentDepth, basis);
		ImageIO.write(image, "PNG", new File(dir, name+"png"));
		
		image= Draw.draw(ifs.generator(), its, 1000/div, true);
		ImageIO.write(image, "PNG", new File(dir, name+".deep.png"));

		
	}
}
