package org.lilian.experiment.dimension;

import static java.lang.Math.exp;
import static java.lang.Math.log;
import static java.lang.Math.pow;
import static org.lilian.experiment.Tools.combine;

import java.util.ArrayList;
import java.util.List;

import org.lilian.experiment.AbstractExperiment;
import org.lilian.experiment.Result;
import org.lilian.experiment.Tools;
import org.lilian.util.Series;

public class Plot extends AbstractExperiment
{
	private static final double ALPHA = 2.5, X_MIN = 5.0, X_MAX = 20; 
	
	private List<Double> xPower, xExpo, xCombined;
	private List<Double> powerlaw = new ArrayList<Double>();
	private List<Double> exponential = new ArrayList<Double>();
	private List<Double> combined = new ArrayList<Double>();
	
	@Override
	protected void setup()
	{

		xExpo = Series.series(5.0, 0.1, 20.0);
		xPower = Series.series(0.0001, 0.1, 5.0);
		xCombined = Series.series(0.01, 0.5, 100.0);
	}

	@Override
	protected void body()
	{
		for(double x : xPower)
			powerlaw.add(powerlaw(x));
		
		for(double x : xExpo)
			exponential.add(exponential(x));
		
		double dExpo = 1.0; // powerlaw(X_MIN) / (powerlaw(X_MIN) - exponential(X_MIN));
		double dPower = 1.0; // dExpo - 1;
		
		logger.info("dPower " + dPower + ", dExpo " + dExpo);
		
 
		
		for(double x : xCombined)
			combined.add(x > X_MIN ? dExpo * exponential(x) : dPower * powerlaw(x));
	}

	public double powerlaw(double x)
	{		
		return (1/x) * pow(x/X_MIN, ALPHA);
	}
	
	public double exponential(double x)
	{
		double c = expInt(X_MAX) - expInt(X_MIN);
		logger.info("c = " + c);
		return 1.0/c * (x * log(x) - x - 1/x);
	}
	
	public double expInt(double x)
	{
		return 0.5 * (x * x - 2.0) * log(x) - (3.0/4.0) * x * x;
	}
	
	@Result(name="powerlaw", plot=Result.Plot.SCATTER)
	public List<List<Double>> powerlaw()
	{
		return combine(xPower, powerlaw);
	}
	
	@Result(name="exponential", plot=Result.Plot.SCATTER)
	public List<List<Double>> exponential()
	{
		return combine(xExpo, exponential);
	}
	
	@Result(name="combined", plot=Result.Plot.SCATTER)
	public List<List<Double>> combined()
	{
		return combine(xCombined, combined);
	}
	
}
