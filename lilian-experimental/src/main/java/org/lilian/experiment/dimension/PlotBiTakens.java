package org.lilian.experiment.dimension;

import static java.lang.Math.exp;
import static java.lang.Math.log;
import static java.lang.Math.pow;
import static org.lilian.experiment.Tools.combine;
import static org.lilian.util.Series.series;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.lilian.Global;
import org.lilian.data.dimension.BiTakens;
import org.lilian.data.dimension.CorrelationIntegral;
import org.lilian.experiment.AbstractExperiment;
import org.lilian.experiment.Parameter;
import org.lilian.experiment.Result;
import org.lilian.experiment.State;
import org.lilian.experiment.Tools;
import org.lilian.experiment.Result.Plot;
import org.lilian.util.Series;

public class PlotBiTakens extends AbstractExperiment
{
	private double d1, d2, split, max;
	
	private List<Double> xs, p, cdf, gen;
	
	public @State BiTakens bi;
	public @State CorrelationIntegral cint;
	
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
		
		Global.log().info("Correlation integral");
		cint = CorrelationIntegral.fromDistances(bi.generate(1000000), 0.001, Tools.max(gen));
		
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
	
	@Result(name="Correlation Integral", plot = Plot.SCATTER)
	public List<List<Double>> ci()
	{
		List<List<Double>> table = new ArrayList<List<Double>>();
		for(int i : series(cint.counts().size()))
			table.add(Arrays.asList(cint.distances().get(i), cint.counts().get(i)));
	
		return table;
	}
	
	@Result(name="generated")
	public List<Double> generated()
	{
		return gen;
	}
	
}
