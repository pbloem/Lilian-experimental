package org.lilian.experiment.powerlaws;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import nl.peterbloem.powerlaws.Continuous;
import nl.peterbloem.powerlaws.Discrete;
import nl.peterbloem.util.Generator;

import org.lilian.experiment.AbstractExperiment;
import org.lilian.experiment.Parameter;
import org.lilian.experiment.Result;
import org.lilian.experiment.State;

public class PowerLawTest extends AbstractExperiment
{

	private double exponent;
	private double cutoff;
	private int size;
	
	public @State Generator<Integer> generator;
	public @State List<Integer> values, sorted;
	
	
	public PowerLawTest(
			@Parameter(name="exponent") double exponent, 
			@Parameter(name="cutoff") double cutoff, 
			@Parameter(name="size") int size)
	{
		this.exponent = exponent;
		this.cutoff = cutoff;
		this.size = size;
		
		generator = new Discrete((int)cutoff, exponent);
	}

	@Override
	protected void setup()
	{
	}

	@Override
	protected void body()
	{
		values = generator.generate(size);
		sorted = new ArrayList<Integer>(values);
		Collections.sort(sorted, Collections.reverseOrder());
	}
	
	@Result(name="values")
	public List<Integer> values()
	{
		return values;
	}

	@Result(name="sorted values")
	public List<Integer> sorted()
	{
		return sorted;
	}
	
}
