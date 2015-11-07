package org.lilian.data.real.fractal.old;

import static org.lilian.util.Series.series;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.lilian.Global;
import org.lilian.data.real.AffineMap;
import org.lilian.data.real.Datasets;
import org.lilian.data.real.Map;
import org.lilian.data.real.Point;
import org.lilian.util.Series;

/**
 * A monte carlo approach to finding similarity transformations.
 * 
 * The idea is simple, sample a large number of transformations, chain random
 * ones together and estimate the distance between the set and the 
 * mapped set.
 * 
 * @author Peter
 *
 */
public class MCSimilarity
{
	private List<Point> data;
	private int dim;
	private Generator gen;
	
	private List<Transform> buffer = new ArrayList<Transform>();
	
	private int bufferSize = 100;
	private int runs = 100;
	private int trials = 100;
	private int sampleSize = 100;
	private int depth = 5;

	public MCSimilarity(List<Point> data, int bufferSize, int runs, int trials,
			int sampleSize, int depth)
	{
		super();
		this.data = data;
		this.bufferSize = bufferSize;
		this.runs = runs;
		this.trials = trials;
		this.sampleSize = sampleSize;
		this.depth = depth;
		
		gen = new Generator(data, 0.005);
	}

	public void step()
	{
		while(buffer.size() < bufferSize)
			buffer.add(new Transform(gen.generate()));
		
		List<Transform> t = new ArrayList<Transform>(depth);
		for(int i : series(runs))
		{
			// * Create a composite of $depth random transformations
			Map composite = null;
			t.clear();
			for(int j : series(depth))
			{
				Transform tr = buffer.get(Global.random.nextInt(buffer.size()));
				t.add(tr);
				composite = composite == null ? 
						tr.map() : composite.compose(tr.map());
			}
			
			// * Estimate the expected distance
			Point from, to;
			double distance = 0.0;
			for(int j : series(trials))
			{
				from = data.get(Global.random.nextInt(data.size()));
				to = composite.map(from);
				
				distance += Datasets.distance(to, data, sampleSize);
			}
			distance = distance / trials;
			
			// * Use estimate as a score
			for(Transform trans : t)
				trans.setScore(Math.min(trans.score(), distance));
		}
		
		Collections.sort(buffer);
		while(buffer.size() > bufferSize/2)
			buffer.remove(buffer.size()-1);
	}
	
	public void print(PrintStream out)
	{
		for(Transform tr : buffer)
			out.println(tr);
	}
	
	private class Transform implements Comparable<Transform>
	{
		private Map map;
		private double score = Double.POSITIVE_INFINITY;
			
		public Transform(Map map)
		{
			this.map = map;
		}

		public Map map()
		{
			return map;
		}

		public int compareTo(Transform other)
		{
			return Double.compare(score, other.score);
		}

		public double score()
		{
			return score;
		}

		public void setScore(double score)
		{
			this.score = score;
		}
		
		public String toString()
		{
			return score + "_" + map;
		}
	}
}
