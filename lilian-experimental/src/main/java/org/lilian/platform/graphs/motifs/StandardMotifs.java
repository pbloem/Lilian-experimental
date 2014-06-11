package org.lilian.platform.graphs.motifs;

import static java.util.Collections.reverseOrder;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.data2semantics.platform.Global;
import org.data2semantics.platform.annotation.In;
import org.data2semantics.platform.annotation.Main;
import org.data2semantics.platform.annotation.Module;
import org.data2semantics.platform.annotation.Out;
import org.data2semantics.platform.util.Series;
import org.nodes.DGraph;
import org.nodes.DNode;
import org.nodes.DTGraph;
import org.nodes.DTNode;
import org.nodes.DegreeComparator;
import org.nodes.Graph;
import org.nodes.Graphs;
import org.nodes.Link;
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
import org.nodes.util.FrequencyModel;
import org.lilian.util.Functions;
import org.lilian.util.Functions.NaturalComparator;
import org.nodes.util.Order;
           
@Module(name="Motif extraction", description="Tests different methods of Motif extraction to avoid overlap")
public class StandardMotifs
{
	
	private DGraph<String> data;
	private DGraph<String> top, second, third;
	private int samples;
	private int depth;
	private double topFreq, secondFreq, thirdFreq;
	private boolean correct, blank;
	private int numMotifs;
	private int remHubs;
	
	private NaturalComparator<String> comp;
	
	private List<Double> overlaps, ratios;
	private List<List<Double>> degrees;
	private List<Integer> frequencies;
	
	public StandardMotifs(
			@In(name="data", print=false) DGraph<String> data,
			@In(name="samples") int samples,
			@In(name="depth") int depth,
			@In(name="correct frequency", description="whether to correct the sampling prob") 
				boolean correct,
			@In(name="blank") boolean blank,
			@In(name="num motifs") int numMotifs,
			@In(name="remove hubs") int remHubs) throws IOException
	{
		this.data = data;
		this.samples = samples;
		this.depth = depth;
		this.correct = correct;
		this.blank = blank;
		this.numMotifs = numMotifs;
		this.remHubs = remHubs;
		
		comp = new Functions.NaturalComparator<String>();
	}
	
	@Main()
	public void body()
	{	
		System.out.println("data size:" + data.size());

		if(blank)
			data = Graphs.blank(data, "x");
		
		FrequencyModel<DGraph<String>> fm = 
				new FrequencyModel<DGraph<String>>();
		Map<DGraph<String>, List<List<Integer>>> occurrences = 
				new LinkedHashMap<DGraph<String>, List<List<Integer>>>();
		
		// * Collect the hubs to remove
		List<DNode<String>> hubs = new ArrayList<DNode<String>>(remHubs);
		if(remHubs > 0)
		{
			List<DNode<String>> nodes = new ArrayList<DNode<String>>(data.nodes());
			Collections.sort(nodes, reverseOrder(new DegreeComparator<String>()));
			
			hubs.addAll(nodes.subList(0, remHubs));
		}
		
		for(DNode<String> node : hubs)
			System.out.println("HUB " + node + "\t" + node.degree());
		
		SubgraphGenerator<String> gen = 
				new SubgraphGenerator<String>(data, depth, hubs);
		
		for(int i : Series.series(samples))
		{
			if(i % 1000 == 0)
				System.out.println("Samples finished: " + i);

			SubgraphGenerator<String>.Result result = gen.generate();
			DGraph<String> sub = Subgraph.dSubgraphIndices(data, result.indices());
			
			// * Reorder nodes to canonical ordering
			sub = Graphs.reorder(sub, Nauty.order(sub, comp));
			
			fm.add(sub, correct ? result.invProbability() : 1.0);
		
			if(! occurrences.containsKey(sub))
				occurrences.put(sub, new ArrayList<List<Integer>>());
			
			occurrences.get(sub).add(result.indices());
		}
		
		List<DGraph<String>> tokens = fm.sorted();
		tokens = tokens.subList(0, numMotifs);
		
		Global.log().info("Starting compression test");
		// * Compression ratios
		ratios = new ArrayList<Double>(numMotifs);
		for(DGraph<String> sub : tokens)
			ratios.add(compressionRatio(data, sub, occurrences.get(sub)));

		Global.log().info("Starting overlap test");
		// * Overlap numbers
		overlaps = new ArrayList<Double>(numMotifs);
		for(DGraph<String> sub : tokens)
			overlaps.add((double)overlap(data, occurrences.get(sub)));
		
		degrees = new ArrayList<List<Double>>(numMotifs);
		for(DGraph<String> sub : tokens)
			degrees.add(degrees(data, sub, occurrences.get(sub)));
		
		frequencies = new ArrayList<Integer>(numMotifs);
		for(DGraph<String> sub : tokens)
			frequencies.add((int)fm.frequency(sub));

	}
	
	@Out(name="ratios")
	public List<Double> ratios()
	{
		return ratios;
	}
	
	@Out(name="overlaps")
	public List<Double> overlaps()
	{
		return overlaps;
	}
	
	@Out(name="degrees")
	public List<List<Double>> degrees()
	{
		return degrees;
	}
	
	@Out(name="frequencies")
	public List<Integer> frequencies()
	{
		return frequencies;
	}
	
	/**
	 * Determines the compression ratio between the graph compressed plainly, and 
	 * the graph compressed with the given occurrences replaced with a symbol node. 
	 * @param graph
	 * @param occurrences
	 * @return
	 */
	public static double compressionRatio(
			DGraph<String> graph, 
			DGraph<String> sub, 
			List<List<Integer>> occurrences)
	{
		EdgeListCompressor<String> comp = new EdgeListCompressor<String>(); 
		double baseline = comp.compressedSize(graph);
		
		double compressed = MotifCompressor.size(graph, sub, occurrences, comp);
		
		return compressed / baseline;
	}
	
	/**
	 * Computes the number of times the most frequently occurring node occurs in
	 * the given motif occurrences.  
	 * @param graph
	 * @param occurrences
	 * @return
	 */
	public static int overlap(
			DGraph<String> graph, 
			List<List<Integer>> occurrences)
	{
		FrequencyModel<Integer> fm = new FrequencyModel<Integer>();
		for(List<Integer> indices : occurrences)
			fm.add(indices);
				
		return (int)fm.frequency(fm.maxToken());		
	}
	
	/**
	 * Computes the number of times the most frequently occurring node occurs in
	 * the given motif occurrences.  
	 * @param graph
	 * @param occurrences
	 * @return
	 */
	public static List<Double> degrees(
			DGraph<String> graph, 
			DGraph<String> sub, 
			List<List<Integer>> occurrences)
	{
		List<Double> means = new ArrayList<Double>(sub.size());
		
		for(int i : Series.series(sub.size()))
		{
			double sum = 0.0;
			for(List<Integer> occ : occurrences)
				sum += graph.get(occ.get(i)).degree();
			
			means.add(sum / (double) occurrences.size());
		}
					
		return means;		
	}
	
}
