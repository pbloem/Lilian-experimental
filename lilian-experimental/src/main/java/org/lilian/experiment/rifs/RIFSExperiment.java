package org.lilian.experiment.rifs;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.imageio.ImageIO;

import org.lilian.Global;
import org.lilian.data.real.Point;
import org.lilian.data.real.Similitude;
import org.lilian.data.real.fractal.random.DiscreteRIFS;
import org.lilian.data.real.fractal.random.RIFSEM;
import org.lilian.data.real.fractal.random.RIFSs;
import org.lilian.experiment.AbstractExperiment;
import org.lilian.experiment.State;
import org.lilian.util.Series;

public class RIFSExperiment extends AbstractExperiment
{
	private static int M = 50;
	private static int N = 100000;
	private static int DEPTH = 8;
	private static int RES = 200;
	private static int NUM_RANDOM = 3;
	
	private int generations = 100;
	
	private static DiscreteRIFS<Similitude> TARGET = RIFSs.koch2UpDown(); 
	
	private List<List<Point>> data = new ArrayList<List<Point>>(N);
	private File genDir;
	
	@State
	public RIFSEM em; 
	
	public RIFSExperiment()
	{
		
		for(int i : Series.series(M))
			data.add(TARGET.randomInstance(N, DEPTH));
	}
	
	@Override
	protected void setup()
	{
		// DiscreteRIFS<Similitude> initial = RIFSs.initialSphere(2, 2, 2, 1.0, 0.33);
		DiscreteRIFS<Similitude> initial = TARGET;
		em = new RIFSEM(initial, data, DEPTH, 1024, 0.0001, 0.3);
		
		genDir = new File(dir, "generations/");
		genDir.mkdirs();
	}
	
	@Override
	protected void body()
	{
		for(int i : Series.series(generations))
		{
			logger.info("Starting generation " + i);
			
			BufferedImage image = RIFSs.draw(em.model(), RES, NUM_RANDOM);
			try {
				
				ImageIO.write(image, "PNG", new File(genDir, String.format("%04d.png", i)));
				
			} catch (IOException e) { throw new RuntimeException(e); }
			
			em.iteration();	
		}
	}

 
}
