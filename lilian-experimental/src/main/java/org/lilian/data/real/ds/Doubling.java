package org.lilian.data.real.ds;

import java.util.ArrayList;
import java.util.List;

import org.lilian.data.real.Point;
import org.lilian.experiment.AbstractExperiment;
import org.lilian.experiment.Environment;
import org.lilian.experiment.Flight;
import org.lilian.experiment.Parameter;
import org.lilian.neural.Activations;
import org.lilian.neural.ThreeLayer;
import org.lilian.search.Builder;
import org.lilian.util.Series;
import org.lilian.util.distance.Distance;
import org.lilian.util.distance.EuclideanDistance;
import org.lilian.util.distance.HausdorffDistance;

public class Doubling extends AbstractExperiment
{
	private List<Point> data;
	private int points;
	private int hidden;
	private int iterations;
	private int population;
	private Distance<List<Point>> distance = new HausdorffDistance<Point>(new EuclideanDistance());
	private double initVar;
	private boolean highQuality;
	private double distanceWeight;
	private int frames;

	public Doubling(
			@Parameter(name="data") List<Point> data,
			@Parameter(name="points") int points,
			@Parameter(name="hidden") int hidden,
			@Parameter(name="iterations") int iterations,
			@Parameter(name="population") int population,
			@Parameter(name="init var") double initVar,
			@Parameter(name="high quality") boolean highQuality,
			@Parameter(name="frames") int frames
			)
	{
		this.data = data;
		this.points = points;
		this.hidden = hidden;
		this.iterations = iterations;
		this.population = population;
		this.initVar = initVar;
		this.highQuality = highQuality;

		this.data = data;
		this.frames = frames;

	}

	@Override
	protected void body()
	{
		Flight flight = new Flight(data, points, hidden, iterations, 
				population, initVar, highQuality);
		
		Environment.current().child(flight);

		ThreeLayer map = flight.model(), im;
		Builder<ThreeLayer> builder = ThreeLayer.builder(
				data.get(0).dimensionality(), hidden, Activations.sigmoid());
		
		List<Double> parameters = map.parameters();
		for(int i : Series.series(frames+1))
		{
			double scalar = 1.0 / frames * i; 
			
			im = builder.build(scale(parameters, scalar));
			
			flight.write(im, "line " + i);
		}
	}
	
	private static List<Double> scale(List<Double> in, double scalar)
	{
		List<Double> result = new ArrayList<Double>(in.size());
		
		for(double d : in)
			result.add(d * scalar);
		
		return result;
	}
	

}
