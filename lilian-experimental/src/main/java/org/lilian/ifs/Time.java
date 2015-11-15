package org.lilian.ifs;

import static org.lilian.util.Series.series;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.apache.commons.math3.distribution.TDistribution;
import org.data2semantics.platform.Global;
import org.data2semantics.platform.annotation.In;
import org.data2semantics.platform.annotation.Main;
import org.lilian.data.real.MVN;
import org.lilian.data.real.Point;
import org.lilian.data.real.Similitude;
import org.lilian.data.real.fractal.EM;
import org.lilian.data.real.fractal.IFS;
import org.lilian.data.real.fractal.IFSs;
import org.lilian.util.Functions;
import org.lilian.util.Series;

public class Time
{
	
	@In(name="repeats")
	public int repeats;
	
	private Map<Integer, List<Double>> resDataSizeExp = new LinkedHashMap<Integer, List<Double>>();
	private Map<Integer, List<Double>> resDataSizeMax = new LinkedHashMap<Integer, List<Double>>();

	private Map<Integer, List<Double>> resDimensionExp = new LinkedHashMap<Integer, List<Double>>();
	private Map<Integer, List<Double>> resDimensionMax = new LinkedHashMap<Integer, List<Double>>();
	
	private Map<Integer, List<Double>> resDepthExp = new LinkedHashMap<Integer, List<Double>>();
	private Map<Integer, List<Double>> resDepthMax = new LinkedHashMap<Integer, List<Double>>();
	
	@Main()
	public void run()
		throws IOException
	{
		org.lilian.Global.random = new java.util.Random();
		ExecutorService exec = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
		
		for(int dataSize : Series.series(25, 25, 1000))
			for(int rep : series(repeats))
				exec.execute(new DataSizeThread(rep, dataSize));

		for(int dimension : Series.series(1, 1, 50))
			for(int rep : series(repeats))
				exec.execute(new DimensionThread(rep, dimension));
		
		for(int depth : Series.series(1, 10))
			for(int rep : series(repeats))
				exec.execute(new DepthThread(rep, depth));
		
		exec.shutdown();
		while(! exec.isTerminated());
		System.out.println("All threads finished");
		
		Writer out = new BufferedWriter(new FileWriter(new File(Global.getWorkingDir(), "size.csv")));
		for(int size : resDataSizeExp.keySet())
		{
			List<Double> samplesExp = resDataSizeExp.get(size);
			
			double expMean = mean(samplesExp);
			double expErr = error(samplesExp);
			
			List<Double> samplesMax = resDataSizeMax.get(size);
			
			double maxMean = mean(samplesMax);
			double maxErr = error(samplesMax);
			
			out.write(((double)size) +  ", " + expMean + ", " + expErr + ", " + maxMean + ", " + maxErr + "\n");
		}
		out.close();
		
		out = new BufferedWriter(new FileWriter(new File(Global.getWorkingDir(), "dimension.csv")));
		for(int size : resDimensionExp.keySet())
		{
			List<Double> samplesExp = resDimensionExp.get(size);
			
			double expMean = mean(samplesExp);
			double expErr = error(samplesExp);
			
			List<Double> samplesMax = resDimensionMax.get(size);
			
			double maxMean = mean(samplesMax);
			double maxErr = error(samplesMax);
			
			out.write(((double)size) +  ", " + expMean + ", " + expErr + ", " + maxMean + ", " + maxErr + "\n");
		}
		out.close();
		
		out = new BufferedWriter(new FileWriter(new File(Global.getWorkingDir(), "depth.csv")));
		for(int size : resDepthExp.keySet())
		{
			List<Double> samplesExp = resDepthExp.get(size);
			
			double expMean = mean(samplesExp);
			double expErr = error(samplesExp);
			
			List<Double> samplesMax = resDepthMax.get(size);
			
			double maxMean = mean(samplesMax);
			double maxErr = error(samplesMax);
			
			out.write(((double)size) +  ", " + expMean + ", " + expErr + ", " + maxMean + ", " + maxErr + "\n");
		}
		out.close();
		
		try
		{
			org.data2semantics.platform.util.Functions.python(Global.getWorkingDir(), "fractals/timing.py");
		} catch (Exception e) {
			System.out.println("Failed to run plot script. " + e);
		}
	}

	private static double mean(List<Double> values)
	{
		double sum = 0.0;
		for(double v : values)
			sum += v;
		
		return sum/values.size();
	}

	private static double error(List<Double> values)
	{
		double mean = mean(values);
		
		double sum = 0.0;
		for(double v : values)
		{
			double diff = mean - v;
			sum += diff *diff;
		}
		
		double std = Math.sqrt(sum/(values.size() -1));
		double error = std /  Math.sqrt(values.size()); 
		
		return error;
	}
	
	public synchronized void doneDataSize(int size, double timeExp, double timeMax)
	{
		if(! resDataSizeExp.containsKey(size))
			resDataSizeExp.put(size, new ArrayList<Double>(repeats));
		
		resDataSizeExp.get(size).add(timeExp);
		
		if(! resDataSizeMax.containsKey(size))
			resDataSizeMax.put(size, new ArrayList<Double>(repeats));
		
		resDataSizeMax.get(size).add(timeMax);	
	}
	
	public synchronized void doneDimension(int dimension, double timeExp, double timeMax)
	{
		if(! resDimensionExp.containsKey(dimension))
			resDimensionExp.put(dimension, new ArrayList<Double>(repeats));
		
		resDimensionExp.get(dimension).add(timeExp);
		
		if(! resDimensionMax.containsKey(dimension))
			resDimensionMax.put(dimension, new ArrayList<Double>(repeats));
		
		resDimensionMax.get(dimension).add(timeMax);
	}
	
	public synchronized void doneDepth(int depth, double timeExp, double timeMax)
	{
		if(! resDepthExp.containsKey(depth))
			resDepthExp.put(depth, new ArrayList<Double>(repeats));
		
		resDepthExp.get(depth).add(timeExp);
		
		if(! resDepthMax.containsKey(depth))
			resDepthMax.put(depth, new ArrayList<Double>(repeats));
		
		resDepthMax.get(depth).add(timeMax);
	}
	
	
	private class DataSizeThread extends Thread
	{
		private int rep;
		private int dataSize;
		
		public DataSizeThread(int rep, int dataSize)
		{
			this.rep = rep;
			this.dataSize = dataSize;
		}

		@Override
		public void run()
		{
			List<Point> data = new MVN(3).generate(dataSize);
			
			IFS<Similitude> initial = IFSs.initialSphere(3, 3, 1.0, 0.5, true);
			
			EM em = new EM(data, -1, initial, 5, false);
			
			long t0 = System.currentTimeMillis();
			em.expectation(false);
			long tExp = System.currentTimeMillis() - t0;
			
			t0 = System.currentTimeMillis();
			em.maximization(1.0, 1.0, 1.0);
			long tMax = System.currentTimeMillis() - t0;
			
			doneDataSize(dataSize, tExp/1000.0, tMax/1000.0);
		}
	}
	
	private class DimensionThread extends Thread
	{
		private int rep;
		private int dimension;
		
		public DimensionThread(int rep, int dimension)
		{
			this.rep = rep;
			this.dimension = dimension;
		}

		@Override
		public void run()
		{
			List<Point> data = new MVN(dimension).generate(250);
			
			IFS<Similitude> initial = IFSs.initialSphere(dimension, 3, 1.0, 0.5, true);
			
			EM em = new EM(data, -1, initial, 5, false);
			
			long t0 = System.currentTimeMillis();
			em.expectation(false);
			long tExp = System.currentTimeMillis() - t0;
			
			t0 = System.currentTimeMillis();
			em.maximization(1.0, 1.0, 1.0);
			long tMax = System.currentTimeMillis() - t0;
			
			doneDimension(dimension, tExp/1000.0, tMax/1000.0);
		}
	}
	
	
	private class DepthThread extends Thread
	{
		private int rep;
		private int depth;
		
		public DepthThread(int rep, int depth)
		{
			this.rep = rep;
			this.depth = depth;
		}

		@Override
		public void run()
		{
			List<Point> data = new MVN(3).generate(250);
			
			IFS<Similitude> initial = IFSs.initialSphere(3, 3, 1.0, 0.5, true);
			
			EM em = new EM(data, -1, initial, depth, false);
			
			long t0 = System.currentTimeMillis();
			em.expectation(false);
			long tExp = System.currentTimeMillis() - t0;
			
			t0 = System.currentTimeMillis();
			em.maximization(1.0, 1.0, 1.0);
			long tMax = System.currentTimeMillis() - t0;
			
			doneDepth(depth, tExp/1000.0, tMax/1000.0);
		}
	}
}
