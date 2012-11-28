package org.lilian.experiment;

import java.util.Arrays;
import java.util.List;

import org.lilian.Global;
import org.lilian.data.real.MappedList;
import org.lilian.data.real.PCA;
import org.lilian.data.real.Point;
import org.lilian.data.real.Similitude;
import org.lilian.data.real.classification.Classification;
import org.lilian.data.real.classification.Classified;
import org.lilian.data.real.fractal.IFS;
import org.lilian.data.real.fractal.IFSs;

/**
 * Tests IFSs as dimension redution systems.
 * 
 * We fit a four-component IFS to a given dataset and use these components to 
 * map the data back to two dimension. We compare the result visually with a 
 * straightforward PCA dimension reduction.
 *  
 * @author Peter
 *
 */
public class IFSDimReduce extends AbstractExperiment
{
	private Classified<Point> data;
	private int generations;
	private int learningDepth;
	private int codingDepth;
	private int learnSampleSize;
	private int evaluationSampleSize;
	
	public Classified<Point> reducedPCA;
	public Classified<Point> reducedIFS;
	
	public IFSDimReduce(
			@Parameter(name="data", description="") Classified<Point> data, 
			@Parameter(name="generations", description="") int generations,
			@Parameter(name="learning depth", description="") int learningDepth,
			@Parameter(name="coding depth", description="") int codingDepth, 
			@Parameter(name="learning sample size", description="") int learnSampleSize,
			@Parameter(name="evaluation sample size", description="") int evaluationSampleSize,
			@Parameter(name="data sample", description="use a subsample of the data") int dataSample)
	{
		this.data = Classification.sample(data, dataSample);
		this.generations = generations;
		this.learningDepth = learningDepth;
		this.codingDepth = codingDepth;
		this.learnSampleSize = learnSampleSize;
		this.evaluationSampleSize = evaluationSampleSize;
	}
	
	public IFSDimReduce(
			@Parameter(name="data", description="") Classified<Point> data, 
			@Parameter(name="generations", description="") int generations,
			@Parameter(name="learning depth", description="") int learningDepth,
			@Parameter(name="coding depth", description="") int codingDepth, 
			@Parameter(name="learning sample size", description="") int learnSampleSize,
			@Parameter(name="evaluation sample size", description="") int evaluationSampleSize)
	{
		this.data = data;
		this.generations = generations;
		this.learningDepth = learningDepth;
		this.codingDepth = codingDepth;
		this.learnSampleSize = learnSampleSize;
		this.evaluationSampleSize = evaluationSampleSize;
	}

	@Override
	protected void setup()
	{

	}

	@Override
	protected void body()
	{
		logger.info("Computing PCA model");
		
		PCA pca = new PCA(data);
		List<Point> reducedPCAPoints = pca.simplify(2); 

		reducedPCA = Classification.combine(reducedPCAPoints, data.classes());
		
		logger.info("Computing IFS model");
		
		IFSModelEM emExperiment = new IFSModelEM(
				data, 0.0, learningDepth, generations, 4, learnSampleSize, 
				evaluationSampleSize, -1, false, "sphere", 0.01); 
				
				
		Environment.current().child(emExperiment);
		
		List<Point> centeredData = new MappedList(data, emExperiment.map());
		IFS<Similitude> ifs = emExperiment.bestModel();
		List<List<Integer>> codes = IFS.codes(ifs, centeredData, codingDepth);
		
		IFS<Similitude> back = IFSs.square();
		List<Point> reducedIFSPoints = IFS.endpoints(back, codes);
		
		reducedIFS = Classification.combine(reducedIFSPoints, data.classes());
	}
	
	@Result(name="reduced PCA")
	public Classified<Point> reducedPCA()
	{
		return reducedPCA;
	}

	@Result(name="reduced IFS")
	public Classified<Point> reducedIFS()
	{
		return reducedIFS;
	}

	@Override
	public List<String> scripts()
	{
		return Arrays.asList("classified.plot.py");
	}
	
	
}
