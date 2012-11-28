package org.lilian.data.real.ds;

import static org.lilian.util.Functions.tic;
import static org.lilian.util.Functions.toc;
import static org.lilian.util.Series.series;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.List;

import javax.imageio.ImageIO;

import org.lilian.data.real.Draw;
import org.lilian.data.real.Generators;
import org.lilian.data.real.MVN;
import org.lilian.data.real.Map;
import org.lilian.data.real.Maps;
import org.lilian.data.real.Point;
import org.lilian.experiment.AbstractExperiment;
import org.lilian.experiment.Parameter;
import org.lilian.experiment.Result;
import org.lilian.neural.Activations;
import org.lilian.neural.ThreeLayer;
import org.lilian.util.Functions;
import org.lilian.util.Series;

public class DSEM extends AbstractExperiment
{
	public static final boolean CHEAT = false;

	private static final int SAMPLES = 10000;
	private static final int RES = 400;
	private List<Point> data;
	private double sigma;
	private int numSources;
	private int hidden;
	private double learningRate;
	private boolean reset;
	private int emSampleSize;
	private int generations;
	private int epochs;
	private double var;
	protected double branchingVariance;
	protected int sampleSize;
	protected int beamWidth;
	
	public EM em;
	public ThreeLayer map = null;
	
	public DSEM(
			@Parameter(name="data")
				List<Point> data, 
			@Parameter(name="map")
				ThreeLayer map, 
			@Parameter(name="sigma")
				double sigma, 
			@Parameter(name="num sources")
				int numSources, 
			@Parameter(name="learning rate")
				double learningRate,
			@Parameter(name="reset")
				boolean reset, 
			@Parameter(name="em sample size")
				int emSampleSize,
			@Parameter(name="generations")
				int generations, 
			@Parameter(name="epochs")
				int epochs,
			@Parameter(name="init var")
				double var,
			@Parameter(name="beam width")
				int beamWidth,
			@Parameter(name="branching variance")
				double branchingVariance,
			@Parameter(name="sample size")
				int sampleSize)
	{
		this(data, sigma, numSources, map.hiddenSize(), learningRate, reset, emSampleSize, generations, epochs, var, beamWidth, branchingVariance, sampleSize);
		this.map = map;
	}	
	
	public DSEM(
		@Parameter(name="data")
		List<Point> data, 
		@Parameter(name="sigma")
			double sigma, 
		@Parameter(name="num sources")
			int numSources, 
		@Parameter(name="hidden")
			int hidden,
		@Parameter(name="learning rate")
			double learningRate,
		@Parameter(name="reset")
			boolean reset, 
		@Parameter(name="em sample size")
			int emSampleSize,
		@Parameter(name="generations")
			int generations, 
		@Parameter(name="epochs")
			int epochs,
		@Parameter(name="init var")
			double var,
		@Parameter(name="beam width")
			int beamWidth,
		@Parameter(name="branching variance")
			double branchingVariance,
		@Parameter(name="sample size")
			int sampleSize)
	{
		this.data = data;
		this.sigma = sigma;
		this.numSources = numSources;
		this.hidden = hidden;
		this.learningRate = learningRate;
		this.reset = reset;
		this.emSampleSize = emSampleSize;
		this.generations = generations;
		this.epochs = epochs;
		this.var = var;
		this.branchingVariance = branchingVariance;
		this.beamWidth = beamWidth;
		this.sampleSize = sampleSize;
	}

	@Override
	protected void setup()
	{
		if(map == null)
			if(CHEAT)
			{
				double rate = 0.001;
				Map target = Maps.henon();
				map = ThreeLayer.random(2, hidden, 0.5, Activations.sigmoid());
				
				List<Point> from = new MVN(2).generate(10000),
				            to = target.map(from);
				
				map.train(from, to, rate, 500);
						
			} else
				map = ThreeLayer.random(
						data.get(0).dimensionality(), 
						hidden, var, Activations.sigmoid());
		
		if(beamWidth < 1)
			em = new EM(data, sigma, numSources, map);
		else
			em = new BranchingEM(
				data, sigma, numSources, map, 
				branchingVariance, beamWidth, sampleSize);
		
		BufferedImage image = Draw.draw(data, RES, true);
		try
		{
			ImageIO.write(image, "PNG", new File(dir, "data.png"));
		} catch (IOException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	@Override
	protected void body()
	{
		for(int generation : series(generations))
		{
			tic();
						
			File gen = new File(dir, "generations/");
			gen.mkdirs();
			
			BufferedImage image = Draw.draw(
					Generators.fromMap(em.map(), new Point(em.dimension())),
					SAMPLES, RES, true);
			
			try
			{
				ImageIO.write(image, "PNG", new File(gen, String.format("gen%04d.png", generation)));
			} catch (IOException e)
			{
				e.printStackTrace();
			}
			
			em.iterate(emSampleSize, epochs, learningRate, reset);
			
			logger.info("Finished generation " + generation + " in " + toc() + " seconds.");
		}

	}
	
	@Result(name="data")
	public BufferedImage data()
	{
		return Draw.draw(data, 500, true);
	}

}
