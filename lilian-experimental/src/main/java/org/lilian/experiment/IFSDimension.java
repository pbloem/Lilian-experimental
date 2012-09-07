package org.lilian.experiment;

import java.util.List;

import org.lilian.data.dimension.Takens;
import org.lilian.data.real.Datasets;
import org.lilian.data.real.Point;
import org.lilian.data.real.Similitude;
import org.lilian.data.real.fractal.IFS;
import org.lilian.util.distance.Distance;
import org.lilian.util.distance.EuclideanDistance;

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
	
	public @State double modelDimension;
	public @State double dataDimension;

	
	public IFSDimension(
			@Parameter(name="generations", description="") int generations, 
			@Parameter(name="depth", description="") int depth, 
			@Parameter(name="components", description="") int components,
			@Parameter(name="dist sample size", description="") int distSampleSize,
			@Parameter(name="test sample size", description="") int testSampleSize, 
			@Parameter(name="data", description="") List<Point> data,
			@Parameter(name="dimension sample size", description="") int dimensionSample)
	{
		super();
		this.generations = generations;
		this.depth = depth;
		this.components = components;
		this.distSampleSize = distSampleSize;
		this.testSampleSize = testSampleSize;
		this.data = data;
		this.dimensionSample = dimensionSample;
	}

	@Override
	protected void setup()
	{
	}

	@Override
	protected void body()
	{	
		FractalEM em = new FractalEM(
				data, 0.0, depth, generations, components, distSampleSize, 
				true, -1, false, false, testSampleSize, 0.0, false, "sphere", 0.0, true, true);
		
		Environment.current().child(em);
		
		IFS<Similitude> model = em.model();
		List<Point> modelDraw = model.generator(depth).generate(dimensionSample);
		List<Point> dataDraw = Datasets.sampleWithoutReplacement(data, dimensionSample);
		
		modelDimension = Takens.fit(modelDraw, metric).fit().dimension();
		dataDimension = Takens.fit(dataDraw, metric).fit().dimension();
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
}
