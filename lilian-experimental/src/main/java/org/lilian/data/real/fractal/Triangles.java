package org.lilian.data.real.fractal;

import static java.lang.Math.abs;
import static org.junit.Assert.*;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.AbstractList;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import javax.imageio.ImageIO;

import org.junit.Test;
import org.lilian.Global;
import org.lilian.data.real.Draw;
import org.lilian.data.real.Maps;
import org.lilian.data.real.Point;
import org.lilian.data.real.Rotation;
import org.lilian.util.Functions;
import org.lilian.util.Series;
import org.lilian.util.graphs.Graph;

public class Triangles
{
	private static final int ITS = 100000;
	private static final int DATA_SIZE = 1000;
	private static final double LAMBDA = 0.1;
	
	private static final int TOP_SIZE = 10000;
		
	private List<Point> data;

	// @Test
	public void test()
	{
		long seed = new Random().nextLong();
		Global.random = new Random(seed);
		System.out.println(seed);
		
		data = IFSs.sierpinski().generator().generate(DATA_SIZE);
		
		File dir = new File("/Users/Peter/Documents/PhD/output/triangles/");
		dir.mkdirs();
		
		List<Maps.MapResult> top = new ArrayList<Maps.MapResult>(TOP_SIZE * 2);
		
		for(int i : Series.series(ITS))
		{
			// * pick two random triangles
			Point[] triangleA = new Point[] {
					data.get(Global.random.nextInt(DATA_SIZE)),
					data.get(Global.random.nextInt(DATA_SIZE)),
					data.get(Global.random.nextInt(DATA_SIZE))
				};
			
			Point[] triangleB = new Point[] {
					data.get(Global.random.nextInt(DATA_SIZE)),
					data.get(Global.random.nextInt(DATA_SIZE)),
					data.get(Global.random.nextInt(DATA_SIZE))
				};
			
			triangleA = discretize(triangleA, LAMBDA);
			triangleB = discretize(triangleB, LAMBDA);
			
			// * Find which one has the smaller surface area
			double surfA = surface(triangleA), surfB = surface(triangleB);
			Point[] big, small;
			
			if(surfA > surfB)
			{
				big = triangleA;
				small = triangleB;
			} else {
				big = triangleB;
				small = triangleA;
			}
			
			// * Find the optimal sim transform by trying all three permutations
			//   and picking the one with the lowest error.
						
			Point[][] perms = new Point[][]{
				new Point[]{big[0], big[1], big[2]},
				new Point[]{big[0], big[2], big[1]},
				new Point[]{big[1], big[0], big[2]},
				new Point[]{big[1], big[2], big[0]},
				new Point[]{big[2], big[0], big[1]},
				new Point[]{big[2], big[1], big[0]}
			};
			
			Maps.MapResult best = null;
			
			for(Point[] b : perms)
			{
				Maps.MapResult result = Maps.findMapResult(Arrays.asList(b), Arrays.asList(small));
				
				if(best == null || best.error() > result.error())
					best = result;
			}
			
			top.add(best);
			
			if(top.size() > TOP_SIZE * 2)
			{
				Collections.sort(top);
				while(top.size() > TOP_SIZE)
					top.remove(top.size()-1);
			}
		}
		
		System.out.println("Finished generating triangles");
		
		Collections.sort(top);
		
//		for(Maps.MapResult res : top)
//			System.out.println(res.error() + "| " + res.scale() + " " + res.translation() + " " + res.rotation());

		List<Point> out = new ArrayList<Point>();
		
		for(Maps.MapResult res : top)
			out.add(new Point(
					abs(res.scale()),
					abs(res.similitude(2000, 1).angles().get(0)/Math.PI)
				));
		
		try
		{
			BufferedImage image = Draw.draw(out, new double[]{0.0, 1.0}, new double[]{0.0, 1.0}, 200, false);
			ImageIO.write(image, "PNG", new File(dir, "density.png"));
		} catch (IOException e)
		{
			e.printStackTrace();
		}		
	}
	
	@Test
	public void testLocality()
	{
		long seed = new Random().nextLong();
		Global.random = new Random(seed);
		System.out.println(seed);
		
		data = IFSs.sierpinski().generator().generate(10000);
		
		File dir = new File("/Users/Peter/Documents/PhD/output/triangles2/");
		dir.mkdirs();
		
		Locality loc = new Locality(data, 25);

		System.out.println("Created Locality");
		
		List<Maps.MapResult> top = new ArrayList<Maps.MapResult>(TOP_SIZE * 2);
		
		BufferedImage image = Draw.draw(data, 1000, false);
		
		Graphics2D g = image.createGraphics();

		g.setStroke(new BasicStroke(2.0f));	
		
		for(int i : Series.series(ITS))
		{
			// * pick two random triangles
			List<Point> small = discretize(loc.grown(5), LAMBDA);
			List<Point> big = discretize(loc.grown(20), LAMBDA);
			
			if(Global.random.nextDouble() < 0.005)
			{
				g.setColor(Color.BLUE);
				
				line(g, small.get(0), small.get(1));
				line(g, small.get(1), small.get(2));
				line(g, small.get(2), small.get(0));
							
				g.setColor(Color.RED);
				
				line(g, big.get(0), big.get(1));
				line(g, big.get(1), big.get(2));
				line(g, big.get(2), big.get(0));
			}
				
			if(i % 5000 == 0)
			{
				try
				{
					ImageIO.write(image, "PNG", new File(dir, "triangles.png"));
				} catch (IOException e)
				{
					e.printStackTrace();
				}
				image = Draw.draw(data, 1000, false);
				g = image.createGraphics();
				
			}
			
			// * Find the optimal sim transform by trying all three permutations
			//   and picking the one with the lowest error.
						
			Point[][] perms = new Point[][]{                 
				new Point[]{big.get(0), big.get(1), big.get(2)},
				new Point[]{big.get(0), big.get(2), big.get(1)},
				new Point[]{big.get(1), big.get(0), big.get(2)},
				new Point[]{big.get(1), big.get(2), big.get(0)},
				new Point[]{big.get(2), big.get(0), big.get(1)},
				new Point[]{big.get(2), big.get(1), big.get(0)}
			};                                   
			
			Maps.MapResult best = null;
			
			for(Point[] b : perms)
			{
				Maps.MapResult result = Maps.findMapResult(Arrays.asList(b), small);
				
				if(best == null || best.error() > result.error())
					best = result;
			}
			
			top.add(best);
			
			if(top.size() > TOP_SIZE * 2)
			{
				Collections.sort(top);
				while(top.size() > TOP_SIZE)
					top.remove(top.size()-1);
			}
		}
		
		Collections.sort(top);
		
//		for(Maps.MapResult res : top)
//			System.out.println(res.error() + "| " + res.scale() + " " + res.translation() + " " + res.rotation());

		List<Point> out = new ArrayList<Point>();
		
		for(Maps.MapResult res : top)
			out.add(new Point(
					abs(res.translation().getEntry(0)),
					abs(res.similitude(2000, 1).angles().get(0)/Math.PI)
				));
		
		try
		{
			BufferedImage outIm = Draw.draw(out, new double[]{0.0, 1.0}, new double[]{0.0, 1.0}, 200, true);
			ImageIO.write(outIm, "PNG", new File(dir, "density.png"));
		} catch (IOException e)
		{
			e.printStackTrace();
		}		
	}
	
	
	//@Test
	public void testLocality2()
	{
		long seed = new Random().nextLong();
		Global.random = new Random(seed);
		System.out.println(seed);
		
		data = IFSs.sierpinski().generator().generate(10000);

		File dir = new File("/Users/Peter/Documents/PhD/output/triangles/");
		dir.mkdirs();
		
		Locality loc = new Locality(data, 25);
		
		System.out.println("Created Locality");

		BufferedImage image = Draw.draw(data, 1000, false);
		
		Graphics2D g = image.createGraphics();

		g.setStroke(new BasicStroke(2.0f));	
		
		int l0 = 5, l1 = 10, l2 = 20;
			
		for(int i : Series.series(30))
		{
			Locality.Node node = loc.random();
			List<List<Point>> ts = node.grow(l2);
			System.out.println(ts.size());
			
			g.setColor(Color.BLUE);		
			if(ts.size() > l0)
			{
				List<Point> w = ts.get(l0);
				
				line(g, w.get(0), w.get(1));
				line(g, w.get(1), w.get(2));
				line(g, w.get(2), w.get(0));			
			}
			
			g.setColor(Color.GREEN);		
			if(ts.size() > l1)
			{
				List<Point> w = ts.get(l1);
				
				line(g, w.get(0), w.get(1));
				line(g, w.get(1), w.get(2));
				line(g, w.get(2), w.get(0));			
			}
			
			g.setColor(Color.RED);		
			if(ts.size() > l2)
			{
				List<Point> w = ts.get(l2);
				
				line(g, w.get(0), w.get(1));
				line(g, w.get(1), w.get(2));
				line(g, w.get(2), w.get(0));			
			}
			
		}
					
		try
		{
			ImageIO.write(image, "PNG", new File(dir, "upwalk.png"));
		} catch (IOException e)
		{
			e.printStackTrace();
		}
	}
	
	private void line(Graphics2D g, Point a, Point b)
	{
		g.drawLine(
				Draw.toPixel(a.get(0), 1000, -1.0, 1.0), 
				Draw.toPixel(a.get(1), 1000, -1.0, 1.0),
				Draw.toPixel(b.get(0), 1000, -1.0, 1.0), 
				Draw.toPixel(b.get(1), 1000, -1.0, 1.0));
	}
	
	public double surface(Point[] t)
	{
		Point v = new Point(t[1].get(0) - t[0].get(0), t[1].get(1)-t[0].get(1));
		Point w = new Point(t[2].get(0) - t[0].get(0), t[2].get(1)-t[0].get(1));

		return Math.abs(0.5 * v.get(0)*w.get(1) - w.get(0)*v.get(1));
	}
	
	public Point[] discretize(Point[] in, double lambda) 
	{
		return new Point[] {
			discretize(in[0], lambda),
			discretize(in[1], lambda),
			discretize(in[2], lambda)};
	}
	
	public List<Point> discretize(List<Point> in, double lambda) 
	{
		return Arrays.asList(
			discretize(in.get(0), lambda),
			discretize(in.get(1), lambda),
			discretize(in.get(2), lambda));
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

	
	public class Locality
	{
		private List<Node> data;
		private int degree;

		public Locality(List<Point> data, int degree)
		{
			this.degree = degree;
			
			this.data = new ArrayList<Node>(data.size());
			
			for(int i : Series.series(data.size()))
				this.data.add(new Node(i, data.get(i)));
			
			for(int i : Series.series(data.size()))
				this.data.get(i).search();
		}
		
		/**
		 * 
		 * @param num The number of points to return
		 * @param length The length of the random walks
		 * @return
		 */
		public List<Point> points(int num, int length)
		{
			Node random = data.get(Global.random.nextInt(data.size()));
			
			List<Node> res = new ArrayList<Node>(num);
			for(int i : Series.series(num))
				res.add(random);

			for(int i : Series.series(length))
				for(int j : Series.series(num))
					res.set(j, res.get(j).walk());
			
			List<Point> points = new ArrayList<Point>(res.size());
			for(Node node : res)
				points.add(node.point());
			
			return points;
		}
		
		public List<Point> points(int num)
		{
			List<Point> points = new ArrayList<Point>(num);
			for(int i : Series.series(num))
				points.add(
						data.get(Global.random.nextInt(data.size())).point());
			
			return points;
		}
		
		
		/**
		 * Returns a series of generally increasing triangles spanned by 
		 * three self-avoiding random walks.
		 * 
		 * @return
		 */
		public List<List<Point>> upWalk(int length)
		{
			List<List<Point>> res = new ArrayList<List<Point>>(3);
			Node start = data.get(Global.random.nextInt(data.size()));
			
			for(int i : Series.series(3))
				res.add(start.avoidingWalk(length));
			
			return res;
		}
		
		public Node random()
		{
			return  data.get(Global.random.nextInt(data.size()));
		}

		/**
		 * Generates a grown triangle
		 * @param n
		 * @return
		 */
		public List<Point> grown(int n)
		{
			Node start = random();
			List<Node> res = null;
			
			while(res == null)
			{
				res = Arrays.asList(start, start, start);
				for(int i : Series.series(n))
				{
					res = grow(res);
					if(res == null)
						break;		
				}
			}
			
			return new PointList(res);
		}

		public List<Node> grow(List<Node> triangle)
		{
			List<Node> grown = new ArrayList<Node>();
			for(int i = 0; i < triangle.size(); i++)
			{
				List<Node> neigh = new ArrayList<Node>(triangle.get(i).nodes());
				
				double d = 0.0;
				for(int j = 0; j < triangle.size(); j++)
					if(i != j)
						d += triangle.get(i).point().distance(triangle.get(j).point());
				
				double nextD = Double.NEGATIVE_INFINITY;
				Node next = null;
				while(nextD < d)
				{
					int choice = Global.random.nextInt(neigh.size());
					next = neigh.get(choice);
					
					nextD = 0.0;
					for(int j = 0; j < triangle.size(); j++)
						if(i != j)
							nextD += next.point().distance(triangle.get(j).point());
					
					if(nextD < d)
						neigh.remove(choice);
					if(neigh.size() == 0)
						return null;
				}
				
				grown.add(next);
			}
			
			return grown;
		}

		private class PointList extends AbstractList<Point>
		{
			List<Node> master;
		
			public PointList(List<Node> master)
			{
				this.master = master;
			}
		
			@Override
			public Point get(int i)
			{
				return master.get(i).point();
			}
		
			@Override
			public int size()
			{
				return master.size();
			}
			
			
			
		}

		public class Node
		{
			private int myIndex;
			private Point me;
			private List<Integer> neighbours = new ArrayList<Integer>(degree * 3 + 1);
			
			public Node(int i, Point point)
			{
				this.myIndex = i;
				this.me = point;	
			}
			
			public void search()
			{
				for(int j = 0; j < data.size(); j++)
					if(j != myIndex)
						insert(j);
				
				while(neighbours.size() > degree)
					neighbours.remove(neighbours.size()-1);
				
				if(neighbours.size() == 0)
					System.out.println("!");
			}

			private void insert(int index)
			{
				neighbours.add(index);
				
				if(neighbours.size() > degree * 3)
				{
					Collections.sort(neighbours, new Comparator());
					while(neighbours.size() > degree)
						neighbours.remove(neighbours.size()-1);
				}
			}
			
			private class Comparator implements java.util.Comparator<Integer>
			{

				public int compare(Integer first, Integer second)
				{
					return Double.compare(
							me.distance(data.get(first).point()), 
							me.distance(data.get(second).point()));
				}				
			}
			
			public Point point()
			{
				return me;
			}
			
			public List<Node> neighbours()
			{
				return new NodeList();
			}
			
			/**
			 * Returns a random neighbour
			 * @return
			 */
			public Node walk()
			{
				return data.get(
						neighbours.get(
								Global.random.nextInt(neighbours.size())));
			}
			
			public List<List<Node>> growInner(int n)
			{
				if(n == 0)
				{
					List<List<Node>> res = new ArrayList<List<Node>>();
					res.add(Arrays.asList(this, this, this));
					return res;
				}
					
				List<List<Node>> g = growInner(n-1);
				if(g.get(g.size()-1) == null)
					return g;
				
				List<Node> next = Locality.this.grow(g.get(g.size()-1));
				g.add(next);
				
				return g;
			}
			
			/**
			 * Grows a triangle in steps from this node
			 * 
			 * @return A list of triangles with increasing surface area
			 */
			public List<List<Point>> grow(int n)
			{
				List<List<Node>> inner = growInner(n);
				
				List<List<Point>> res = new ArrayList<List<Point>>();
				for(List<Node> ln : inner)
					if(ln != null)
						res.add(new PointList(ln));
				
				return res;
			}
			
			/**
			 * Returns a length n self-avoiding random walk starting at this 
			 * node
			 * @param length
			 * @return
			 */
			public List<Point> avoidingWalk(int length)
			{
				ArrayList<Node> walk = new ArrayList<Node>(length);
				walk.add(this);
				
				Node next;
				while(walk.size() < length)
				{
					next = walk();
//					while(walk.contains(next))
//						next = walk();

					walk.add(next);
				}
				
				return new PointList(walk);
			}
			
			public List<Point> growth(int n)
			{
				Collection<Node> col = growthInner(n);
				
				List<Point> res = new ArrayList<Point>(col.size());
				for(Node node : col)
					res.add(node.point());
				
				return res;	
			}
			
			private Collection<Node> growthInner(int n)
			{
				if(n == 0)
				{
					List<Node> l = new ArrayList<Node>(n);
					l.add(this);
					return l;
				}
				
				Set<Node> nodes = new HashSet<Node>();
				for(int i : neighbours)
				{
					nodes.addAll(data.get(i).growthInner(n - 1));
				}

				return nodes;
			}
			
			private class NodeList extends AbstractList<Node>
			{

				@Override
				public Node get(int i)
				{
					return data.get(neighbours.get(i));
				}

				@Override
				public int size()
				{
					return neighbours.size();
				}
			}
			
			public boolean equals(Object other)
			{
				if(!(other instanceof Node))
					return false;
				
				return myIndex == ((Node)other).myIndex;
			}
			
			public List<Node> nodes()
			{
				return new NodeList();
			}
		}
	
	}
}
