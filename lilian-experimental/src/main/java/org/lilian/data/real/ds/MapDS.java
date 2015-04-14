package org.lilian.data.real.ds;

import static org.lilian.util.Series.series;

import java.util.Collection;

import org.lilian.data.real.AbstractDensity;
import org.lilian.data.real.AbstractGenerator;
import org.lilian.data.real.Density;
import org.lilian.data.real.MVN;
import org.lilian.data.real.Map;
import org.lilian.data.real.Point;
import org.lilian.util.Series;

public class MapDS extends AbstractGenerator<Point>
{
	private Map map;
	
	private Point generatorPoint;

	public MapDS(Map map)
	{
		super();
		this.map = map;
		
		generatorPoint = new MVN(map.dimension(), 0.01).generate();
		for(int i : series(50))
			generate();
	}
	
	public Point generate()
	{
		generatorPoint = map.map(generatorPoint);
		
		return generatorPoint;
	}
	
	public Density inverseEstimator(int iterations)
	{
		if(! map.invertible())
			throw new IllegalStateException("Map must be invertible to use this estimator.");
		
		return new InverseEstimator(iterations);
	}
	
	private class InverseEstimator extends AbstractDensity 
	{
		int iterations;
		Map inverse = map.inverse();
		MVN base;
		
		public InverseEstimator(int iterations)
		{
			this.iterations = iterations;
			
			base = new MVN(map.dimension(), (double) (iterations*iterations));
		}

		@Override
		public double density(Point p)
		{
			for(int i : series(iterations))
				p = inverse.map(p);
				
			return base.density(p);
		}

		@Override
		public int dimension()
		{
			return inverse.dimension();
		}
		
		
	}
	
	
	public Density densityEstimator(int iterations, double var)
	{
		return new MapDensity(iterations, var);
	}
	
	private class MapDensity extends AbstractDensity
	{
		int iterations;
		double var;
		
		MVN base;
		
		public MapDensity(int iterations, double var)
		{
			this.iterations = iterations;
			this.var = var;
			
			base = new MVN(dimension(), var);
		}

		@Override
		public double density(Point p)
		{
			double density = 0.0;
			for(int i : series(iterations))
			{
				Point center = generate();
				Point transpose = new Point(p.getVector().subtract(center.getBackingData())); 
				
				density += base.density(transpose);
			}
			
			return density / iterations;
		}

		@Override
		public int dimension()
		{
			return map.dimension();
		}
	}
}
