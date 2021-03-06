package org.lilian.data.real.fractal.old;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.apache.commons.math.linear.RealVector;
import org.junit.Test;
import org.lilian.Global;
import org.lilian.data.real.AffineMap;
import org.lilian.data.real.Map;
import org.lilian.data.real.Maps;
import org.lilian.data.real.Point;
import org.lilian.data.real.Similitude;
import org.lilian.util.MatrixTools;

public class SimilarityOld
{
	private static double VAR = 5.0;
	private int maxCandidates = 100;
	private int maxN = 5;
	
	private int samples = 10;
	private double margin = 0.05;
	
	private List<Candidate> candidates = new ArrayList<Candidate>(maxCandidates + 1);
	private List<Point> data;
	
	private int numBoosts = 0;
	
	public SimilarityOld(List<Point> data, double margin, int maxN)
	{
		this.data = data;
		this.margin = margin;
		this.maxN = maxN;
	}
	
	public void step()
	{
		// * Draw a new map
		Point a0 = data.get(Global.random.nextInt(data.size())),
		      a1 = data.get(Global.random.nextInt(data.size())),
		      b0 = data.get(Global.random.nextInt(data.size())),
		      b1 = data.get(Global.random.nextInt(data.size()));  
		
		Map map = Maps.findSimilitude(
				Arrays.asList(a0, a1),
				Arrays.asList(b0, b1));
		
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
	
	public static List<Map> powers(Map map, int max)
	{
		List<Map >powers = new ArrayList<Map>(max);
		
		powers.add(map);
		Map last = map;
		
		for(int i = 0; i < max; i++)
			powers.add(last = last.compose(map));
		
		return powers;
	}
	
	public int boosted()
	{
		return numBoosts;
	}
	
	public List<Map> maps()
	{
		List<Map> maps = new ArrayList<Map>(candidates.size());
		for(Candidate candidate : candidates)
			maps.add(candidate.map());
		
		return maps;
	}
	
	/**
	 * Finds the similitude that maps line segment a to line segment b
	 * 
	 * The similitude always aligns points a0 and b0 then rotates, then scales.
	 * @param a0
	 * @param a1
	 * @param b
	 * @return
	 */
	public static Similitude findSimilitude(Point a0, Point a1, Point b0, Point b1)
	{
		return new Similitude(
				findScale(a0, a1, b0, b1),
				findTranslation(a0, b0),
				findAngles(a0, a1, b0, b1)
			);
		
	}
	
	public static double findScale(Point a0, Point a1, Point b0, Point b1)
	{
		RealVector a = a1.getVector().subtract(a0.getVector());
		RealVector b = b1.getVector().subtract(b0.getVector());
		
		double la = a.getNorm(), lb = b.getNorm();
		
		return Math.min(la/lb, lb/la);
	}
	
	public static List<Double> findTranslation(Point a, Point b)
	{
		RealVector t = a.getVector().subtract(b.getVector());
		
		ArrayList<Double> list = new ArrayList<Double>(t.getDimension());
		for(int i = 0; i < t.getDimension(); i++)
			list.add(t.getEntry(i));
		
		return list;
	}
	
	public static List<Double> findAngles(Point a0, Point a1, Point b0, Point b1)
	{

		int dim = a0.size();
		List<Double> angles	= new ArrayList<Double>((dim*dim-dim)/2);
		
		for(int i = 0; i < dim-1; i++)
			for(int j = i+1; j < dim; j++)
			{
				double[] a = new double[]{a1.get(i) - a0.get(i), a1.get(j) - a0.get(j)};
				double[] b = new double[]{b1.get(i) - b0.get(i), b1.get(j) - b0.get(j)};
			
				// product of magnitudes
				double mag = Math.sqrt((a[0]*a[0] + a[1]*a[1]) * (b[0]*b[0] + b[1]*b[1]));
				
				// dot product (= product of magnitudes * cos(angle))
				double dot = a[0] * b[0] + a[1] * b[1];
				angles.add( mag == 0.0 ? 0.0 : Math.acos(dot/mag) );
			}
		
		return angles;	
	}
}
