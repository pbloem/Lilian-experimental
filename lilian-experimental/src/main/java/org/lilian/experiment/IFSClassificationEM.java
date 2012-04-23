package org.lilian.experiment;

import static org.lilian.util.Series.series;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.imageio.ImageIO;

import org.lilian.data.real.Datasets;
import org.lilian.data.real.Draw;
import org.lilian.data.real.Map;
import org.lilian.data.real.Point;
import org.lilian.data.real.Similitude;
import org.lilian.data.real.classification.Classification;
import org.lilian.data.real.classification.Classified;
import org.lilian.data.real.classification.ClassifierTarget;
import org.lilian.data.real.classification.Classifiers;
import org.lilian.data.real.fractal.IFS;
import org.lilian.data.real.fractal.IFSClassifier;
import org.lilian.data.real.fractal.IFSTarget;
import org.lilian.data.real.fractal.IFSs;
import org.lilian.data.real.fractal.em.EM;
import org.lilian.search.Builder;
import org.lilian.search.Parametrizable;
import org.lilian.search.evo.ES;
import org.lilian.search.evo.Target;
import org.lilian.util.Functions;
import org.lilian.util.Series;
import org.lilian.util.distance.Distance;
import org.lilian.util.distance.HausdorffDistance;
import org.lilian.util.distance.SquaredEuclideanDistance;

public class IFSClassificationEM extends AbstractExperiment
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
	protected int trainingSample;
	protected int testingSample;
	protected int resolution;
	protected int distSampleSize;
	protected int beamWidth;
	protected boolean print;
	
	/**
	 * State information
	 */
	public @State int currentGeneration;
	public @State List<EM> ems;
	public @State List<Double> scores;
	
	private Distance<List<Point>> distance = new HausdorffDistance<Point>(new SquaredEuclideanDistance());

	private double bestDistance = Double.MAX_VALUE;
	
	public IFSClassificationEM(
			@Parameter(name="training data") 		
				Classified<Point> trainingData,
			@Parameter(name="test data") 			
				Classified<Point> testData,
			@Parameter(name="generations") 			
				int generations,
			@Parameter(name="number of components") 
				int components,
			@Parameter(name="depth")				
				int depth,
			@Parameter(name="initial variance", description="Parameter variance for the initial population")
				double initialVar,
			@Parameter(name="test sample size", description="How much of the data to use in testing (random sample)")
				int testingSample,
			@Parameter(name="resolution")
				int resolution,
			@Parameter(name="distribution sample size")
				int distSampleSize,
			@Parameter(name="beam width")
				int beamWidth,
			@Parameter(name="print classifier")
				boolean print
	)
	{
		this.trainingData = trainingData;
		this.testData = testData;
		this.generations = generations;
		this.components = components;
		this.dim = trainingData.get(0).dimensionality();
		this.depth = depth;
		this.initialVar = initialVar;
		this.resolution = resolution;
		this.distSampleSize = distSampleSize;
		this.beamWidth = beamWidth;
		this.print = print;
		
		this.classes = trainingData.numClasses();
	}
	
	@Override
	protected void body()
	{
		Functions.tic();		
		while(currentGeneration < generations)
		{
			currentGeneration++;
			
			// * Iterate the EM's
			for(EM em : ems)
			{
				em.distributePoints(distSampleSize, depth, beamWidth);
				em.findIFS();
			}
			
			// * Construct a model
			IFSClassifier ic = null;
			for(int i : series(trainingData.numClasses()))
			{
				double prior = trainingData.points(i).size() / (double)trainingData.size();
				IFS<Similitude> ifs = ems.get(i).model();
				if(ic == null)
					ic = new IFSClassifier(ifs, prior, depth);
				else
					ic.add(ifs, prior);
			}
			
			write(ic, dir, String.format("generation%04d", currentGeneration));
			
			double d = Classification.symmetricError(
					ic,
					Classification.sample(testData, testingSample)
					);

			scores.add(d);
			bestDistance = Math.min(bestDistance, d);
			
			logger.info("generation " + currentGeneration + ": " + Functions.toc() + " seconds.");
			Functions.tic();				
			save();
		}
	}
	
	public void setup()
	{
		currentGeneration = 0;
		
		ClassifierTarget target = new ClassifierTarget(trainingData); 
		
		Builder<IFSClassifier> builder = 
			IFSClassifier.builder(classes, depth,
				IFS.builder(components, 
						Similitude.similitudeBuilder(dim)));
		List<List<Double>> initial = ES.initial(populationSize, builder.numParameters(), initialVar);
		
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
		
		scores = new ArrayList<Double>(generations);
		
		ems = new ArrayList<EM>(trainingData.numClasses());
		for(int i : series(trainingData.numClasses()))
		{
			EM em = new EM(components, dim, trainingData.points(i), initialVar, true);
			em.distributePoints(distSampleSize, depth, beamWidth);
			ems.add(em);
		}
	}
	
	@Result(name = "Scores")
	public List<Double> scores()
	{
		return scores;
	}

	@Result(name = "Best score")
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
	private void write(IFSClassifier ifs, File dir, String name)
	{
		double[] xrange = new double[]{-1, 1};
		double[] yrange = new double[]{-1, 1};
		try
		{		
			BufferedImage image; 
			for(int i : Series.series(ifs.size()))
			{
				image = Draw.draw(ifs.model(i).generator(), 1000000, xrange, yrange, resolution, resolution, true);
				ImageIO.write(image, "PNG", new File(dir, name + "." + i + ".png") );
			}
			
			long tt0 = System.currentTimeMillis();
			
			int r = print || currentGeneration == generations - 1 ? resolution : 50;
			image = Classifiers.draw(ifs, r);
			logger.info("Writing classifier at resolution of " + r + " took " +  (System.currentTimeMillis()-tt0)/1000.0 + " seconds.");
	
			ImageIO.write(image, "PNG", new File(dir, name + ".png") );
		} catch (IOException e)
		{
			e.printStackTrace();
		}
	}		
	
}
