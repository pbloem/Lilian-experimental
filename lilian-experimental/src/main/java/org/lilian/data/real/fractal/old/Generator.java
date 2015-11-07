package org.lilian.data.real.fractal.old;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.lilian.Global;
import org.lilian.data.real.Map;
import org.lilian.data.real.Maps;
import org.lilian.data.real.Point;
import org.lilian.data.real.Rotation;
import org.lilian.data.real.Similitude;
import org.lilian.util.Series;

public class Generator
{
	private List<Point> data; 
	private int dim;

	private double lambda;
	
	
	public Generator(List<Point> data, double lambda)
	{
		this.data = data;
		this.lambda = lambda;
		
		dim = data.get(0).size();
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
		
		List<Double> angles = Rotation.findAngles(a, b, 5000, 10);
		
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
