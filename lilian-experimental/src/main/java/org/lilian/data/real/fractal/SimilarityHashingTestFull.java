package org.lilian.data.real.fractal;

import static java.lang.Math.abs;
import static java.lang.Math.max;
import static java.lang.Math.min;
import static org.junit.Assert.*;
import static org.lilian.util.Series.series;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import javax.imageio.ImageIO;

import org.apache.commons.math.linear.RealVector;
import org.junit.Test;
import org.lilian.Global;
import org.lilian.data.real.AffineMap;
import org.lilian.data.real.Draw;
import org.lilian.data.real.Generator;
import org.lilian.data.real.MVN;
import org.lilian.data.real.Map;
import org.lilian.data.real.Maps;
import org.lilian.data.real.Point;
import org.lilian.data.real.Rotation;
import org.lilian.data.real.Similitude;
import org.lilian.util.Functions;
import org.lilian.util.Series;

public class SimilarityHashingTestFull
{
	// private static final int DATA_SIZE = 100000;
	private static final int SAMPLES = 5000;
	private static final double LAMBDA = 0.1;
	
	@Test
	public void testMod()
	{
		System.out.println(7 % 2.0 * Math.PI);
		System.out.println(-2.5 * Math.PI % 2.0 * Math.PI);
	}
	
	@Test
	public void test()
	{
		long seed = new Random().nextLong();
		Global.random = new Random(seed);
		System.out.println(seed);
		
		File dir = new File("/Users/Peter/Documents/PhD/simhash_full_4/");
		dir.mkdirs();
		
		Generator<Point> im = IFSs.sierpinski().generator();
		// Generator im = IFSs.cantorA().generator();
		// Generator im = new MVN(2);
		// Generator im = IFSs.random(2, 4, 0.55).generator();
		Generator<Similitude> gen = new SSGenerator(im.generate(SAMPLES));
		
		try
		{
			BufferedImage image = Draw.draw(
					im, SAMPLES, 
					new double[]{-1.0, 1.0}, new double[]{-1.0, 1.0},
					1000, 1000, true);
			ImageIO.write(image, "PNG", new File(dir, "generator.png"));
		} catch (IOException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		for(int i : series(100))
		{
			System.out.println(gen.generate());
		}
	}
	
	/**
	 * Convex combination of and b
	 * @author Peter
	 *
	 * 
	 * @param weight The weight of a
	 */
	public static List<Double> combine(List<Double> a, List<Double> b, double weight)
	{
		List<Double> result = new ArrayList<Double>(a.size());
		
		for(int i = 0; i < a.size(); i++)
			result.add(a.get(i) * weight + b.get(i) + (1.0 - weight));
		
		return result;
	}
	
	public static class SSGenerator implements Generator<Similitude>
	{		
		private List<Point> data ;
		private int dim;

		public SSGenerator(List<Point> data)
		{
			super();
			this.data = data;
			this.dim = data.get(0).dimensionality();
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
			return discretize(data.get(Global.random.nextInt(data.size())), LAMBDA);
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

		public List<Similitude> generate(int n)
		{
			List<Similitude> points = new ArrayList<Similitude>(n);
			for(int i : Series.series(n))
				points.add(generate());
			
			return points;			
		}
	}
}
