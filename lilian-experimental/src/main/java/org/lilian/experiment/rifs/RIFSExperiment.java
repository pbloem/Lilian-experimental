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
import org.lilian.data.real.fractal.random.ChoiceTree;
import org.lilian.data.real.fractal.random.DiscreteRIFS;
import org.lilian.data.real.fractal.random.RIFSEM;
import org.lilian.data.real.fractal.random.RIFSs;
import org.lilian.experiment.AbstractExperiment;
import org.lilian.experiment.State;
import org.lilian.util.Series;

public class RIFSExperiment extends AbstractExperiment
{
	// * stable: 100 datasets, 128 samples per dataset, depth 6 (on koch and cantor)
	
	// * If we start with the correct trees for the data, the cantor model is
	//   successfully induced from a random initial model. (in less than 
	//   five generations)
	
	private static int M = 5;
	private static int N = 10000;
	private static int N_SAMPLE = 1024;
	private static int DEPTH = 10;
	private static int RES = 200;
	private static int NUM_RANDOM = 3;
	
	private int generations = 100;
	
	private static DiscreteRIFS<Similitude> TARGET = RIFSs.cantor(); 
	
	private List<List<Point>> data = new ArrayList<List<Point>>(M);
	private List<ChoiceTree> dataTrees = new ArrayList<ChoiceTree>(M);
	
	private File genDir;
	
	@State
	public RIFSEM em; 
	
	public RIFSExperiment()
	{
		
		for(int i : Series.series(M))
		{
			ChoiceTree tree = ChoiceTree.random(TARGET, DEPTH);
			dataTrees.add(tree);
			data.add(TARGET.generator(tree).generate(N));
		}
	}
	
	@Override
	protected void setup()
	{
		// DiscreteRIFS<Similitude> initial = RIFSs.initialSphere(2, 2, 2, 1.0, 0.33);
		DiscreteRIFS<Similitude> initial = TARGET;
	
		em = new RIFSEM(initial, data, DEPTH, N_SAMPLE, 0.00001, 0.3);
		
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
