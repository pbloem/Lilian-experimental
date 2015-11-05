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
import org.lilian.data.real.MOG;
import org.lilian.data.real.MVN;
import org.lilian.data.real.Map;
import org.lilian.data.real.Point;
import org.lilian.data.real.Similitude;
import org.lilian.data.real.classification.Classification;
import org.lilian.data.real.classification.Classified;
import org.lilian.data.real.classification.ClassifierTarget;
import org.lilian.data.real.classification.Classifiers;
import org.lilian.data.real.classification.DensityClassifier;
import org.lilian.data.real.fractal.old.IFS;
import org.lilian.data.real.fractal.old.IFSClassifier;
import org.lilian.data.real.fractal.old.IFSClassifierBasic;
import org.lilian.data.real.fractal.old.IFSTarget;
import org.lilian.data.real.fractal.old.IFSs;
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

public class MOGClassificationEM extends AbstractExperiment
{
	
	protected Classified<Point> trainingData;
	protected Classified<Point> testData;	
	protected int generations;
	protected int components;
	protected int dim;
	protected int classes;
	protected int resolution;
	protected boolean print;
	
	/**
	 * State information
	 */
	public @State List<MogEM> emExperiments;
	public @State double score;
		
	public MOGClassificationEM(
			@Parameter(name="data") 		
				Classified<Point> data,
			@Parameter(name="test ratio", description="What percentage of the data to use for testing")
				double testRatio,
			@Parameter(name="generations") 			
				int generations,
			@Parameter(name="number of components") 
				int components,
			@Parameter(name="resolution")
				int resolution,
			@Parameter(name="print classifier")
				boolean print
	)
	{	
		Pair<Classified<Point>, Classified<Point>> split = Classification.split(data, testRatio);
		this.trainingData = split.second();
		this.testData = split.first();
	
		this.generations = generations;
		this.components = components;
		this.dim = trainingData.get(0).dimensionality(); 
		this.resolution = resolution;
		this.print = print;
		
		this.classes = trainingData.numClasses();
	}
	
	@Override
	protected void body()
	{
		for(MogEM em : emExperiments)
			Environment.current().child(em);

		// * Construct a model
		DensityClassifier<MOG> ic = null;
		for(int i : series(trainingData.numClasses()))
		{
			double prior = trainingData.points(i).size() / (double) trainingData.size();
			MOG mog =  emExperiments.get(i).bestLikelihoodModel();
			
			if(ic == null)
				ic = new DensityClassifier<MOG>(mog, prior);
			else
				ic.add(mog, prior);
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
	
		emExperiments = new ArrayList<MogEM>(trainingData.numClasses());
		for(int i : series(trainingData.numClasses()))
		{
			List<Point> points = trainingData.points(i);
			logger.info("Dataset size for class " + i + ": " + points.size());
			
			MogEM em = new MogEM(points, 0.0, generations, components, -1, false);
			
			emExperiments.add(em);
		}
	}
	
	@Result(name = "Symmetric error")
	public double scores()
	{
		return score;
	}

	/**
	 * Print out the current best approximation 
	 * @param ifs
	 * @param dir
	 * @param name
	 */
	private void write(DensityClassifier<MOG> dc, File dir, String name)
	{
		double[] xrange = new double[]{-1, 1};
		double[] yrange = new double[]{-1, 1};
		try
		{		 
			BufferedImage image; 
			for(int i : Series.series(dc.size()))
			{
				image = Draw.draw(dc.models().get(i), 1000000, xrange, yrange, resolution, resolution, true);
				ImageIO.write(image, "PNG", new File(dir, name + "." + i + ".png") );
			}
			
			long tt0 = System.currentTimeMillis();
			if(dc.dimension() == 2)
			{
				image = Classifiers.draw(dc, xrange, yrange, resolution);
				logger.info("Writing classifier at resolution of " + resolution + " took " +  (System.currentTimeMillis()-tt0)/1000.0 + " seconds.");
	
				ImageIO.write(image, "PNG", new File(dir, name + ".png") );
			}
			
			List<Point> points = new ArrayList<Point>();
			for(int i : Series.series(dc.size()))
				points.addAll(
						dc.models().get(i).generate((int) (1000000 *  dc.prior(i)))
				);

			image = Draw.draw(points, 1000, true);
	
			ImageIO.write(image, "PNG", new File(dir, name + ".points.png") );			
		} catch (IOException e)
		{
			e.printStackTrace();
		}
	}		
	
}
