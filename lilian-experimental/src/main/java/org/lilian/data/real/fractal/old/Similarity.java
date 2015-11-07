package org.lilian.data.real.fractal.old;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.lilian.Global;
import org.lilian.data.real.Generator;
import org.lilian.data.real.Map;
import org.lilian.data.real.Maps;
import org.lilian.data.real.Point;
import org.lilian.data.real.Rotation;
import org.lilian.data.real.Similitude;
import org.lilian.util.Series;

public class Similarity
{
	private static double VAR = 2.0;
	
	private List<Point> data; 
	private List<Candidate> candidates = new ArrayList<Candidate>();
	private int maxN;
	private int dim;
	private int samples;
	private double margin;
	private int maxCandidates;
	private double lambda;
	
	private int numBoosts = 0;
	
	
	
	public Similarity(List<Point> data, int maxN, int samples, double margin,
			int maxCandidates, double lambda)
	{
		this.data = data;
		this.maxN = maxN;
		this.samples = samples;
		this.margin = margin;
		this.maxCandidates = maxCandidates;
		this.lambda = lambda;
		
		dim = data.get(0).size();
	}

	public void step()
	{
	
		Map map = generate();
		List<Map> powers = powers(map, maxN);
		
		boolean boosted = false;
		for(Candidate candidate : candidates)
		{
			for(int i = 1; i <= maxN; i++)
				if(!boosted && equals(candidate.power(i), map, samples, margin))
				{
					candidate.boost();
					boosted = true;
				}
			
			for(int i = 1; i <= maxN; i++)
				if(!boosted && equals(candidate.map(), powers.get(i), samples, margin))
				{
					candidate.setMap(map);
					candidate.boost();
					boosted = true;
				}
		}

		// *
		if(!boosted)
			candidates.add(new Candidate(map));
		
		if(candidates.size() > maxCandidates)
		{
			if(numBoosts > 0)
				Collections.sort(candidates);
			else
				Collections.shuffle(candidates);
				
			candidates = new ArrayList<Candidate>(candidates.subList(0, maxCandidates/2));
		}			

	}
	
	private static List<Map> powers(Map map, int max)
	{
		List<Map >powers = new ArrayList<Map>(max);
		
		powers.add(map);
		Map last = map;
		
		for(int i = 0; i < max; i++)
			powers.add(last = last.compose(map));
		
		return powers;
	}
	
	public List<Map> maps()
	{
		List<Map> maps = new ArrayList<Map>(candidates.size());
		for(Candidate candidate : candidates)
			maps.add(candidate.map());
		
		return maps;
	}
	
	public int boosted()
	{
		return numBoosts;
	}
	
//	public List<IFS<Similitude>> compose()
//	{
//		
//	}
	
	/**
	 * Tests whether these maps are functionally equal
	 * 
	 * @param first
	 * @param second
	 * @param margin
	 * @return
	 */
	public static boolean equals(Map first, Map second, int samples, double margin)
	{
		List<Point> in = new ArrayList<Point>(samples);
		for(int i = 0; i < samples; i++)
			in.add(Point.random(first.dimension(), VAR));
			
		List<Point> outFirst = first.map(in);
		List<Point> outSecond = second.map(in);
		
		for(int i = 0; i < samples; i++)
			for(int j = 0; j < first.dimension(); j++)
				if(Math.abs(outFirst.get(i).get(j) - outSecond.get(i).get(j)) > margin)
					return false;
		
		return true;
	}
	
	
	private class Candidate implements Comparable<Candidate>
	{
		double weight = 1.0;
		List<Map> powers;
	
		public Candidate(Map map)
		{
			setMap(map);
		}

		public void boost()
		{
			weight++;
			numBoosts++;
		}
		
		public Map map()
		{
			return powers.get(0);
		}
		
		public Map power(int n)
		{
			return powers.get(n-1);
		}
		
		public void setMap(Map map)
		{
			powers = powers(map, maxN);
		}

		public int compareTo(Candidate other)
		{
			return Double.compare(this.weight, other.weight);
		}
	}
	
	public List<Point> points(int n)
	{
		List<Point> points = new ArrayList<Point>(n);
		
		for(int i = 0; i < n; i++)
			points.add(point());
		
		return points;
	}
	
	public Point point()
	{
		return discretize(data.get(Global.random.nextInt(data.size())), lambda);
	}
	
	public double generateScale()
	{
		Point[] a = new Point[]{point(), point()};
		Point[] b = new Point[]{point(), point()};
		
		double al = a[1].getVector().subtract(a[0].getVector()).getNorm();
		double bl = b[1].getVector().subtract(b[0].getVector()).getNorm();
		
		return Math.min(al/bl, bl/al);
	}
	
	public List<Double> generateTrans()
	{
		Point a = point();
		Point b = point();
		
		List<Double> t = new ArrayList<Double>(a.size());
		for(int i = 0; i < a.size(); i++)
			t.add(Math.abs(a.get(i) - b.get(i)));
		
		return t;
	}	
	
	public List<Double> generateAngles()
	{			
		List<Point> a = points(dim);
		List<Point> b = points(dim);
		
		List<Double> angles = Rotation.findAngles(a, b, 2000, 1);
		
		return angles;
	}
	
	public Similitude generate()
	{
		return new Similitude(
				generateScale(),
				generateTrans(),
				generateAngles());
	}
	
	public Point[] discretize(Point[] in, double lambda) 
	{
		return new Point[] {
			discretize(in[0], lambda),
			discretize(in[1], lambda)};
	}
	
	public Point discretize(Point in, double lambda) 
	{
		return new Point(
			discretize(in.get(0), lambda),
			discretize(in.get(1), lambda));
	}
	
	public double discretize(double in, double lambda)
	{
		return in - in % lambda;
	}		
}
