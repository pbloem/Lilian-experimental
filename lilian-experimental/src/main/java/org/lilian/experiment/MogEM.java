package org.lilian.experiment;

import static org.lilian.data.real.Draw.toPixel;
import static org.lilian.util.Series.series;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import javax.imageio.ImageIO;

import org.lilian.Global;
import org.lilian.data.real.AffineMap;
import org.lilian.data.real.Datasets;
import org.lilian.data.real.Draw;
import org.lilian.data.real.Generator;
import org.lilian.data.real.MOG;
import org.lilian.data.real.MVN;
import org.lilian.data.real.Map;
import org.lilian.data.real.MappedList;
import org.lilian.data.real.Maps;
import org.lilian.data.real.Point;
import org.lilian.data.real.Similitude;
import org.lilian.data.real.classification.Classification;
import org.lilian.data.real.classification.Classified;
import org.lilian.data.real.fractal.IFS;
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

public class MogEM extends AbstractExperiment
{	
	private static double[] xrange = new double[]{-2.1333, 2.1333};
	private static double[] yrange = new double[]{-1.2, 1.2};	

	protected List<Point> trainingData;
	protected List<Point> testData;		
	protected int generations;
	protected int components;
	protected int sampleSize;
	protected boolean highQuality;
	
	/**
	 * State information
	 */
	public @State int currentGeneration;
	public @State MOG current;
	public @State List<Double> scores;
	public @State MOG bestModel;
	public @State MOG bestLikelihoodModel;
	public @State AffineMap map;
	
	private Distance<List<Point>> distance = new HausdorffDistance<Point>(new SquaredEuclideanDistance());

	private double bestDistance = Double.POSITIVE_INFINITY;
	private double bestLikelihood = Double.NEGATIVE_INFINITY;

	public MogEM(
			@Parameter(name="data") 				
				List<Point> data, 
			@Parameter(name="test ratio", description="What percentage of the data to use for testing")
				double testRatio,
			@Parameter(name="generations") 			
				int generations,
			@Parameter(name="number of components")
				int components, 
			@Parameter(name="test sample size", description="The sample size for evaluating the model")
				int sampleSize,
			@Parameter(name="high quality", description="true: full HD 10E7, iterations, false: 1/16th HD, 10E4 iterations")
				boolean highQuality
			)
	{
	
		List<Point> dataCopy = new ArrayList<Point>(data);
		
		this.trainingData = new ArrayList<Point>(data.size());
		this.testData = new ArrayList<Point>(data.size());
		
		for(int i : series((int)(data.size() * testRatio)))
		{
			int draw = Global.random.nextInt(dataCopy.size());
			testData.add(dataCopy.remove(draw));
		}
		
		trainingData.addAll(dataCopy);

		this.generations = generations;
		this.components = components;
		this.sampleSize = sampleSize;
		this.highQuality = highQuality;
	}
	
	@Override
	protected void body()
	{
		Functions.tic();		
		while(currentGeneration < generations)
		{
			double d;
			
			if(sampleSize != -1)
				d = distance.distance(
					Datasets.sample(trainingData, sampleSize),
					current.generate(sampleSize));
			else
				d = distance.distance(trainingData, current.generate(trainingData.size()));
				
			scores.add(d);
			if(bestDistance > d)
			{
				bestDistance = d;
				bestModel = current;
			}
			
			double ll = 0.0;
			for(Point p : (sampleSize == -1 ? trainingData : Datasets.sample(trainingData, sampleSize)) )
				ll += Math.log(current.density(p));
				
			if(bestLikelihood < ll)
			{
				bestLikelihood = ll;
				bestLikelihoodModel = current;
			}

			if(true)
			{
				if(current.dimension() == 2)
					write(current, dir, String.format("generation%04d", currentGeneration));
				logger.info("generation " + currentGeneration + ": " + Functions.toc() + " seconds.");
				Functions.tic();				
				save();
			}	
			
			logger.info("Scores (" + currentGeneration + ") : " + scores());
			logger.info("Best HD (" + currentGeneration + ") : " + bestScore());
			logger.info("Best LL (" + currentGeneration + ") : " + bestLikelihood());
		
			currentGeneration++;

			
			List<List<Double>> codes = current.expectation(trainingData);
			current = MOG.maximization(codes, trainingData);
		}
		

	}
	
	public void setup()
	{		
		currentGeneration = 0;
		
		scores = new ArrayList<Double>(generations);
		
		logger.info("Data size: " + trainingData.size());
		
		BufferedImage image = Draw.draw(trainingData, xrange, yrange, 1920, 1080, true, false);
		
		try
		{
			ImageIO.write(image, "PNG", new File(dir, "data.png"));
		} catch (IOException e)
		{
			throw new RuntimeException(e);
		}
		
		List<List<Double>> codes = MOG.initial(trainingData.size(), components);
		current = MOG.maximization(codes, trainingData);		
	}
	
	@Result(name = "Best training score", description="The best (lowest) training score over all generations.")
	public double bestScore()
	{
		return bestDistance;
	}
	
	@Result(name = "Test score", description="The score on the test data for the model with the lowest training score.")
	public double testScore()
	{
		double d;
		if(sampleSize != -1)
			d = distance.distance(
				Datasets.sample(testData, sampleSize),
				current.generate(sampleSize));
		else
			d = distance.distance(testData, current.generate(testData.size()));
		
		return d;
	}
	
	
	@Result(name = "Best likelihood", description="The best (highest) likelihood over all generations.")
	public double bestLikelihood()
	{
		return bestLikelihood;
	}
	
	@Result (name ="Below 0.05", description="1 if the best score is below 0.05, 0 otherwise")
	public double below()
	{
		return bestDistance < 0.05 ? 1 : 0;
	}

	@Result(name = "Scores", description="The scores over successive generations.")
	public List<Double> scores()
	{
		return scores;
	}
	
	@Result(name = "Ratios", description="Ratio of the current score divided by the last score")
	public List<Double> scoreRatios()
	{
		List<Double> ratios = new ArrayList<Double>(scores().size());
		
		for(int i = 1; i < scores().size(); i++)
			ratios.add(scores().get(i) / scores.get(i-1));
		
		return ratios;
	}

	/**
	 * Print out the current best approximation at full HD resolution.
	 * @param ifs
	 * @param dir
	 * @param name
	 */
	private void write(MOG current, File dir, String name)
	{		
		int div = highQuality ? 4 : 16;
		int its = highQuality ? (int)10000000 : 1000;
		
		BufferedImage image = Draw.draw(current, its, xrange, yrange, 1920/div, 1080/div, false);
		try
		{
			ImageIO.write(image, "PNG", new File(dir, name + ".png") );
		} catch (IOException e)
		{
			e.printStackTrace();
		}
	}
	

	public MOG model()
	{
		return current;
	}

	public MOG bestModel()
	{
		return bestModel;
	}
	
	public MOG bestLikelihoodModel()
	{
		return bestLikelihoodModel;
	}
}
