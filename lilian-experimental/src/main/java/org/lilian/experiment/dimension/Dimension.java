package org.lilian.experiment.dimension;

import static org.lilian.util.Series.series;

import java.util.ArrayList;
import java.util.List;

import nl.peterbloem.powerlaws.Continuous;

import org.lilian.data.real.Point;
import org.lilian.experiment.AbstractExperiment;
import org.lilian.experiment.Parameter;
import org.lilian.experiment.Result;
import org.lilian.experiment.State;
import org.lilian.util.Series;
import org.lilian.util.distance.Distance;
import org.lilian.util.distance.EuclideanDistance;

public class Dimension extends AbstractExperiment {

	private List<Point> data;
	private Distance<Point> metric = new EuclideanDistance();

	public @State Continuous distribution;
	
	public Dimension(
			@Parameter(name="data") List<Point> data)
	{
		this.data = data;
	}

	@Override
	protected void setup() {
		// TODO Auto-generated method stub
		
	}

	@Override
	protected void body() 
	{
		int n = data.size();
		List<Double> distances = new ArrayList<Double>((n*n - n)/2);
		for(int i : series(data.size()))
			for(int j : series(i+1, data.size()))
				distances.add(metric.distance(data.get(i), data.get(j)));
				
		logger.info("number of distances: " + distances.size());
		
		distribution = Continuous.fit(distances).fit();
	}
	
	@Result(name="dimension")
	public double dimension()
	{
		return distribution.exponent();
	}
	
	@Result(name="min")
	public double min()
	{
		return distribution.xMin();
	}
}
