package org.lilian.ifs;

import static org.lilian.util.Series.series;

import java.awt.image.BufferedImage;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.imageio.ImageIO;

import org.data2semantics.platform.Global;
import org.data2semantics.platform.annotation.In;
import org.data2semantics.platform.annotation.Main;
import org.lilian.data.real.Draw;
import org.lilian.data.real.MVN;
import org.lilian.data.real.Map;
import org.lilian.data.real.Point;
import org.lilian.data.real.Similitude;
import org.lilian.data.real.fractal.EM;
import org.lilian.data.real.fractal.IFS;
import org.lilian.data.real.fractal.IFSs;
import org.lilian.platform.graphs.Random;
import org.lilian.search.Parametrizable;
import org.lilian.util.Functions;
import org.lilian.util.Series;

import com.itextpdf.text.log.SysoLogger;

public class LearnIFS 
{

	@In(name="data")
	public List<Point> data;
	
	@In(name="high quality")
	public boolean highQuality;

	@In(name="num components")
	public int numComponents;
	
	@In(name="depth")
	public int depth;

	@In(name="sample size") 
	public int sampleSize;
	
	@In(name="iterations")
	public int iterations;

	@In(name="repeats")
	public int repeats;

	public List<Double> likelihoods = new ArrayList<Double>();

	public double bestLikelihood = Double.POSITIVE_INFINITY;
	public List<IFS<Similitude>> bestHistory = null;
	public List<List<Double>> bestDepths = null;
	public List<Similitude> bestPosts = null;
	
	@Main
	public void run()
		throws IOException
	{
		org.lilian.Global.random = new java.util.Random();
		
		
		Similitude trans = new Similitude(1.0, new Point(10.0, 0.0), new Point(0.0));
		// data = trans.map(data);
		
		BufferedImage image = Draw.draw(data, 400, true);
		ImageIO.write(image, "PNG", new File(Global.getWorkingDir(), "data.png"));

		ExecutorService exec = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());

		for(int rep : series(repeats))
			exec.execute(new Repeat(rep, Global.getWorkingDir()));

		exec.shutdown();
		
		while(! exec.isTerminated());
		
		System.out.println("All threads finished");
		
		File dir = new File(Global.getWorkingDir(), "best/");
		dir.mkdirs();
		
		Writer out = new BufferedWriter(new FileWriter(new File(Global.getWorkingDir(), "likelihoods.csv")));
		for(double ll  : likelihoods)
			out.write(ll + "\n");
		out.close();
		
		for(int i : series(iterations))
			write(bestHistory.get(i), bestDepths.get(i), bestPosts.get(i), dir, String.format("iteration.%06d", i));
	}
	
	public synchronized void done(double likelihood, List<IFS<Similitude>> history, List<List<Double>> depths, List<Similitude> posts)
	{
		likelihoods.add(likelihood);
		
		if(likelihood < bestLikelihood)
		{
			bestLikelihood = likelihood;
			bestHistory = history;
			bestDepths = depths;
			bestPosts = posts;
		}
	}

	private <M extends Map & Parametrizable> void write(IFS<Similitude> ifs, List<Double> depths, Similitude post, File dir, String name) throws IOException
	{
		int div = highQuality ? 1 : 4;
		int its = highQuality ? (int) 1000000 : 10000;
				
		BufferedImage image;
		
		image = Draw.draw(ifs, its, 1000/div, true, depths, post);
		ImageIO.write(image, "PNG", new File(dir, name+".png"));
		
		image = Draw.draw(ifs.generator(), its, 1000/div, true, post);
		ImageIO.write(image, "PNG", new File(dir, name+".deep.png"));
	}
	
	private class Repeat extends Thread
	{		
		private int rep;
		private File dir;
		
		public Repeat(int rep, File dir)
		{
			this.rep = rep;
			this.dir = dir;
		}

		@Override
		public void run()
		{
			List<IFS<Similitude>> history = new ArrayList<IFS<Similitude>>(iterations);
			List<List<Double>> dHistory = new ArrayList<List<Double>>(iterations);			
			List<Similitude> pHistory = new ArrayList<Similitude>(iterations);			
			
			IFS<Similitude> initial = IFSs.initialSphere(2, numComponents, 1.0, 0.5, true);

			EM em = new EM(data, sampleSize, initial, depth, true);
			
			history.add(em.model());
			dHistory.add(em.depths());
						
			for(int i : series(iterations))
			{
				em.iterate();
				
				history.add(em.model());
				dHistory.add(em.depths());
				pHistory.add(em.post());
			}
			
			try
			{
				write(em.model(), em.depths(), em.post(), dir, String.format("rep.%04d", rep));
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
			
			double likelihood = -em.logLikelihood(data);
			
			done(likelihood, history, dHistory, pHistory);			
		}
	}
}
