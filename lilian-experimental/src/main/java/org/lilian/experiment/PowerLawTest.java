package org.lilian.experiment;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.lilian.util.statistics.ContinuousPowerLaw;

public class PowerLawTest extends AbstractExperiment
{

	private double exponent;
	private double cutoff;
	private int size;
	
	public @State ContinuousPowerLaw generator;
	public @State List<Double> values, sorted;
	
	
	public PowerLawTest(
			@Parameter(name="exponent") double exponent, 
			@Parameter(name="cutoff") double cutoff, 
			@Parameter(name="size") int size)
	{
		this.exponent = exponent;
		this.cutoff = cutoff;
		this.size = size;
		
		generator = new ContinuousPowerLaw(cutoff, exponent);
	}

	@Override
	protected void setup()
	{
	}

	@Override
	protected void body()
	{
		values = generator.generate(size);
		sorted = new ArrayList<Double>(values);
		Collections.sort(sorted, Collections.reverseOrder());
	}
	
	@Result(name="values")
	public List<Double> values()
	{
		return values;
	}

	@Result(name="sorted values")
	public List<Double> sorted()
	{
		return sorted;
	}
	
}
