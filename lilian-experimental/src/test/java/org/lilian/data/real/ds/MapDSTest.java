package org.lilian.data.real.ds;

import static org.junit.Assert.*;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;

import javax.imageio.ImageIO;

import org.junit.Test;
import org.lilian.data.real.Densities;
import org.lilian.data.real.Draw;
import org.lilian.data.real.MVN;
import org.lilian.data.real.Point;

public class MapDSTest
{

	@Test
	public void test()
	{
		MapDS henon = DSs.henon();
		int iterations = 10000;
		
		File dir = new File("/Users/Peter/Documents/DS/");
		dir.mkdirs();
		
		for(double var : Arrays.asList(0.1, 0.01, 0.001, 0.0001, 0.00001))
		{
			int res = 50;
			BufferedImage image = Densities.draw(henon.densityEstimator(iterations, var), new double[]{-2.0, 2.0}, new double[]{-2.0, 2.0}, res, res, false);
			try
			{
				ImageIO.write(image, "PNG", new File(dir, var+ ".png"));
			} catch (IOException e)
			{
				throw new RuntimeException(e);
			}
			
			System.out.println(var + " finished");
		}
		
		BufferedImage image = Draw.draw(henon, 100000, new double[]{-2.0, 2.0}, new double[]{-2.0, 2.0}, 1000, 1000, false);
		
		try
		{
			ImageIO.write(image, "PNG", new File(dir, "generated.png"));
		} catch (IOException e)
		{
			throw new RuntimeException(e);
		}
	}
	
	@Test
	public void testInverse()
	{
		MapDS henon = DSs.henon();
		
		File dir = new File("/Users/Peter/Documents/DS-inverse/");
		dir.mkdirs();
		
		for(int iterations : Arrays.asList(1, 2, 3, 4, 5, 6, 8, 10))
		{
			int res = 1000;
			BufferedImage image = Densities.draw(henon.inverseEstimator(iterations), new double[]{-2.0, 2.0}, new double[]{-2.0, 2.0}, res, res, false);
			try
			{
				ImageIO.write(image, "PNG", new File(dir, iterations+ ".png"));
			} catch (IOException e)
			{
				throw new RuntimeException(e);
			}
			
			System.out.println(iterations + " finished");
		}
		
		BufferedImage image = Draw.draw(henon, 100000, new double[]{-2.0, 2.0}, new double[]{-2.0, 2.0}, 1000, 1000, false);
		
		try
		{
			ImageIO.write(image, "PNG", new File(dir, "generated.png"));
		} catch (IOException e)
		{
			throw new RuntimeException(e);
		}
	}	
	
	@Test
	public void testMVN()
	{
		
		File dir = new File("/Users/Peter/Documents/MVN/");
		dir.mkdirs();
		
		for(double var : Arrays.asList(1.0, 0.75, 0.5, 0.25, 0.01, 0.001))
		{
			BufferedImage image = Densities.draw(new MVN(2, var), 1000, true);
			try
			{
				ImageIO.write(image, "PNG", new File(dir, var+ ".png"));
			} catch (IOException e)
			{
				throw new RuntimeException(e);
			}
			
		}
	}

}
