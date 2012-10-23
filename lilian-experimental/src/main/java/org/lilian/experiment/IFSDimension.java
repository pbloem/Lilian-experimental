package org.lilian.experiment;

import java.util.List;

import org.lilian.data.dimension.Takens;
import org.lilian.data.real.Datasets;
import org.lilian.data.real.Point;
import org.lilian.data.real.Similitude;
import org.lilian.data.real.fractal.IFS;
import org.lilian.util.distance.Distance;
import org.lilian.util.distance.EuclideanDistance;

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
	
	public @State double modelDimension;
	public @State double dataDimension;

	
	public IFSDimension(
			@Parameter(name="generations", description="") int generations, 
			@Parameter(name="depth", description="") int depth, 
			@Parameter(name="components", description="") int components,
			@Parameter(name="dist sample size", description="") int distSampleSize,
			@Parameter(name="test sample size", description="") int testSampleSize, 
			@Parameter(name="data", description="") List<Point> data,
			@Parameter(name="dimension sample size", description="") int dimensionSample,
			@Parameter(name="ks samples", description="") int ksSamples)
	{
		super();
		this.generations = generations;
		this.depth = depth;
		this.components = components;
		this.distSampleSize = distSampleSize;
		this.testSampleSize = testSampleSize;
		this.data = data;
		this.dimensionSample = dimensionSample;
		this.ksSamples = ksSamples;
	}

	@Override
	protected void setup()
	{
		logger.info("Data size: " + data.size());
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
		
		modelDimension = Takens.bigFit(modelDraw, metric).fit(ksSamples).dimension();
		dataDimension = Takens.bigFit(dataDraw, metric).fit(ksSamples).dimension();
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
}
