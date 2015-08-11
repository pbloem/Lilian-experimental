package org.lilian.motifs;

import static java.lang.String.format;
import static java.util.Collections.reverseOrder;
import static org.data2semantics.platform.util.Series.series;
import static org.lilian.util.Functions.tic;
import static org.lilian.util.Functions.toc;
import static org.nodes.compression.Functions.log2;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
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
import org.nodes.UGraph;
import org.nodes.UNode;
import org.nodes.algorithms.Nauty;
import org.nodes.compression.EdgeListCompressor;
import org.nodes.data.Data;
import org.nodes.data.GML;
import org.nodes.data.RDF;
import org.nodes.motifs.MotifCompressor;
import org.nodes.motifs.UPlainMotifExtractor;
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

	private UGraph<String> data;
	boolean directed;
	
	private int samples;
	private int minDepth, maxDepth;
	private int numMotifs;

	private NaturalComparator<String> comparator;
	EdgeListCompressor<String> comp = new EdgeListCompressor<String>(false);

	public Optima(
			@In(name = "data", print = false) UGraph<String> data,
			@In(name = "samples") int samples,
			@In(name = "min depth") int minDepth,
			@In(name = "max depth") int maxDepth,
			@In(name = "num motifs") int numMotifs) throws IOException
	{

		this.data = Graphs.blank(data, "x");
		directed = data instanceof DGraph<?>;

		this.samples = samples;
		this.numMotifs = numMotifs;

		this.minDepth = minDepth;
		this.maxDepth = maxDepth;

		comparator = new Functions.NaturalComparator<String>();
	}

	@Main()
	public void body() throws IOException
	{
		for (int depth : Series.series(minDepth, maxDepth + 1))
			iteration(depth);
	}

	public void iteration(int depth) throws IOException
	{
		UPlainMotifExtractor<String> ex = new UPlainMotifExtractor<String>(data, samples, depth);

		List<UGraph<String>> tokens = ex.subgraphs();
		System.out.println(tokens.size() + " motifs found... ");

		tokens = tokens.subList(0, Math.min(numMotifs, tokens.size()));
		
		double baseline = comp.compressedSize(data);

		Global.log().info("Starting compression test");

		int i = 0;
		for (UGraph<String> sub : tokens)
		{
			System.out.println("motif " +  i);
			// * Sort the occurrences by exdegree (ascending)
			List<List<Integer>> occurrences = new ArrayList<List<Integer>>(ex.occurrences(sub));
			List<Integer> exDegrees = new ArrayList<Integer>(occurrences.size());

			for (List<Integer> occurrence : occurrences)
				exDegrees.add(exDegree(data, occurrence));

			Comparator<Integer> nat = Functions.natural();
			Functions.sort(occurrences, exDegrees, nat);
			
			File csv = new File(Global.getWorkingDir(), format("motif.%02d.%02d.csv", depth, i));
			BufferedWriter out = new BufferedWriter(new FileWriter(csv));
			
			int step = (int)Math.ceil(occurrences.size() / 10.0);
			
			for(int cap : series(0, step, occurrences.size()))
			{
				double profitWithReset = baseline - size(data, sub, occurrences.subList(0, cap), comp, true);
				double profitWithoutReset = baseline - size(data, sub, occurrences.subList(0, cap), comp, false);
				int degree = exDegrees.get(cap);
				
				out.write(format("%d, %f, %f, %d\n", cap, profitWithReset, profitWithoutReset, degree));
				out.flush();
			}
			out.close();

			i++;
		}
	}





	public static int exDegree(UGraph<String> graph, List<Integer> occurrence)
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

	public double size(UGraph<String> graph, UGraph<String> sub,
			List<List<Integer>> occurrences, Compressor<Graph<String>> comp,
			boolean resetWiring)
	{
		List<List<Integer>> wiring = new ArrayList<List<Integer>>();
		UGraph<String> copy = MotifCompressor.subbedGraph(graph, sub,
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

	public double silhouetteSize(UGraph<String> subbed)
	{
		double bits = 0.0;

		// * Store the structure
		bits += comp.compressedSize(subbed);

		// * Store the labels
		OnlineModel<Integer> model = new OnlineModel<Integer>(Arrays.asList(
				new Integer(0), new Integer(1)));

		for (UNode<String> node : subbed.nodes())
			bits += -Functions.log2(model.observe(node.label().equals(
					MotifCompressor.MOTIF_SYMBOL) ? 0 : 1));

		return bits;
	}

	public double wiringBits(UGraph<String> sub, List<List<Integer>> wiring,
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

	public double motifSize(UGraph<String> sub)
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
