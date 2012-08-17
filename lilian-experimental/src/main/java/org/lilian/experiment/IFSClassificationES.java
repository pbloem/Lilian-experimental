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
import org.lilian.search.Builder;
import org.lilian.search.Parametrizable;
import org.lilian.search.evo.ES;
import org.lilian.search.evo.Target;
import org.lilian.util.Functions;
import org.lilian.util.Series;
import org.lilian.util.distance.Distance;
import org.lilian.util.distance.HausdorffDistance;
import org.lilian.util.distance.SquaredEuclideanDistance;

public class IFSClassificationES extends AbstractExperiment
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
	
	/**
	 * State information
	 */
	public @State int currentGeneration;
	public @State ES<IFSClassifier> es;
	public @State List<Double> scores;
	
	private Distance<List<Point>> distance = new HausdorffDistance<Point>(new SquaredEuclideanDistance());

	private double bestDistance = Double.MAX_VALUE;
	
	public IFSClassificationES(
			@Parameter(name="data") 		
				Classified<Point> data,
			@Parameter(name="test ratio", description="What percentage of the data to use for testing")
				double testRatio,
			@Parameter(name="population size") 		
				int populationSize, 
			@Parameter(name="generations") 			
				int generations,
			@Parameter(name="number of components") 
				int components,
			@Parameter(name="depth")				
				int depth,
			@Parameter(name="initial variance", description="Parameter variance for the initial population")
				double initialVar,
			@Parameter(name="training sample size", description="How much of the data to use in training (random sample)")
				int trainingSample,
			@Parameter(name="test sample size", description="How much of the data to use in testing (random sample)")
				int testingSample,
			@Parameter(name="resolution")
				int resolution
	)
	{
		
		Classified<Point> dataCopy = Classification.empty();
		int max = data.numClasses();
		
		for(int i : series(data.size()))
			dataCopy.add(data.get(i), data.cls(i));
		
		this.trainingData = Classification.empty();
		this.testData = Classification.empty();
		
		for(int i : series((int)(data.size() * testRatio)))
		{
			int draw = Global.random.nextInt(dataCopy.size());
			testData.add(dataCopy.get(draw), dataCopy.cls(draw));
			dataCopy.remove(draw);
		}
		
		for(int i : series(dataCopy.size()))
			trainingData.add(dataCopy.get(i), dataCopy.cls(i));
		
		testData.setMaxClass(max - 1);
		trainingData.setMaxClass(max - 1);
		
		this.populationSize = populationSize;
		this.generations = generations;
		this.components = components;
		this.dim = trainingData.get(0).dimensionality();
		this.depth = depth;
		this.initialVar = initialVar;
		this.resolution = resolution;
		
		this.classes = trainingData.numClasses();
	}
	
	@Override
	protected void body()
	{
		Functions.tic();		
		while(currentGeneration < generations)
		{
			currentGeneration++;

			es.breed();
			
			if(true)
			{
				logger.info("generation " + currentGeneration + ": " + Functions.toc() + " seconds.");
				
				write(es.best().instance(), dir, String.format("generation%04d", currentGeneration));
				Functions.tic();				
				save();
			}				
			
			double d = Classification.symmetricError(
					es.best().instance(),
					Classification.sample(testData, testingSample)
					);
			scores.add(d);
			bestDistance = Math.min(bestDistance, d);

		}
	}
	
	public void setup()
	{
		currentGeneration = 0;
		
		ClassifierTarget target = new ClassifierTarget(trainingData); 
		
		Builder<IFSClassifier> builder = 
			IFSClassifier.builder(classes, depth,
				IFS.builder(components, 
						Similitude.similitudeBuilder(dim)),
						AffineMap.affineMapBuilder(dim));
		List<List<Double>> initial = ES.initial(populationSize, builder.numParameters(), initialVar);
		
		logger.info("Created initial population");
		
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
		
		es = new ES<IFSClassifier>(
				builder, target, initial, 
				2, initial.size()*2, 0, 
				ES.CrossoverMode.UNIFORM);
		
		logger.info("Created ES object");
		
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
	 * Print out the current best approximation at full HD resolution.
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
				image = Draw.draw(ifs.model(i).generator(), 100000, xrange, yrange, resolution, resolution, true);
				ImageIO.write(image, "PNG", new File(dir, name + "." + i + ".png") );
			}
			
			if(ifs.dimension() == 2)
			{
				long tt0 = System.currentTimeMillis();
				image = Classifiers.draw(ifs, resolution);
				logger.info("Writing classifier at resolution of " + resolution + " took " +  (System.currentTimeMillis()-tt0)/1000.0 + " seconds.");
	
				ImageIO.write(image, "PNG", new File(dir, name + ".png") );
			}
		} catch (IOException e)
		{
			e.printStackTrace();
		}
	}		
	
}
