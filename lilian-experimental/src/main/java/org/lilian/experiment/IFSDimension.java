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
 * @author Peter
 *
 */
public class IFSDimension extends AbstractExperiment
{
	Distance<Point> metric = new EuclideanDistance();
	
	private int generations;
	private int depth;
	private int components;
	private int distSampleSize;
	private int testSampleSize;
	private List<Point> data;
	private int dimensionSample;
	private int ksSamples;
	private int bootstraps;
	
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
			@Parameter(name="dist sample size", description="") int distSampleSize,
			@Parameter(name="test sample size", description="") int testSampleSize, 
			@Parameter(name="data", description="") List<Point> data,
			@Parameter(name="dimension sample size", description="") int dimensionSample,
			@Parameter(name="ks samples", description="") int ksSamples,
			@Parameter(name="bootstraps", description="") int bootstraps)
	{
		this.generations = generations;
		this.depth = depth;
		this.components = components;
		this.distSampleSize = distSampleSize;
		this.testSampleSize = testSampleSize;
		this.data = data;
		this.dimensionSample = dimensionSample;
		this.ksSamples = ksSamples;
		this.bootstraps = bootstraps;
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
		FractalEM em = new FractalEM(
				data, 0.0, depth, generations, components, distSampleSize, 
				true, -1, false, false, testSampleSize, 0.0, false, "sphere", 0.0, true, true);
		
		Environment.current().child(em);
		
		IFS<Similitude> model = em.model();
		List<Point> modelDraw = model.generator(depth).generate(dimensionSample == -1 ? data.size() : dimensionSample);
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
			modelDraw = model.generator(depth).generate(testSampleSize);
			dataDraw = Datasets.sampleWithoutReplacement(data, testSampleSize);
			
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
