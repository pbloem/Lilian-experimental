package org.lilian.experiment;

import static org.lilian.util.Series.series;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.imageio.ImageIO;

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
	protected int resolution;
	protected int distSampleSize;
	protected int testSampleSize;
	protected int beamWidth;
	protected boolean print;
	
	/**
	 * State information
	 */
	public @State List<FractalEM> emExperiments;
	public @State double score;
	
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
			@Parameter(name="resolution")
				int resolution,
			@Parameter(name="distribution sample size")
				int distSampleSize,
			@Parameter(name="test sample size")
				int testSampleSize,
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
		this.resolution = resolution;
		this.distSampleSize = distSampleSize;
		this.testSampleSize = testSampleSize;
		this.print = print;
		
		this.classes = trainingData.numClasses();
	}
	
	@Override
	protected void body()
	{
		for(FractalEM em : emExperiments)
			Environment.current().child(em);

		// * Construct a model
		IFSClassifier ic = null;
		for(int i : series(trainingData.numClasses()))
		{
			double prior = trainingData.points(i).size() / (double)trainingData.size();
			IFS<Similitude> ifs = emExperiments.get(i).bestModel();
			AffineMap map = emExperiments.get(i).map();
			if(ic == null)
				ic = new IFSClassifier(ifs, prior, map, depth);
			else
				ic.add(ifs, prior, map);
		}
		
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
	
		emExperiments = new ArrayList<FractalEM>(trainingData.numClasses());
		for(int i : series(trainingData.numClasses()))
		{
			FractalEM em = new FractalEM(
					trainingData.points(i),
					depth, generations, components, dim, distSampleSize,
					true, -1, false, false, testSampleSize, 0.0, false, "sphere", 
					0.0, true);
			
			emExperiments.add(em);
		}
	}
	
	@Result(name = "Scores")
	public double scores()
	{
		return score;
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
			
			int r = resolution;
			image = Classifiers.draw(ifs, r);
			logger.info("Writing classifier at resolution of " + r + " took " +  (System.currentTimeMillis()-tt0)/1000.0 + " seconds.");
	
			ImageIO.write(image, "PNG", new File(dir, name + ".png") );
		} catch (IOException e)
		{
			e.printStackTrace();
		}
	}		
	
}
