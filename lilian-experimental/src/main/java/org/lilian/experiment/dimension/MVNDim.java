package org.lilian.experiment.dimension;

import java.util.List;

import org.lilian.data.dimension.Takens;
import org.lilian.data.real.MVN;
import org.lilian.data.real.Point;
import org.lilian.experiment.AbstractExperiment;
import org.lilian.experiment.Parameter;
import org.lilian.experiment.Result;
import org.lilian.experiment.State;
import org.lilian.util.distance.Distance;
import org.lilian.util.distance.EuclideanDistance;

public class MVNDim extends AbstractExperiment
{
	Distance<Point> metric = new EuclideanDistance(); 
	
	private int dim;
	private int size, candidates, perCandidate, ksSamples;
	
	public @State double dimension;
	
	public MVNDim(
			@Parameter(name="dim") int dim, 
			@Parameter(name="size") int size,
			@Parameter(name="candidates") int candidates,
			@Parameter(name="per candidate") int perCandidate,
			@Parameter(name="ks samples") int ksSamples)
	{
		super();
		this.dim = dim;
		this.size = size;
		this.ksSamples = ksSamples;
		
		this.candidates = candidates;
		this.perCandidate = perCandidate;
	}

	@Override
	protected void setup()
	{
		// TODO Auto-generated method stub
		
	}

	@Override
	protected void body()
	{
		List<Point> data = new MVN(dim).generate(size);
		
		dimension = Takens.bigFit(data, metric).fit(candidates, perCandidate, ksSamples).dimension();
	}
	
	@Result(name="dimension")
	public double dimension()
	{
		return dimension;
	}
	
	@Result(name="error")
	public double error()
	{
		return Math.abs((double) dim - dimension);
	}

}
