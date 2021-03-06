//package org.lilian.experiment;
//
//import java.io.File;
//import java.io.IOException;
//import java.util.ArrayList;
//import java.util.Arrays;
//import java.util.List;
//
//import org.lilian.data.real.Generators;
//import org.lilian.data.real.Point;
//import org.lilian.util.distance.Distance;
//import org.lilian.util.distance.EuclideanDistance;
//
//public class IFSDimMulti extends AbstractExperiment
//{
//	public static final int SIZE = 1000000;
//	Distance<Point> metric = new EuclideanDistance();
//	
//	private int generations;
//	private List<Integer> depths;
//	private List<Integer> components;
//	private int distSampleSize;
//	private int testSampleSize;
//	private List<String> datasets;
//	private int dimensionSample;
//	private int bootstraps;
//	private double spanningPointsVariance;
//	private boolean deepening;
//	
//	public @State List<IFSDimension> results = new ArrayList<IFSDimension>(); 
//	
//	public IFSDimMulti(
//			@Parameter(name="generations", description="") int generations, 
//			@Parameter(name="depths", description="") List<Integer> depths, 
//			@Parameter(name="components", description="") List<Integer> components,
//			@Parameter(name="dist sample size", description="") int distSampleSize,
//			@Parameter(name="test sample size", description="") int testSampleSize, 
//			@Parameter(name="datasets", description="") List<String> datasets,
//			@Parameter(name="dimension sample size", description="") int dimensionSample,
//			@Parameter(name="bootstraps", description="") int bootstraps,
//			@Parameter(name="spanning points variance") double spanningPointsVariance,
//			@Parameter(name="deepening", description="If true, the algorithm starts at depth 1 and increases linearly to the target depth")
//				boolean deepening)
//	{
//		super();
//		this.generations = generations;
//		this.depths = depths;
//		this.components = components;
//		this.distSampleSize = distSampleSize;
//		this.testSampleSize = testSampleSize;
//		this.datasets = datasets;
//		this.dimensionSample = dimensionSample;
//		this.bootstraps = bootstraps;
//		this.spanningPointsVariance = spanningPointsVariance;
//		this.deepening = deepening;
//	}	
//
//	@Override
//	protected void body()
//	{
//		for(String dataset : datasets)
//			for(int comp : components)
//				for(int depth : depths)
//				{
//					File dataFile = new File(dataset);
//					List<Point> dataPoints = null;
//					if (dataset.equals("henon"))
//						dataPoints = Generators.henon().generate(SIZE);	
//					else if (dataset.equals("ikeda"))
//						dataPoints = Generators.ikeda().generate(SIZE);	
//					else if (dataset.equals("rossler"))
//						dataPoints = Generators.rossler().generate(SIZE);
//					else if (dataset.equals("logistic"))
//						dataPoints = Generators.logistic().generate(SIZE);					
//					else
//						try
//						{
//							dataPoints = Resources.csvClassification(dataFile);
//						} catch (IOException e)
//						{ throw new RuntimeException(e); }
//					
//					IFSDimension experiment = 
//							new IFSDimension(
//									generations, depth, comp, distSampleSize, 
//									testSampleSize, dataPoints, dimensionSample, 
//									bootstraps, spanningPointsVariance, 
//									deepening);
//					
//					results.add(experiment);
//					
//					Environment.current().child(experiment);
//				}
//	}
//	
//	@Result(name="table")
//	public List<List<Double>> table()
//	{
//		List<List<Double>> table = new ArrayList<List<Double>>();
//		
//		for(IFSDimension experiment : results)
//		{
//			double dataDim = experiment.dataDimension();
//			double dataDimU = experiment.dataDimUncertainty();
//			double modelDim = experiment.modelDimension();
//			double modelDimU = experiment.modelDimUncertainty();
//			
//			double bigger, smaller, biggerU, smallerU;
//			
//			if(dataDim > modelDim)
//			{
//				bigger = dataDim;
//				biggerU = dataDimU;
//				smaller = modelDim;
//				smallerU = modelDimU;
//			} else 
//			{
//				bigger = modelDim;
//				biggerU = modelDimU;
//				smaller = dataDim;
//				smallerU = dataDimU;
//			}
//			
//			double distance = bigger - smaller;
//			
//			double distanceLowerBound = (bigger - biggerU) - (smaller + smallerU);
//			if(distanceLowerBound < 0.0) distanceLowerBound = 0;
//			
//			double distanceUpperBound = (bigger + biggerU) - (smaller - smallerU);
//			double fit = experiment.modelFit();
//			
//			table.add(
//					Arrays.asList(
//						modelDim,
//						modelDimU,
//						dataDim,
//						dataDimU,
//						fit,
//						experiment.modelFitUncertainty()
//					));
//		}
//		
//		return table;
//	}
//
//	@Override
//	public List<String> scripts()
//	{
//		return Arrays.asList("multidim.plot.py");
//	}
//	
//	
//
//}
