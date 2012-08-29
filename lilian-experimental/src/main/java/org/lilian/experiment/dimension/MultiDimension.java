package org.lilian.experiment.dimension;

import static org.lilian.util.Series.series;

import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import nl.peterbloem.powerlaws.Functions;

import org.lilian.Global;
import org.lilian.data.dimension.CorrelationIntegral;
import org.lilian.data.dimension.MultiTakens;
import org.lilian.data.dimension.Takens;
import org.lilian.data.real.Draw;
import org.lilian.data.real.Point;
import org.lilian.experiment.AbstractExperiment;
import org.lilian.experiment.Parameter;
import org.lilian.experiment.Result;
import org.lilian.experiment.Result.Plot;
import org.lilian.experiment.State;
import org.lilian.util.Series;
import org.lilian.util.distance.Distance;
import org.lilian.util.distance.EuclideanDistance;

public class MultiDimension extends AbstractExperiment
{
	private List<Point> data;
	private int samples, plotSamples, maxDepth;
	private double epsilon, sigThreshold;
	private double stepSize; 
	
	public @State List<MultiTakens> fits;
	
	public @State int instances;
	public @State int embeddingDimension;
	public @State List<Double> genDistances;
	public @State List<Double> distances;
	public @State List<Double> generated;
	public @State double significance;
	public @State BufferedImage plot;
	
	public @State CorrelationIntegral cint;

	public MultiDimension(
			@Parameter(name="data") 
				List<Point> data,
			@Parameter(name="samples", description="The number of times to sample for maxDistance values (use -1 to use all values)")
				int samples,
			@Parameter(name="epsilon", description="The accuracy required in the significance calculation.")
				double epsilon,
			@Parameter(name="significance threshold")
				double sigThreshold,
			@Parameter(name="ci step size", description="Stepsize for the correlation integral.")
				double stepSize,
			@Parameter(name="plot samples", description="How many points to generate/sample for the plots of distances")
				int plotSamples,
			@Parameter(name="max depth", description="maximum recursion depth")
				int maxDepth)
	
	{
		this.data = data;
		this.samples = samples;
		this.epsilon = epsilon;
		this.sigThreshold = sigThreshold;
		this.stepSize = stepSize;
		this.plotSamples = plotSamples;
		this.maxDepth = maxDepth;
	}

	@Override
	protected void setup()
	{
		plot = Draw.draw(data, 500, true);
	}

	@Override
	protected void body()
	{		
		Distance<Point> metric = new EuclideanDistance();

		distances = MultiTakens.distances(data, metric);
		double max = distances.get(distances.size() - 1);
		
		instances = data.size();
		embeddingDimension = data.get(0).dimensionality();
		
		logger.info(instances + " instances of " + embeddingDimension + " features.");
		
		fits = MultiTakens.multifit(distances, true, maxDepth, epsilon, sigThreshold);

		Global.log().info("Finished fitting model.");				
						
		int gSamples = plotSamples == -1 ? distances.size() : plotSamples;
		genDistances = new ArrayList<Double>(gSamples);
		for(int i : series(gSamples))
		{
			Point a = data.get(Global.random.nextInt(data.size())),
			      b = data.get(Global.random.nextInt(data.size()));
			
			double distance = metric.distance(a, b);
			genDistances.add(distance);
		}
		
		Collections.sort(genDistances);
		
		generated = MultiTakens.generate(fits, genDistances, genDistances.size());
		
		Global.log().info("Calculating significance.");				
		// significance = takens.significance(Takens.distances(data, metric), epsilon, samples);
		
		Global.log().info("Correlation integral");
		cint = CorrelationIntegral.fromDataset(data, stepSize, max, metric);
	}
	
	@Result(name="data plot")
	public BufferedImage plot()
	{
		return plot;
	}

	@Result(name="fitted distributions")
	public List<List<Object>> fittedDistributions()
	{
		List<List<Object>> results = new ArrayList<List<Object>>(fits.size());
		
		for(MultiTakens distribution: fits)
		{
			List<Object> row = new ArrayList<Object>();
			row.add(distribution.minDistance());
			row.add(distribution.maxDistance());
			row.add(distribution.dimension());
			
			row.add(distribution.captures(distances));
			row.add(distribution.ksTest(distances, true));
			row.add(distribution.significance(distances, epsilon));
			
			results.add(row);
		}
		
		return results;
	}
	
	@Result(name="distances", description="Some distances sampled from the data.")
	public List<Double> distances()
	{
		return genDistances;
	}
	
	@Result(name="significance")
	public double significance()
	{
		return significance;
	}
	
	@Result(name="generated distances", description="Distances generated from the induced distribution.")
	public List<Double> generated()
	{
		return generated;
	}
	
	@Result(name="instances")
	public int instances()
	{
		return instances;
	}
	
	@Result(name="specific")
	public double specific()
	{
		MultiTakens multi = MultiTakens.fit(data, new EuclideanDistance()).fit(0.004, 0.328);
		
		return multi.dimension();
	}
	
	@Result(name="embedding dimension")
	public int embeddingDimension()
	{
		return embeddingDimension;
	}
	
	@Result(name="Correlation Integral", plot = Plot.SCATTER)
	public List<List<Double>> ci()
	{
		List<List<Double>> table = new ArrayList<List<Double>>();
		for(int i : series(cint.counts().size()))
			table.add(Arrays.asList(cint.distances().get(i), cint.counts().get(i)));
	
		return table;
	}
		
//	@Result(name="KS values")
//	public List<List<Double>> distances()
//	{
//		List<List<Double>> table = new ArrayList<List<Double>>();
//		for(Double distance : fit.unique(true))
//		{
//			Takens<Point> takens = fit.fit(distance);
//			table.add(Arrays.asList(distance, takens.dimension(), takens.ksTest(data) ));
//		}
//	
//		return table;
//	}
}
