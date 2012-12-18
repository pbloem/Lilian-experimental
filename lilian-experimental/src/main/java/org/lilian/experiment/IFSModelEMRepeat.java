package org.lilian.experiment;

import static org.lilian.util.Series.series;

import java.util.ArrayList;
import java.util.List;

import org.lilian.Global;
import org.lilian.data.real.AffineMap;
import org.lilian.data.real.MVN;
import org.lilian.data.real.Point;
import org.lilian.data.real.Similitude;
import org.lilian.data.real.fractal.EM;
import org.lilian.data.real.fractal.IFS;
import org.lilian.util.Series;
import org.lilian.util.distance.Distance;
import org.lilian.util.distance.HausdorffDistance;
import org.lilian.util.distance.SquaredEuclideanDistance;

public class IFSModelEMRepeat extends AbstractExperiment
{
	protected List<Point> data;
	protected double testRatio;
	protected int depth;
	protected int generations;
	protected int components;
	protected int dim;
	protected int emSampleSize;
	protected int trainSampleSize;
	protected int testSampleSize;
	protected boolean highQuality;
	protected String initStrategy;
	protected int numSources;
	protected boolean centerData;
	protected double branchingVariance;
	protected int beamWidth;
	protected double spanningPointsVariance;
	protected String gof;
	protected boolean deepening;
	protected int previousDepth = -1;
	protected int repeats; 
	
	/**
	 * State information
	 */
	public @State int bestIndex;
	public @State IFSModelEM best;
	public @State List<Double> scores = new ArrayList<Double>();
	public @State boolean usingApproximation = true;

	public IFSModelEMRepeat(
			@Parameter(name="em-repeats")
				int repeats,
			@Parameter(name="data") 				
				List<Point> data, 
			@Parameter(name="test ratio", description="What percentage of the data to use for testing")
				double testRatio,				
			@Parameter(name="depth") 				
				int depth, 
			@Parameter(name="generations") 			
				int generations,
			@Parameter(name="number of components")
				int components, 
			@Parameter(name="em sample size", description="How many points to use in each iteration of the EM algorithm.") 
				int emSampleSize,
			@Parameter(name="train sample size", description="The sample size (from the training set) for evaluating the model at each iteration.")
				int trainSampleSize,
			@Parameter(name="test sample size", description="Sample size (from the test set) for the final evaluation.")
				int testSampleSize,
			@Parameter(name="high quality", description="true: full HD 10E7, iterations, false: 1/16th HD, 10E4 iterations")
				boolean highQuality,
			@Parameter(name="init strategy", description="What method to use to initialize the EM algorithm (random, spread, sphere, points, identity)")
				String initStrategy,
			@Parameter(name="spanning points variance", description="When multiple points per code are found, we use point sampled from a basic MVN with this distribution to describe the from and to set.")
				double spanningPointsVariance,
			@Parameter(name="goodness of fit test", description="The method used to select the best model from the iterations of the EM model. Options: hausdorff (Hasudorff distance), likelihood (log likelihood), none (just use last iteration)")
				String goodnessOfFitTest,
			@Parameter(name="deepening", description="If true, the algorithm starts at depth 1 and increases linearly to the target depth")
				boolean deepening
			)
	{
		this(repeats, data, testRatio, depth, generations, components, emSampleSize, trainSampleSize, testSampleSize, highQuality, initStrategy, 1, true, spanningPointsVariance, goodnessOfFitTest, deepening);
	}
	
	public IFSModelEMRepeat(
			@Parameter(name="em-repeats")
				int repeats,
			@Parameter(name="data") 				
				List<Point> data, 
			@Parameter(name="test ratio", description="What percentage of the data to use for testing")
				double testRatio,				
			@Parameter(name="depth") 				
				int depth, 
			@Parameter(name="generations") 			
				int generations,
			@Parameter(name="number of components")
				int components, 
			@Parameter(name="em sample size", description="How many points to use in each iteration of the EM algorithm.") 
				int emSampleSize,
			@Parameter(name="train sample size", description="The sample size (from the training set) for evaluating the model at each iteration.")
				int trainSampleSize,
			@Parameter(name="test sample size", description="Sample size (from the test set) for the final evaluation.")
				int testSampleSize,
			@Parameter(name="high quality", description="true: full HD 10E7, iterations, false: 1/16th HD, 10E4 iterations")
				boolean highQuality,
			@Parameter(name="init strategy", description="What method to use to initialize the EM algorithm (random, spread, sphere, points, identity, maps)")
				String initStrategy,
			@Parameter(name="num sources", description="The number of sources used when determining codes.")
				int numSources,
			@Parameter(name="center data")
				boolean centerData,
			@Parameter(name="beam width")
				int beamWidth,
			@Parameter(name="branching variance")
				double branchingVariance,
			@Parameter(name="spanning points variance", description="When multiple points per code are found, we use point sampled from a basic MVN with this distribution to describe the from and to set.")
				double spanningPointsVariance,
			@Parameter(name="goodness of fit test", description="The method used to select the best model from the iterations of the EM model. Options: hausdorff (Hasudorff distance), likelihood (log likelihood), none (just use last iteration)")
				String goodnessOfFitTest,
			@Parameter(name="deepening", description="If true, the algorithm starts at depth 1 and increases linearly to the target depth")
				boolean deepening
			)
	{
		this(repeats, data, testRatio, depth, generations, components, emSampleSize, trainSampleSize, testSampleSize, highQuality, initStrategy, numSources, centerData, spanningPointsVariance, goodnessOfFitTest, deepening);
		
		this.branchingVariance = branchingVariance;
		this.beamWidth = beamWidth;
	}
	
	public IFSModelEMRepeat(
			@Parameter(name="em-repeats")
				int repeats,
			@Parameter(name="data") 				
				List<Point> data, 
			@Parameter(name="test ratio", description="What percentage of the data to use for testing")
				double testRatio,				
			@Parameter(name="depth") 				
				int depth, 
			@Parameter(name="generations") 			
				int generations,
			@Parameter(name="number of components")
				int components, 
			@Parameter(name="em sample size", description="How many points to use in each iteration of the EM algorithm.") 
				int emSampleSize,
			@Parameter(name="train sample size", description="The sample size (from the training set) for evaluating the model at each iteration.")
				int trainSampleSize,
			@Parameter(name="test sample size", description="Sample size (from the test set) for the final evaluation.")
				int testSampleSize,
			@Parameter(name="high quality", description="true: full HD 10E7, iterations, false: 1/16th HD, 10E4 iterations")
				boolean highQuality,
			@Parameter(name="init strategy", description="What method to use to initialize the EM algorithm (random, spread, sphere, points, identity, maps)")
				String initStrategy,
			@Parameter(name="num sources", description="The number of sources used when determining codes.")
				int numSources,
			@Parameter(name="center data")
				boolean centerData,
			@Parameter(name="spanning points variance", description="When multiple points per code are found, we use point sampled from a basic MVN with this distribution to describe the from and to set.")
				double spanningPointsVariance,
			@Parameter(name="goodness of fit test", description="The method used to select the best model from the iterations of the EM model. Options: hausdorff (Hasudorff distance), likelihood (log likelihood), none (just use last iteration)")
				String goodnessOfFitTest,
			@Parameter(name="deepening", description="If true, the algorithm starts at depth 1 and increases linearly to the target depth")
				boolean deepening
			)
	{
		this.repeats = repeats; 
		
		this.data = data;
		this.testRatio = testRatio;
		
		this.depth = depth;
		this.generations = generations;
		this.components = components;
		this.dim = data.get(0).dimensionality();
		
		this.highQuality = highQuality;
		this.initStrategy = initStrategy;
		
		this.emSampleSize = emSampleSize;
		this.trainSampleSize = trainSampleSize;
		this.testSampleSize = testSampleSize;
		
		this.numSources = numSources;
		this.centerData = centerData;
		
		this.spanningPointsVariance = spanningPointsVariance;
		this.gof = goodnessOfFitTest;
		
		this.deepening = deepening;
	}	
	
	@Override
	protected void body()
	{
		double bestScore = gof.equals("likelihood") ?
				Double.NEGATIVE_INFINITY : Double.POSITIVE_INFINITY;
		
		for(int i : series(repeats))
		{
			IFSModelEM exp = new IFSModelEM(data, testRatio, depth, generations, components, emSampleSize, trainSampleSize, testSampleSize, highQuality, initStrategy, numSources, centerData, beamWidth, branchingVariance, spanningPointsVariance, gof, deepening);
			
			Environment.current().child(exp);
			
			double score = exp.bestScore();
			scores.add(score);
			
			if(gof.equals("likelihood"))
			{
				if(usingApproximation)
				{
					if(exp.usingApproximateLikelihood())
					{
						best = exp;
						bestIndex = i;
						bestScore = score;		
					} else
					{
						usingApproximation = false;
						best = exp;
						bestIndex = i;
						bestScore = score;
					}
				} else
				{
					if(score >= bestScore && ! exp.usingApproximateLikelihood())
					{
						best = exp;
						bestIndex = i;
						bestScore = score;
					}
				}
			} else
			{
				if(score <= bestScore)
				{
					best = exp;
					bestIndex = i;
					bestScore = score;
				}
			}
		}
	}
	
	public IFSModelEM best()
	{
		return best;
	}
	
	@Result(name="best index")
	public int bestIndex()
	{
		return bestIndex;
	}
	
	@Result(name="scores")
	public List<Double> scores()
	{
		return scores;
	}

}
