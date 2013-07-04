package org.lilian.experiment.rifs;

import static org.lilian.util.Series.series;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.imageio.ImageIO;

import org.lilian.Global;
import org.lilian.data.real.Datasets;
import org.lilian.data.real.Draw;
import org.lilian.data.real.Point;
import org.lilian.data.real.Similitude;
import org.lilian.data.real.fractal.IFS;
import org.lilian.data.real.fractal.random.ChoiceTree;
import org.lilian.data.real.fractal.random.DiscreteRIFS;
import org.lilian.data.real.fractal.random.RIFSEM;
import org.lilian.data.real.fractal.random.RIFSs;
import org.lilian.experiment.AbstractExperiment;
import org.lilian.experiment.Environment;
import org.lilian.experiment.Experiment;
import org.lilian.experiment.IFSModelEM;
import org.lilian.experiment.Parameter;
import org.lilian.experiment.State;
import org.lilian.util.Functions;
import org.lilian.util.Permutations;
import org.lilian.util.Series;
import org.lilian.util.distance.Distance;
import org.lilian.util.distance.EuclideanDistance;
import org.lilian.util.distance.HausdorffDistance;

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
	private int numSources;
	private String initStrategy;
	private int initLearnTrials;
	private int initLearnGenerations;
	private int initLearnDepth;
	private int initLearnSample;

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
				int mapsPerComponent,
			@Parameter(name="num sources")
				int numSources,
			@Parameter(name="init strategy")
				String initStrategy,
			@Parameter(name="init learn trials")
				int initLearnTrials,	
			@Parameter(name="init learn generations")
				int initLearnGenerations,	
			@Parameter(name="init learn depth")
				int initLearnDepth,
			@Parameter(name="init learn sample")
				int initLearnSample
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
		this.numSources = numSources;
		this.initStrategy = initStrategy;
		this.initLearnTrials = initLearnTrials;
		this.initLearnGenerations = initLearnGenerations;
		this.initLearnDepth = initLearnDepth;
		this.initLearnSample = initLearnSample;
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
		DiscreteRIFS<Similitude> initial = null;
		if(initStrategy.equals("sphere"))
		{
			initial = RIFSs.initialSphere(
					data.get(0).get(0).dimensionality(), 
					componentIFSs, mapsPerComponent,
					1.0, 0.33);
		} else if(initStrategy.equals("learn"))
		{
			// * This is a complicated initialization strategy.
			//   It trains a regular IFS modle on the flattended dataset, and tries 
			//   all permutations of its maps to find the best rifs model (by hausdorff distance)
			//   The process is repeated a number of times.
			
			int SETSAMPLE = 50;
			int TSAMPLE = 50;
			int SAMPLEDEPTH = 10;
			
//			List<Point> flat = new ArrayList<Point>();
//			for(List<Point> set : data)
//				flat.addAll(set);
			
//			int compTot = componentIFSs * mapsPerComponent;
			
			DiscreteRIFS<Similitude> bestModel = null;
			double bestScore = Double.POSITIVE_INFINITY;
			Distance<List<List<Point>>> hDistance = new HausdorffDistance<List<Point>>(new HausdorffDistance<Point>(new EuclideanDistance()));
			
			for(int t : series(initLearnTrials))
			{
				IFS<Similitude> meanModel = null;
				
				for(int j : series(componentIFSs))
				{
					List<Point> sample = data.get(Global.random.nextInt(data.size()));
					
					// * Learn an IFS for the flattened dataset
					// TODO: Magic numbers
					IFSModelEM experiment = new IFSModelEM(sample, 0.0, 
							initLearnDepth, initLearnGenerations, mapsPerComponent, initLearnSample, initLearnSample, 0, false, "sphere", 
							0.001, "haussdorff", false);			
					Environment.current().child(experiment);
		
					IFS<Similitude> model = experiment.bestModel();
					
					for(int c : series(model.size()))
					{
						if(meanModel == null)
							meanModel = new IFS<Similitude>(model.get(c), model.probability(c));
						else
							meanModel.addMap(model.get(c), model.probability(c));
					}
				}
					
				// * Try all permutations of the maps of the learned IFS
				for(int[] perm : new Permutations(meanModel.size()))
				{
					int c = 0;
					DiscreteRIFS<Similitude> rifs = null;
					for(int i : series(componentIFSs))
					{
						IFS<Similitude> compModel = null;
						double totalPrior = 0.0;
						
						for(int j : series(mapsPerComponent))
						{
							Similitude sim = meanModel.get(perm[c]);
							double prior = meanModel.probability(perm[c]);
							
							if(compModel == null)
								compModel = new IFS<Similitude>(sim, prior);
							else
								compModel.addMap(sim, prior);
							
							totalPrior += prior;
							c ++;
						}
						
						if(rifs == null)
							rifs = new DiscreteRIFS<Similitude>(compModel, totalPrior);
						else
							rifs.addModel(compModel, totalPrior);
					}
					
					// * Test the rifs
					List<List<Point>> dataSample = sample(data, SETSAMPLE, TSAMPLE);
					List<List<Point>> rifsSample = rifs.randomInstances(TSAMPLE, SETSAMPLE, SAMPLEDEPTH);
					
					double score = hDistance.distance(dataSample, rifsSample);
					
					if(score < bestScore)
					{
						bestScore = score;
						bestModel = rifs; 
					}
				}
			}
			
			initial = bestModel;
		}
		
		logger.info("Initial model found");
	
		em = new RIFSEM(initial, data, depth, sample, spanningPointsVariance, perturbVar, numSources);
		
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
	
	private static <T> List<List<T>> sample(List<List<T>> data, int setSample, int tSample)
	{
		List<List<T>> result = new ArrayList<List<T>>(setSample);
		for(int i : series(setSample))
		{
			List<T> toSample = data.get(Global.random.nextInt(data.size()));
			result.add(Datasets.sample(toSample, tSample));
		}
		return result;
	}

 
}
