package org.lilian.experiment;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.List;

import javax.imageio.ImageIO;

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
	private static final double VAR = 0.000001;
	private static final boolean HIGH_QUALITY = false;
		
	private int points;
	private int hidden;
	private int iterations;
	private int population;
	private Distance<List<Point>> distance = new HausdorffDistance<Point>(new EuclideanDistance());

	public ES<ThreeLayer> es;
	
	public Flight(
			@Parameter(name="points") int points,
			@Parameter(name="hidden") int hidden,
			@Parameter(name="iterations") int iterations,
			@Parameter(name="population") int population)
	{
		this.points = points;
		this.hidden = hidden;
		this.iterations = iterations;
		this.population = population;
	}

	@Override
	protected void setup()
	{
		Builder<ThreeLayer> builder = ThreeLayer.builder(DIM, hidden, Activations.sigmoid());
		es = new ES<ThreeLayer>(
				builder,
				new FlightTarget(),
				ES.initial(population, builder.numParameters(), VAR), false
				);
	}

	@Override
	protected void body()
	{
		for(int i : Series.series(iterations))
		{
			es.breed();
			
			if(i % 1 == 0)
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
		int div = HIGH_QUALITY ? 1 : 16;
		int its = HIGH_QUALITY ? (int) 10000000 : 100;
		
		File sub = new File(dir, "generations/");
		sub.mkdirs();
		
//		BufferedImage image = Draw.draw(ifs, its, xrange, yrange, 1920/div, 1080/div, true, depth, basis);
//		BufferedImage image = Draw.draw(ifs, its, xrange, yrange, 1920/div, 1080/div, true);
		BufferedImage image = Draw.draw(
				NeuralNetworks.orbit(nn, new MVN(DIM).generate(), its), 
				xrange, yrange, 1920, 1080, true, false);

		try
		{
			ImageIO.write(image, "PNG", new File(sub, name + ".png") );
		} catch (IOException e)
		{
			e.printStackTrace();
		}
	}
	
	private class FlightTarget implements Target<ThreeLayer>
	{
		private static final long serialVersionUID = 273846960467310845L;
		MVN mvn = new MVN(2);
		
		@Override
		public double score(ThreeLayer nn)
		{
			List<Point> orbit = NeuralNetworks.orbit(nn, mvn.generate(), points);
			List<Point> target = mvn.generate(points);

			return - distance.distance(orbit, target);
		}
		
	}

}
