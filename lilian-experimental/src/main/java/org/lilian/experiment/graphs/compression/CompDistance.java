package org.lilian.experiment.graphs.compression;

import static org.lilian.graphs.Graphs.reduce;
import static org.lilian.graphs.Graphs.subgraph;
import static org.lilian.util.Series.series;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.data2semantics.tools.graphs.GML;
import org.lilian.Global;
import org.lilian.data.real.classification.Classification;
import org.lilian.data.real.classification.Classified;
import org.lilian.data.real.clustering.KMedioids;
import org.lilian.experiment.AbstractExperiment;
import org.lilian.experiment.Parameter;
import org.lilian.experiment.Result;
import org.lilian.experiment.State;
import org.lilian.graphs.Graphs;
import org.lilian.graphs.UTGraph;
import org.lilian.graphs.UTNode;
import org.lilian.graphs.compression.SubdueCompressor;
import org.lilian.graphs.compression.ZIPGraphCompressor;
import org.lilian.graphs.data.Data;
import org.lilian.graphs.random.RandomGraphs;
import org.lilian.util.Compressor;
import org.lilian.util.Permutations;
import org.lilian.util.Series;
import org.lilian.util.distance.CompressionDistance;
import org.lilian.util.distance.Distance;

public class CompDistance extends AbstractExperiment
{
	public int paToAttach;
	public int nodes;
	public int perClass;
	public int kmIterations = 20;
	
	public enum Comp {ZIP, SUBDUE, RANDOM};
	
	private Classified<UTGraph<String, String>> data;
	private Comp compressor = Comp.ZIP;

	@State 
	public List<List<Double>> confusion = new ArrayList<List<Double>>();
	private int maxBest = 1;
	private int beamWidth = 5;
	private int matcherBeamWidth;
	private boolean sparse = false;
	private int maxSubSize = 10;
	private int iterations = 15;
	private double threshold = 2.0;
	private int steps = 2;
	
	private int fractalOffspring = 3;
	private int fractalLinks = 1;
	private int fractalDepth = 3;
	
	public CompDistance(
			@Parameter(name="matcher beam width")
				int matcherBeamWidth,
			@Parameter(name="nodes")
				int nodes,
			@Parameter(name="per class")
				int perClass,
			@Parameter(name="to attach")
				int toAttach,
			@Parameter(name="compressor")
				String compressor,
			@Parameter(name="data source")
				String dataSource				
		)
	{
		this.matcherBeamWidth = matcherBeamWidth;
		this.nodes = nodes;
		this.perClass = perClass;
		this.paToAttach = toAttach;		
		
		if(compressor.equals("zip"))
			this.compressor = Comp.ZIP;
		if(compressor.equals("subdue"))
			this.compressor = Comp.SUBDUE;
		if(compressor.equals("random"))
			this.compressor = Comp.RANDOM;		

		if(dataSource.equals("random2"))
			dataRandom2();
		if(dataSource.equals("random4"))
			dataRandom4();
		if(dataSource.equals("natural"))
			dataNatural();
		
	}
	
	private void dataRandom2()
	{
		List<UTGraph<String, String>> instances = new ArrayList<UTGraph<String, String>>();
		data = Classification.empty();
		
		double sum = 0.0;
		double nodesSum = 0.0;

		for(int i : series(perClass))
		{
			instances.add(
					Graphs.shuffle(
							RandomGraphs.preferentialAttachment(nodes, paToAttach)));
			
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
	}
	
	private void dataRandom4()
	{
		List<UTGraph<String, String>> instances = new ArrayList<UTGraph<String, String>>();
		data = Classification.empty();
		
		double sum = 0.0;
		double nodesSum = 0.0;

		for(int i : series(perClass))
		{
			instances.add(
					Graphs.shuffle(
							RandomGraphs.preferentialAttachment(nodes, paToAttach)));
			
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
		
		sum = 0.0;
		nodesSum = 0.0;
		for(int i : series(perClass))
		{
			instances.add(RandomGraphs.fractal(fractalDepth, fractalOffspring, fractalLinks, 0.0));
			
			nodesSum += instances.get(instances.size() - 1).size();
			sum += instances.get(instances.size() - 1).numLinks();
		}
		
		data.addAll(instances, 2);
		
		instances.clear();
		
		double meanLinksFractalFP = sum / perClass;
		double meanNodesFractalFP = nodesSum / perClass;

		System.out.println("fractal (pure) " + meanNodesFractalFP  + " " + meanLinksFractalFP);
		
		sum = 0.0;
		nodesSum = 0.0;

		for(int i : series(perClass))
		{
			instances.add(RandomGraphs.fractal(fractalDepth, fractalOffspring, fractalLinks, 1.0));
			
			nodesSum += instances.get(instances.size() - 1).size();
			sum += instances.get(instances.size() - 1).numLinks();
		}
		double meanLinksFractalSW = sum/ perClass;
		double meanNodesFractalSW = nodesSum / perClass;

		System.out.println("fractal (small-world) " + meanNodesFractalSW + " " + meanLinksFractalSW);
		
		data.addAll(instances, 3);
	}
	
	private void dataNatural()
	{
		UTGraph<String, String> ecoli = null, neural = null;		
		try {
			ecoli  = Data.readString(new File("/home/peter/Documents/datasets/graphs/ecoli/EC.dat"));
			neural = Data.readString(new File("/home/peter/Documents/datasets/graphs/neural/celegans.txt"));
		} catch (IOException e) {
			throw new RuntimeException(e);
		} 			
		
		List<UTGraph<String, String>> masters = Arrays.asList(ecoli, neural);
		data = sample(masters, perClass, nodes);
	}
	
	@Override
	protected void setup()
	{
	}

	@Override
	protected void body()
	{
		int k = data.numClasses();
		
		Compressor<UTGraph<String, String>> comp = null;
		
		if(compressor == Comp.SUBDUE)
		{
			comp = new SubdueCompressor<String, String>(maxSubSize, maxBest, beamWidth, iterations, threshold, sparse, matcherBeamWidth);
		} else if(compressor == Comp.ZIP)
		{
			comp = new ZIPGraphCompressor<String, String>();
		} else if(compressor == Comp.RANDOM)
		{
			comp = null;
		}
		
		Distance<UTGraph<String, String>> distance = (comp!= null) ? 
				new CompressionDistance<UTGraph<String, String>>(comp) :
				new RandomDistance();
		
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
		double best = Double.POSITIVE_INFINITY;
		
		for(int[] shuffle : new Permutations(confusion.size()))
		{
			double error = error(shuffle);
			if(error < best)
				best = error;
		}
		
		return best;
	}
	
	private double error(int[] shuffle)
	{
		double accuracy = 0.0;
		for(int i : series(confusion.size()))
		{
			int j = shuffle[i];
			accuracy += confusion.get(i).get(j);
		}
		
		System.out.println(accuracy);
		
		return 1.0 - accuracy;		
	}
	
	@Result(name = "Confusion matrix")
	public List<List<Double>> confusion()
	{
		return confusion;
	}
	
	@Result(name = "num nodes")
	public List<Double> numNodes()
	{
		List<Double> result = new ArrayList<Double>();
		for(int cls : series(data.numClasses()))
		{
			double sum = 0.0;
			int n = 0;
			for(UTGraph<String, String> graph : data.points(cls))
			{
				sum += graph.size();
				n++;
			}
			result.add(sum/n);			
		}
		return result;
	}
	
	@Result(name = "num links")
	public List<Double> numLinks()
	{
		List<Double> result = new ArrayList<Double>();
		for(int cls : series(data.numClasses()))
		{
			double sum = 0.0;
			int n = 0;
			for(UTGraph<String, String> graph : data.points(cls))
			{
				sum += graph.numLinks();
				n++;
			}
			result.add(sum/n);			
		}
		
		return result;
	}		

	public static Classified<UTGraph<String, String>> sample(List<UTGraph<String, String>> masters, int perClass, int steps)
	{
		Classified<UTGraph<String, String>> out = Classification.empty();
		
		for(int m : series(masters.size()))
			for(int p : series(perClass))
			{
				UTGraph<String, String> sample = sampleRW(masters.get(m), steps);
				System.out.println(sample.size() + " " + sample.numLinks());
				out.add(sample, m);
			}
		
		return out;
	}

	public static UTGraph<String, String> sample(UTGraph<String, String> graph, int steps)
	{
		UTNode<String, String> center = graph.nodes().get(Global.random.nextInt(graph.size()));
		
		Set<UTNode<String, String>> 
			neighborhood = new HashSet<UTNode<String, String>>(),
			next = new HashSet<UTNode<String,String>>();
		
		neighborhood.add(center);
		
		for(int i : series(steps))
		{
			next.clear();
			for(UTNode<String, String> node : neighborhood)
				next.addAll(node.neighbors());
			
			neighborhood.addAll(next);
		}
		
		System.out.print(".");
		
		return reduce(subgraph(graph, neighborhood));
	}
	
	public static UTGraph<String, String> sampleRW(UTGraph<String, String> graph, int steps)
	{
		UTNode<String, String> center = graph.nodes().get(Global.random.nextInt(graph.size()));
		
		List<UTNode<String, String>> nodes = new ArrayList<UTNode<String,String>>(steps);
		nodes.add(center);
		
		while(nodes.size() < steps)
		{
			List<UTNode<String, String>> neighbors = 
				new ArrayList<UTNode<String,String>>(nodes.get(nodes.size()-1).neighbors());
			
			UTNode<String, String> choice = neighbors.get(Global.random.nextInt(neighbors.size()));
			
			nodes.add(choice);
		}
		
	
		System.out.print(".");
		
		return reduce(subgraph(graph, nodes));
	}
	
	public static class RandomDistance implements Distance<UTGraph<String, String>>
	{
		private static final long serialVersionUID = 1L;

		@Override
		public double distance(UTGraph<String, String> a,
				UTGraph<String, String> b) 
		{
			return Global.random.nextDouble();
		}
	}
}
