package org.lilian.fractalpaper;

import static org.lilian.data.real.Draw.invert;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;

import org.lilian.data.real.Draw;
import org.lilian.data.real.Generator;
import org.lilian.data.real.MVN;
import org.lilian.data.real.Point;
import org.lilian.data.real.Similitude;
import org.lilian.data.real.fractal.IFS;
import org.lilian.data.real.fractal.IFSs;
import org.lilian.data.real.fractal.em.EM;
import org.lilian.experiment.AbstractExperiment;
import org.lilian.util.Series;

public class Generate extends AbstractExperiment
{
	public static final int RES = 750;
	public static final int ITS = 100000000;


	@Override
	protected void body()
	{		
		try
		{	
			Generator<Point> gen;
			BufferedImage image;
			
			File measures = new File(dir, "measures/");
			measures.mkdirs();
//			gen = IFSs.sierpinskiOff(6.0, 1.0, 1.0).generator();
//			image = Draw.draw(gen, ITS, RES, true);
//
//			ImageIO.write(invert(image), "PNG", new File(measures, "sierpinski-off.png"));
			
//			gen = IFSs.square(1.0, 2.0, 4.0, 8.0).generator();
//			image = Draw.draw(gen, ITS, RES, true);
//
//			ImageIO.write(invert(image), "PNG", new File(measures, "square.png"));
//			
//			IFS<Similitude> ifs = IFSs.square(1.0, 2.0, 4.0, 8.0);
			
			IFS<Similitude> mvn = EM.learn(new MVN(2, 0.3).generate(100000), 2, 5, 1000, 2000);
			image = Draw.draw(mvn.generator(), ITS, RES, true);

			ImageIO.write(invert(image), "PNG", new File(measures, "mvn.png"));
			
//			for(int i : Series.series(5))
//			{
//				ifs = IFSs.perturb(ifs, IFS.builder(4, Similitude.similitudeBuilder(2)), 0.1);
//				gen = ifs.generator();
//				image = Draw.draw(gen, ITS, RES, true);
//
//				ImageIO.write(invert(image), "PNG", new File(measures, String.format("perturb%03d.png", i)));
//			}
//			
//			for(int i : Series.series(20))
//			{
//				gen = IFSs.randomSimilitude(2, 3, 0.5).generator();
//				image = Draw.draw(gen, ITS, RES, true);
//
//				ImageIO.write(invert(image), "PNG", new File(measures, String.format("random%03d.png", i)));
//
//			}
		} catch (IOException e)
		{
			throw new RuntimeException(e);
		}

	}
	
}
