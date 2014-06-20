package org.lilian.data.real.fractal;

import static org.lilian.util.Series.series;

import java.awt.image.BufferedImage;
import java.util.List;

import org.data2semantics.platform.annotation.In;
import org.data2semantics.platform.annotation.Main;
import org.data2semantics.platform.annotation.Module;
import org.data2semantics.platform.annotation.Out;
import org.lilian.data.real.Datasets;
import org.lilian.data.real.Draw;
import org.lilian.data.real.Point;
import org.lilian.data.real.Similitude;
import org.lilian.util.Series;

@Module(name = "Depth test")
public class DepthTest
{
	@In(name="data", print=false)
	public List<Point> data;
	
	@In(name="variance")
	public double variance;
	
	@In(name="res")
	public int res;

	@In(name="sample")
	public int sample;
	
	@In(name="repeats")
	public int repeats;
	
	@Out(name="best depth")
	public double bestDepth;
	
	@Out(name="perturbed")
	public BufferedImage perturbed()
	{
		return im;
	}
	private BufferedImage im;
	
	@Main
	public void body()
	{
		IFS<Similitude> ifs = IFSs.sierpinskiSim();
		IFS<Similitude> perturbed = IFSs.perturb(ifs, IFS.builder(ifs.size(), Similitude.similitudeBuilder(2)), variance);
		
		bestDepth = -1.0;
		double bestLL = Double.NEGATIVE_INFINITY;
		
		for(double depth : series(0.0, 0.2, 6.0))
		{
			double logLikelihoodMean = 0;
			
			for(int i : series(repeats))
			{
				List<Point> data = Datasets.sample(this.data, sample);
				double logLikelihood = 0.0;
	
				for(Point point : data)
					logLikelihood += Math.log(IFS.density(perturbed, point, depth));
				
				logLikelihoodMean += logLikelihood;
			}
		
			logLikelihoodMean /= (double) repeats;
			
			if(logLikelihoodMean > bestLL)
			{
				bestLL = logLikelihoodMean;
				bestDepth = depth;
			}
		}
	
		System.out.println("finished variance " + variance + " " + bestDepth);
	
	}

}
