package org.lilian.experiment.graphs.compression;

import static org.lilian.util.Series.series;

import java.util.ArrayList;
import java.util.List;

import org.lilian.data.real.classification.Classification;
import org.lilian.data.real.classification.Classified;
import org.lilian.data.real.clustering.KMedioids;
import org.lilian.experiment.AbstractExperiment;
import org.lilian.experiment.Result;
import org.lilian.experiment.State;
import org.lilian.graphs.Graphs;
import org.lilian.graphs.UTGraph;
import org.lilian.graphs.compression.SubdueCompressor;
import org.lilian.graphs.compression.ZIPGraphCompressor;
import org.lilian.graphs.random.RandomGraphs;
import org.lilian.util.Compressor;
import org.lilian.util.Series;
import org.lilian.util.distance.CompressionDistance;
import org.lilian.util.distance.Distance;

public class CompDistance extends AbstractExperiment
{
	public int paToAttach = 2;
	public int nodes = 30;
	public int perClass = 3;
	public int kmIterations = 20;
	
	public enum Comp {ZIP, SUBDUE};
	
	private Classified<UTGraph<String, String>> data;
	private Comp compressor = Comp.SUBDUE;

	@State
	public double error;
	@State 
	public List<List<Double>> confusion = new ArrayList<List<Double>>();
	private int maxBest = 5;
	private int beamWidth = 10;
	private boolean sparse = false;
	private int maxSubSize = 4;
	private int iterations = 4;
	private double threshold = 2.0;
	
	private int fractalOffspring = 3;
	private int fractalLinks = 1;
	private int fractalDepth = 3;
			
	@Override
	protected void setup()
	{
		List<UTGraph<String, String>> instances = new ArrayList<UTGraph<String, String>>();
		data = Classification.empty();
		
		double sum = 0.0;
		double nodesSum = 0.0;

		for(int i : series(perClass))
		{
			instances.add(
					Graphs.shuffle(
							RandomGraphs.preferentialAttachment(nodes, 2)));
			
			nodesSum += instances.get(instances.size() - 1).size();
			sum += instances.get(instances.size() - 1).numLinks();
		}
		
		data.addAll(instances, 0);
		
		instances.clear();
		double meanLinksPA = sum / perClass;
		double meanNodesPA = nodesSum / perClass;

		System.out.println("pa " + meanNodesPA + " " + meanLinksPA);
		
		sum = 0.0;
		nodesSum = 0.0;

		for(int i : series(perClass))
		{
			int m = (int) meanLinksPA;
			instances.add(
					Graphs.shuffle(RandomGraphs.random(nodes,  m)));
			
			nodesSum += instances.get(instances.size() - 1).size();
			sum += instances.get(instances.size() - 1).numLinks();
		}
		data.addAll(instances, 1);
		
		instances.clear();
		double meanLinksRandom = sum / perClass;
		double meanNodesRandom = nodesSum / perClass;

		System.out.println("random " + meanNodesRandom + " " + meanLinksRandom);
		
//		sum = 0.0;
//		nodesSum = 0.0;
//		for(int i : series(perClass))
//		{
//			instances.add(RandomGraphs.fractal(fractalDepth, fractalOffspring, fractalLinks, 0.0));
//			
//			nodesSum += instances.get(instances.size() - 1).size();
//			sum += instances.get(instances.size() - 1).numLinks();
//		}
//		
//		data.addAll(instances, 2);
//		
//		instances.clear();
//		
//		double meanLinksFractalFP = sum / perClass;
//		double meanNodesFractalFP = nodesSum / perClass;
//
//		System.out.println("fractal (pure) " + meanNodesFractalFP  + " " + meanLinksFractalFP);
//		
//		sum = 0.0;
//		nodesSum = 0.0;
//
//		for(int i : series(perClass))
//		{
//			instances.add(RandomGraphs.fractal(fractalDepth, fractalOffspring, fractalLinks, 1.0));
//			
//			nodesSum += instances.get(instances.size() - 1).size();
//			sum += instances.get(instances.size() - 1).numLinks();
//		}
//		double meanLinksFractalSW = sum/ perClass;
//		double meanNodesFractalSW = nodesSum / perClass;
//
//		System.out.println("fractal (small-world) " + meanNodesFractalSW + " " + meanLinksFractalSW);
//		
//		data.addAll(instances, 3);
	}

	@Override
	protected void body()
	{
		int k = data.numClasses();
		
		Compressor<UTGraph<String, String>> comp = null;
		
		if(compressor == Comp.SUBDUE)
		{
			comp = new SubdueCompressor<String, String>(maxSubSize, maxBest, beamWidth, iterations, threshold, sparse);
		} else if(compressor == Comp.ZIP)
		{
			comp = new ZIPGraphCompressor<String, String>();
		}
		
		Distance<UTGraph<String, String>> distance = 
				new CompressionDistance<UTGraph<String, String>>(comp);
		
		// * Perform Clustering
		KMedioids<UTGraph<String, String>> km = 
				new KMedioids<UTGraph<String,String>>(
						new ArrayList<UTGraph<String, String>>(data), 
						distance, data.numClasses());
		km.iterate(kmIterations);
		
		// * Optimize mapping 
		Classified<UTGraph<String, String>> result = km.clustered();
		
		for(int gold : series(k))
		{
			List<Double> row = new ArrayList<Double>();
			confusion.add(row);
			
			for(int induced : series(k))
			{
				double matches = 0.0;;
				for(int d : series(data.size()))
					if(data.cls(d) == gold && result.cls(d) == induced)
						matches++;
						
				row.add(matches/data.size());
			}
		}
		
		logger.info("Confusion matrix: " + confusion + ". (rows are gold targets)");
			
	}
	
	@Result(name = "Error")
	public double error()
	{
		return error;
	}
	
	@Result(name = "Confusion matrix")
	public List<List<Double>> confusion()
	{
		return confusion;
	}

}
