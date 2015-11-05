package org.lilian.data.real.fractal;

import static org.junit.Assert.*;
import static org.lilian.util.Series.series;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import javax.imageio.ImageIO;

import org.junit.Test;
import org.lilian.Global;
import org.lilian.data.real.AffineMap;
import org.lilian.data.real.Draw;
import org.lilian.data.real.Map;
import org.lilian.data.real.Maps;
import org.lilian.data.real.Point;
import org.lilian.data.real.fractal.old.IFS;
import org.lilian.data.real.fractal.old.IFSs;
import org.lilian.search.Builder;
import org.lilian.search.Parametrizable;
import org.lilian.search.evo.ES;
import org.lilian.search.evo.Target;
import org.lilian.util.Functions;
import org.lilian.util.Series;
import org.lilian.util.distance.EuclideanDistance;
import org.lilian.util.distance.HausdorffDistance;

public class Tests
{
	
	private static final int DATA_SIZE = 100000;
	private static final int SAMPLES = 200;
	private static final int POP_SIZE = 100;
	private static final int GENERATIONS = 200;
	
	@Test
	public void test()
	{
		long seed = new Random().nextLong();
		Global.random = new Random(seed);
		System.out.println(seed);
		
		File dir = new File("/Users/Peter/Documents/PhD/es_simple/");
		dir.mkdirs();

		IFS<AffineMap> ifs = IFSs.sierpinski();
		List<Point> in = ifs.generator().generate(DATA_SIZE);
		Map m1 = ifs.get(0);
		List<Point> out = m1.map(in);
		Collections.shuffle(out);
		
		try
		{
			BufferedImage image = Draw.draw(in, 1000, true);
			ImageIO.write(image, "PNG", new File(dir, "in.png"));
			image = Draw.draw(out, 1000, true);
			ImageIO.write(image, "PNG", new File(dir, "out.png"));
		} catch (IOException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		Map map = Maps.findSimilitude(in, out);
		List<Point> outFixed = map.map(in);

		try
		{
			BufferedImage image = Draw.draw(outFixed, 1000, true);
			ImageIO.write(image, "PNG", new File(dir, "out_fixed.png"));
		} catch (IOException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		
//		ES<IFS<AffineMap>> es = new ES<IFS<AffineMap>>(
//				builder, target, initial, 
//				2, initial.size()*2, 0, 
//				ES.CrossoverMode.UNIFORM);
		
		Builder<AffineMap> builder = AffineMap.affineMapBuilder(2);
		ES<AffineMap> es = new ES<AffineMap>(
			builder, new MapTarget(in, out), ES.initial(POP_SIZE, builder.numParameters(), 0.1)
			);
		
		Functions.tic();
		for(int i : series(GENERATIONS))
		{
			es.breed();
			
			if(i % (GENERATIONS/10) == 0)
			{
				try
				{
					BufferedImage image = Draw.draw(es.best().instance().map(in), 1000, true);
					ImageIO.write(image, "PNG", new File(dir, String.format("%04dres.png", i)));
				} catch (IOException e)
				{
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				
				System.out.println(i);
			}
			
		}
	}

	private class MapTarget implements Target<AffineMap> {

		List<Point> dataIn;
		List<Point> dataOut;
		
		public MapTarget(List<Point> dataIn, List<Point> dataOut)
		{
			this.dataIn = dataIn;
			this.dataOut = dataOut;
		}

		public double score(AffineMap map)
		{
			List<Point> inSample;
			List<Point> outSample;
			
			inSample = new ArrayList<Point>(SAMPLES);
			outSample = new ArrayList<Point>(SAMPLES);
			
			for(int i = 0; i < SAMPLES; i++)
			{
				inSample.add(dataIn.get(Global.random.nextInt(dataIn.size())));
				outSample.add(dataOut.get(Global.random.nextInt(dataOut.size())));
			}
			
			List<Point> mOut = map.map(inSample);
			
			return - HausdorffDistance.hausdorff(mOut, outSample);
		}
		
	}
}
