package org.lilian.experiment.rifs;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.imageio.ImageIO;

import org.lilian.Global;
import org.lilian.data.real.Draw;
import org.lilian.data.real.Point;
import org.lilian.data.real.Similitude;
import org.lilian.data.real.fractal.random.ChoiceTree;
import org.lilian.data.real.fractal.random.DiscreteRIFS;
import org.lilian.data.real.fractal.random.RIFSEM;
import org.lilian.data.real.fractal.random.RIFSs;
import org.lilian.experiment.AbstractExperiment;
import org.lilian.experiment.Parameter;
import org.lilian.experiment.State;
import org.lilian.util.Series;

public class RIFSExperiment extends AbstractExperiment
{
	// * stable: 100 datasets, 128 samples per dataset, depth 6 (on koch and cantor)
	
	// * If we start with the correct trees for the data, the cantor model is
	//   successfully induced from a random initial model. (in less than 
	//   five generations)
	
	private static int RES = 200;
	private static int NUM_RANDOM = 3;
		
	private List<List<Point>> data;
	private int sample;
	private int depth;
	private int generations;
	private double spanningPointsVariance;
	private double perturbVar;
	private int componentIFSs;
	private int mapsPerComponent;
	
	private File genDir;
	
	@State
	public RIFSEM em; 
	
	public RIFSExperiment(
			@Parameter(name="data") 	
				List<List<Point>> data,
			@Parameter(name="sample") 	
				int sample,
			@Parameter(name="depth") 	
				int depth,
			@Parameter(name="generations")
				int generations,
			@Parameter(name="spanning points variance")
				double spanningPointsVariance,
			@Parameter(name="perturb var")
				double perturbVar,
			@Parameter(name="component IFSs")
				int componentIFSs,
			@Parameter(name="maps per component")
				int mapsPerComponent
	)
	{
		this.data = data;
		this.depth = depth;
		this.sample = sample;
		this.generations = generations;
		this.spanningPointsVariance = spanningPointsVariance;
		this.perturbVar = perturbVar;
		this.componentIFSs = componentIFSs;
		this.mapsPerComponent = mapsPerComponent;
	}
	
	@Override
	protected void setup()
	{
		
		// * Draw the data
		File dataDir = new File(dir, "data/");
		dataDir.mkdirs();
		
		for(int i :Series.series( data.size()))
		{
			BufferedImage image = Draw.draw(data.get(i), RES, true);
			try
			{
				ImageIO.write(image, "PNG", new File(dataDir, String.format("data%04d.png", i)));
			} catch (IOException e)
			{
				throw new RuntimeException(e);
			}
		}
		
		// * Set up the EM
		DiscreteRIFS<Similitude> initial = RIFSs.initialSphere(
				data.get(0).get(0).dimensionality(), 
				componentIFSs, mapsPerComponent,
				1.0, 0.33);
	
		em = new RIFSEM(initial, data, depth, sample, spanningPointsVariance, perturbVar);
		
		genDir = new File(dir, "generations/");
		genDir.mkdirs();
	}
	
	@Override
	protected void body()
	{
		for(int i : Series.series(generations))
		{
			logger.info("Starting generation " + i);
			
			BufferedImage image = RIFSs.draw(em.model(), RES, NUM_RANDOM);
			try {
				
				ImageIO.write(image, "PNG", new File(genDir, String.format("%04d.png", i)));
				
			} catch (IOException e) { throw new RuntimeException(e); }
			
			em.iteration();	
		}
	}

 
}
