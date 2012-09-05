package org.lilian.experiment.dimension;

import java.util.ArrayList;
import java.util.List;

import org.lilian.data.dimension.Takens;
import org.lilian.data.real.MVN;
import org.lilian.data.real.Point;
import org.lilian.experiment.AbstractExperiment;
import org.lilian.experiment.Parameter;
import org.lilian.experiment.Result;
import org.lilian.experiment.State;
import org.lilian.experiment.Tools;
import org.lilian.util.Series;
import org.lilian.util.distance.Distance;
import org.lilian.util.distance.EuclideanDistance;

public class MVNDim extends AbstractExperiment
{
	public static final double MAX = 8.0;
	
	Distance<Point> metric = new EuclideanDistance(); 
	
	
	private int dim;
	private int size, numCandidates, perCandidate, ksSamples;
	
	public @State double dimension;
	public @State List<Double> candidates, errors, ksValues;
	
	public MVNDim(
			@Parameter(name="dim") int dim, 
			@Parameter(name="size") int size,
			@Parameter(name="candidates") int numCandidates,
			@Parameter(name="per candidate") int perCandidate,
			@Parameter(name="ks samples") int ksSamples)
	{
		super();
		this.dim = dim;
		this.size = size;
		this.ksSamples = ksSamples;
		
		this.numCandidates = numCandidates;
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
		
		Takens.BigFit<Point> bigFit = Takens.bigFit(data, metric);
		// candidates = bigFit.candidates(numCandidates, perCandidate);
		candidates = Series.series(MAX/numCandidates, MAX/numCandidates, MAX);
		
		errors = new ArrayList<Double>(candidates.size());
		ksValues = new ArrayList<Double>(candidates.size());

		for(double candidate : candidates)
		{
			Takens takens = bigFit.fit(candidate);
			errors.add((double) dim - takens.dimension());
			ksValues.add(takens.ksTest(bigFit.sample(ksSamples), true));
		}
		
		
	}
	
	@Result(name="errors")
	public List<List<Double>> errors()
	{
		return Tools.combine(candidates, errors, ksValues );
	}

}
