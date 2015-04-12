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
import org.lilian.data.real.fractal.EMOld;
import org.lilian.data.real.fractal.SimEM;
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
	private static final boolean CENTER_UNIFORM = true;
	
	@Reportable
	private static final double VAR = 0.1;

	@Reportable
	private static final double RADIUS = 0.7;

	@Reportable
	private static final double SCALE = 0.1;

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
	protected int numSources;
	protected boolean centerData;
	protected double branchingVariance;
	protected int beamWidth;
	protected double spanningPointsVariance;
	protected String goodnessOfFitTest;
	protected boolean deepening;
	protected int previousDepth = -1;
	
	/**
	 * State information
	 */
	public @State int currentGeneration;
	public @State EMOld<Similitude> em;
	public @State List<Double> scores;
	public @State IFS<Similitude> bestModel, model;
	public @State boolean usingApproximation = true; // for the likelihood gof test (true so long as all non-approximated tests score 0.0)
	public @State int bestGeneration = -1;
	public @State AffineMap map;
	public @State double testScore;
	public @State MVN basis;
	
	private Distance<List<Point>> distance = new HausdorffDistance<Point>(new SquaredEuclideanDistance());
	
	private boolean branching = false;

	private double bestScore;

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
				String initStrategy,
			@Parameter(name="spanning points variance", description="When multiple points per code are found, we use point sampled from a basic MVN with this distribution to describe the from and to set.")
				double spanningPointsVariance,
			@Parameter(name="goodness of fit test", description="The method used to select the best model from the iterations of the EM model. Options: hausdorff (Hasudorff distance), likelihood (log likelihood), none (just use last iteration)")
				String goodnessOfFitTest,
			@Parameter(name="deepening", description="If true, the algorithm starts at depth 1 and increases linearly to the target depth")
				boolean deepening
			)
	{
		this(data, testRatio, depth, generations, components, emSampleSize, trainSampleSize, testSampleSize, highQuality, initStrategy, 1, true, spanningPointsVariance, goodnessOfFitTest, deepening);
	}
	
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
			@Parameter(name="init strategy", description="What method to use to initialize the EM algorithm (random, spread, sphere, points, identity, maps)")
				String initStrategy,
			@Parameter(name="num sources", description="The number of sources used when determining codes.")
				int numSources,
			@Parameter(name="center data")
				boolean centerData,
			@Parameter(name="beam width")
				int beamWidth,
			@Parameter(name="branching variance")
				double branchingVariance,
			@Parameter(name="spanning points variance", description="When multiple points per code are found, we use point sampled from a basic MVN with this distribution to describe the from and to set.")
				double spanningPointsVariance,
			@Parameter(name="goodness of fit test", description="The method used to select the best model from the iterations of the EM model. Options: hausdorff (Hasudorff distance), likelihood (log likelihood), none (just use last iteration)")
				String goodnessOfFitTest,
			@Parameter(name="deepening", description="If true, the algorithm starts at depth 1 and increases linearly to the target depth")
				boolean deepening
			)
	{
		this(data, testRatio, depth, generations, components, emSampleSize, trainSampleSize, testSampleSize, highQuality, initStrategy, numSources, centerData, spanningPointsVariance, goodnessOfFitTest, deepening);
		
		branching = beamWidth > 0;
	
		this.branchingVariance = branchingVariance;
		this.beamWidth = beamWidth;
	}
	
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
			@Parameter(name="init strategy", description="What method to use to initialize the EM algorithm (random, spread, sphere, points, identity, maps)")
				String initStrategy,
			@Parameter(name="num sources", description="The number of sources used when determining codes.")
				int numSources,
			@Parameter(name="center data")
				boolean centerData,
			@Parameter(name="spanning points variance", description="When multiple points per code are found, we use point sampled from a basic MVN with this distribution to describe the from and to set.")
				double spanningPointsVariance,
			@Parameter(name="goodness of fit test", description="The method used to select the best model from the iterations of the EM model. Options: hausdorff (Hasudorff distance), likelihood (log likelihood), none (just use last iteration)")
				String goodnessOfFitTest,
			@Parameter(name="deepening", description="If true, the algorithm starts at depth 1 and increases linearly to the target depth")
				boolean deepening
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
		
		this.numSources = numSources;
		this.centerData = centerData;
		
		this.spanningPointsVariance = spanningPointsVariance;
		this.goodnessOfFitTest = goodnessOfFitTest;
		
		this.deepening = deepening;
	}	
	
	public void setup()
	{		
		currentGeneration = 0;
		
		scores = new ArrayList<Double>(generations);
		
		if(centerData)
		{
			map = CENTER_UNIFORM ? 
				Maps.centerUniform(trainingData) :
				Maps.centered(trainingData) ;
			trainingData = new MappedList(trainingData, map);
		}
		
		logger.info("Data size: " + trainingData.size());
		
		
		if(dim > 1)
		{		
			BufferedImage image = Draw.draw(trainingData, 1000, true, false);

			try
			{
				ImageIO.write(image, "PNG", new File(dir, "data.png"));
			} catch (IOException e)
			{
				throw new RuntimeException(e);
			}
		}
		
		IFS<Similitude> model = null;
		if(initStrategy.toLowerCase().equals("random"))
			model = IFSs.initialRandom(dim, components, VAR);
		else if(initStrategy.toLowerCase().equals("sphere"))
			model = IFSs.initialSphere(dim, components, RADIUS, SCALE);
		else if(initStrategy.toLowerCase().equals("spread"))
			model = IFSs.initialSpread(dim, components, RADIUS, SCALE);
		else if(initStrategy.toLowerCase().equals("points"))
			model = IFSs.initialPoints(SCALE, Datasets.sample(trainingData, components), false);
		else if(initStrategy.toLowerCase().equals("identity"))
			model = IFSs.initialIdentity(dim, components, IDENTITY_INIT_VAR);
		else if(initStrategy.toLowerCase().equals("koch"))
			model = IFSs.koch2Sim();
		else if(initStrategy.toLowerCase().equals("maps"))
			model = IFSs.initialMaps(trainingData, components, 2);
		
		if(model == null)
			throw new IllegalArgumentException("Initialization strategy \""+initStrategy+"\" not recognized.");
				
		em = new SimEM(model, trainingData, numSources, 
					Similitude.similitudeBuilder(dim), spanningPointsVariance);
		
		basis = em.basis();
		
		if(dim > 1)
		{
			BufferedImage image = Draw.draw(
					em.basis().generate(trainingData.size()), 1000, true, false);
	
			try
			{
				ImageIO.write(image, "PNG", new File(dir, "basis.png"));
			} catch (IOException e)
			{
				throw new RuntimeException(e);
			}
			
			image = Draw.draw(new MVN(2).generate(trainingData.size()), 1000, true, false);
	
			try
			{
				ImageIO.write(image, "PNG", new File(dir, "plain.png"));
			} catch (IOException e)
			{
				throw new RuntimeException(e);
			}	
		}
		
		if(goodnessOfFitTest.toLowerCase().equals("hausdorff"))
		{
			bestScore = Double.POSITIVE_INFINITY;
		} else if(goodnessOfFitTest.toLowerCase().equals("likelihood"))
		{
			bestScore = Double.NEGATIVE_INFINITY;
		}	
	}
	
	@Override
	protected void body()
	{
		Functions.tic();		
		while(currentGeneration < generations)
		{
			model = em.model();
			
			// * Measure model performance
			
			List<Point> dataSample = trainSampleSize == -1 ? trainingData : Datasets.sample(trainingData, trainSampleSize);
			List<Point> modelSample = null;
			if(goodnessOfFitTest.toLowerCase().equals("hausdorff"))
				modelSample = em.model().generator(depth, em.basis()).generate(dataSample.size()); 
			
			if(goodnessOfFitTest.toLowerCase().equals("hausdorff"))
			{
				double d = distance.distance(modelSample,  dataSample);
				
				scores.add(d);
				if(d < bestScore)
				{
					bestScore = d;
					bestModel = em.model();
					bestGeneration = currentGeneration;
				}
			} else if(goodnessOfFitTest.toLowerCase().equals("likelihood"))
			{
				double d = 0.0, dApprox = 0.0;
				
				for(Point p : dataSample)
				{
					IFS.SearchResult result = IFS.search(model, p, depth, basis);
					d += Math.log(result.probSum());
					dApprox += Math.log(result.approximation());
				}
				
				logger.info("score: " + d + " approx:" + dApprox + "using approx: " + usingApproximation);
				scores.add(d);
				
				if(usingApproximation)
				{ 
					if(d > Double.NEGATIVE_INFINITY) // first model with non-zero prob
					{
						usingApproximation = false;
						bestModel = model;
						bestScore = d;
					} else if (dApprox > bestScore)
					{
						bestModel = model;
						bestScore = dApprox;
					}
				} else 
				{
					if (d > bestScore)
					{
						bestModel = model;
						bestScore = d;
					}	
				}
				
			} else if(goodnessOfFitTest.toLowerCase().equals("none"))
			{
				// assume convergence
				bestModel = model;
			} else
			{
				throw new IllegalArgumentException("Goodness of fit test '"+goodnessOfFitTest+"' not recognized. ");
			}

			if(dim == 2)
				write(em.model(), dir, String.format("generation%04d", currentGeneration));
			logger.info("generation " + currentGeneration + ": " + Functions.toc() + " seconds.");
			Functions.tic();		
			
			// * save the current state of the experiment
			// save();
			
			
			int d = deepening ? 
					(int) Math.floor(depth * (currentGeneration/(double)generations)) + 1 :
					depth;
			
			// * revert to best model when increasing depth
			if(d != previousDepth && model != null)
				em.setModel(bestModel);
			previousDepth = d;
			
			em.iterate(emSampleSize, d);
						
			currentGeneration++;	
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
		} else {
			Global.log().info("Test data size == 0, no test.");
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
		return basis;
	}
	
	public boolean usingApproximateLikelihood()
	{
		return usingApproximation;
	}
	
	@Result(name="best generation")
	public int bestGeneration()
	{
		return bestGeneration;
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
		if(bestModel == null)
			return null;
		
		BufferedImage image = Draw.draw(
				bestModel.generator(depth, em.basis()).generate(100000), 
				1000, true, false);
		return image;
	}
	
	@Result(name="best model deep")
	public BufferedImage bestModelDeepImage()
	{
		BufferedImage image = Draw.draw(
				bestModel.generator().generate(100000), 
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
		int div = highQuality ? 1 : 4;
		int its = highQuality ? (int) 10000000 : 10000;
		
		File genDir = new File(dir, "generations");
		genDir.mkdirs();
		
		BufferedImage image;
		
		image= Draw.draw(ifs, its, new double[]{-1.1, 1.1}, new double[]{-1.1, 1.1}, 1000/div, 1000/div, true, depth, em.basis());
		
		try
		{
			ImageIO.write(image, "PNG", new File(genDir, name + ".png") );
		} catch (IOException e)
		{
			e.printStackTrace();
		}
		
		image= Draw.draw(ifs.generator().generate(its), 1000/div, true, false);
		
		try
		{
			ImageIO.write(image, "PNG", new File(genDir, name + ".deep.png") );
		} catch (IOException e)
		{
			e.printStackTrace();
		}
		
	}
}
