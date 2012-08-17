package org.lilian.experiment;

import static org.lilian.data.real.Draw.toPixel;
import static org.lilian.util.Series.series;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import javax.imageio.ImageIO;

import org.lilian.Global;
import org.lilian.data.real.AffineMap;
import org.lilian.data.real.Datasets;
import org.lilian.data.real.Draw;
import org.lilian.data.real.Generator;
import org.lilian.data.real.MVN;
import org.lilian.data.real.Map;
import org.lilian.data.real.MappedList;
import org.lilian.data.real.Maps;
import org.lilian.data.real.Point;
import org.lilian.data.real.Similitude;
import org.lilian.data.real.classification.Classification;
import org.lilian.data.real.classification.Classified;
import org.lilian.data.real.fractal.IFS;
import org.lilian.data.real.fractal.IFSTarget;
import org.lilian.data.real.fractal.IFSs;
import org.lilian.data.real.fractal.em.EM;
import org.lilian.search.Builder;
import org.lilian.search.Parametrizable;
import org.lilian.search.evo.ES;
import org.lilian.search.evo.Target;
import org.lilian.util.Functions;
import org.lilian.util.Series;
import org.lilian.util.distance.Distance;
import org.lilian.util.distance.HausdorffDistance;
import org.lilian.util.distance.SquaredEuclideanDistance;

public class FractalEM extends AbstractExperiment
{
	
	private static double[] xrange = new double[]{-2.1333, 2.1333};
	private static double[] yrange = new double[]{-1.2, 1.2};	
	
	@Reportable
	private static final double VAR = 0.6;

	@Reportable
	private static final double RADIUS = 0.7;

	@Reportable
	private static final double SCALE = 0.5;

	@Reportable
	private static final double IDENTITY_INIT_VAR = 0.1;

	protected List<Point> trainingData;
	protected List<Point> testData;		
	protected int depth;
	protected int generations;
	protected int components;
	protected int dim;
	protected int distSampleSize;
	protected boolean considerVariance;
	protected int beamWidth;
	protected boolean deepening;
	protected boolean greedy;
	protected int sampleSize;
	protected double threshold;	
	protected boolean highQuality;
	protected String initStrategy;
	protected double noise;
	protected boolean centerData;
	
	/**
	 * State information
	 */
	public @State int currentGeneration;
	public @State EM em;
	public @State List<Double> scores;
	public @State IFS<Similitude> bestModel;
	public @State IFS<Similitude> bestLikelihoodModel;
	public @State AffineMap map;
	public @State MVN basis; 
	
	private Distance<List<Point>> distance = new HausdorffDistance<Point>(new SquaredEuclideanDistance());

	private double bestDistance = Double.POSITIVE_INFINITY;
	private double bestLikelihood = Double.NEGATIVE_INFINITY;

	public FractalEM(
			@Parameter(name="data") 				
				List<Point> data, 
			@Parameter(name="test ratio", description="What percentage of the data to use for testing")
				double testRatio,				
			@Parameter(name="depth") 				
				int depth, 
			@Parameter(name="generations") 			
				int generations,
			@Parameter(name="number of components")
				int components, 
			@Parameter(name="dimension") 			
				int dim,
			@Parameter(name="distribution sample size") 
				int distSampleSize,
			@Parameter(name="consider variance")	
				boolean considerVariance,
			@Parameter(name="beam width", description="Beam width to use when searching for codes")
				int beamWidth,
			@Parameter(name="gradual deepening", description="If true, depth is increased if quality stalls")
				boolean deepening,
			@Parameter(name="greedy", description="If true, the algorithm only moves to the next model if it has lower training error")
				boolean greedy,
			@Parameter(name="test sample size", description="The sample size for evaluating the model")
				int sampleSize,
			@Parameter(name="deepening threshold", description="The model will increse depth if successive generations don't improve by this rate")
				double threshold,
			@Parameter(name="high quality", description="true: full HD 10E7, iterations, false: 1/16th HD, 10E4 iterations")
				boolean highQuality,
			@Parameter(name="init strategy", description="What method to use to initialize the EM algorithm (random, spread, sphere, points, identity)")
				String initStrategy,
			@Parameter(name="noise", description="Gaussian noise added to the model after each iteration.")
				double noise,
			@Parameter(name="center data")
				boolean centerData
			)
	{
	
		List<Point> dataCopy = new ArrayList<Point>(data);
		
		this.trainingData = new ArrayList<Point>(data.size());
		this.testData = new ArrayList<Point>(data.size());
		
		for(int i : series((int)(data.size() * testRatio)))
		{
			int draw = Global.random.nextInt(dataCopy.size());
			testData.add(dataCopy.remove(draw));
		}
		
		trainingData.addAll(dataCopy);

		this.depth = depth;
		this.generations = generations;
		this.components = components;
		this.dim = dim;
		this.distSampleSize = distSampleSize;
		this.considerVariance = considerVariance;
		this.beamWidth = beamWidth;
		
		this.deepening = deepening;
		this.greedy = greedy;
		this.sampleSize = sampleSize;
		this.threshold = threshold;
		
		this.highQuality = highQuality;
		this.initStrategy = initStrategy;
		
		this.noise = noise;
		this.centerData = centerData;
	}
	
	private int depth()
	{
		if(!deepening)
			return depth;
		
		double v = currentGeneration / (double) generations;
		int vint = (int)(Math.floor(v) + 1.0);
		return vint <= depth ?	vint : depth;			
	}
	
	@Override
	protected void body()
	{
		Functions.tic();		
		while(currentGeneration < generations)
		{
			double d;
			
			if(sampleSize != -1)
				d = distance.distance(
					Datasets.sample(trainingData, sampleSize),
					em.model().generator(depth, basis).generate(sampleSize));
			else
				d = distance.distance(trainingData, em.model().generator().generate(trainingData.size()));
				
			scores.add(d);
			if(bestDistance > d)
			{
				bestDistance = d;
				bestModel = em.model();
			}
			
			double ll = 0.0;
			for(Point p : Datasets.sample(testData, sampleSize))
				ll += Math.log(IFS.density(em.model(), p, depth));
				
			if(bestLikelihood < ll)
			{
				bestLikelihood = ll;
				bestLikelihoodModel = em.model();
			}

			if(true)
			{
				if(dim == 2)
					write(em.model, dir, String.format("generation%04d", currentGeneration));
				logger.info("generation " + currentGeneration + ": " + Functions.toc() + " seconds.");
				Functions.tic();				
				save();
			}	
			
			logger.info("Scores (" + currentGeneration + ") : " + scores());
			logger.info("Best HD (" + currentGeneration + ") : " + bestScore());
			logger.info("Best LL (" + currentGeneration + ") : " + bestLikelihood());

			
			logger.info("Parameters   (" + currentGeneration + ") : " + em.model().parameters());			
			currentGeneration++;

			em.distributePoints(distSampleSize, depth(), beamWidth);
			em.findIFS(greedy, noise);
		}
		

	}
	
	public void setup()
	{		
		currentGeneration = 0;
		
		scores = new ArrayList<Double>(generations);
		
		map = centerData ? Maps.centered(trainingData) : AffineMap.identity(dim);
		trainingData = new MappedList(trainingData, map);	
		
		logger.info("Data size: " + trainingData.size());
		
		BufferedImage image = Draw.draw(trainingData, xrange, yrange, 1920, 1080, true, false);

		basis = MVN.findSpherical(trainingData);
		logger.info("basis: " + basis);
		
		try
		{
			ImageIO.write(image, "PNG", new File(dir, "data.png"));
		} catch (IOException e)
		{
			throw new RuntimeException(e);
		}
		
		IFS<Similitude> model = null;
		if(initStrategy.toLowerCase().equals("random"))
			model = EM.initialRandom(dim, components, VAR);
		else if(initStrategy.toLowerCase().equals("sphere"))
			model = EM.initialSphere(dim, components, RADIUS, SCALE);
		else if(initStrategy.toLowerCase().equals("spread"))
			model = EM.initialSpread(dim, components, RADIUS, SCALE);
		else if(initStrategy.toLowerCase().equals("points"))
			model = EM.initialPoints(SCALE, Datasets.sample(trainingData, components));
		else if(initStrategy.toLowerCase().equals("identity"))
			model = EM.initialIdentity(dim, components, IDENTITY_INIT_VAR);
		
		if(model == null)
			throw new IllegalArgumentException("Initialization strategy \""+initStrategy+"\" not recognized.");
				
		em = new EM(model, trainingData, considerVariance, greedy ? new IFSTarget<Similitude>(sampleSize, trainingData) : null);
		em.distributePoints(distSampleSize, depth(), beamWidth);
	}
	
	@Result(name = "Best training score", description="The best (lowest) training score over all generations.")
	public double bestScore()
	{
		return bestDistance;
	}
	
	@Result(name = "Test score", description="The score on the test data for the model with the lowest training score.")
	public double testScore()
	{
		double d;
		if(sampleSize != -1)
			d = distance.distance(
				Datasets.sample(testData, sampleSize),
				em.model().generator().generate(sampleSize));
		else
			d = distance.distance(testData, em.model().generator().generate(testData.size()));
		
		return d;
	}
	
	
	@Result(name = "Best likelihood", description="The best (highet) likelihood over all generations.")
	public double bestLikelihood()
	{
		return bestLikelihood;
	}
	
	@Result (name ="Below 0.05", description="1 if the best score is below 0.05, 0 otherwise")
	public double below()
	{
		return bestDistance < 0.05 ? 1 : 0;
	}

	@Result(name = "Scores", description="The scores over successive generations.")
	public List<Double> scores()
	{
		return scores;
	}
	
	@Result(name = "Ratios", description="Ratio of the current score divided by the last score")
	public List<Double> scoreRatios()
	{
		List<Double> ratios = new ArrayList<Double>(scores().size());
		
		for(int i = 1; i < scores().size(); i++)
			ratios.add(scores().get(i) / scores.get(i-1));
		
		return ratios;
	}

	/**
	 * Print out the current best approximation at full HD resolution.
	 * @param ifs
	 * @param dir
	 * @param name
	 */
	private <M extends Map & Parametrizable> void write(IFS<M> ifs, File dir, String name)
	{		
		int div = highQuality ? 1 : 16;
		int its = highQuality ? (int)100000 : 1000;
		
		BufferedImage image = Draw.draw(ifs, its, xrange, yrange, 1920/div, 1080/div, true, depth, basis);
		try
		{
			ImageIO.write(image, "PNG", new File(dir, name + ".png") );
		} catch (IOException e)
		{
			e.printStackTrace();
		}
	}
	
	
	public static BufferedImage drawMap(List<Point> from, List<Point> to)
	{
		double[] xrange = {-1.0, 1.0};
		double[] yrange = {-1.0, 1.0};
		int res = 1000;
		boolean log = false;
		
		double 	xDelta = xrange[1] - xrange[0],
				yDelta = yrange[1] - yrange[0];
		
		double maxDelta = Math.max(xDelta, yDelta); 		
		double minDelta = Math.min(xDelta, yDelta);
		
		double step = minDelta/(double) res;
		
		int xRes = (int) (xDelta / step);
		int yRes = (int) (yDelta / step);

		float max = Float.NEGATIVE_INFINITY;
		float min = 0.0f;		
		
		float[][] matrixFrom = new float[yRes][];
		float[][] matrixTo = new float[yRes][];

		for(int x = 0; x < xRes; x++)
		{
			matrixFrom[x] = new float[yRes];
			matrixTo[x] = new float[yRes];

			for(int y = 0; y < yRes; y++)
			{
				matrixFrom[x][y] = 0.0f;
				matrixTo[x][y] = 0.0f;
			}
		}
		
		BufferedImage image = 
				new BufferedImage(xRes, yRes, BufferedImage.TYPE_INT_RGB);
		
		Graphics2D g = image.createGraphics();
		for(int i = 0; i < from.size(); i++)
		{
			Point pointFrom = from.get(i);
			Point pointTo = to.get(i);
						
			int xf = toPixel(pointFrom.get(0), xRes, xrange[0], xrange[1]); 
			int yf = toPixel(pointFrom.get(1), yRes, yrange[0], yrange[1]);

			int xt = toPixel(pointTo.get(0), xRes, xrange[0], xrange[1]); 
			int yt = toPixel(pointTo.get(1), yRes, yrange[0], yrange[1]);			
			
			g.setColor(Color.WHITE);
			g.setStroke(new BasicStroke(2.0f));	
			g.drawLine(xf, yf, xt, yt);
			
			g.setColor(Color.BLUE);
			g.fillOval(xf-2, yf-2, 4, 4);
			
			g.setColor(Color.GREEN);
			g.fillOval(xt-1, yt-1, 2, 2);
			
		}
		g.dispose();
		
		return image;
	}

	public IFS<Similitude> model()
	{
		return em.model();
	}

	public IFS<Similitude> bestModel()
	{
		return bestModel;
	}
	
	public IFS<Similitude> bestLikelihoodModel()
	{
		return bestLikelihoodModel;
	}
	
	public MVN basis()
	{
		return basis;
	}
	
	/**
	 * The map that centers the data
	 * @return
	 */
	public AffineMap map()
	{
		return map;
	}
}
