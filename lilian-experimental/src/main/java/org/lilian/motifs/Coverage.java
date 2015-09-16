package org.lilian.motifs;

import static org.nodes.util.Functions.log2;
import static org.nodes.util.Functions.log2Sum;
import static org.nodes.util.LogNum.fromDouble;
import static org.nodes.util.Series.series;
import static org.nodes.models.USequenceEstimator.CIMethod;
import static org.nodes.models.USequenceEstimator.CIType;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.math3.distribution.NormalDistribution;
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.data2semantics.platform.Global;
import org.data2semantics.platform.annotation.In;
import org.data2semantics.platform.annotation.Main;
import org.data2semantics.platform.annotation.Module;
import org.data2semantics.platform.annotation.Out;
import org.data2semantics.platform.util.FrequencyModel;
import org.lilian.experiment.Tools;
import org.nodes.DGraph;
import org.nodes.Graph;
import org.nodes.Graphs;
import org.nodes.Link;
import org.nodes.MapUTGraph;
import org.nodes.Node;
import org.nodes.UGraph;
import org.nodes.models.DSequenceEstimator;
import org.nodes.models.USequenceEstimator;
import org.nodes.random.RandomGraphs;
import org.nodes.util.Functions;
import org.nodes.util.LogNum;
import org.nodes.util.Pair;
import org.nodes.util.Series;
import org.nodes.util.bootstrap.BCaCI;
import org.nodes.util.bootstrap.LogBCaCI;
import org.nodes.util.bootstrap.LogNormalCI;
import org.nodes.util.bootstrap.LogPercentileCI;
import org.nodes.util.bootstrap.PercentileCI;

@Module(name="Test confidence intervals")
public class Coverage
{
	@In(name="single sample")
	public int singleSample;
	
	@In(name="coverage sample")
	public int covSample;
		
	@In(name="alpha")
	public double alpha;
	
	@In(name="gold standard")
	public double gold;
	
	@In(name="coverage sizes")
	public List<Integer> covSizes;
	
	@In(name="data")
	public Graph<String> data;
	
	@In(name="bootstraps")
	public int bsSamples;
	
	@Main(print=false)
	public void samples() throws IOException
	{
		boolean directed = data instanceof DGraph<?>;
		
		// * transform the data into a simple, undirected graph
		if(directed)
			data = Graphs.toSimpleDGraph((DGraph<String>)data);
		else
			data = Graphs.toSimpleUGraph((UGraph<String>)data);
		
		// * Construct a single sample to plot
		
		if(gold < 0.0)
		{
			Global.log().info("Sampling a single set of log probabilites.");
			
			if(directed)
			{
				DSequenceEstimator<String> model = new DSequenceEstimator<String>((DGraph<String>)data);
				for(int i : series(singleSample))
				{
					model.nonuniform();
					if(i % (singleSample/100) == 0)
						System.out.print(".");
				}
				System.out.println();
				
				gold = model.logNumGraphsML();

			} else
			{
				USequenceEstimator<String> model = new USequenceEstimator<String>((UGraph<String>)data);
				for(int i : series(singleSample))
				{
					model.nonuniform();
					if(i % (singleSample/100) == 0)
						System.out.print(".");
				}
				System.out.println();
				
				gold = model.logNumGraphsML();
			}
		}
		
		Global.log().info("Gold standard: " + gold);
			
		// * Estimate coverage
		Global.log().info("Estimating coverage for small samples.");
		
		coverages      = new ArrayList<Double>(covSizes.size());
		intSizes       = new ArrayList<Double>(covSizes.size());
		intSizesStdDev = new ArrayList<Double>(covSizes.size());
				
		for(int n : covSizes)
		{
			Global.log().info("size: " + n);
			
			int hits = 0;
			List<Double> sizes = new ArrayList<Double>(covSample);
			for(int i : series(covSample))
			{
				List<Double> logSamples;
				
				if(directed)
					logSamples = (new DSequenceEstimator<String>((DGraph<String>)data, n)).logSamples();
				else
					logSamples = (new USequenceEstimator<String>(data, n)).logSamples();
								
				LogNormalCI ci = new LogNormalCI(logSamples, 20000);
		
				Pair<Double, Double> interval = ci.twoSided(alpha);
				System.out.println(interval);
					
				sizes.add(interval.second() - interval.first());
					
				if(gold > interval.first() && gold < interval.second())
					hits ++;
			}
			
			// * estimate of coverage
			coverages.add(hits / (double) covSample);
			double mean = Tools.mean(sizes);
			double stdDev = Tools.standardDeviation(sizes);
			
			intSizes.add(mean);
			intSizesStdDev.add(stdDev);
			
			Global.log().info("coverage: " + hits/ (double) covSample);
			Global.log().info("mean size: " + mean + " ("+stdDev+")");
		}
	}
	
	@Out(name="coverages")
	public List<Double> coverages;
	
	@Out(name="interval sizes")
	public List<Double> intSizes;

	@Out(name="interval sizes std dev")
	public List<Double> intSizesStdDev;

	public static double lowerBound(List<Double> bootstraps, double alpha)
	{
		int iBelow = (int)(alpha * bootstraps.size());
		double rem = alpha * bootstraps.size() - (double) iBelow;
		
		if(iBelow == bootstraps.size() -1)
			return bootstraps.get(iBelow);
		
		return interpolate(bootstraps.get(iBelow), bootstraps.get(iBelow+1), (1.0 - rem)).logMag();
	}
	
	/**
	 * Returns a value such that, by estimate, we have 1-alpha confidence that 
	 * the true value of the mean is below the value.  
	 * 
	 * @param alpha
	 * @return
	 */
	public static double upperBound(List<Double> bootstraps, double alpha)
	{
		int iBelow = (int)((1.0 - alpha) * bootstraps.size());
		double rem = ((1.0 - alpha) * bootstraps.size()) - iBelow;
		
		if(iBelow == bootstraps.size() -1)
			return bootstraps.get(iBelow);
		
		return interpolate(bootstraps.get(iBelow), bootstraps.get(iBelow+1), (1.0 - rem)).logMag();
	}	
	
	/**
	 * Returns an upper and a lower bound such that, by estimate, we have 
	 * 1-alpha confidence that the true value is between the two bounds. 
	 * 
	 * @param alpha
	 * @return
	 */
	public static Pair<Double, Double> twoSided(List<Double> bootstraps, double alpha)
	{
		return new Pair<Double, Double>(
			lowerBound(bootstraps, alpha*0.5),
			upperBound(bootstraps, alpha*0.5));
	}
	
	public static LogNum interpolate(double l, double r, double leftWeight)
	{
		LogNum left  = new LogNum(l, true, 2.0);
		LogNum right = new LogNum(r, true, 2.0);
		
		// interpolated values
		LogNum lw = left.times(fromDouble(leftWeight, 2.0));
		LogNum rw = right.times(fromDouble(1.0-leftWeight, 2.0));
		
		return lw.plus(rw);
	}
}