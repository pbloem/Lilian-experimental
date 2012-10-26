package org.lilian.experiment.dynamics;

import static org.lilian.util.Series.series;

import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

import org.lilian.data.real.Draw;
import org.lilian.data.real.Map;
import org.lilian.data.real.MappedList;
import org.lilian.data.real.Maps;
import org.lilian.data.real.PCA;
import org.lilian.data.real.Point;
import org.lilian.experiment.AbstractExperiment;
import org.lilian.experiment.Parameter;
import org.lilian.experiment.Result;
import org.lilian.experiment.State;
import org.lilian.util.Series;

public class Dynamics extends AbstractExperiment
{

	protected List<Point> data;
	protected int delay;

	public @State List<Point> delayed;
	public @State List<Point> reduced;
	
	public Dynamics(
		@Parameter(name="data") List<Point> data, 
		@Parameter(name="delay") int delay)
	{
		this.data = data;
		this.delay = delay;
	}

	@Override
	protected void body()
	{		
		delayed = new ArrayList<Point>(data.size()/delay + 1);

//		int i = 0;
//		while(i < data.size() - delay)
//		{
//			Point p = data.get(i);
//			for(int j : Series.series(1, delay))
//				p = cat(p, data.get(i + j));
//			delayed.add(p);
//			
//			i += delay;
//		}
		
		for(int i : series(data.size() - delay))
			delayed.add(cat(data.get(i), data.get(i+delay)));
		
		// PCA pca = new PCA(delayed);
		// reduced = pca.simplify(2);
	}
	
	@Result(name="data")
	public List<Point> data()
	{
		return data;
	}
	
	@Result(name="delayed")
	public List<Point> delayed()
	{
		return delayed;
	}
	
	@Result(name="size")
	public int size()
	{
		return data.size();
	}
	
	@Result(name="plot")
	public BufferedImage plot()
	{
		Map map = Maps.centered(delayed);
		return Draw.draw(new MappedList(delayed, map), 500, false);
	}

	@Result(name="trace")
	public BufferedImage trace()
	{
		return Draw.trace(delayed, 800, 0.8, true);
	}
	
	private Point cat(Point a, Point b)
	{
		
		Point p = new Point(a.size() + b.size());
		
		for(int i : series(a.size()))
			p.set(i,  a.get(i));
		for(int i : series(b.size()))
			p.set(i + a.size(),  b.get(i));
		
		return p;
	}
}
