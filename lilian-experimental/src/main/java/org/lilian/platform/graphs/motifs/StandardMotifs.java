package org.lilian.platform.graphs.motifs;

import static java.util.Collections.reverseOrder;
import static org.data2semantics.platform.util.Series.series;
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
import org.nodes.data.Data;
import org.nodes.data.GML;
import org.nodes.data.RDF;
import org.nodes.motifs.MotifCompressor;
import org.nodes.random.RandomGraphs;
import org.nodes.random.SimpleSubgraphGenerator;
import org.nodes.random.SubgraphGenerator;
import org.nodes.util.Compressor;
import org.nodes.util.FrequencyModel;
import org.nodes.util.OnlineModel;
import org.lilian.util.Functions;
import org.lilian.util.Functions.NaturalComparator;
import org.nodes.util.Order;

import au.com.bytecode.opencsv.CSVWriter;
           
@Module(name="Motif extraction", description="Tests single-iteration motif extraction, without label information.")
public class StandardMotifs
{
	
	private DGraph<String> data;
	private int samples;
	private int depth;
	private boolean correct, blank;
	private int numMotifs;
	
	private NaturalComparator<String> comparator;
	EdgeListCompressor<String> comp = new EdgeListCompressor<String>(false); 
	
	private List<Double> ratios, sizes;
	private boolean resetWiring;
	
	private Map<DGraph<String>, Integer> exDegrees = new HashMap<DGraph<String>, Integer>();
	
	public StandardMotifs(
			@In(name="data", print=false) String dataFile,
			@In(name="samples") int samples,
			@In(name="depth") int depth,
			@In(name="correct frequency", description="whether to correct the sampling prob") 
				boolean correct,
			@In(name="num motifs") int numMotifs,
			@In(name="reset wiring") boolean resetWiring) 
		throws IOException
	{
		
		data = Data.edgeListDirected(new File(dataFile));
		data = Graphs.blank(data, "x");
		
		this.samples = samples;
		this.depth = depth;
		this.correct = correct;
		this.numMotifs = numMotifs;
		this.resetWiring = resetWiring;
		
		comparator = new Functions.NaturalComparator<String>();
	}
	
	@Main()
	public void body()
		throws IOException
	{	
		System.out.println("data size:" + data.size());
		
		FrequencyModel<DGraph<String>> fm = 
				new FrequencyModel<DGraph<String>>();
		
		// * The non-overlapping instances
		Map<DGraph<String>, List<List<Integer>>> occurrences = 
				new LinkedHashMap<DGraph<String>, List<List<Integer>>>();
		
		// * The nodes that are occupied by instances
		Map<DGraph<String>, Set<Integer>> nodes = 
				new LinkedHashMap<DGraph<String>, Set<Integer>>();
		
		SubgraphGenerator<String> gen = 
				new SubgraphGenerator<String>(data, depth, Collections.EMPTY_LIST);
		
		int skipped = 0;
		for(int i : Series.series(samples))
		{
			if(i % 10000 == 0)
				System.out.println("Samples finished: " + i);

			SubgraphGenerator<String>.Result result = gen.generate();
			DGraph<String> sub = Subgraph.dSubgraphIndices(data, result.indices());
			
			// * Reorder nodes to canonical ordering
			sub = Graphs.reorder(sub, Nauty.order(sub, comparator));
			
			fm.add(sub, correct ? result.invProbability() : 1.0);
			List<Integer> occurrence = result.indices(); // * NB: not in correct order. is this a problem?
			
			// * check for overlap
			boolean overlap = false;
			
			if(nodes.containsKey(sub))
			{
				Set<Integer> taken = nodes.get(sub);
				for(int index : occurrence)
					if(taken.contains(index))
					{
						overlap = true;
						break;
					}
			}
			
			// * record the occurrence
			if(! overlap)
			{				
				if(! occurrences.containsKey(sub))
					occurrences.put(sub, new ArrayList<List<Integer>>());
				
				occurrences.get(sub).add(occurrence);
				
				if(! nodes.containsKey(sub))
					nodes.put(sub, new HashSet<Integer>());
				
				Set<Integer> subNodes = nodes.get(sub);
				for(int index : occurrence)
					subNodes.add(index);
			} else 
				skipped++;
		}
		
		System.out.println(skipped + " occurrences skipped due to overlap ");
				
		List<DGraph<String>> tokens = fm.sorted();
		
		numMotifs = Math.min(numMotifs, tokens.size()); 
		tokens = tokens.subList(0, numMotifs);
		
		baseline = comp.compressedSize(data);
		
		Global.log().info("Starting compression test");
		sizes = new ArrayList<Double>(numMotifs);
		ratios = new ArrayList<Double>(numMotifs);
				
		int i = 0;
		for(DGraph<String> sub : tokens)
		{	
			System.out.println("          motif: " + sub.toString());
			System.out.println("ooo   frequency: " + (int)fm.frequency(sub));
			System.out.println("ooo     degrees: " + degrees(data, sub, occurrences.get(sub)));
			System.out.println("ooo      sample: " + degreesSample(data, sub, occurrences.get(sub), -15));
			System.out.println("ooo    baseline: " + baseline);
			
			double size = size(data, sub, occurrences.get(sub), comp, resetWiring);
			ratios.add(baseline - size);
			strings.add(sub.toString());
			
			// doBins(i,data, sub, occurrences.get(sub), comp, Arrays.asList(1, 2, 5, 10, 20, 50, 100, 200, 500, 1000));
			i++;
		}						
			
	}
	

	
//	private void findOptimum(int motifIndex, DGraph<String> data, DGraph<String> sub,
//			List<List<Integer>> occIn, EdgeListCompressor<String> comp2)
//		throws IOException
//	{
//		System.out.println("--- num occurrences: " +  occIn.size());
//		
//		// * Sort the occurrences by exdegree (ascending)
//		List<List<Integer>> occurrences = new ArrayList<List<Integer>>(occIn);
//		List<Integer> exDegrees = new ArrayList<Integer>(occIn.size());
//		
//		for(List<Integer> occurrence : occurrences)
//			exDegrees.add(exDegree(data, occurrence));
//		
//		Comparator<Integer> nat = Functions.natural(); 
//		Functions.sort(occurrences, exDegrees, nat);
//		
//		// * create a new CSV file
//		File dir = new File(Global.getWorkingDir(), "motif-optima");
//		dir.mkdirs();
//		File out = new File(dir, String.format("motif-%04d.csv", motifIndex));
//		
//		BufferedWriter writer = new BufferedWriter(new FileWriter(out));
//		
//		for(int n : series(occurrences.size()))
//		{
//			if(n % 50 == 0)
//				System.out.println(n + " occurrences checked");
//			
//			// * Check the compression level with only the first i occurrences 
//			//   (compared to the baseline)
//			double size = size(data, sub, occurrences.subList(0, n), comp, resetWiring);
//			double profit = baseline - size;
//			
//			// the smallest _excluded_ exdegree
//			double exDegree = exDegrees.get(n);
//			
//			// * Write a line in the csv file
//			writer.write(n+", "+ exDegree+", "+ profit+"\n");
//		}
//		// * Close stream etc.
//		writer.close();
//	}

	private void doBins(int motifIndex, DGraph<String> data, DGraph<String> sub,
			List<List<Integer>> occIn, EdgeListCompressor<String> comp2, 
			List<Integer> binEdges)
		throws IOException
	{
		System.out.println("--- num occurrences: " +  occIn.size());
		
		// * Sort the occurrences by exdegree (ascending)
		List<List<Integer>> occurrences = new ArrayList<List<Integer>>(occIn);
		List<Integer> exDegrees = new ArrayList<Integer>(occIn.size());
		
		for(List<Integer> occurrence : occurrences)
			exDegrees.add(exDegree(data, occurrence));
		
		Comparator<Integer> nat = Functions.natural(); 
		Functions.sort(occurrences, exDegrees, nat);
		
		List<List<List<Integer>>> bins = new ArrayList<List<List<Integer>>>(binEdges.size() + 1);
		
		List<List<Integer>> bin = new ArrayList<List<Integer>>();				
		bins.add(bin);
			
		int u = 0;
		int upper = binEdges.get(u); 
		int exDegree = -1;
		for(int i : series(occurrences.size()))
		{
			List<Integer> occurrence = occurrences.get(i);
			exDegree = exDegrees.get(i);
			
			while(exDegree > upper)
			{
				bin = new ArrayList<List<Integer>>();
				bins.add(bin);
				
				u++;
				upper = u < binEdges.size() ? binEdges.get(u) : Integer.MAX_VALUE; 
			}
			
			bin.add(occurrence);
		}
		
		while(u < binEdges.size())
		{
			bins.add(new ArrayList<List<Integer>>());
			u++;
		}
				
		// * create a new CSV file
		File dir = new File(Global.getWorkingDir(), "motif-bins");
		dir.mkdirs();
		File out = new File(dir, String.format("bins-%04d.csv", motifIndex));
		
		BufferedWriter writer = new BufferedWriter(new FileWriter(out));
		
		System.out.println(bins.size());
		System.out.println(binEdges.size());
		
		int j = 0;
		for(List<List<Integer>> currentBin : bins)
		{		
			// * Check the compression level with only the occurrences in the bin
			double sizeWithReset = size(data, sub, currentBin, comp, true);
			double sizeWithoutReset = size(data, sub, currentBin, comp, false);
			double profitWithReset = baseline - sizeWithReset;
			double profitWithoutReset = baseline - sizeWithoutReset;
			
			int lower = j == 0 ? 0 : binEdges.get(j-1); 
			upper = j < binEdges.size() ? binEdges.get(j) : -1;
			j++;
			
			// * Write a line in the CSV file
			writer.write(lower + ", " + upper +", "+ profitWithReset+ ", " + profitWithoutReset + "," +  currentBin.size() + "\n");
		}
		
		// * Close stream etc.
		writer.close();
	}

	private static double baseline;
	@Out(name="baseline")
	public double baseline()
	{
		return baseline;
	}
	
	@Out(name="differences")
	public List<Double> ratios()
	{
		return ratios;
	}
	
	@Out(name="sizes")
	public List<Double> sizes()
	{
		return sizes;
	}
	
	private List<String> strings = new ArrayList<String>();
	@Out(name="strings")
	public List<String> strings()
	{
		return strings;
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
			{
				int nodeIndex = occ.get(i);
				Node<String> node = graph.get(nodeIndex);
				for(Node<String> neighbor : node.neighbors())
					if(! occ.contains(neighbor.index()) )
						sum++;
			}
			
			means.add(sum / (double) occurrences.size());
		}
					
		return means;		
	}
	
	public static int exDegree(DGraph<String> graph, List<Integer> occurrence)
	{
		int sum = 0;
		
		for(int i : Series.series(occurrence.size()))
		{
			int nodeIndex = occurrence.get(i);
			Node<String> node = graph.get(nodeIndex);
			
			for(Node<String> neighbor : node.neighbors())
				if(! occurrence.contains(neighbor.index()))
					sum++;
		}
					
		return sum;		
	}
	
	public static List<String> degreesSample(
			DGraph<String> graph, 
			DGraph<String> sub, 
			List<List<Integer>> occurrences, int sampleSize)
	{
		List<String> sample = new ArrayList<String>(sub.size());
		List<Integer> choices = new ArrayList<Integer>(sampleSize < 0 ? occurrences.size() : sampleSize);
		
		if(sampleSize < 0)
			choices = series(occurrences.size());
		else 
			for(int i : Series.series(sampleSize))
				choices.add(Global.random().nextInt(occurrences.size()));
		
		for(int i : Series.series(sub.size()))
		{
			String s = "";
			
			for(int choice : choices)
			{
				List<Integer> occ = occurrences.get(choice);
				
				int exDeg = 0;
				int nodeIndex = occ.get(i);
				
				Node<String> node = graph.get(nodeIndex);
				for(Node<String> neighbor : node.neighbors())
					if(! occ.contains(neighbor.index()) )
						exDeg++;
				
				s += exDeg + "-";
			}
			
			sample.add(s.substring(0, s.length() - 1));
		}
					
		return sample;		
	}
	
	public double size(
			DGraph<String> graph, DGraph<String> sub,
			List<List<Integer>> occurrences, 
			Compressor<Graph<String>> comp, boolean resetWiring)
	{
		List<List<Integer>> wiring = new ArrayList<List<Integer>>();
		DGraph<String> copy = MotifCompressor.subbedGraph(graph, sub, occurrences, wiring);

		double graphsBits = 0.0;
		
		double motif = motifSize(sub);
		graphsBits += motif;
		
		double silhouette = silhouetteSize(copy);
		graphsBits += silhouette;
		
		double wiringBits = wiringBits(sub, wiring, resetWiring);
		graphsBits += wiringBits;
		
//		System.out.println("***      motif: " + motif);
//		System.out.println("*** silhouette: " + silhouette);
//		System.out.println("***     wiring: " + wiringBits);

		return graphsBits;
	}
	
	public double silhouetteSize(DGraph<String> subbed)
	{
		double bits = 0.0;
		
		// * Store the structure
		bits += comp.compressedSize(subbed);
		
		// * Store the labels
		OnlineModel<Integer> model = new OnlineModel<Integer>(
				Arrays.asList(new Integer(0), new Integer(1)));

		for(DNode<String> node : subbed.nodes())
			bits += - Functions.log2(model.observe(
					node.label().equals(MotifCompressor.MOTIF_SYMBOL) ? 0 : 1));
		
		return bits;
	}

	public double wiringBits(DGraph<String> sub, List<List<Integer>> wiring, boolean reset)
	{
		OnlineModel<Integer> om = new OnlineModel<Integer>(Series.series(sub.size()));

		double bits = 0.0;
		for (List<Integer> motifWires : wiring)
		{
			if(reset)
				om = new OnlineModel<Integer>(Series.series(sub.size()));
			
			for(int wire : motifWires)
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
