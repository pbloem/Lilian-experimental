package org.lilian.data.real.fractal;

import static java.lang.Math.PI;
import static org.junit.Assert.*;

import java.util.Arrays;
import java.util.List;

import org.junit.Test;
import org.lilian.data.real.AffineMap;
import org.lilian.data.real.Map;
import org.lilian.data.real.Point;
import org.lilian.data.real.Rotation;
import org.lilian.data.real.Similitude;
import org.lilian.data.real.fractal.old.IFSs;
import org.lilian.util.Functions;
import org.lilian.util.MatrixTools;
import org.lilian.util.Series;

public class SimilarityTest
{
	public static int DATASIZE = 10000;
	public static int STEPS = 300;
	
	// @Test
	public void testEquals()
	{
		Map id = AffineMap.identity(2);
		
		assertTrue(SimilarityOld.equals(id, id, 10, 0.05));
		
		Map m1 = new AffineMap(Arrays.asList(1.0,0.0, 0.0,1.0, 0.02,0.02));
		assertTrue(SimilarityOld.equals(id, m1, 10, 0.05));
		assertFalse(SimilarityOld.equals(id, m1, 10, 0.01));
	}
	
	@Test
	public void run()
	{
		List<Point> data = IFSs.sierpinski().generator().generate(DATASIZE);
		
		MCSimilarity sim = new MCSimilarity(data, 100, 100, 10, 50, 3);
		
		Functions.tic();
		for(int i : Series.series(STEPS))
		{		
			sim.step();

			if(i % (STEPS/30) == 0)
			{
				sim.print(System.out);
				System.out.println("****************");
			}
		}
	
		sim.print(System.out);
	}

	// @Test
	public void runScales()
	{
		List<Point> data = IFSs.sierpinski().generator().generate(DATASIZE);
		
		SimilarityScaling sim = new SimilarityScaling(data, 1E-10, 1);
		for(int i : Series.series(STEPS))
		{
			sim.step();
			if(i % (STEPS/100) == 0)
			System.out.println(sim.boosted() + " ");
		}
		
		for(Double scale : sim.scales())
			System.out.println(scale);
	}
	// @Test
	public void testFindAngles2()
	{
		Point a0 = new Point(0.0, 0.0),
		      a1 = new Point(1.0, 0.0);
		
		List<Double> angles = Arrays.asList(Math.PI * 0.5);
		Similitude sim = new Similitude(1.0, Arrays.asList(0.0, 0.0), angles);
		
		Point b0 = sim.map(a0),
		      b1 = sim.map(a1);
	
		List<Double> out = SimilarityOld.findAngles(a0, a1, b0, b1);

		assertEquals(angles.get(0), out.get(0), 0.000001);
	}	
	
	//@Test
	public void testFindAngles3()
	{
		Point a0 = new Point(0.0, 0.0, 0.0),
		      a1 = new Point(1.0, 0.0, 0.0);
		
		List<Double> angles = Arrays.asList(PI*0.25, PI*0.25, PI*0.0);
		Similitude sim = new Similitude(1.0, Arrays.asList(0.0, 0.0, 0.0), angles);
		
		Point b0 = sim.map(a0),
		      b1 = sim.map(a1);
		
		System.out.println(b0 + " " + b1);
	
		List<Double> out = SimilarityOld.findAngles(a0, a1, b0, b1);
		
		System.out.println(angles);
		System.out.println(out);
		
		Similitude sim2 = new Similitude(1.0, Arrays.asList(0.0, 0.0, 0.0), out);
		
		Point c0 = sim2.map(a0),
		      c1 = sim2.map(a1);
		
		System.out.println(c0 + " " + c1);
	}
	
	// @Test
	public void rmTest()
	{
		Rotation.toRotationMatrix(Arrays.asList(1.0, 1.0, 1.0, 1.0, 1.0, 1.0));
	}

}
