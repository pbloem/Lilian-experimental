package org.lilian.experiment;

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
import org.lilian.data.real.Map;
import org.lilian.data.real.Point;
import org.lilian.data.real.Similitude;
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

public class FractalEM extends AbstractExperiment
{
	@Reportable
	private static final double VAR = 0.6;
	
	@Reportable
	private static final int SAMPLE_SIZE = 150;
	
	@Reportable
	private static final int SAMPLE_SIZE_TEST = 500;
	
	protected List<Point> data;
	protected int depth;
	protected int generations;
	protected int components;
	protected int dim;
	protected int distSampleSize;
	
	/**
	 * State information
	 */
	public @State int currentGeneration;
	public @State EM em;
	public @State List<Double> scores;
	
	private Distance<List<Point>> distance = new HausdorffDistance<Point>(new SquaredEuclideanDistance());

	private double bestDistance = Double.MAX_VALUE;
	
	public FractalEM(
			@Parameter(name="data") 				List<Point> data, 
			@Parameter(name="depth") 				int depth, 
			@Parameter(name="generations") 			int generations,
			@Parameter(name="number of components") int components, 
			@Parameter(name="dimension") 			int dim,
			@Parameter(name="distribution sample size") int distSampleSize
			)
	{
		this.data = data;
		this.depth = depth;
		this.generations = generations;
		this.components = components;
		this.dim = dim;
		this.distSampleSize = distSampleSize;
		
	}
	
	@Override
	protected void body()
	{
		Functions.tic();		
		while(currentGeneration < generations)
		{
			currentGeneration++;

			em.distributePoints(distSampleSize);
			em.findIFS();
			
			double d = distance.distance(
					Datasets.sample(data, SAMPLE_SIZE_TEST),
					em.model().generator().generate(SAMPLE_SIZE_TEST));
			scores.add(d);
			bestDistance = Math.min(bestDistance, d);

			if(true)
			{
				write(em.model, dir, String.format("generation%04d", currentGeneration));
				out.println("generation " + currentGeneration + ": " + Functions.toc() + " seconds.");
				Functions.tic();				
				save();
			}	
			
			out.println("Scores: " + scores());
			out.println("Best: " + bestScore());
		}
		

	}
	
	public void setup()
	{
		currentGeneration = 0;
		
		scores = new ArrayList<Double>(generations);
		
		em = new EM(components, dim, depth, data, VAR);
		em.distributePoints(SAMPLE_SIZE);
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
	private static <M extends Map & Parametrizable> void write(IFS<M> ifs, File dir, String name)
	{
		double[] xrange = new double[]{-2.1333, 2.1333};
		double[] yrange = new double[]{-1.2, 1.2};
		
		BufferedImage image = Draw.draw(ifs.generator(), 10000000, xrange, yrange, 1920, 1080, true);
		try
		{
			ImageIO.write(image, "PNG", new File(dir, name + ".png") );
		} catch (IOException e)
		{
			e.printStackTrace();
		}
	}		
	
}
