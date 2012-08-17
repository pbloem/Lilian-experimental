package org.lilian.experiment.powerlaws;

import static org.lilian.util.Series.series;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import nl.peterbloem.powerlaws.Functions;

import org.lilian.Global;
import org.lilian.data.dimension.Takens;
import org.lilian.data.real.Point;
import org.lilian.experiment.AbstractExperiment;
import org.lilian.experiment.Parameter;
import org.lilian.experiment.Result;
import org.lilian.experiment.State;
import org.lilian.util.Series;
import org.lilian.util.distance.Distance;
import org.lilian.util.distance.EuclideanDistance;

public class Dimension extends AbstractExperiment
{
	private List<Point> data;
	private int samples;
	private double epsilon;
	
	public @State Takens.Fit fit;
	public @State Takens takens;
	
	
	public @State int instances;
	public @State int embeddingDimension;
	public @State List<Double> distances;
	public @State List<Double> generated;
	public @State double significance;

	public Dimension(
			@Parameter(name="data") 
				List<Point> data,
			@Parameter(name="samples", description="The number of times to sample for maxDistance values (use -1 to use all values)")
				int samples,
			@Parameter(name="epsilon", description="The accuracy required in the significance calculation.")
				double epsilon)
	{
		this.data = data;
		this.samples = samples;
		this.epsilon = epsilon;
	}

	@Override
	protected void setup()
	{
	}

	@Override
	protected void body()
	{
		instances = data.size();
		embeddingDimension = data.get(0).dimensionality();
		
		logger.info(instances + " instances of " + embeddingDimension + " features.");
		
		fit = Takens.fit(data, new EuclideanDistance());
		takens = 
				samples == -1 ? 
						fit.fit() :
						fit.fitSampled(samples);
						
		Global.log().info("Finished fitting model.");				
						
		int gSamples = samples == -1 ? data.size() : samples;
		distances = new ArrayList<Double>(gSamples);
		Distance<List<Double>> metric = new EuclideanDistance();
		for(int i : series(gSamples))
		{
			Point a = data.get(Global.random.nextInt(data.size())),
			      b = data.get(Global.random.nextInt(data.size()));
			
			double distance = metric.distance(a, b);
			distances.add(distance);
		}
		Collections.sort(distances);
		
		generated = takens.generate(gSamples);
		Collections.sort(generated);
		
		Global.log().info("Calculating significance.");				
		
		// significance = takens.significance(Takens.distances(data, metric), epsilon, samples);
	}

	@Result(name="dimension")
	public double dimension()
	{
		return takens.dimension();
	}
	
	@Result(name="max distance")
	public double maxDistance()
	{
		return takens.maxDistance();
	}
	
	@Result(name="distances", description="Some distances sampled from the data.")
	public List<Double> distances()
	{
		return distances;
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
	
	@Result(name="embedding dimension")
	public int embeddingDimension()
	{
		return embeddingDimension;
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
