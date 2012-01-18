package org.lilian.data.real.fractal;

import static org.junit.Assert.*;

import java.util.List;

import org.apache.commons.math.linear.RealVector;
import org.junit.Test;
import org.lilian.data.real.Point;
import org.lilian.data.real.Similitude;

public class Similarity
{

	@Test
	public void test()
	{
		fail("Not yet implemented");
	}

	private class Candidate {
		double weight = 1.0;
		Similitude map;
		
		
	}
	
//	
//	/**
//	 * Finds the similitude that maps line segment a to line segment b
//	 * 
//	 * The similitude always aligns points a0 and b0 then rotates, then scales.
//	 * @param a0
//	 * @param a1
//	 * @param b
//	 * @return
//	 */
//	public static Similitude findSimilitude(Point a0, Point a1, Point b0, Point b1)
//	{
//	}
//	
//	public static double findScale(Point a0, Point a1, Point b0, Point b1)
//	{
//		RealVector a = a1.getVector().subtract(a0.getVector());
//		RealVector b = b1.getVector().subtract(b0.getVector());
//		
//		double la = a.getNorm(), lb = b.getNorm();
//		
//		return Math.min(la/lb, lb/la);
//	}
//	
//	public static List<Double> findTranslation(Point a, Point b)
//	{
//	}
//	
//	public static List<Double> findAngles()
//	{
//	}
//	
	
}
