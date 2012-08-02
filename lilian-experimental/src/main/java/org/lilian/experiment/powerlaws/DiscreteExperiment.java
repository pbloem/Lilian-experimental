package org.lilian.experiment.powerlaws;

import static org.lilian.util.Series.series;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import nl.peterbloem.powerlaws.Continuous;
import nl.peterbloem.powerlaws.Discrete;
import nl.peterbloem.powerlaws.Functions;
import nl.peterbloem.powerlaws.PowerLaw;

import org.lilian.Global;
import org.lilian.data.real.Point;
import org.lilian.experiment.AbstractExperiment;
import org.lilian.experiment.Parameter;
import org.lilian.experiment.Result;
import org.lilian.experiment.State;
import org.lilian.experiment.Tools;
import org.lilian.util.Series;

public class DiscreteExperiment extends AbstractExperiment
{	
	private List<Integer> data;
	private Integer n;
	private Double epsilon;
	private int bootstrapSize;
	
	public @State PowerLaw<Integer> cpl;
	public @State PowerLaw.Fit<Integer, Discrete> fit;
	public @State double significance;
	public @State double mean, stdDev, max;
	
	public @State int nTail;
	public @State double nTailUncertainty;
	public @State double xMinUncertainty;
	public @State double exponentUncertainty;

	
	public DiscreteExperiment(
			@Parameter(name="data") List<Point> dataPoints, 
			@Parameter(name="n") int n)
	{
		data = new ArrayList<Integer>(dataPoints.size());
		for(Point point : dataPoints)
			data.add(point.get(0).intValue());
			
		this.n = n;	}
	
	public DiscreteExperiment(
			@Parameter(name="data") List<Point> dataPoints, 
			@Parameter(name="epsilon") double epsilon,
			@Parameter(name="bootstrap size") int bootstrapSize)
	{
		data = new ArrayList<Integer>(dataPoints.size());
		for(Point point : dataPoints)
			data.add(point.get(0).intValue());
		
		this.epsilon = epsilon;
		this.bootstrapSize = bootstrapSize;
	}

	@Override
	protected void setup()
	{

	}

	@Override
	protected void body()
	{
		mean = Tools.mean(data);
		stdDev = Tools.standardDeviation(data);
		max = Double.NEGATIVE_INFINITY;
		for(double datum : data)
			max = Math.max(max,  datum);
		
		logger.info("Start fitting power law.");
		fit = Discrete.fit(data);
		cpl = fit.fit();

		logger.info("Start bootstrap.");
		
		List<Double> exponents = new ArrayList<Double>(bootstrapSize);
		List<Double> ntails = new ArrayList<Double>(bootstrapSize);
		List<Double> xMins = new ArrayList<Double>(bootstrapSize);
		
		List<Integer> bData = new ArrayList<Integer>(bootstrapSize);
		for(int i : series(bootstrapSize))
		{
			// * Draw data randomly
			bData.clear();
			for(int j : series(data.size()))
				bData.add(data.get(Global.random.nextInt(data.size())));
				
			// * fit model
			PowerLaw<Integer> cpl = Discrete.fit(bData).fit();
			
			// * extract parameters
			exponents.add(cpl.exponent());
			xMins.add((double)cpl.xMin());
			
			int n = 0;
			for(double d : bData)
				if(d >= cpl.xMin()) n++;
			
			ntails.add((double)n);
		}
		
		nTailUncertainty = Tools.standardDeviation(ntails);
		xMinUncertainty = Tools.standardDeviation(xMins);
		exponentUncertainty = Tools.standardDeviation(exponents);
		
		logger.info("Start estimating significance.");
		
		significance = -1.0;
		significance = n == null ?
				cpl.significance(data, epsilon) :
				cpl.significance(data, n);
		
		logger.info("Finished estimating significance.");
	}

	@Result(name="exponent")
	public double exponent()
	{
		return cpl.exponent();
	}
	
	@Result(name="x min")
	public double xMin()
	{
		return cpl.xMin();
	}
	
	@Result(name="significance")
	public double significance()
	{
		return significance;
	}
	
	@Result(name="mean")
	public double mean()
	{
		return mean;
	}
	
	@Result(name="std dev")
	public double stdDev()
	{
		return stdDev;
	}
	
	@Result(name="max")
	public double max()
	{
		return max;
	}
	
	@Result(name="size")
	public double size()
	{
		return data.size();
	}
	
	@Result(name="specific")
	public double specific()
	{
		return Discrete.fit(data).fit(12).exponent();
	}
	
	@Result(name="distances")
	public List<List<Number>> distances()
	{
		List<List<Number>> table = new ArrayList<List<Number>>();
		for(Integer datum : Functions.unique(data))
			table.add(Arrays.asList((Number)datum, fit.fit(datum).ksTest(data) ));
	
		return table;
	}
	
	@Result(name="exponents")
	public List<List<Number>> exponents()
	{
		List<List<Number>> table = new ArrayList<List<Number>>();
		for(Integer datum : Functions.unique(data))
			table.add(Arrays.asList((Number)datum, fit.fit(datum).exponent() ));
	
		return table;
	}
	
	@Result(name="points in tail")
	public int tailSize()
	{
		int n = 0;
		for(double d : data)
			if(d >= xMin()) n++;
		
		return n;
	}
	
	@Result(name="Standard error for exponent")
	public String standardError()
	{
//		double correction = 1.0/data.size();
//		double sigma = (exponent() - 1.0)/Math.sqrt(data.size());
//		
//		return sigma + " + (0, " + correction + ")";
		
		return "TODO";
	}
	
	@Result(name="exponent uncertainty")
	public double exponentUncertainty()
	{
		return exponentUncertainty;
	}	
	
	@Result(name="xMin uncertainty")
	public double xMinUncertainty()
	{
		return xMinUncertainty;
	}
	
	@Result(name="nTail uncertainty")
	public double nTailUncertainty()
	{
		return nTailUncertainty;
	}	
}
