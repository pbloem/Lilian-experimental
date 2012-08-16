package org.lilian.experiment.powerlaws;

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
	private Distance<List<Double>> distance = new EuclideanDistance();
	private List<Point> data;
	private double dimension;
	
	public @State List<Double> allPairs;
	public @State List<Double> consecutive;

	public @State List<Double> allPairsBootstrap;
	public @State List<Double> consecutiveBootstrap;	
	
	public @State List<Double> allPairsComp;
	public @State List<Double> consecutiveComp;
	
	public @State double ks;
	public @State double ksBootstrap;
	public @State double ksComp;
	public @State Takens comp;
	
	public DistancesKS(
			@Parameter(name="data") List<Point> data,
			@Parameter(name="dimension", description="An estimate of the intrinsic dimension to generate a comparison") double dimension)
	{
		this.data = data;
		this.dimension = dimension;
	}
	
	@Override
	protected void setup()
	{
		comp = new Takens(dimension, 1.0);
	}

	@Override
	protected void body()
	{
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
		
		
		// * Bootstrap 
		
		allPairsBootstrap = comp.generate(allPairs.size());
		consecutiveBootstrap = comp.generate(consecutive.size());
		
		for(int i : series(allPairs.size()))
			allPairsBootstrap.add(allPairs.get(Global.random.nextInt(allPairs.size())));
		for(int i : series(consecutive.size()))
			consecutiveBootstrap.add(consecutive.get(Global.random.nextInt(consecutive.size())));
		
		Collections.sort(consecutiveBootstrap);
		Collections.sort(allPairsBootstrap);
		
		ksBootstrap = KSStatistic.ks(consecutiveBootstrap, allPairsBootstrap, true);
		
		// * Comparison
		
		allPairsComp = comp.generate(allPairs.size());
		consecutiveComp = comp.generate(consecutive.size());
		
		Collections.sort(consecutiveComp);
		Collections.sort(allPairsComp);
		
		ksComp = KSStatistic.ks(consecutiveComp, allPairsComp, true);
		
	}
	
	
	
	@Override
	protected void tearDown()
	{
		allPairs = null;
		consecutive = null;

		allPairsBootstrap = null;
		consecutiveBootstrap = null;	
		
		allPairsComp = null;
		consecutiveComp = null;		

	}

	@Result(name="ks")
	public double ks()
	{
		return ks;
	}
	
	@Result(name="ks Bootstrap")
	public double ksBootstrap()
	{
		return ksBootstrap;
	}	
	
	@Result(name="ks Comp")
	public double ksComp()
	{
		return ksComp;
	}	

}
