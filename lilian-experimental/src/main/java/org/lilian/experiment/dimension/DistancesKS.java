package org.lilian.experiment.dimension;

import static org.lilian.util.Series.series;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.lilian.Global;
import org.lilian.data.dimension.Takens;
import org.lilian.data.real.Point;
import org.lilian.experiment.AbstractExperiment;
import org.lilian.experiment.Parameter;
import org.lilian.experiment.Result;
import org.lilian.experiment.State;
import org.lilian.statistics.KSStatistic;
import org.lilian.util.Series;
import org.lilian.util.distance.Distance;
import org.lilian.util.distance.EuclideanDistance;

public class DistancesKS extends AbstractExperiment
{
	private Distance<Point> distance = new EuclideanDistance();
	private List<Point> data;
	
	public @State List<Double> allPairs;
	public @State List<Double> consecutive;
	
	public @State double ks;
	
	public DistancesKS(
			@Parameter(name="data") List<Point> data)
	{
		this.data = data;
	}
	
	@Override
	protected void setup()
	{
	}

	@Override
	protected void body()
	{
		Collections.shuffle(data);
		
		int n = data.size();
		allPairs = new ArrayList<Double>((n*n - n)/2);
		for(int i : series(n))
			for(int j : series(i+1, n))
				allPairs.add(distance.distance(data.get(i), data.get(j)));
		
		consecutive = new ArrayList<Double>(n/2 + 1);
		for(int i : series(0, 2, n))
			if(i+1 < data.size())
				consecutive.add(distance.distance(data.get(i), data.get(i+1)));
		
		Collections.sort(consecutive);
		Collections.sort(allPairs);
		
		ks = KSStatistic.ks(consecutive, allPairs, true);
	}
	
	@Override
	protected void tearDown()
	{
		allPairs = null;
		consecutive = null;
	}
	
	@Result(name="consecutive")
	public List<Double> cons()
	{
		return consecutive;
	}
	
	@Result(name="all pairs")
	public List<Double> all()
	{
		return allPairs;
	}

	@Result(name="ks")
	public double ks()
	{
		return ks;
	}
}
