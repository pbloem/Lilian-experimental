package org.lilian.data.real.ds;

import java.util.List;

import org.lilian.data.real.Point;
import org.lilian.experiment.AbstractExperiment;
import org.lilian.experiment.Environment;
import org.lilian.experiment.Experiment;
import org.lilian.experiment.Flight;
import org.lilian.experiment.Parameter;
import org.lilian.neural.ThreeLayer;
import org.lilian.util.distance.Distance;
import org.lilian.util.distance.EuclideanDistance;
import org.lilian.util.distance.HausdorffDistance;

public class Pretrain extends AbstractExperiment
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
	
	private double sigma;
	private int numSources;
	private double learningRate;
	private boolean reset;
	private int emSampleSize;
	private int generations;
	private int epochs;
	private double var;
	protected double branchingVariance;
	protected int sampleSize;
	protected int beamWidth;
	
	public Pretrain(
			@Parameter(name="data") List<Point> data,
			@Parameter(name="points") int points,
			@Parameter(name="hidden") int hidden,
			@Parameter(name="iterations") int iterations,
			@Parameter(name="population") int population,
			@Parameter(name="init var") double initVar,
			@Parameter(name="high quality") boolean highQuality,
			@Parameter(name="sigma")
				double sigma, 
			@Parameter(name="num sources")
				int numSources, 
			@Parameter(name="learning rate")
				double learningRate,
			@Parameter(name="reset")
				boolean reset, 
			@Parameter(name="em sample size")
				int emSampleSize,
			@Parameter(name="generations")
				int generations, 
			@Parameter(name="epochs")
				int epochs,
			@Parameter(name="var")
				double var,
			@Parameter(name="beam width")
				int beamWidth,
			@Parameter(name="branching variance")
				double branchingVariance,
			@Parameter(name="sample size")
				int sampleSize)
	{
		this.data = data;
		this.points = points;
		this.hidden = hidden;
		this.iterations = iterations;
		this.population = population;
		this.initVar = initVar;
		this.highQuality = highQuality;

		this.data = data;
		this.sigma = sigma;
		this.numSources = numSources;
		this.hidden = hidden;
		this.learningRate = learningRate;
		this.reset = reset;
		this.emSampleSize = emSampleSize;
		this.generations = generations;
		this.epochs = epochs;
		this.var = var;
		this.branchingVariance = branchingVariance;
		this.beamWidth = beamWidth;
		this.sampleSize = sampleSize;
	}

	@Override
	protected void body()
	{
		Flight flight = new Flight(data, points, hidden, iterations, 
				population, initVar, highQuality);
		
		Environment.current().child(flight);

		ThreeLayer map = flight.model();
		
		DSEM dsem = new DSEM(data, map, sigma, numSources, learningRate, reset, 
				emSampleSize, generations, epochs, var,
				beamWidth, branchingVariance, sampleSize);
		
		Environment.current().child(dsem);
	}

	
}
