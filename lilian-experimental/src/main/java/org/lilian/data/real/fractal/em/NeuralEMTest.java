package org.lilian.data.real.fractal.em;

import static org.junit.Assert.*;
import static org.lilian.util.Series.series;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;

import org.junit.Test;
import org.lilian.data.real.Draw;
import org.lilian.data.real.Generators;
import org.lilian.data.real.Point;
import org.lilian.data.real.fractal.IFS;
import org.lilian.data.real.fractal.NeuralEM;
import org.lilian.neural.ThreeLayer;
import org.lilian.util.Series;

public class NeuralEMTest
{

	@Test
	public void test()
	{
		for(int i : series(20))
		{
			IFS<ThreeLayer> model = NeuralEM.initial(2, 3, 3, 0.9);
			
			File dir = new File("/Users/Peter/Documents/PhD/output/ann-ifs");
			dir.mkdirs();
			
			try
			{
				BufferedImage image;
				
				image = Draw.draw(model.generator(), 10000000, 1000, true);
				ImageIO.write(image, "PNG", new File(dir, String.format("ann%04d.png", i)));
				
			} catch (IOException e)
			{
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}

}
