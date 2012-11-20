package org.lilian.data.real.fractal.em;

import static org.junit.Assert.*;
import static org.lilian.data.real.Draw.toPixel;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Paint;
import java.awt.Stroke;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

import javax.imageio.ImageIO;

import org.junit.Test;
import org.lilian.Global;
import org.lilian.data.real.AffineMap;
import org.lilian.data.real.Datasets;
import org.lilian.data.real.Draw;
import org.lilian.data.real.Map;
import org.lilian.data.real.Maps;
import org.lilian.data.real.Point;
import org.lilian.data.real.Similitude;
import org.lilian.data.real.fractal.IFS;
import org.lilian.data.real.fractal.IFSs;
import org.lilian.util.Functions;
import org.lilian.util.Series;

public class EMTests
{
	public static int DEPTH = 20;
	public static double NOISE = 0.00;
	public static int TRIALS = 100;

	@Test
	public void test()
	{
		Global.random = new Random();
		
		int transform = Global.random.nextInt(3);	
		int n = Similitude.similitudeBuilder(2).numParameters();
		List<Double> average = new ArrayList<Double>(new Point(n));
		
		for(int i : Series.series(TRIALS))
		{
			IFS<AffineMap> 	ifs = IFSs.sierpinski();
			List<Integer> 	codeA = random(3, DEPTH), 
							codeB = random(3, DEPTH);
			
	
			List<Integer> 	codeAS = append(codeA, transform),
					 		codeBS = append(codeB, transform);
			
			Point source = new Point(2);
			Point a = noise(ifs.compose(codeA).map(source), NOISE);
			Point b = noise(ifs.compose(codeB).map(source), NOISE);
			
			Point as = noise(ifs.compose(codeAS).map(source), NOISE);
			Point bs = noise(ifs.compose(codeBS).map(source), NOISE);
			
			Similitude sim = Maps.findMap(
					Arrays.asList(a, b), 
					Arrays.asList(as, bs));
			
			List<Double> params = sim.parameters();
			for(int j : Series.series(n))
				average.set(j, average.get(j) + params.get(j));
		}
		
		for(int j : Series.series(n))
			average.set(j, average.get(j) / TRIALS);
		
		Similitude sim = Similitude.similitudeBuilder(2).build(average);
		System.out.println(sim);
	}

	public List<Integer> random(int max, int depth)
	{
		List<Integer> res = new ArrayList<Integer>(depth);
		for(int i : Series.series(depth))
			res.add(Global.random.nextInt(max));
		
		return res;
	}
	
	public List<Integer> append(List<Integer> in, int symbol)
	{
		List<Integer> out = new ArrayList<Integer>(in);
		out.remove(out.size() - 1);
		out.add(0, symbol);
		return out;
	}
	
	public Point noise(Point p, double noise)
	{
		Point res = new Point(p);
		for(int i : Series.series(p.size()))
			res.set(i, res.get(i) + Global.random.nextGaussian() * noise);
		
		return res;	
	}
	
	@Test
	public void drawCodes() throws IOException
	{
		Global.random = new Random();
		File dir = new File("/home/peter/Documents/PhD/output/codesSearch3/");
		dir.mkdirs();
		
		IFS<AffineMap> ifs = IFSs.sierpinski();
		BufferedImage im = 
				Draw.draw(ifs.generator(), 10000000, 1000, true);
		ImageIO.write(im, "PNG", new File(dir, "model.png"));
		
		for(int depth : Series.series(1, 16))
		{
			im = Draw.drawCodes(ifs, 
							new double[]{-1.0, 1.0}, new double[]{-1.0, 1.0}, 
							300, depth, 1);
			ImageIO.write(im, "PNG", new File(dir, "out"+depth+".png"));
			System.out.println(".");
		}
	}
	
	private static final int SAMPLE_SIZE = 10000;
	
//	@Test
//	public void testEM() throws IOException
//	{
//		Global.random = new Random(42);
//		File dir = new File("/Users/peter/Documents/PhD/output/em_test_AM/");
//		dir.mkdirs();
//		
//		// IFS<AffineMap> ifs = IFS.makeAffine(IFSs.koch2Sim());
//		
//		List<Point> data = Datasets.ball(32).generate(500000);
//		// List<Point> data = ifs.generator().generate(100000);
////		BufferedImage im = 
////				Draw.draw(data, 1000, true);
////		ImageIO.write(im, "PNG", new File(dir, "data.png"));
//		
//		int num = 2;
//		int dim = data.get(0).dimensionality();
//		EM em = new EM(num, dim, data, 0.2, true, null);
//		em.distributePoints(SAMPLE_SIZE, 6, -1);
//		
//		for(int i : Series.series(100))
//		{
//			Functions.tic();
////			EM.Maps maps = em.findMaps();
////			System.out.println(maps);
////			for(int j : Series.series(num))
////			{
////				System.out.println(j);
////				im = drawMap(maps.from(j), maps.to(j));
////				ImageIO.write(im, "PNG", new File(dir, String.format("map%d.%02d.png", j, i)));			
////			}
//			
//			em.findIFS();
//			
////			if(i % 1 == 0)
////			{
////				im = Draw.draw(em.model().generator(), 10000000, 1000, true);
////				ImageIO.write(im, "PNG", new File(dir, String.format("gen%04d.png", i)));
////				
////				System.out.println();
////			}
//			
//			em.distributePoints(SAMPLE_SIZE, 6, -1);
//			
//			System.out.print("Step "+i+" completed in " + Functions.toc() + "seconds.");
//		}
//	}
	
	public BufferedImage drawMap(List<Point> from, List<Point> to)
	{
		double[] xrange = {-1.0, 1.0};
		double[] yrange = {-1.0, 1.0};
		int res = 1000;
		boolean log = false;
		
		double 	xDelta = xrange[1] - xrange[0],
				yDelta = yrange[1] - yrange[0];
		
		double maxDelta = Math.max(xDelta, yDelta); 		
		double minDelta = Math.min(xDelta, yDelta);
		
		double step = minDelta/(double) res;
		
		int xRes = (int) (xDelta / step);
		int yRes = (int) (yDelta / step);

		float max = Float.NEGATIVE_INFINITY;
		float min = 0.0f;		
		
		float[][] matrixFrom = new float[yRes][];
		float[][] matrixTo = new float[yRes][];

		for(int x = 0; x < xRes; x++)
		{
			matrixFrom[x] = new float[yRes];
			matrixTo[x] = new float[yRes];

			for(int y = 0; y < yRes; y++)
			{
				matrixFrom[x][y] = 0.0f;
				matrixTo[x][y] = 0.0f;
			}
		}
		
		BufferedImage image = 
				new BufferedImage(xRes, yRes, BufferedImage.TYPE_INT_RGB);
		
		Graphics2D g = image.createGraphics();
		for(int i = 0; i < from.size(); i++)
		{
			Point pointFrom = from.get(i);
			Point pointTo = to.get(i);
			
//			System.out.println(pointFrom + " " + pointTo);
			
			int xf = toPixel(pointFrom.get(0), xRes, xrange[0], xrange[1]); 
			int yf = toPixel(pointFrom.get(1), yRes, yrange[0], yrange[1]);

			int xt = toPixel(pointTo.get(0), xRes, xrange[0], xrange[1]); 
			int yt = toPixel(pointTo.get(1), yRes, yrange[0], yrange[1]);			
			
			g.setColor(Color.WHITE);
			g.setStroke(new BasicStroke(2.0f));	
			g.drawLine(xf, yf, xt, yt);
			
			g.setColor(Color.BLUE);
			g.fillOval(xf-16, yf-16, 32, 32);
			
			g.setColor(Color.GREEN);
			g.fillOval(xt-12, yt-12, 24, 24);
			
		}
		g.dispose();
		
		return image;
	}
	
//	@Test
//	public void findScalarTest()
//	{
//		List<Double> x, y;
//		x = Arrays.asList(0.0, 2.0, 4.0, 6.0, 8.0, 10.0, 12.0, 14.0);
//		y = Arrays.asList(0.0, 1.0, 2.0, 3.0, 4.0, 5.0, 6.0, 7.0);
//		
//		assertEquals(EM.findScalar(x, y), 2.0, 0.0);
//		
//		x = Arrays.asList(0.0, 2.0, 4.0, 6.0, 8.0, 10.0, 12.0, 14.0);
//		y = Arrays.asList(0.0, 1.0, 2.0, 3.0, 4.0, 5.0, 6.0, 7.1);
//		
//		assertEquals(EM.findScalar(x, y), 2.0, 0.1);
//	
//	}
	
	@Test
	public void testEMInitial() throws IOException	
	{
		int num = 20;
		double[] xrange = new double[]{-2.1333, 2.1333};
		double[] yrange = new double[]{-1.2, 1.2};
		
		Global.random = new Random(42);
		File dir = new File("/Users/peter/Documents/PhD/output/emInital/");
		dir.mkdirs();
		
		int dim = 2;
		int comp = 3;
		
		BufferedImage image;
		
//		for(int i : Series.series(num))
//		{
//			IFS<Similitude> model = EM.initialRandom(dim, comp, 0.6);
//			image = Draw.draw(model.generator(), 10000000, xrange, yrange, 1920, 1080, true);
//			ImageIO.write(image, "PNG", new File(dir, String.format("random.%04d.png", i)));
//		}
//		
//		for(int i : Series.series(num))
//		{
//			IFS<Similitude> model = EM.initialSphere(dim, comp, 0.7, 0.5);
//			image = Draw.draw(model.generator(), 10000000, xrange, yrange, 1920, 1080, true);
//			ImageIO.write(image, "PNG", new File(dir, String.format("sphere.%04d.png", i)));
//		}
		
//		for(int i : Series.series(num))
//		{
//			IFS<Similitude> model = EM.initialSpread(dim, comp, 1.0, 0.5);
//			// image = Draw.draw(model.generator(), 10000000, xrange, yrange, 1920, 1080, true);
//			image = Draw.draw(model.generator(), 10000000, 1000, true);
//			ImageIO.write(image, "PNG", new File(dir, String.format("spread.%04d.png", i)));
//		}		
	}
}
