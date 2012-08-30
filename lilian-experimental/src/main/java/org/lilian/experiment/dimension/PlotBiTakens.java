package org.lilian.experiment.dimension;

import static java.lang.Math.exp;
import static java.lang.Math.log;
import static java.lang.Math.pow;
import static org.lilian.experiment.Tools.combine;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.lilian.data.dimension.BiTakens;
import org.lilian.experiment.AbstractExperiment;
import org.lilian.experiment.Parameter;
import org.lilian.experiment.Result;
import org.lilian.experiment.State;
import org.lilian.experiment.Tools;
import org.lilian.util.Series;

public class PlotBiTakens extends AbstractExperiment
{
	private double d1, d2, split, max;
	
	private List<Double> xs, p, cdf, gen;
	
	public @State BiTakens bi;
	
	public PlotBiTakens(
			@Parameter(name="d1") double d1, 
			@Parameter(name="d2") double d2, 
			@Parameter(name="split") double split, 
			@Parameter(name="max") double max)
	{
		this.d1 = d1;
		this.d2 = d2;
		this.split = split;
		this.max = max;
	}

	@Override
	protected void setup()
	{
		xs = Series.series(-1.0, 0.01, max + 1.0);
		
		p = new ArrayList<Double>(xs.size());
		cdf = new ArrayList<Double>(xs.size());
		
		bi = new BiTakens(split, max, d1, d2);
	}

	@Override
	protected void body()
	{
		for(double x : xs)
			p.add(bi.p(x));
		
		for(double x : xs)
			cdf.add(bi.cdf(x));
		
		gen =  bi.generate(xs.size());
		Collections.sort(gen);
		
	}
	
	@Result(name="left prior")
	public double leftPrior()
	{
		return bi.leftPrior();
	}
	
	@Result(name="right prior")
	public double rightPrior()
	{
		return bi.rightPrior();
	}	
	
	@Result(name="split cdf (right)")
	public double splitCDF()
	{
		return bi.right().cdf(split);
	}	
	
	@Result(name="p", plot=Result.Plot.SCATTER)
	public List<List<Double>> p()
	{
		return combine(xs, p);
	}
	
	@Result(name="cdf", plot=Result.Plot.SCATTER)
	public List<List<Double>> cdf()
	{
		return combine(xs, cdf);
	}
	
	@Result(name="generated")
	public List<Double> generated()
	{
		return gen;
	}
	
}
