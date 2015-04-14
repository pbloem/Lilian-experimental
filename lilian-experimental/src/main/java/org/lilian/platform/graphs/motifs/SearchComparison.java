package org.lilian.platform.graphs.motifs;

import static java.util.Collections.reverseOrder;
import static org.data2semantics.platform.util.Series.series;
import static org.lilian.util.Functions.tic;
import static org.lilian.util.Functions.toc;
import static org.nodes.compression.Functions.log2;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.data2semantics.platform.Global;
import org.data2semantics.platform.annotation.In;
import org.data2semantics.platform.annotation.Main;
import org.data2semantics.platform.annotation.Module;
import org.data2semantics.platform.annotation.Out;
import org.data2semantics.platform.util.Series;
import org.nodes.DGraph;
import org.nodes.DLink;
import org.nodes.DNode;
import org.nodes.DTGraph;
import org.nodes.DTNode;
import org.nodes.DegreeComparator;
import org.nodes.Graph;
import org.nodes.Graphs;
import org.nodes.Link;
import org.nodes.MapDTGraph;
import org.nodes.Node;
import org.nodes.Subgraph;
import org.nodes.TGraph;
import org.nodes.algorithms.Nauty;
import org.nodes.compression.EdgeListCompressor;
import org.nodes.compression.MotifCompressor;
import org.nodes.data.Data;
import org.nodes.data.GML;
import org.nodes.data.RDF;
import org.nodes.random.RandomGraphs;
import org.nodes.random.SubgraphGenerator;
import org.nodes.util.Compressor;
import org.nodes.util.FrequencyModel;
import org.nodes.util.OnlineModel;
import org.lilian.util.Fibonacci;
import org.lilian.util.Functions;
import org.lilian.util.Functions.NaturalComparator;
import org.lilian.util.Pair;
import org.nodes.util.Order;

import au.com.bytecode.opencsv.CSVWriter;

@Module(name = "Plot Optima", description = "")
public class SearchComparison
{
	private static List<DGraph<String>> datasets = new ArrayList<DGraph<String>>(3);
	
	static {
		try
		{
			datasets.add(
				Graphs.blank(	
					Data.edgeListDirected(
							new File("/home/peter-extern/datasets/graphs/p2p/p2p.txt"))
							, ""));

			datasets.add(
					Graphs.blank(	
						Data.edgeListDirected(
								new File("/home/peter-extern/datasets/graphs/ecoli/EC.dat"))
								, ""));
			
			datasets.add(
					Graphs.blank(	
						Data.edgeListDirected(
								new File("/home/peter-extern/datasets/graphs/collab/collab.txt"))
								, ""));
			
			
//			datasets.add(Data.edgeListDirectedUnlabeled(new File("/Users/Peter/Documents/datasets/graphs/amazon/amazon0302.txt"), true));
//			
//			datasets.add(Data.edgeListDirectedUnlabeled(new File("/Users/Peter/Documents/datasets/graphs/email/email.eu.txt"), true));
//			
		} catch (IOException e)
		{
			throw new RuntimeException(e);
		}
	}

	private int samples;
	private int minDepth, maxDepth;
	private boolean correct, blank;
	private int numMotifs;

	private NaturalComparator<String> comparator;
	EdgeListCompressor<String> compressor = new EdgeListCompressor<String>(false);

	private boolean resetWiring;

	public SearchComparison(
			@In(name = "samples") int samples,
			@In(name = "min depth") int minDepth,
			@In(name = "max depth") int maxDepth,
			@In(name = "correct frequency", description = "whether to correct the sampling prob") boolean correct,
			@In(name = "num motifs") int numMotifs,
			@In(name = "reset wiring") boolean resetWiring) throws IOException
	{
		this.samples = samples;
		this.correct = correct;
		this.numMotifs = numMotifs;
		this.resetWiring = resetWiring;

		this.minDepth = minDepth;
		this.maxDepth = maxDepth;

		comparator = new Functions.NaturalComparator<String>();
	}

	@Main()
	public void body() throws IOException
	{
		File dir = new File(Global.getWorkingDir(), "plot");
		dir.mkdirs();
		File outFile = new File(dir, "plot.csv");

		BufferedWriter out = new BufferedWriter(new FileWriter(outFile));

		for(int d : Series.series(datasets.size()))
			for (int depth : Series.series(minDepth, maxDepth + 1))
				iteration(d, depth, out);

		out.close();
	}

	public void iteration(int d, int depth, BufferedWriter out) throws IOException
	{
		DGraph<String> data = datasets.get(d);
;
		
		FrequencyModel<DGraph<String>> fm = new FrequencyModel<DGraph<String>>();

		// * The non-overlapping instances
		Map<DGraph<String>, List<List<Integer>>> occurrences = new LinkedHashMap<DGraph<String>, List<List<Integer>>>();

		// * The nodes that are occupied by instances
		Map<DGraph<String>, Set<Integer>> nodes = new LinkedHashMap<DGraph<String>, Set<Integer>>();

		SubgraphGenerator<String> gen = new SubgraphGenerator<String>(data,
				depth, Collections.EMPTY_LIST);

		int skipped = 0;
		for (int i : Series.series(samples))
		{
			if (i % 10000 == 0)
				System.out.println("Samples finished: " + i);

			SubgraphGenerator<String>.Result result = gen.generate();
			DGraph<String> sub = Subgraph.dSubgraphIndices(data,
					result.indices());

			// * Reorder nodes to canonical ordering
			sub = Graphs.reorder(sub, Nauty.order(sub, comparator));

			fm.add(sub, correct ? result.invProbability() : 1.0);
			List<Integer> occurrence = result.indices(); // * NB: not in correct
													     // order. is this a
													     // problem?

			// * check for overlap
			boolean overlap = false;

			if (nodes.containsKey(sub))
			{
				Set<Integer> taken = nodes.get(sub);
				for (int index : occurrence)
					if (taken.contains(index))
					{
						overlap = true;
						break;
					}
			}

			// * record the occurrence
			if (!overlap)
			{
				if (!occurrences.containsKey(sub))
					occurrences.put(sub, new ArrayList<List<Integer>>());

				occurrences.get(sub).add(occurrence);

				if (!nodes.containsKey(sub))
					nodes.put(sub, new HashSet<Integer>());

				Set<Integer> subNodes = nodes.get(sub);
				for (int index : occurrence)
					subNodes.add(index);
			} else
				skipped++;
		}

		System.out.println(skipped + " occurrences skipped due to overlap ");

		List<DGraph<String>> tokens = fm.sorted();

		numMotifs = Math.min(numMotifs, tokens.size());
		tokens = tokens.subList(0, numMotifs);

		double baseline = compressor.compressedSize(data);

		Global.log().info("Starting compression test");

		int i = 0;
		for (DGraph<String> sub : tokens)
		{
			// * Sort the occurrences by exdegree (ascending)
			List<List<Integer>> sortedOccurrences = new ArrayList<List<Integer>>(occurrences.get(sub));
			List<Integer> exDegrees = new ArrayList<Integer>(sortedOccurrences.size());

			for (List<Integer> occurrence : sortedOccurrences)
				exDegrees.add(exDegree(data, occurrence));

			Comparator<Integer> nat = Functions.natural();
			Functions.sort(sortedOccurrences, exDegrees, nat);
		
			System.out.println("\n motif " + i + " depth " + depth + ", " + occurrences.get(sub).size() + " occurrences \n");
			tic();
			Pair<Integer, Double> linear = findLinear (data, sub, sortedOccurrences, baseline);
			double linearTime = toc();
			
			Pair<Integer, Double> phi = findPhi(data, sub, sortedOccurrences, baseline);
			double phiTime = toc();
			
			out.write(i + ", " + d + ", " + depth + ", " + linear.second() + ", " + phi.second() + ", " +  linearTime + ", " + phiTime + "\n");
			out.flush();
			
			i++;
		}
	}

	/**
	 * Linear search
	 * 
	 * @param data
	 * @param motif
	 * @param occurrences
	 * @param baseline
	 * @return
	 * @throws IOException
	 */
	private Pair<Integer, Double> findLinear(DGraph<String> data,
			DGraph<String> motif, List<List<Integer>> occurrences, double baseline) throws IOException
	{		
		System.out.println("\n" + occurrences.size());
		
		double maxProfit = Double.NEGATIVE_INFINITY;
		int index = -1;
		for (int n : series(occurrences.size()))
		{
			if(n%10==0)
				System.out.print('.');
			
			// * Check the compression level with only the first i occurrences
			// (compared to the baseline)
			double size = size(data, motif, occurrences.subList(0, n), compressor,
					resetWiring);
			double profit = baseline - size;
			
			if(profit >  maxProfit)
			{
				maxProfit = profit;
				index = n;
			}
		}
		
		return new Pair<Integer, Double>(index, maxProfit);
	}
	
	/**
	 * Golden section search
	 * @param data
	 * @param motif
	 * @param occurrences
	 * @param baseline
	 * @return
	 * @throws IOException
	 */
	private Pair<Integer, Double> findPhi(
			DGraph<String> data,
			DGraph<String> motif, List<List<Integer>> occurrences, 
			double baseline) 
		throws IOException
	{
		// * Find the first fibonacci number that is bigger than the number of occurrences 
		// TODO: there's a small chance that rounding errors will fuck this up.
		int n = occurrences.size();
		int to = Fibonacci.isFibonacci(n) ? n : (int)Fibonacci.get((int) Math.ceil(Fibonacci.getIndexApprox(n)));

		System.out.println("\n" + n + " to: "  + to);
		
		FindPhi fp = new FindPhi(data, motif, occurrences, baseline);
		Pair<Integer, Double> results = fp.find(0, to);
		
		return results;
		
	}	
	
	private class FindPhi {
		DGraph<String> data; 
		DGraph<String> motif;
		List<List<Integer>> occurrences; 
		double baseline;
		
		public FindPhi(DGraph<String> data, DGraph<String> motif,
				List<List<Integer>> occurrences, double baseline)
		{
			this.data = data;
			this.motif = motif;
			this.occurrences = occurrences;
			this.baseline = baseline;
		}

		public Pair<Integer, Double> find(int from, int to)
		{
			System.out.println(from + " " + to);
			int range = to - from;
			
			if(range <= 2)
			{
				// return the best of from, from +1 and to
				int x0 = from, x1= from + 1, x2 = to;
				double y0 = sample(x0),
					   y1 = sample(x1),
					   y2 = sample(x2);
				return y0 > y1 && y0 > y2 ? new Pair<Integer, Double>(x0, y0) : (y1 > y2 ? new Pair<Integer, Double>(x1, y1) : new Pair<Integer, Double>(x2, y2));
			}
			
			int r0 = (int)Fibonacci.previous(range);
			int mid1 = to - r0;
			int mid2 = from + r0;
			
			double y1 = sample(mid1);
			double y2 = sample(mid2);
			
			if(y1 > y2)
				return find(from, mid2);
			return find(mid1, to);
		}
		
		private Map<Integer, Double> cache = new HashMap<Integer, Double>();
		
		public double sample(int n)
		{
			if(! cache.containsKey(n))
			{
				double size = size(data, motif, occurrences.subList(0, Math.min(occurrences.size(), n)), compressor,
						resetWiring);
				double profit = baseline - size;
				cache.put(n, profit);
				
				return profit;
			}
			
			return cache.get(n);
		}
	}

	/**
	 * Computes the number of times the most frequently occurring node occurs in
	 * the given motif occurrences.
	 * 
	 * @param graph
	 * @param occurrences
	 * @return
	 */
	public static int overlap(DGraph<String> graph,
			List<List<Integer>> occurrences)
	{
		FrequencyModel<Integer> fm = new FrequencyModel<Integer>();
		for (List<Integer> indices : occurrences)
			fm.add(indices);

		return (int) fm.frequency(fm.maxToken());
	}

	/**
	 * Computes the number of times the most frequently occurring node occurs in
	 * the given motif occurrences.
	 * 
	 * @param graph
	 * @param occurrences
	 * @return
	 */
	public static List<Double> degrees(DGraph<String> graph,
			DGraph<String> sub, List<List<Integer>> occurrences)
	{
		List<Double> means = new ArrayList<Double>(sub.size());

		for (int i : Series.series(sub.size()))
		{
			double sum = 0.0;
			for (List<Integer> occ : occurrences)
			{
				int nodeIndex = occ.get(i);
				Node<String> node = graph.get(nodeIndex);
				for (Node<String> neighbor : node.neighbors())
					if (!occ.contains(neighbor.index()))
						sum++;
			}

			means.add(sum / (double) occurrences.size());
		}

		return means;
	}

	public static int exDegree(DGraph<String> graph, List<Integer> occurrence)
	{
		int sum = 0;

		for (int i : Series.series(occurrence.size()))
		{
			int nodeIndex = occurrence.get(i);
			Node<String> node = graph.get(nodeIndex);

			for (Node<String> neighbor : node.neighbors())
				if (!occurrence.contains(neighbor.index()))
					sum++;
		}

		return sum;
	}

	public static List<String> degreesSample(DGraph<String> graph,
			DGraph<String> sub, List<List<Integer>> occurrences, int sampleSize)
	{
		List<String> sample = new ArrayList<String>(sub.size());
		List<Integer> choices = new ArrayList<Integer>(
				sampleSize < 0 ? occurrences.size() : sampleSize);

		if (sampleSize < 0)
			choices = series(occurrences.size());
		else
			for (int i : Series.series(sampleSize))
				choices.add(Global.random().nextInt(occurrences.size()));

		for (int i : Series.series(sub.size()))
		{
			String s = "";

			for (int choice : choices)
			{
				List<Integer> occ = occurrences.get(choice);

				int exDeg = 0;
				int nodeIndex = occ.get(i);

				Node<String> node = graph.get(nodeIndex);
				for (Node<String> neighbor : node.neighbors())
					if (!occ.contains(neighbor.index()))
						exDeg++;

				s += exDeg + "-";
			}

			sample.add(s.substring(0, s.length() - 1));
		}

		return sample;
	}

	public double size(DGraph<String> graph, DGraph<String> sub,
			List<List<Integer>> occurrences, Compressor<Graph<String>> comp,
			boolean resetWiring)
	{
		List<List<Integer>> wiring = new ArrayList<List<Integer>>();
		DGraph<String> copy = MotifCompressor.subbedGraph(graph, sub,
				occurrences, wiring);

		double graphsBits = 0.0;

		double motif = motifSize(sub);
		graphsBits += motif;

		double silhouette = silhouetteSize(copy);
		graphsBits += silhouette;

		double wiringBits = wiringBits(sub, wiring, resetWiring);
		graphsBits += wiringBits;

		return graphsBits;
	}

	public double silhouetteSize(DGraph<String> subbed)
	{
		double bits = 0.0;

		// * Store the structure
		bits += compressor.compressedSize(subbed);

		// * Store the labels
		OnlineModel<Integer> model = new OnlineModel<Integer>(Arrays.asList(
				new Integer(0), new Integer(1)));

		for (DNode<String> node : subbed.nodes())
			bits += -Functions.log2(model.observe(node.label().equals(
					MotifCompressor.MOTIF_SYMBOL) ? 0 : 1));

		return bits;
	}

	public double wiringBits(DGraph<String> sub, List<List<Integer>> wiring,
			boolean reset)
	{
		OnlineModel<Integer> om = new OnlineModel<Integer>(Series.series(sub
				.size()));

		double bits = 0.0;
		for (List<Integer> motifWires : wiring)
		{
			if (reset)
				om = new OnlineModel<Integer>(Series.series(sub.size()));

			for (int wire : motifWires)
				bits += -log2(om.observe(wire));
		}

		return bits;
	}

	public double motifSize(DGraph<String> sub)
	{
		double bits = 0.0;

		// * Store the structure
		bits += compressor.compressedSize(sub);

		return bits;
	}

	private static boolean alive(List<? extends Node<String>> nodes)
	{
		for (Node<String> node : nodes)
			if (node.dead())
				return false;

		return true;
	}
}
