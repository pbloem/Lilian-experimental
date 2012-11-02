package org.lilian;

import java.util.List;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;


import org.lilian.data.real.Draw;
import org.lilian.data.real.Generator;
import org.lilian.data.real.Generators;
import org.lilian.data.real.PCA;
import org.lilian.data.real.PCAIterative;
import org.lilian.data.real.Point;
import org.lilian.data.real.classification.Classification;
import org.lilian.data.real.classification.Classified;
import org.lilian.experiment.Resources;

public class Quick
{

	/**
	 * @param args
	 * @throws IOException 
	 */
	public static void main(String[] args) throws IOException
	{
//		int n = 20, sub = 5000;
//		
//		Classified<Point> mnist = Resources.csvClassification(
//				new File("/Users/Peter/Documents/datasets/classification/digits/digits.csv"));
//		// mnist = Classification.sample(mnist, sub);
//		
//		PCAIterative pca = new PCAIterative(mnist, n, 10);
//		List<Point> simple = pca.simplify(n);
//		
//		Classified<Point> result = Classification.combine(simple, mnist.classes());
//		Classification.write(result, new File("/Users/Peter/Documents/datasets/classification/digits/digits.20.csv"));
//				
		
		Generator<Point> ikeda = Generators.ikeda();
		
		BufferedImage image = Draw.draw(ikeda, 100000000, 1000, false);
		ImageIO.write(image, "PNG", new File("/Users/Peter/Desktop/quicktest.png"));
	}

}
