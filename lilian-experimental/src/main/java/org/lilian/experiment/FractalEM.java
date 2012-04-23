package org.lilian.experiment;

import static org.lilian.data.real.Draw.toPixel;

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
	private static final int SAMPLE_SIZE_TEST = 1000;
	
	protected List<Point> data;
	protected int depth;
	protected int generations;
	protected int components;
	protected int dim;
	protected int distSampleSize;
	protected boolean considerVariance;
	protected int beamWidth;
	
	/**
	 * State information
	 */
	public @State int currentGeneration;
	public @State EM em;
	public @State List<Double> scores;
	
	private Distance<List<Point>> distance = new HausdorffDistance<Point>(new SquaredEuclideanDistance());

	private double bestDistance = Double.MAX_VALUE;
	
	public FractalEM(
			@Parameter(name="data") 				
				List<Point> data, 
			@Parameter(name="depth") 				
				int depth, 
			@Parameter(name="generations") 			
				int generations,
			@Parameter(name="number of components")
				int components, 
			@Parameter(name="dimension") 			
				int dim,
			@Parameter(name="distribution sample size") 
				int distSampleSize,
			@Parameter(name="consider variance")	
				boolean considerVariance,
			@Parameter(name="beam width", description="Beam width to use when searching for codes")
				int beamWidth
			)
	{
		this.data = data;
		this.depth = depth;
		this.generations = generations;
		this.components = components;
		this.dim = dim;
		this.distSampleSize = distSampleSize;
		this.considerVariance = considerVariance;
		this.beamWidth = beamWidth;
		
	}
	
	@Override
	protected void body()
	{
		Functions.tic();		
		while(currentGeneration < generations)
		{
			currentGeneration++;

			em.distributePoints(distSampleSize, beamWidth);
			
//			EM.Maps maps = em.findMaps();
//			System.out.println(maps);
//			for(int j : Series.series(components))
//			{
//				System.out.println(j);
//				BufferedImage im = drawMap(maps.from(j), maps.to(j));
//				try
//				{
//					ImageIO.write(im, "PNG", new File(dir, String.format("map%d.%02d.png", j, currentGeneration)));
//				} catch (IOException e)
//				{
//					throw new RuntimeException(e);
//				}			
//			}			
			
			em.findIFS();
			
			double d = distance.distance(
					Datasets.sample(data, SAMPLE_SIZE_TEST),
					em.model().generator().generate(SAMPLE_SIZE_TEST));
			scores.add(d);
			bestDistance = Math.min(bestDistance, d);

			if(true)
			{
				write(em.model, dir, String.format("generation%04d", currentGeneration));
				logger.info("generation " + currentGeneration + ": " + Functions.toc() + " seconds.");
				Functions.tic();				
				save();
			}	
			
			logger.info("Scores (" + currentGeneration + ") : " + scores());
			logger.info("Best   (" + currentGeneration + ") : " + bestScore());
		}
		

	}
	
	public void setup()
	{
		currentGeneration = 0;
		
		scores = new ArrayList<Double>(generations);
		
		logger.info("Data size: " + data.size());
		
		BufferedImage im = Draw.draw(data, 300, true);
		try
		{
			ImageIO.write(im, "png", new File(dir, "data.png"));
		} catch (IOException e)
		{
			throw new RuntimeException(e);
		}
		
		em = new EM(components, dim, depth, data, VAR, considerVariance);
		em.distributePoints(distSampleSize, beamWidth);
	}
	
	@Result(name = "Scores", description="The scores over successive generations.")
	public List<Double> scores()
	{
		return scores;
	}

	@Result(name = "Best score", description="The best (lowest) score over all generations.")
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
	
	
	public static BufferedImage drawMap(List<Point> from, List<Point> to)
	{
		double[] xrange = {-1.0, 1.0};
		double[] yrange = {-1.0, 1.0};
		int res = 1000;
		boolean log = false;
		
		double 	xDelta = xrange[1] - xrange[0],
				yDelta = yrange[1] - yrange[0];
		
		double maxDelta = Math.max(xDelta, yDelta); 		
		double minDelta = Math.min(xDelta, yDelta);
		
		double step = minDelta/(double) res;
		
		int xRes = (int) (xDelta / step);
		int yRes = (int) (yDelta / step);

		float max = Float.NEGATIVE_INFINITY;
		float min = 0.0f;		
		
		float[][] matrixFrom = new float[yRes][];
		float[][] matrixTo = new float[yRes][];

		for(int x = 0; x < xRes; x++)
		{
			matrixFrom[x] = new float[yRes];
			matrixTo[x] = new float[yRes];

			for(int y = 0; y < yRes; y++)
			{
				matrixFrom[x][y] = 0.0f;
				matrixTo[x][y] = 0.0f;
			}
		}
		
		BufferedImage image = 
				new BufferedImage(xRes, yRes, BufferedImage.TYPE_INT_RGB);
		
		Graphics2D g = image.createGraphics();
		for(int i = 0; i < from.size(); i++)
		{
			Point pointFrom = from.get(i);
			Point pointTo = to.get(i);
			
//			System.out.println(pointFrom + " " + pointTo);
			
			int xf = toPixel(pointFrom.get(0), xRes, xrange[0], xrange[1]); 
			int yf = toPixel(pointFrom.get(1), yRes, yrange[0], yrange[1]);

			int xt = toPixel(pointTo.get(0), xRes, xrange[0], xrange[1]); 
			int yt = toPixel(pointTo.get(1), yRes, yrange[0], yrange[1]);			
			
			g.setColor(Color.WHITE);
			g.setStroke(new BasicStroke(2.0f));	
			g.drawLine(xf, yf, xt, yt);
			
			g.setColor(Color.BLUE);
			g.fillOval(xf-2, yf-2, 4, 4);
			
			g.setColor(Color.GREEN);
			g.fillOval(xt-1, yt-1, 2, 2);
			
		}
		g.dispose();
		
		return image;
	}	
	
}
