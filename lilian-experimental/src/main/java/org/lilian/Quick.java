package org.lilian;

import static org.lilian.util.Series.series;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;


import org.lilian.data.real.Cameras;
import org.lilian.data.real.Datasets;
import org.lilian.data.real.Draw;
import org.lilian.data.real.Generator;
import org.lilian.data.real.Generators;
import org.lilian.data.real.Map;
import org.lilian.data.real.MappedList;
import org.lilian.data.real.Maps;
import org.lilian.data.real.PCA;
import org.lilian.data.real.PCAIterative;
import org.lilian.data.real.Point;
import org.lilian.data.real.classification.Classification;
import org.lilian.data.real.classification.Classified;
import org.lilian.data.real.classification.Classifiers;
import org.lilian.data.real.fractal.IFSClassifierBasic;
import org.lilian.experiment.Resources;
import org.lilian.util.Series;

public class Quick
{

	/**
	 * @param args
	 * @throws IOException 
	 */
	public static void main(String[] args) throws IOException
	{
//		List<Point> data = Generators.logistic().generate(1000000);
//		
//		Set<Point> set = new HashSet<Point>(data);
//		Global.log().info("Uniquification retained " + (set.size()) + " points out of " + data.size());
//		data = new ArrayList<Point>(set);
		
		// for(Point p : data)
		//	System.out.println(p);
		
//		int n = 10, sub = 5000;
//		
//		Classified<Point> mnist = Resources.csvClassification(
//				new File("/Users/Peter/Documents/datasets/classification/digits/digits.csv"));
//		// mnist = Classification.sample(mnist, sub);
//		
//		PCAIterative pca = new PCAIterative(mnist, n, 10);
//		List<Point> simple = pca.simplify(n);
//		
//		Classified<Point> result = Classification.combine(simple, mnist.classes());
//		Classification.write(result, new File("/Users/Peter/Documents/datasets/classification/digits/digits."+n+".csv"));
//						
//		List<Point> small = Generators.lorenz().generate(10000);
//
//		Map center = Maps.centered(small),
//		   camera = Cameras.basic();
//		Map map = center.compose(camera);
//		// Map map = center;
//		
//		//for(Point p  : new MappedList(small, center))
//		//	System.out.println(p);
//		//map = Cameras.basic();
//		
//		Generator<Point> gen = Generators.mapped(Generators.lorenz(), map);
//
//		
//		BufferedImage image = Draw.draw(gen, 1000000000, 1000, false);
//		ImageIO.write(image, "PNG", new File("/Users/Peter/Desktop/quicktest.png"));
		
		IFSClassifierBasic ifsc = IFSClassifierBasic.sierpinski(7);
		
		BufferedImage image = Classifiers.draw(ifsc, 320);
		ImageIO.write(image, "PNG", new File("/Users/Peter/Desktop/quicktest.png"));

		
	}

}
