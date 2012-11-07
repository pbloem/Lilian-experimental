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
import org.lilian.data.real.fractal.IFSTarget;
import org.lilian.data.real.fractal.IFSs;
import org.lilian.data.real.fractal.em.EM;
import org.lilian.experiment.old.FractalEM;
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
	protected boolean useLikelihood;
	protected double initialVar;
	protected int resolution;
	protected int distSampleSize;
	protected int testSampleSize;
	protected int beamWidth;
	protected boolean print;
	protected boolean dataBasis;
	
	/**
	 * State information
	 */
	public @State List<FractalEM> emExperiments;
	public @State double score;
	public @State List<Generator<Point>> bases = new ArrayList<Generator<Point>>();
	
	private double bestDistance = Double.MAX_VALUE;
	
	public IFSClassificationEM(
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
			@Parameter(name="use likelihood")
				boolean useLikelihood,
			@Parameter(name="resolution")
				int resolution,
			@Parameter(name="distribution sample size")
				int distSampleSize,
			@Parameter(name="test sample size")
				int testSampleSize,
			@Parameter(name="print classifier")
				boolean print,
			@Parameter(name="data basis")
				boolean dataBasis
	)
	{	
		Pair<Classified<Point>, Classified<Point>> split = Classification.split(data, testRatio);
		this.trainingData = split.second();
		this.testData = split.first();
	
		this.generations = generations;
		this.components = components;
		this.dim = trainingData.get(0).dimensionality();
		this.depth = depth;
		this.useLikelihood = useLikelihood; 
		this.resolution = resolution;
		this.distSampleSize = distSampleSize;
		this.testSampleSize = testSampleSize;
		this.print = print;
		this.dataBasis = dataBasis;
		
		this.classes = trainingData.numClasses();
	}
	
	@Override
	protected void body()
	{
		for(FractalEM em : emExperiments)
			Environment.current().child(em);

		// * Construct a model
		IFSClassifierBasic ic = null;
		for(int i : series(trainingData.numClasses()))
		{
			double prior = trainingData.points(i).size() / (double) trainingData.size();
			IFS<Similitude> ifs =  useLikelihood ? emExperiments.get(i).bestLikelihoodModel() : emExperiments.get(i).bestModel();
			AffineMap map = emExperiments.get(i).map();
			
			if(ic == null)
				ic = new IFSClassifierBasic(ifs, prior, map, emExperiments.get(i).basis(), depth);
			else
				ic.add(ifs, prior, map, emExperiments.get(i).basis());
			
			bases.add(emExperiments.get(i).basis());
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
			List<Point> points = trainingData.points(i);
			logger.info("Dataset size for class " + i + ": " + points.size());
			
			FractalEM em = new FractalEM(
					points, 0.0,
					depth, generations, components, distSampleSize,
					true, -1, false, false, testSampleSize, 0.0, false, "sphere", 
					0.0, true, dataBasis);
			
			emExperiments.add(em);
		}
	}
	
	@Result(name = "Symmetric error")
	public double scores()
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
	private void write(IFSClassifierBasic ifs, File dir, String name)
	{
		double[] xrange = new double[]{-1, 1};
		double[] yrange = new double[]{-1, 1};
		try
		{		 
			BufferedImage image; 
			for(int i : Series.series(ifs.size()))
			{
				image = Draw.draw(ifs.model(i).generator(depth, bases.get(i)), 1000000, xrange, yrange, resolution, resolution, true, ifs.preMap(i));
				ImageIO.write(image, "PNG", new File(dir, name + "." + i + ".png") );
			}
			
			long tt0 = System.currentTimeMillis();
			if(ifs.dimension() == 2)
			{
				image = Classifiers.draw(ifs, xrange, yrange, resolution);
				logger.info("Writing classifier at resolution of " + resolution + " took " +  (System.currentTimeMillis()-tt0)/1000.0 + " seconds.");
	
				ImageIO.write(image, "PNG", new File(dir, name + ".png") );
			}
			
			List<Point> points = new ArrayList<Point>();
			for(int i : Series.series(ifs.size()))
				points.addAll(
					ifs.preMap(i).inverse().map(
						ifs.model(i).generator(depth, bases.get(i)).generate(1000000)
					)
				);

			image = Draw.draw(points, 1000, true);
	
			ImageIO.write(image, "PNG", new File(dir, name + ".points.png") );			
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
		emExperiments = null;
	}		
	
	
	
}
