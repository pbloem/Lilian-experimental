//package org.lilian.experiment;
//
//import java.util.Arrays;
//import java.util.List;
//
//import org.lilian.Global;
//import org.lilian.data.real.MappedList;
//import org.lilian.data.real.PCA;
//import org.lilian.data.real.PCAEco;
//import org.lilian.data.real.Point;
//import org.lilian.data.real.Similitude;
//import org.lilian.data.real.classification.Classification;
//import org.lilian.data.real.classification.Classified;
//import org.lilian.data.real.fractal.old.IFS;
//import org.lilian.data.real.fractal.old.IFSs;
//
///**
// * Tests IFSs as dimension redution systems.
// * 
// * We fit a four-component IFS to a given dataset and use these components to 
// * map the data back to two dimension. We compare the result visually with a 
// * straightforward PCA dimension reduction.
// *  
// * @author Peter
// *
// */
//public class IFSDimReduceHigh extends AbstractExperiment
//{
//	private Classified<Point> data;
//	private int generations;
//	private int learningDepth;
//	private int codingDepth;
//	private int learnSampleSize;
//	private int intDim;
//	private int evaluationSampleSize;
//	private boolean deepening;
//	
//	public Classified<Point> reducedPCA;
//	public Classified<Point> reducedIFS;
//	
//	public IFSDimReduceHigh(
//			@Parameter(name="data", description="") 
//				Classified<Point> data, 
//			@Parameter(name="generations", description="") 
//				int generations,
//			@Parameter(name="learning depth", description="") 
//				int learningDepth,
//			@Parameter(name="coding depth", description="") 
//				int codingDepth, 
//			@Parameter(name="learning sample size", description="") 
//				int learnSampleSize,
//			@Parameter(name="evaluation sample size", description="") 
//				int evaluationSampleSize,
//			@Parameter(name="inter dim", description="Intermediary dimension. The dataset is reduced to this dimension by PCA before the IFS method is applied")
//				int intDim,
//			@Parameter(name="data sample", description="use a subsample of the data") 
//				int dataSample,
//			@Parameter(name="deepening", description="If true, the algorithm starts at depth 1 and increases linearly to the target depth")
//				boolean deepening)
//	{
//		this.data = Classification.sample(data, dataSample);
//		this.generations = generations;
//		this.learningDepth = learningDepth;
//		this.codingDepth = codingDepth;
//		this.learnSampleSize = learnSampleSize;
//		this.intDim = intDim;
//		this.evaluationSampleSize = evaluationSampleSize;
//		this.deepening = deepening;
//	}
//	
//	@Override
//	protected void setup()
//	{
//
//	}
//
//	@Override
//	protected void body()
//	{
//		logger.info("Computing PCA model");
//		
//		PCAEco pca = new PCAEco(data);
//		
//		List<Point> reducedPCAPoints = pca.simplify(2); 
//
//		reducedPCA = Classification.combine(reducedPCAPoints, data.classes());
//		
//		logger.info("Computing IFS model");
//		
//		List<Point> subData = pca.simplify(intDim);
//		
//		IFSModelEM emExperiment = new IFSModelEM(
//				data, 0.0, learningDepth, generations, 4, learnSampleSize, 
//				evaluationSampleSize, -1, false, "sphere", 0.01, "hausdorff",
//				deepening); 
//				
//		
//		Environment.current().child(emExperiment);
//		
//		List<Point> centeredData = new MappedList(subData, emExperiment.map());
//		IFS<Similitude> ifs = emExperiment.bestModel();
//		List<List<Integer>> codes = IFS.codes(ifs, centeredData, codingDepth);
//		
//		IFS<Similitude> back = IFSs.square();
//		List<Point> reducedIFSPoints = IFS.endpoints(back, codes);
//		
//		reducedIFS = Classification.combine(reducedIFSPoints, data.classes());
//	}
//	
//	@Result(name="reduced PCA")
//	public Classified<Point> reducedPCA()
//	{
//		return reducedPCA;
//	}
//
//	@Result(name="reduced IFS")
//	public Classified<Point> reducedIFS()
//	{
//		return reducedIFS;
//	}
//
//	@Override
//	public List<String> scripts()
//	{
//		return Arrays.asList("classified.plot.py");
//	}
//	
//	
//}
