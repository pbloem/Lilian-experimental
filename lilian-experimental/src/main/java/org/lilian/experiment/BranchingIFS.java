package org.lilian.experiment;

import java.util.List;

import org.lilian.data.real.Point;
import org.lilian.data.real.fractal.em.EM;
import org.lilian.util.distance.Distance;
import org.lilian.util.distance.HausdorffDistance;
import org.lilian.util.distance.SquaredEuclideanDistance;

public class BranchingIFS extends AbstractExperiment
{
	
	private static double[] xrange = new double[]{-2.1333, 2.1333};
	private static double[] yrange = new double[]{-1.2, 1.2};	
	
	@Reportable
	private static final double VAR = 0.6;

	@Reportable
	private static final double RADIUS = 0.7;

	@Reportable
	private static final double SCALE = 0.5;

	@Reportable
	private static final double IDENTITY_INIT_VAR = 0.1;
	
	protected List<Point> data;
	protected int depth;
	protected int generations;
	protected int components;
	protected int dim;
	protected int distSampleSize;
	protected boolean considerVariance;
	protected int beamWidth;
	protected boolean deepening;
	protected boolean greedy;
	protected int sampleSize;
	protected double threshold;	
	protected boolean highQuality;
	protected String initStrategy;
	protected double noise;

	
	/**
	 * State information
	 */
	public @State int currentGeneration;
	public @State EM em;
	public @State List<Double> scores;
	
	private Distance<List<Point>> distance = new HausdorffDistance<Point>(new SquaredEuclideanDistance());

	private double bestDistance = Double.MAX_VALUE;
	
	public BranchingIFS(
			@Parameter(name="data") 				
				List<Point> data,
			@Parameter(name="em-steps", description="number of EM steps per generation") 				
				int emSteps, 
			@Parameter(name="es-steps", description="number of ES steps per generation") 				
				int esSteps, 				
			@Parameter(name="depth") 				
				int depth, 
			@Parameter(name="populationSize")
				int populationSize,
			@Parameter(name="generations") 			
				int generations,
			@Parameter(name="number of components")
				int components, 
			@Parameter(name="dimension") 			
				int dim,
			@Parameter(name="distribution sample size", description="Number of datapoints to use for each EM step") 
				int distSampleSize,
			@Parameter(name="model test sample size", description="Sample size to use when evaluating models.") 
				int modelTestSampleSize,	
			@Parameter(name="high quality", description="true: full HD 10E7, iterations, false: 1/16th HD, 10E4 iterations")
				boolean highQuality,
			@Parameter(name="init strategy", description="What method to use to initialize the EM algorithm (random, spread, sphere, points, identity)")
				String initStrategy
			)
	{
	
		this.data = data;
		this.depth = depth;
		this.generations = generations;
		this.components = components;
		this.dim = dim;
		this.distSampleSize = distSampleSize;

		this.highQuality = highQuality;
		this.initStrategy = initStrategy;
		
		this.noise = noise;
	}
	@Override
	protected void setup()
	{
		// TODO Auto-generated method stub

	}

	@Override
	protected void body()
	{
		// TODO Auto-generated method stub

	}

}
