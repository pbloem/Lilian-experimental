package org.lilian.experiment;

import static org.lilian.util.Series.series;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.imageio.ImageIO;

import org.lilian.Global;
import org.lilian.data.real.AffineMap;
import org.lilian.data.real.Datasets;
import org.lilian.data.real.Draw;
import org.lilian.data.real.Generator;
import org.lilian.data.real.MVN;
import org.lilian.data.real.Map;
import org.lilian.data.real.Point;
import org.lilian.data.real.Similitude;
import org.lilian.data.real.classification.Classification;
import org.lilian.data.real.classification.Classified;
import org.lilian.data.real.classification.ClassifierTarget;
import org.lilian.data.real.classification.Classifiers;
import org.lilian.data.real.fractal.IFS;
import org.lilian.data.real.fractal.IFSClassifier;
import org.lilian.data.real.fractal.IFSClassifierBasic;
import org.lilian.data.real.fractal.IFSClassifierSingle;
import org.lilian.data.real.fractal.IFSTarget;
import org.lilian.data.real.fractal.IFSs;
import org.lilian.search.Builder;
import org.lilian.search.Parametrizable;
import org.lilian.search.evo.ES;
import org.lilian.search.evo.Target;
import org.lilian.util.Functions;
import org.lilian.util.Pair;
import org.lilian.util.Series;
import org.lilian.util.distance.Distance;
import org.lilian.util.distance.HausdorffDistance;
import org.lilian.util.distance.SquaredEuclideanDistance;

public class IFSClassificationEMSingle extends AbstractExperiment
{
	
	protected Classified<Point> trainingData;
	protected Classified<Point> testData;	
	protected int populationSize;
	protected int generations;
	protected int components;
	protected int dim;
	protected int classes;
	protected int depth;
	protected double initialVar;
	protected int resolution;
	protected int emSampleSize;
	protected int trainSampleSize;
	protected int testSampleSize;
	protected boolean print;
	protected boolean smooth;
	protected int numSources;
	protected int beamWidth;
	protected double branchingVariance;
	protected double spanningPointsVariance;
	protected boolean deepening;
	
	/**
	 * State information
	 */
	public @State IFSModelEM emExperiment;
	public @State double score;
	public @State List<Generator<Point>> bases = new ArrayList<Generator<Point>>();
	
	private double bestDistance = Double.MAX_VALUE;
	
	public IFSClassificationEMSingle(
			@Parameter(name="data") 		
				Classified<Point> data,
			@Parameter(name="test ratio", description="What percentage of the data to use for testing")
				double testRatio,
			@Parameter(name="generations") 			
				int generations,
			@Parameter(name="number of components") 
				int components,
			@Parameter(name="depth")				
				int depth,
			@Parameter(name="resolution")
				int resolution,
			@Parameter(name="em sample size")
				int emSampleSize,
			@Parameter(name="train sample size")
				int trainSampleSize,				
			@Parameter(name="test sample size")
				int testSampleSize,
			@Parameter(name="print classifier")
				boolean print,
			@Parameter(name="smooth")
				boolean smooth,
			@Parameter(name="num sources")
				int numSources,
			@Parameter(name="branching variance")
				double branchingVariance,
			@Parameter(name="beam width")
				int beamWidth,
			@Parameter(name="spanning points variance")
				double spanningPointsVariance,
			@Parameter(name="deepening", description="If true, the algorithm starts at depth 1 and increases linearly to the target depth")
				boolean deepening
			
	)
	{	
		Pair<Classified<Point>, Classified<Point>> split = Classification.split(data, testRatio);
		this.trainingData = split.second();
		this.testData = split.first();
	
		this.generations = generations;
		this.components = components;
		this.dim = trainingData.get(0).dimensionality();
		this.depth = depth;
		this.resolution = resolution;
		this.emSampleSize = emSampleSize;
		this.trainSampleSize = trainSampleSize;
		this.testSampleSize = testSampleSize;
		this.print = print;
		this.smooth = smooth;
		
		this.classes = trainingData.numClasses();
		this.numSources = numSources;
		
		this.branchingVariance = branchingVariance;
		this.beamWidth = beamWidth;
		
		this.spanningPointsVariance = spanningPointsVariance;
		this.deepening = deepening;
	}
	
	@Override
	protected void body()
	{
		Environment.current().child(emExperiment);

		// * Construct a model
		IFS<Similitude> ifs = emExperiment.bestModel();
		AffineMap map = emExperiment.map();
			
		IFSClassifierSingle ic = new IFSClassifierSingle(ifs, depth, smooth, map, classes);
		
		logger.info("Model finished. Training code tree.");
		ic.train(trainingData);
		
		write(ic, dir, "classifier");
		
		logger.info("Calculating score");
		
		score = Classification.symmetricError(ic, testData);
	}
	
	public void setup()
	{		
		// * Draw the dataset
		BufferedImage image;
		
		logger.info("Directory:" + dir);
		
		try
		{
			image = Classifiers.draw(trainingData, 100, true);
			ImageIO.write(image, "PNG", new File(dir, "train.png"));
			image = Classifiers.draw(testData, 100, true);
			ImageIO.write(image, "PNG", new File(dir, "test.png"));		
		} catch (IOException e)
		{
			logger.warning("Failed to write dataset. " + e.toString() + " " + Arrays.toString(e.getStackTrace()));
		}	

		emExperiment = new IFSModelEM(
				trainingData, 0.0, depth, generations, components, 
				emSampleSize, trainSampleSize, -1, false, "sphere", numSources, 
				true, beamWidth, branchingVariance, spanningPointsVariance, 
				"likelihood", deepening);
			
	}
	
	@Result(name = "Symmetric error")
	public double score()
	{
		return score;
	}

	@Result(name = "Best distance")
	public double bestScore()
	{
		return bestDistance;
	}

	/**
	 * Print out the current best approximation 
	 * @param ifs
	 * @param dir
	 * @param name
	 */
	private void write(IFSClassifierSingle ifs, File dir, String name)
	{
		double[] xrange = new double[]{-1, 1};
		double[] yrange = new double[]{-1, 1};
		try
		{		 			
			long tt0 = System.currentTimeMillis();
			if(ifs.dimension() == 2)
			{
				BufferedImage image = Classifiers.draw(ifs, xrange, yrange, resolution);
				logger.info("Writing classifier at resolution of " + resolution + " took " +  (System.currentTimeMillis()-tt0)/1000.0 + " seconds.");
	
				ImageIO.write(image, "PNG", new File(dir, name + ".png") );
			}
		} catch (IOException e)
		{
			e.printStackTrace();
		}	
	}

	@Override
	protected void tearDown()
	{
		super.tearDown();
		
		trainingData = null;
		testData = null;
		emExperiment = null;
	}		
	
	
	
}
