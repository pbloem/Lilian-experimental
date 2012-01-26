package org.lilian.facticity;

import static org.junit.Assert.*;

import java.awt.Color;
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
import org.lilian.data.real.Point;
import org.lilian.util.BitString;
import org.lilian.util.Compressor;
import org.lilian.util.Functions;
import org.lilian.util.GZIPCompressor;
import org.lilian.util.Series;

public class BitTest
{
	public static long STEPS = (long)1E10;
	
	public static int X_RES = 1920;
	public static int Y_RES = 1080;
	public static int DELTA = (int)(STEPS/20l);
	
	public static double[] RHOS = new double[]{1.0/1000, 1.0/2000, 1.0/4000, 1.0/8000, 1.0/16000};
	
	public Compressor<BitString> comp = new GZIPCompressor<BitString>(1);
	@Test
	public void test()
	{
		long seed = new Random().nextLong();
		System.out.println(seed);
		Global.random = new Random(seed);
		
		File dir = new File("/Users/Peter/Documents/PhD/output/bits/");
		dir.mkdirs();
		
		int size = X_RES * Y_RES * 32;
		System.out.println("size: " + size);
		
		List<List<Double>> data = new ArrayList<List<Double>>((int)STEPS/DELTA);
		for(long i = 0; i < STEPS; i += DELTA)
		{
			List<Double> list = new ArrayList<Double>(RHOS.length+1);
			list.add((double) i);
			data.add(list);
		}
		
		System.out.println(data.size());
		
		for(double rho : RHOS)
		{
			
			System.out.println();
			BitString string = BitString.zeros(size);

			for(long i = 0; i < STEPS; i++)
			{
				if(i%(STEPS/20) == 0)
					System.out.print(".");
				
				
				if(i % DELTA == 0)
					data.get((int)i/DELTA).add(comp.ratio(string));
				
				if(Global.random.nextDouble() > rho)
				{
					// * diffuse
					int from   = Global.random.nextInt(size),
						offset = Global.random.nextInt(size),
					    to     = Global.random.nextBoolean() ? 
					    		Functions.mod(from + offset, size): 
					    		Functions.mod(from - offset, size);
					    // Global.random.nextInt(size);
					string.set(to, string.get(from));
				} else 
				{
					// * create
					string.set(Global.random.nextInt(size), Global.random.nextBoolean());
				}
			}
		
			// * to image
			List<Integer> ints = string.toIntegers();
			
			BufferedImage image = 
					new BufferedImage(X_RES, Y_RES, BufferedImage.TYPE_INT_RGB);
			
			int c = 0;
			for(int i = 0; i < X_RES; i++)
				for(int j = 0; j < Y_RES; j++)
				{
					Color colorObj  = new Color(ints.get(c));
					image.setRGB(i, j, colorObj.getRGB());					
					
					c++;
				}
			
			try
			{
				ImageIO.write(image, "PNG", new File(dir, "out" + rho +".png"));
			} catch (IOException e)
			{
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		
		}
		
		try
		{
			Functions.toCSV(data, new File(dir, "out.csv"));
		} catch (IOException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}		
	}
	

	@Test
	public void random()
	{
		long seed = new Random(0).nextLong();
		System.out.println(seed);
		Global.random = new Random(seed);
		
		File dir = new File("/Users/Peter/Documents/PhD/output/bits/");
		dir.mkdirs();
		
		BufferedImage image = 
				new BufferedImage(X_RES, Y_RES, BufferedImage.TYPE_INT_RGB);
		
		int c = 0;
		for(int i = 0; i < X_RES; i++)
			for(int j = 0; j < Y_RES; j++)
			{
				Color colorObj  = new Color(Global.random.nextInt());
				image.setRGB(i, j, colorObj.getRGB());					
				
				c++;
			}
		
		try
		{
			ImageIO.write(image, "PNG", new File(dir, "random.png"));
		} catch (IOException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	@Test
	public void mod()
	{
		System.out.println( -2 % 10);
	}

}
