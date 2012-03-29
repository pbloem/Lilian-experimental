package org.lilian.facticity;

import static org.junit.Assert.*;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.junit.Test;
import org.lilian.data.real.Point;
import org.lilian.util.Functions;
import org.lilian.util.Series;

public class CollapseCurve
{
	public static final int[] N = new int[]{10, 50, 100, 500, 1000, 5000, 10000, 50000, 100000, 500000, 1000000};
		
	@Test
	public void test() throws IOException
	{
		File dir = new File("/Users/Peter/Documents/PhD/output/collapse/");
		dir.mkdirs();
		
		for(int n : N)
		{			
			List<Point> data = new ArrayList<Point>(n);

			for(int i : Series.series(n))
			{
				double modelSize = log(i);
				double dataSize = entropy(i/(double)n) * n;
				
				modelSize = modelSize + 2 * log(modelSize);
				
				double k = (modelSize+dataSize)/n, v = modelSize/n;
				data.add(new Point(k, v));
			}
			
			Functions.toCSV(data, new File(dir, String.format("out%07d.csv", n)));
		}
	}

	public double log(double in)
	{
		if(in == 0.0)
			return 0.0;
		
		return Math.log(in)/Math.log(2.0);
	}
	
	public double entropy(double p)
	{
		return - p * log(p) - (1.0 - p)* log(1.0 - p);
	}
}
