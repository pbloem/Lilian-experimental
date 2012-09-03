package org.lilian.experiment.dimension;

import static org.lilian.util.Series.series;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.lilian.Global;
import org.lilian.data.dimension.Takens;
import org.lilian.data.real.Generator;
import org.lilian.data.real.Point;
import org.lilian.experiment.AbstractExperiment;
import org.lilian.experiment.Parameter;
import org.lilian.experiment.Result;
import org.lilian.experiment.State;
import org.lilian.statistics.KSStatistic;
import org.lilian.util.Series;
import org.lilian.util.distance.Distance;
import org.lilian.util.distance.EuclideanDistance;

public class GeneratedKS extends AbstractExperiment
{
	private Distance<Point> distance = new EuclideanDistance();
	private Generator<Point> generator;
	private int size;
	
	public @State List<Double> allPairs;
	public @State List<Double> truePairs;
	public @State List<Double> consecutive1, consecutive2;
	
	public @State double ks, ksTrue;
	
	public GeneratedKS(
			@Parameter(name="generator") Generator<Point> generator,
			@Parameter(name="size") int size)
	{
		this.generator = generator;
		this.size = size;
	}
	
	@Override
	protected void setup()
	{
	}

	@Override
	protected void body()
	{
		allPairs = Takens.distances(generator.generate(size), new EuclideanDistance());
		
		truePairs = new ArrayList<Double>((size*size - size)/2);
		for(int i : series((size*size-size)/2))
				truePairs.add(distance.distance(generator.generate(), generator.generate()));
		
		consecutive1 = new ArrayList<Double>(size/2 + 1);
		consecutive2 = new ArrayList<Double>(size/2 + 1);
		for(int i : series(0, 2, size))
			consecutive1.add(distance.distance(generator.generate(), generator.generate()));
		for(int i : series(0, 2, size))
			consecutive2.add(distance.distance(generator.generate(), generator.generate()));
		
		Collections.sort(consecutive1);
		Collections.sort(consecutive2);	
		Collections.sort(truePairs);
		
		ks = KSStatistic.ks(consecutive1, allPairs, true);
		ksTrue = KSStatistic.ks(consecutive2, truePairs, true);

	}
	
	@Override
	protected void tearDown()
	{
		truePairs = null;
		allPairs = null;
		consecutive1 = null;
		consecutive2 = null;
	}

	@Result(name="ks")
	public double ks()
	{
		return ks;
	}
	
	@Result(name="ks true")
	public double ksTrue()
	{
		return ksTrue;
	}
}
