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
import org.lilian.util.Functions;
import org.lilian.util.Functions.NaturalComparator;
import org.nodes.util.Order;

import au.com.bytecode.opencsv.CSVWriter;

@Module(name = "Plot Optima", description = "")
public class Optima
{

	private DGraph<String> data;
	private int samples;
	private int minDepth, maxDepth;
	private boolean correct, blank;
	private int numMotifs;

	private NaturalComparator<String> comparator;
	EdgeListCompressor<String> comp = new EdgeListCompressor<String>(false);

	private boolean resetWiring;

	public Optima(
			@In(name = "data", print = false) String dataFile,
			@In(name = "samples") int samples,
			@In(name = "min depth") int minDepth,
			@In(name = "max depth") int maxDepth,
			@In(name = "correct frequency", description = "whether to correct the sampling prob") boolean correct,
			@In(name = "num motifs") int numMotifs,
			@In(name = "reset wiring") boolean resetWiring) throws IOException
	{

		data = Data.edgeListDirected(new File(dataFile));
		data = Graphs.blank(data, "x");

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

		for (int depth : Series.series(minDepth, maxDepth + 1))
			iteration(depth, out);

		out.close();
	}

	public void iteration(int depth, BufferedWriter out) throws IOException
	{
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

		double baseline = comp.compressedSize(data);

		Global.log().info("Starting compression test");

		int i = 0;
		for (DGraph<String> sub : tokens)
		{
			System.out.println("\n motif " + i + " depth " + depth + "\n");
			tic();
			double[] results = findLinear(data, sub, occurrences.get(sub), comp, baseline);
			
			out.write(i + ", " + depth + ", " + results[0] + ", " + results[1] + ", " + toc() + "\n");
			out.flush();
			
			i++;
		}
	}

	private double[] findLinear(DGraph<String> data,
			DGraph<String> sub, List<List<Integer>> occIn,
			EdgeListCompressor<String> comp2, double baseline) throws IOException
	{		
		// * Sort the occurrences by exdegree (ascending)
		List<List<Integer>> occurrences = new ArrayList<List<Integer>>(occIn);
		List<Integer> exDegrees = new ArrayList<Integer>(occIn.size());

		for (List<Integer> occurrence : occurrences)
			exDegrees.add(exDegree(data, occurrence));

		Comparator<Integer> nat = Functions.natural();
		Functions.sort(occurrences, exDegrees, nat);

		double maxProfit = Double.NEGATIVE_INFINITY;
		double maxDegree = -1;
		for (int n : series(occurrences.size()))
		{
			if(n%10==0)
				System.out.print('.');
			
			// * Check the compression level with only the first i occurrences
			// (compared to the baseline)
			double size = size(data, sub, occurrences.subList(0, n), comp,
					resetWiring);
			double profit = baseline - size;

			// the smallest _excluded_ exdegree
			double exDegree = exDegrees.get(n);
			
			if(profit >  maxProfit)
			{
				maxProfit = profit;
				maxDegree = exDegree;
			}
		}
		
		return new double [] {maxDegree, maxProfit};
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
		bits += comp.compressedSize(subbed);

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
		bits += comp.compressedSize(sub);

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
