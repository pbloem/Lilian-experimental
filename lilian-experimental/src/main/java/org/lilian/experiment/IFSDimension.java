package org.lilian.experiment;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.lilian.Global;
import org.lilian.data.dimension.Takens;
import org.lilian.data.real.Datasets;
import org.lilian.data.real.Point;
import org.lilian.data.real.Similitude;
import org.lilian.data.real.fractal.IFS;
import org.lilian.util.Series;
import org.lilian.util.distance.Distance;
import org.lilian.util.distance.EuclideanDistance;
import org.lilian.util.distance.HausdorffDistance;

/**
 * Test the hypothesis that IFS models scale their dimension to that of the
 * dataset.
 * 
 * @author Peter
 *
 */
public class IFSDimension extends AbstractExperiment
{
	@Reportable
	public static final boolean SAMPLE_DEEP = true; 
	
	Distance<Point> metric = new EuclideanDistance();
	
	private int generations;
	private int depth;
	private int components;
	private int emSampleSize;
	private int trainSampleSize;
	private List<Point> data;
	private int dimensionSample;
	private int bootstraps;
	private double spanningPointsVariance;
	
	public @State double modelDimension;
	public @State double modelDimUncertainty;
	public @State double dataDimension;
	public @State double dataDimUncertainty;
	public @State double modelFit;
	public @State double modelFitUncertainty;
	
	public IFSDimension(
			@Parameter(name="generations", description="") int generations, 
			@Parameter(name="depth", description="") int depth, 
			@Parameter(name="components", description="") int components,
			@Parameter(name="em sample size", description="") int emSampleSize,
			@Parameter(name="train sample size", description="") int trainSampleSize, 
			@Parameter(name="data", description="") List<Point> data,
			@Parameter(name="dimension sample size", description="") int dimensionSample,
			@Parameter(name="bootstraps", description="") int bootstraps,
			@Parameter(name="spanning points variance") double spanningPointsVariance)
	{
		this.generations = generations;
		this.depth = depth;
		this.components = components;
		this.emSampleSize = emSampleSize;
		this.trainSampleSize = trainSampleSize;
		this.data = data;
		this.dimensionSample = dimensionSample;
		this.bootstraps = bootstraps;
		this.spanningPointsVariance = spanningPointsVariance;
	}

	@Override
	protected void setup()
	{
		logger.info("Data size: " + data.size());

		Set<Point> set = new HashSet<Point>(data);
		Global.log().info("Uniquification removed " + (data.size()-set.size()) + " points out of " + data.size());
		data = new ArrayList<Point>(set);
	}

	@Override
	protected void body()
	{	
		IFSModelEM em = new IFSModelEM(
				data, 0.0, depth, generations, components, emSampleSize, 
				trainSampleSize, -1, false, "sphere", spanningPointsVariance);
				
				
		Environment.current().child(em);
		
		IFS<Similitude> model = em.model();
		
		List<Point> modelDraw = 
				SAMPLE_DEEP ? 
						(model.generator(depth).generate(dimensionSample == -1 ? data.size() : dimensionSample)):
						(model.generator(depth).generate(dimensionSample == -1 ? data.size() : dimensionSample));
		
		List<Point> dataDraw = dimensionSample == -1 ?
				data :
				Datasets.sampleWithoutReplacement(data, dimensionSample);
		
		List<Double> modelDistances = Takens.distances(modelDraw, metric);
		List<Double> dataDistances = Takens.distances(dataDraw, metric); 

		modelDimension = Takens.fit(modelDistances, true).fit().dimension();
		dataDimension = Takens.fit(dataDistances, true).fit().dimension();
		
		logger.info("Calculating uncertainties.");
		modelDimUncertainty = Takens.uncertainties(modelDistances, bootstraps).alpha();
		dataDimUncertainty = Takens.uncertainties(dataDistances, bootstraps).alpha();
		
		logger.info("Calculating fit");
		List<Double> values = new ArrayList<Double>(bootstraps);
		HausdorffDistance<Point> distance = new HausdorffDistance<Point>(new EuclideanDistance());
		for(int i : Series.series(bootstraps))
		{
			modelDraw = model.generator(depth).generate(trainSampleSize);
			dataDraw = Datasets.sampleWithoutReplacement(data, trainSampleSize);
			
			values.add(distance.distance(modelDraw, dataDraw));
		}
		
		modelFit = Tools.mean(values);
		modelFitUncertainty = Tools.standardDeviation(values);
		
	}
	
	@Result(name="model dimension")
	public double modelDimension()
	{
		return modelDimension;
	}
	
	@Result(name="data dimension")
	public double dataDimension()
	{
		return dataDimension;
	}
	
	@Result(name="model dimension uncertainty")
	public double modelDimUncertainty()
	{
		return modelDimUncertainty;
	}
	
	@Result(name="data dimension uncertainty")
	public double dataDimUncertainty()
	{
		return dataDimUncertainty;
	}
	
	@Result(name="data size")
	public double dataSize()
	{
		return data.size();
	}
	
	@Result(name="embedding dimension")
	public double embeddingDim()
	{
		return data.get(0).dimensionality();
	}
	
	@Result(name="model fit")
	public double modelFit()
	{
		return modelFit;
	}
	
	@Result(name="model fit uncertainty")
	public double modelFitUncertainty()
	{
		return modelFitUncertainty;
	}
}
