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
import org.lilian.data.real.Map;
import org.lilian.data.real.MogEM;
import org.lilian.data.real.Point;
import org.lilian.data.real.Similitude;
import org.lilian.data.real.fractal.EM;
import org.lilian.data.real.fractal.IFS;
import org.lilian.data.real.fractal.IFSs;
import org.lilian.search.Parametrizable;

public class IFSHigh
{
	@In(name="data")
	public List<Point> data;
	
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

	public List<Double> ifsLikelihoods = new ArrayList<Double>();
	public List<Double> isoLikelihoods = new ArrayList<Double>();
	public List<Double> mogLikelihoods = new ArrayList<Double>();
	
	@Main
	public void run()
		throws IOException
	{
		org.lilian.Global.random = new java.util.Random();
		
		ExecutorService exec = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());

		for(int rep : series(repeats))
		{
			exec.execute(new RepeatISO(rep, Global.getWorkingDir()));
			exec.execute(new RepeatMOG(rep, Global.getWorkingDir()));
			exec.execute(new RepeatIFS(rep, Global.getWorkingDir()));

		}

		exec.shutdown();
		
		while(! exec.isTerminated());
		
		System.out.println("All threads finished");
		
		Writer out = new BufferedWriter(new FileWriter(new File(Global.getWorkingDir(), "likelihoods.csv")));
		for(int i : series(ifsLikelihoods.size()))
			out.write(ifsLikelihoods.get(i) + ", " + isoLikelihoods.get(i) + ", " + mogLikelihoods.get(i) + "\n"); 
		out.close();
	
		try
		{
			org.data2semantics.platform.util.Functions.python(Global.getWorkingDir(), "fractals/likelihoods.py");
		} catch (Exception e) {
			System.out.println("Failed to run plot script. " + e);
		}
	}
	
	public synchronized void doneIFS(double likelihood)
	{
		ifsLikelihoods.add(likelihood);
	}
	
	public synchronized void doneMOG(double likelihood)
	{
		mogLikelihoods.add(likelihood);
	}

	public synchronized void doneISO(double likelihood)
	{
		isoLikelihoods.add(likelihood);
	}

	private class RepeatIFS extends Thread
	{		
		private int rep;
		private File dir;
		
		public RepeatIFS(int rep, File dir)
		{
			this.rep = rep;
			this.dir = dir;
		}

		@Override
		public void run()
		{
			IFS<Similitude> initial = IFSs.initialSphere(data.get(0).dimensionality(), numComponents, 1.0, 0.5, true);

			EM em = new EM(data, sampleSize, initial, depth, true);
						
			for(int i : series(iterations))
				em.iterate();
						
			doneIFS(-em.logLikelihood(data));	
			
			System.out.println("IFS Thread " + rep + " finished");
		}
	}
	
	private class RepeatISO extends Thread
	{		
		private int rep;
		private File dir;
		
		public RepeatISO(int rep, File dir)
		{
			this.rep = rep;
			this.dir = dir;
		}

		@Override
		public void run()
		{
			IFS<Similitude> initial = IFSs.initialSphere(data.get(0).dimensionality(), numComponents, 1.0, 0.5, true);

			EM em = new EM(data, -1, initial, 1, false);
			em.setDepths(Arrays.asList(0.0, 1.0));
						
			for(int i : series(iterations))
				em.iterate(0.0, 1.0, 0.0);
			
//				try
//				{
//					BufferedImage image = Draw.draw(em.model(), 100000, 500, true, em.depths(), em.post());
//					ImageIO.write(image, "PNG", new File(dir, String.format("iso.%04d.%04d.png", rep, i)));
//				} catch (IOException e)
//				{
//					throw new RuntimeException(e);
//				}
			
						
			doneISO(-em.logLikelihood(data));	
			
			System.out.println("ISO Thread " + rep + " finished");
		}
	}
	
	private class RepeatMOG extends Thread
	{		
		private int rep;
		private File dir;
		
		public RepeatMOG(int rep, File dir)
		{
			this.rep = rep;
			this.dir = dir;
		}

		@Override
		public void run()
		{
			MogEM em = new MogEM(data, numComponents);
			
			for(int i : series(iterations))
				em.iterate();
				
//				try
//				{
//					BufferedImage image = Draw.draw(em.model(), 100000, 500, true);
//					ImageIO.write(image, "PNG", new File(dir, String.format("mog.%04d.%04d.png", rep, i)));
//				} catch (IOException e)
//				{
//					throw new RuntimeException(e);
//				}
			
			
			doneMOG(-em.model().logDensity(data));
			
			System.out.println("MOG Thread " + rep + " finished");
		}
	}
	

	public <M extends Map & Parametrizable> void write(IFS<Similitude> ifs, List<Double> depths, Similitude post, File dir, String name) throws IOException
	{
		int div =  2;
		int its =  100000;
				
		BufferedImage image;
		
		image = Draw.draw(ifs, its, 1000/div, true, depths, post);
		ImageIO.write(image, "PNG", new File(dir, name+".png"));
		
		image = Draw.draw(ifs.generator(), its, 1000/div, true, post);
		ImageIO.write(image, "PNG", new File(dir, name+".deep.png"));
	}	
}
