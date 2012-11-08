package org.lilian.experiment;

import static org.lilian.util.Series.series;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.imageio.ImageIO;

import org.lilian.Global;
import org.lilian.data.real.AffineMap;
import org.lilian.data.real.Datasets;
import org.lilian.data.real.Draw;
import org.lilian.data.real.MVN;
import org.lilian.data.real.Map;
import org.lilian.data.real.MappedList;
import org.lilian.data.real.Maps;
import org.lilian.data.real.Point;
import org.lilian.data.real.Similitude;
import org.lilian.data.real.fractal.IFS;
import org.lilian.data.real.fractal.EM;
import org.lilian.data.real.fractal.IFSTarget;
import org.lilian.data.real.fractal.IFSs;
import org.lilian.search.Parametrizable;
import org.lilian.util.Functions;
import org.lilian.util.distance.Distance;
import org.lilian.util.distance.HausdorffDistance;
import org.lilian.util.distance.SquaredEuclideanDistance;

public class IFSModelEM extends AbstractExperiment
{
	@Reportable
	private static final double VAR = 0.6;

	@Reportable
	private static final double RADIUS = 0.7;

	@Reportable
	private static final double SCALE = 0.5;

	@Reportable
	private static final double IDENTITY_INIT_VAR = 0.1;
	
	protected List<Point> trainingData;
	protected List<Point> testData;		
	protected int depth;
	protected int generations;
	protected int components;
	protected int dim;
	protected int emSampleSize;
	protected int trainSampleSize;
	protected int testSampleSize;
	protected boolean highQuality;
	protected String initStrategy;

	/**
	 * State information
	 */
	public @State int currentGeneration;
	public @State EM em;
	public @State List<Double> scores;
	public @State IFS<Similitude> bestModel, model;
	public @State AffineMap map;
	public @State double testScore;	
	
	private Distance<List<Point>> distance = new HausdorffDistance<Point>(new SquaredEuclideanDistance());

	private double bestScore = Double.POSITIVE_INFINITY;

	public IFSModelEM(
			@Parameter(name="data") 				
				List<Point> data, 
			@Parameter(name="test ratio", description="What percentage of the data to use for testing")
				double testRatio,				
			@Parameter(name="depth") 				
				int depth, 
			@Parameter(name="generations") 			
				int generations,
			@Parameter(name="number of components")
				int components, 
			@Parameter(name="em sample size", description="How many points to use in each iteration of the EM algorithm.") 
				int emSampleSize,
			@Parameter(name="train sample size", description="The sample size (from the training set) for evaluating the model at each iteration.")
				int trainSampleSize,
			@Parameter(name="test sample size", description="Sample size (from the test set) for the final evaluation.")
				int testSampleSize,
			@Parameter(name="high quality", description="true: full HD 10E7, iterations, false: 1/16th HD, 10E4 iterations")
				boolean highQuality,
			@Parameter(name="init strategy", description="What method to use to initialize the EM algorithm (random, spread, sphere, points, identity)")
				String initStrategy
			)
	{
	
		// * Split the data in test and training sets
		List<Point> dataCopy = new ArrayList<Point>(data);
		
		this.trainingData = new ArrayList<Point>(data.size());
		this.testData = new ArrayList<Point>(data.size());
		
		for(int i : series((int)(data.size() * testRatio)))
		{
			int draw = Global.random.nextInt(dataCopy.size());
			testData.add(dataCopy.remove(draw));
		}
		
		trainingData.addAll(dataCopy);
		
		Global.log().info("Training data size " + trainingData.size());		
		Global.log().info("Test data size " + testData.size());

		this.depth = depth;
		this.generations = generations;
		this.components = components;
		this.dim = data.get(0).dimensionality();
		
		this.highQuality = highQuality;
		this.initStrategy = initStrategy;
		
		this.emSampleSize = emSampleSize;
		this.trainSampleSize = trainSampleSize;
		this.testSampleSize = testSampleSize;
	}	
	
	public void setup()
	{		
		currentGeneration = 0;
		
		scores = new ArrayList<Double>(generations);
		
		map = Maps.centered(trainingData);
		trainingData = new MappedList(trainingData, map);	
		
		logger.info("Data size: " + trainingData.size());
		
		BufferedImage image = Draw.draw(trainingData, 1000, true, false);

		try
		{
			ImageIO.write(image, "PNG", new File(dir, "data.png"));
		} catch (IOException e)
		{
			throw new RuntimeException(e);
		}
		
		IFS<Similitude> model = null;
		if(initStrategy.toLowerCase().equals("random"))
			model = IFSs.initialRandom(dim, components, VAR);
		else if(initStrategy.toLowerCase().equals("sphere"))
			model = IFSs.initialSphere(dim, components, RADIUS, SCALE);
		else if(initStrategy.toLowerCase().equals("spread"))
			model = IFSs.initialSpread(dim, components, RADIUS, SCALE);
		else if(initStrategy.toLowerCase().equals("points"))
			model = IFSs.initialPoints(SCALE, Datasets.sample(trainingData, components));
		else if(initStrategy.toLowerCase().equals("identity"))
			model = IFSs.initialIdentity(dim, components, IDENTITY_INIT_VAR);
		
		if(model == null)
			throw new IllegalArgumentException("Initialization strategy \""+initStrategy+"\" not recognized.");
				
		em = new EM(model, trainingData);
	}
	
	@Override
	protected void body()
	{
		Functions.tic();		
		while(currentGeneration < generations)
		{
			model = em.model();
			
			double d;
			
			if(trainSampleSize != -1)
				d = distance.distance(
					Datasets.sample(trainingData, trainSampleSize),
					em.model().generator(depth, em.basis()).generate(trainSampleSize));
			else
				d = distance.distance(
					trainingData, 
					em.model().generator(depth, em.basis()).generate(trainingData.size()));
				
			scores.add(d);
			if(bestScore > d)
			{
				bestScore = d;
				bestModel = em.model();
			}


			if(dim == 2)
				write(em.model(), dir, String.format("generation%04d", currentGeneration));
			logger.info("generation " + currentGeneration + ": " + Functions.toc() + " seconds.");
			Functions.tic();				
			save();
						
			currentGeneration++;

			em.iterate(emSampleSize, depth);

		}
		
		if(testData.size() > 0)
		{
			if(testSampleSize != -1)
				testScore = distance.distance(
					Datasets.sample(testData, testSampleSize),
					em.model().generator(depth, em.basis()).generate(testSampleSize));
			else
				testScore = distance.distance(
						testData, 
						em.model().generator(depth, em.basis()).generate(testData.size()));
		}
		
	}
	
	@Override
	protected void tearDown()
	{
		super.tearDown();
		
		trainingData = null;
		testData = null;
		em = null;
	}

	@Result(name = "Best training score", description="The best (lowest) training score over all generations.")
	public double bestScore()
	{
		return bestScore;
	}
	
	@Result(name = "Test score", description="The score on the test data for the model with the lowest training score.")
	public double testScore()
	{
		return testScore;
	}

	@Result(name = "Scores", description="The scores over successive generations.")
	public List<Double> scores()
	{
		return scores;
	}
	
	public IFS<Similitude> model()
	{
		return model;
	}

	public IFS<Similitude> bestModel()
	{
		return bestModel;
	}
	
	public MVN basis()
	{
		return em.basis();
	}
	
	/**
	 * The map that centers the data
	 * @return
	 */
	public AffineMap map()
	{
		return map;
	}
	
	@Result(name="best model")
	public BufferedImage bestModelImage()
	{
		BufferedImage image = Draw.draw(
				bestModel.generator(depth, em.basis()).generate(10000000), 
				1000, true, false);
		return image;
	}
	
	/**
	 * Print out the current best approximation at full HD resolution.
	 * @param ifs
	 * @param dir
	 * @param name
	 */
	private <M extends Map & Parametrizable> void write(IFS<M> ifs, File dir, String name)
	{		
		int its = highQuality ? (int) 10000000 : 100000;
		
		File genDir = new File(dir, "generations");
		genDir.mkdirs();
		
//		BufferedImage image = Draw.draw(ifs, its, xrange, yrange, 1920/div, 1080/div, true, depth, basis);
//		BufferedImage image = Draw.draw(ifs, its, xrange, yrange, 1920/div, 1080/div, true);
		BufferedImage image = Draw.draw(ifs.generator(depth, em.basis()).generate(its), 1000, true, false);
		
		try
		{
			ImageIO.write(image, "PNG", new File(genDir, name + ".png") );
		} catch (IOException e)
		{
			e.printStackTrace();
		}
	}
}
