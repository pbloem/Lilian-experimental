package org.lilian.experiment.powerlaws;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.lilian.Global;
import org.lilian.data.dimension.Takens;
import org.lilian.data.real.Datasets;
import org.lilian.data.real.Point;
import org.lilian.experiment.AbstractExperiment;
import org.lilian.experiment.Parameter;
import org.lilian.experiment.Result;
import org.lilian.experiment.State;
import org.lilian.experiment.Tools;
import org.lilian.util.Functions;
import org.lilian.util.Series;
import org.lilian.util.distance.Distance;
import org.lilian.util.distance.EuclideanDistance;

public class DimensionSearch extends AbstractExperiment
{

	private Distance<Point> metric = new EuclideanDistance();
	private List<Point> data;
	private Takens dist;
	private int searches;
	private int sampleSize;
	
	public @State List<List<Double>> values;
	public @State List<List<Double>> stds;

	
	public DimensionSearch(
			@Parameter(name="data") List<Point> data,
			@Parameter(name="searches") int searches,
			@Parameter(name="sample size") int sampleSize)
	{
		this.data = data;
		this.searches = searches;
		this.sampleSize = sampleSize;
	}

	@Override
	protected void setup()
	{
		values = new ArrayList<List<Double>>();
		stds = new ArrayList<List<Double>>();

	}

	@Override
	protected void body()
	{
		
		Takens.Fit fit = Takens.fit(data, new EuclideanDistance());
		List<Double> distances = fit.distances();
		
		List<Double> candidates = new ArrayList<Double>(searches);
		for(int i : Series.series(searches))
		{
			List<Double> sample = Datasets.sample(distances, sampleSize);
			
			Takens dist = Takens.fit(sample, false).fit();
			candidates.add(dist.maxDistance());
		}
		
		Collections.sort(candidates);
		System.out.println(candidates);
		
		dist = fit.fit(candidates);
	}
	
	@Result(name="Result")
	public double result()
	{	
		return dist.dimension();
	}	
}
