package org.lilian.experiment;

import static org.lilian.data.real.Draw.toPixel;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Composite;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.List;

import javax.imageio.ImageIO;

import org.lilian.Global;
import org.lilian.data.real.Datasets;
import org.lilian.data.real.Draw;
import org.lilian.data.real.MVN;
import org.lilian.data.real.Map;
import org.lilian.data.real.Point;
import org.lilian.data.real.fractal.IFS;
import org.lilian.neural.Activations;
import org.lilian.neural.FullNN;
import org.lilian.neural.NeuralNetworks;
import org.lilian.neural.ThreeLayer;
import org.lilian.search.Builder;
import org.lilian.search.Parametrizable;
import org.lilian.search.evo.ES;
import org.lilian.search.evo.Target;
import org.lilian.util.Series;
import org.lilian.util.distance.Distance;
import org.lilian.util.distance.EuclideanDistance;
import org.lilian.util.distance.HausdorffDistance;

public class Flight extends AbstractExperiment
{
	private static double[] xrange = new double[]{-2.1333, 2.1333};
	private static double[] yrange = new double[]{-1.2, 1.2};	
	
	private static final int DIM = 2;
	private static final boolean HIGH_QUALITY = false;
		
	private List<Point> data;
	private int points;
	private int hidden;
	private int iterations;
	private int population;
	private Distance<List<Point>> distance = new HausdorffDistance<Point>(new EuclideanDistance());
	private double initVar;
	private boolean highQuality;
	private double distanceWeight;

	public ES<ThreeLayer> es;
	
	public Flight(
			@Parameter(name="data") List<Point> data,
			@Parameter(name="points") int points,
			@Parameter(name="hidden") int hidden,
			@Parameter(name="iterations") int iterations,
			@Parameter(name="population") int population,
			@Parameter(name="init var") double initVar,
			@Parameter(name="high quality") boolean highQuality,
			@Parameter(name="distance weight") double distanceWeight)
	{
		this.data = data;
		this.points = points;
		this.hidden = hidden;
		this.iterations = iterations;
		this.population = population;
		this.initVar = initVar;
		this.highQuality = highQuality;
		this.distanceWeight = distanceWeight;
		
	}

	@Override
	protected void setup()
	{
		Builder<ThreeLayer> builder = ThreeLayer.builder(DIM, hidden, Activations.sigmoid());
		es = new ES<ThreeLayer>(
				builder,
				new FlightTarget(),
				ES.initial(population, builder.numParameters(), initVar), false
				);
		
		BufferedImage image = Draw.draw(data, xrange, yrange, 1920, 1080, true, false);
		
		try
		{
			ImageIO.write(image, "PNG", new File(dir, "data.png"));
		} catch (IOException e)
		{
			throw new RuntimeException(e);
		}
	}

	@Override
	protected void body()
	{
		for(int i : Series.series(iterations))
		{
			es.breed();
			
			if(i % 100 == 0)
			{
				write(es.best().instance(), String.format("generation%04d", i));
				logger.info(i + ": " + es.best().parameters());
			}
		}
	}
	

	/**
	 * Print out the current best approximation at full HD resolution.
	 * @param ifs
	 * @param dir
	 * @param name
	 */
	private void write(ThreeLayer nn, String name)
	{		
		int div = highQuality ? 1 : 16;
		int its = highQuality ? (int) 1000000 : 100000;
		
		File sub = new File(dir, "generations/");
		sub.mkdirs();
		
//		BufferedImage image = Draw.draw(ifs, its, xrange, yrange, 1920/div, 1080/div, true, depth, basis);
//		BufferedImage image = Draw.draw(ifs, its, xrange, yrange, 1920/div, 1080/div, true);
		BufferedImage image = Draw.draw(
				NeuralNetworks.orbit(nn, new MVN(DIM).generate(), its), 
				xrange, yrange, 1920/div, 1080/div, true, false);

		try
		{
			ImageIO.write(image, "PNG", new File(sub, name + ".png") );
		} catch (IOException e)
		{
			e.printStackTrace();
		}
		
		for(int step : Series.series(1, 25))
		{
			List<Point> sequence = NeuralNetworks.orbit(nn, new MVN(DIM).generate(), 5000 * step);
			int w = 1920, h = 1080;
			
			image = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
			Graphics2D graphics = image.createGraphics();
			
			graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
	
			graphics.setColor(new Color(1.0f, 1.0f, 1.0f, 0.05f));
			graphics.setStroke(new BasicStroke(0.5f));	
		
			Point last = sequence.get(0), current;
			
			for(int i : Series.series(1, step, sequence.size()))
			{
				current = sequence.get(i);
				
				graphics.drawLine(
						toPixel(last.get(0), w, -1.0, 1.0),
						toPixel(last.get(1), h, -1.0, 1.0), 
						toPixel(current.get(0), w, -1.0, 1.0), 
						toPixel(current.get(1), h, -1.0, 1.0));
				
				last = current;
			}
			
			graphics.dispose();
			try
			{
				ImageIO.write(image, "PNG", new File(sub, name + "." + step + ".line.png") );
			} catch (IOException e)
			{
				e.printStackTrace();
			}
		}
	}
	
	private class FlightTarget implements Target<ThreeLayer>
	{
		private static final long serialVersionUID = 273846960467310845L;
		private MVN mvn = new MVN(2);
		private Distance<Point> ed = new EuclideanDistance();
		
		
		@Override
		public double score(ThreeLayer nn)
		{
			List<Point> orbit = NeuralNetworks.orbit(nn, mvn.generate(), points);
			// List<Point> target = mvn.generate(points);
			List<Point> target = Datasets.sample(data, points); 

			// * calculate mean distance between steps
			double sum = 0.0;
			Point last = orbit.get(0), current;
			for(int i : Series.series(1, orbit.size()))
			{
				current = orbit.get(i);
				
				sum += ed.distance(current, last);
				
				last = current;
			}
			
			sum /= orbit.size() - 1.0;
			
			// Global.log().info("avg distance " + sum);
			
			double dist = distance.distance(orbit, target);
			
			// Global.log().info("hauss dist " + dist);
			
			return - (
					(1.0 - distanceWeight) * dist +
					distanceWeight * sum
				);
		}
		
	}

}
