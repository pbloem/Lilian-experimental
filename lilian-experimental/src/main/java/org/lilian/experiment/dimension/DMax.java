package org.lilian.experiment.dimension;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.lilian.Global;
import org.lilian.data.dimension.Takens;
import org.lilian.data.real.MVN;
import org.lilian.data.real.Point;
import org.lilian.data.real.fractal.IFSs;
import org.lilian.experiment.AbstractExperiment;
import org.lilian.experiment.Parameter;
import org.lilian.experiment.Resources;
import org.lilian.experiment.Result;
import org.lilian.experiment.Tools;
import org.lilian.util.Series;
import org.lilian.util.distance.Distance;
import org.lilian.util.distance.EuclideanDistance;

public class DMax extends AbstractExperiment
{
	private Distance<Point> metric = new EuclideanDistance();
	
	private List<List<Point>> datasets;
	private List<Double> targets;
	private List<String> names;
	private List<Double> ks, e;
	private List<Integer> sizes; // = Arrays.asList(100, 200, 300, 400, 500);
	private List<Double> sizeColumn;

	public DMax(
			@Parameter(name="sizes") List<Integer> sizes)
	{
		this.sizes = sizes;
	}

	@Override
	protected void setup()
	{
		
		datasets = new ArrayList<List<Point>>();
		names = new ArrayList<String>();
		sizeColumn = new ArrayList<Double>();
		targets = new ArrayList<Double>();
		
		for(int n : sizes)
		{			
			
			datasets.add(Resources.sierpinski(n));
			names.add("sierpinski");
			targets.add(1.58496250072);
			sizeColumn.add((double)n);
			
			datasets.add(Resources.koch(n));
			names.add("koch");
			targets.add(1.26185950714);
			sizeColumn.add((double)n);
			
			datasets.add(new MVN(1).generate(n));
			names.add("mvn1");
			targets.add(1.0);
			sizeColumn.add((double)n);
			
			datasets.add(new MVN(2).generate(n));
			names.add("mvn2");
			targets.add(2.0);
			sizeColumn.add((double)n);
			
			datasets.add(new MVN(3).generate(n));
			names.add("mvn3");
			targets.add(3.0);
			sizeColumn.add((double)n);
			
			datasets.add(new MVN(5).generate(n));
			names.add("mvn5");
			targets.add(5.0);	
			sizeColumn.add((double)n);	
		
			datasets.add(new MVN(10).generate(n));
			names.add("mvn10");
			targets.add(10.0);
			sizeColumn.add((double)n);
			
		}
	}

	@Override
	protected void body()
	{
		ks = new ArrayList<Double>();
		e = new ArrayList<Double>();
		
		for(int i : Series.series(datasets.size()))
		{
			ks.add(Takens.fit(datasets.get(i), metric).fit().maxDistance());
			e.add(Takens.fit(datasets.get(i), metric).fitError(targets.get(i)).maxDistance());
			
			Global.log().info("Finished " + names.get(i));
		}
	}
	
	@Result(name="result", plot=Result.Plot.SCATTER)
	public List<List<Double>> results()
	{
		return Tools.combine(ks, e, sizeColumn);
	}

	@Result(name="names")
	public List<String> names()
	{
		return names;
	}
	
}
