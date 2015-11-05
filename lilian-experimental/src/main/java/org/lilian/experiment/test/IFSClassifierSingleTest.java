package org.lilian.experiment.test;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.List;

import javax.imageio.ImageIO;

import org.lilian.data.real.Datasets;
import org.lilian.data.real.Point;
import org.lilian.data.real.Similitude;
import org.lilian.data.real.classification.Classification;
import org.lilian.data.real.classification.Classifier;
import org.lilian.data.real.classification.Classifiers;
import org.lilian.data.real.fractal.old.IFS;
import org.lilian.data.real.fractal.old.IFSClassifierSingle;
import org.lilian.data.real.fractal.old.IFSs;
import org.lilian.experiment.AbstractExperiment;

public class IFSClassifierSingleTest extends AbstractExperiment
{
	private int depth = 3;
	
	@Override
	protected void setup()
	{
	}

	@Override
	protected void body()
	{
		List<Point> points = Datasets.cube(2).generate(512);
		
		Classifier master = Classifiers.ifs(depth);
		
		BufferedImage image = Classifiers.draw(master, 8);
		try
		{
			ImageIO.write(image, "PNG", new File(dir, "master.png"));
		} catch (IOException e)
		{
			throw new RuntimeException(e);
		}
		
		IFS<Similitude> model = IFSs.square();
		
		IFSClassifierSingle single = new IFSClassifierSingle(model, depth, false, 2);
		single.train(Classification.combine(points, master.classify(points)));
		
		image = Classifiers.draw(single, 8);
		try
		{
			ImageIO.write(image, "PNG", new File(dir, "res.png"));
		} catch (IOException e)
		{
			throw new RuntimeException(e);
		}	
	}

}
