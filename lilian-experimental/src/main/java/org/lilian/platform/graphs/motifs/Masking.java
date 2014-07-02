package org.lilian.platform.graphs.motifs;

import static java.util.Collections.reverseOrder;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
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
import org.nodes.LightDGraph;
import org.nodes.Link;
import org.nodes.MapDTGraph;
import org.nodes.Node;
import org.nodes.Subgraph;
import org.nodes.TGraph;
import org.nodes.algorithms.Nauty;
import org.nodes.compression.EdgeListCompressor;
import org.nodes.compression.MotifCompressor;
import org.nodes.compression.MotifVar;
import org.nodes.compression.NeighborListCompressor;
import org.nodes.data.Data;
import org.nodes.data.GML;
import org.nodes.data.RDF;
import org.nodes.random.RandomGraphs;
import org.nodes.random.SubgraphGenerator;
import org.nodes.util.BitString;
import org.nodes.util.Compressor;
import org.nodes.util.FrequencyModel;
import org.nodes.util.Generator;
import org.lilian.util.Functions;
import org.lilian.util.Functions.NaturalComparator;
import org.nodes.util.Order;

import au.com.bytecode.opencsv.CSVWriter;

@Module(name = "Masking Motif extraction", description = "Tests different methods of Motif extraction to avoid overlap")
public class Masking
{	
	private static final int MIN_OCCURRENCES = 10;

	private DGraph<String> data;
	private DGraph<String> top, second, third;
	private int samples;
	private int minSize, maxSize;
	private double topFreq, secondFreq, thirdFreq;
	private boolean correct;
	private int numMotifs;
	private int remHubs;

	private NaturalComparator<String> comp;

	private List<Double> overlaps, ratios;
	private List<List<Double>> degrees;
	private List<Integer> frequencies;

	private List<DGraph<String>> tokens;

	private Generator<Integer> intGen;

	public Masking(
			@In(name = "data", print = false) DGraph<String> data,
			@In(name = "samples") int samples,
			@In(name = "min size") int minSize,
			@In(name = "max size") int maxSize,
			@In(name = "correct frequency", description = "whether to correct the sampling prob") boolean correct,
			@In(name = "num motifs") int numMotifs,
			@In(name = "remove hubs") int remHubs) throws IOException
	{
		this.data = data;
		this.samples = samples;
		this.correct = correct;
		this.numMotifs = numMotifs;
		this.remHubs = remHubs;

		this.minSize = minSize;
		this.maxSize = maxSize;

		comp = new Functions.NaturalComparator<String>();
		intGen = new Iterative.UniformGenerator(minSize, maxSize);
	}

	@Main()
	public void body() throws IOException
	{
		System.out.println("data size:" + data.size());

		FrequencyModel<DGraph<String>> fm = new FrequencyModel<DGraph<String>>();
		
		// * Places in the graph where each motif occurs
		Map<DGraph<String>, List<List<Integer>>> occurrences = new LinkedHashMap<DGraph<String>, List<List<Integer>>>();
		// * Those nodes that have been taken by one of the occurrence. If a new occurrence contains one of these,
		//   it will not be added
		Map<DGraph<String>, Set<Integer>> taken = new LinkedHashMap<DGraph<String>, Set<Integer>>();
		
		// * Hub removal
		List<DNode<String>> hubs = new ArrayList<DNode<String>>(remHubs);
		if (remHubs > 0)
		{
			List<DNode<String>> nodes = new ArrayList<DNode<String>>(
					data.nodes());
			Collections.sort(nodes,
					reverseOrder(new DegreeComparator<String>()));

			hubs.addAll(nodes.subList(0, remHubs));
		}

		for (DNode<String> node : hubs)
			System.out.println("HUB " + node + "\t" + node.degree());

		SubgraphGenerator<String> gen = new SubgraphGenerator<String>(data,
				intGen, hubs);

		Functions.tic();

		// * Sampling
		for (int i : Series.series(samples))
		{
			if (i % 1000 == 0)
				System.out.println("Samples finished: " + i);
			
			// * Sample a subgraph
			SubgraphGenerator<String>.Result result = gen.generate();
			DGraph<String> sub = Subgraph.dSubgraphIndices(data,
					result.indices());
			sub = Graphs.blank(sub, "");

			// * Reorder nodes to canonical ordering
			Order canonical = Nauty.order(sub, comp);
			sub = Graphs.reorder(sub, canonical);
			List<Integer> indices = canonical.apply(result.indices());
			
			// * Check if any of the nodes of the occurrence have been used already
			boolean overlaps;
			if(! taken.containsKey(sub))
				overlaps = false;
			else
				overlaps = Functions.overlap(taken.get(sub), indices) > 0;
			
			// * Add it as an occurrence	
			if(! overlaps)
			{	
				fm.add(sub, correct ? result.invProbability() : 1.0);
	
				if (! occurrences.containsKey(sub))
					occurrences.put(sub, new ArrayList<List<Integer>>());
	
				occurrences.get(sub).add(indices);
				
				if (! taken.containsKey(sub))
					taken.put(sub, new HashSet<Integer>());
				
				taken.get(sub).addAll(indices);
			}
		}

		tokens = fm.sorted();
		
		for(DGraph<String> token : tokens.subList(0, 10))
			System.out.println("--- " + token);

		System.out.println("Finished sampling. "+tokens.size()+ " tokens found. Time taken: " + Functions.toc()
				+ " seconds.");

		// * Masking
		
		double topBits = Double.MAX_VALUE;
		DGraph<String> topMotif = null;
		List<List<Integer>> topOccurrences = null;
		List<List<String>> topLabels = null;
		
		CSVWriter writer = new CSVWriter(new FileWriter(new File("motifs.csv")));
		
		for (DGraph<String> sub : tokens)
		{
			System.out.println("Starting motif (" + fm.frequency(sub) + ")" + sub);
			int n = sub.size();
			
			if(fm.frequency(sub) < MIN_OCCURRENCES)
				break;
			
			double currentTopBits = Double.MAX_VALUE;
			DGraph<String> motif = null;
			List<List<Integer>> occOut = null;
			List<List<String>> labels = null;
			double bits = -1.0;
			
			DGraph<String> currentTopMotif = null;
			List<List<Integer>> currentTopOccurrences = null;
			List<List<String>> currentTopLabels = null;
			
			for (BitString mask : BitString .all(n))
			{				
				System.out.print(".");
				
				occOut = new ArrayList<List<Integer>>();
				labels = new ArrayList<List<String>>();

				motif = mask(sub, mask, data,
						occurrences.get(sub), occOut, labels);

				MotifVar mv = new MotifVar(data, motif,occOut);
				bits = mv.size();
				
				if(bits < currentTopBits)
				{
					currentTopBits = bits;
					currentTopMotif = motif;
					currentTopOccurrences = occOut;
					currentTopLabels = labels;
				}				
			}
			
			List<Integer> occChoice = Functions.choose(currentTopOccurrences);
			DGraph<String> randomOcc = Subgraph.dSubgraphIndices(data, occChoice);
			
			writer.writeNext(new String[]{currentTopOccurrences.size()+"", currentTopBits+"", currentTopMotif+"", randomOcc+""});
			System.out.println();
			
			if(currentTopBits < topBits)
			{
				topBits = currentTopBits;
				topMotif = currentTopMotif;
				topOccurrences = currentTopOccurrences;
				topLabels = currentTopLabels;
			}	
			
		}
		
		System.out.println("top motif " + topMotif);
		
		Compressor<Graph<String>> compressor = new EdgeListCompressor<String>();
		
		System.out.println("baseline (edge list) " + compressor.compressedSize(data));
		
		compressor = new NeighborListCompressor<String>();
		
		System.out.println("baseline (neighbor list)" + compressor.compressedSize(data));
	}

	@Out(name = "tokens")
	public List<DGraph<String>> tokens()
	{
		return tokens;
	}

	@Out(name = "ratios")
	public List<Double> ratios()
	{
		return ratios;
	}

	@Out(name = "overlaps")
	public List<Double> overlaps()
	{
		return overlaps;
	}

	@Out(name = "degrees")
	public List<List<Double>> degrees()
	{
		return degrees;
	}

	@Out(name = "frequencies")
	public List<Integer> frequencies()
	{
		return frequencies;
	}

	/**
	 * Determines the compression ratio between the graph compressed plainly,
	 * and the graph compressed with the given occurrences replaced with a
	 * symbol node.
	 * 
	 * @param graph
	 * @param occurrences
	 * @return
	 */
	public static double compressionRatio(DGraph<String> graph,
			DGraph<String> sub, List<List<Integer>> occurrences)
	{
		EdgeListCompressor<String> comp = new EdgeListCompressor<String>();
		double baseline = comp.compressedSize(graph);

		double compressed = MotifCompressor.size(graph, sub, occurrences, comp);

		return compressed / baseline;
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
				sum += graph.get(occ.get(i)).degree();

			means.add(sum / (double) occurrences.size());
		}

		return means;
	}

	/**
	 * Applies mask to substring.
	 * 
	 * For the nonmasked nodes, it chooses the labels from the occurrences that
	 * maximize the number of occurrences for the masked motif.
	 * 
	 * @param sub
	 * @param mask
	 *            1s (true) are masked
	 * @param occurrences
	 * @param occurrencesOut
	 * @return
	 */
	public static DGraph<String> mask(DGraph<String> sub, BitString mask,
			DGraph<String> data, List<List<Integer>> occurrences,
			List<List<Integer>> occurrencesOut, List<List<String>> labels)
	{
		// * Tally the frequencies of non-masked nodes
		int unmasked = mask.numZeros();
		
		// - Indices of unmasked bits
		List<Integer> indices = new ArrayList<Integer>(unmasked);
		for (int i : Series.series(mask.size()))
			if (!mask.get(i))
				indices.add(i);

		List<String> choice;
		FrequencyModel<List<String>> fm = new FrequencyModel<List<String>>();
		
		for (List<Integer> occurrence : occurrences)
		{
			choice = new ArrayList<String>(unmasked);
			for (int index : indices)
				choice.add(data.get(occurrence.get(index)).label());
	
			fm.add(choice);
		}

		choice = fm.maxToken();

		// * Filter out the non-matching occurrences
		for (List<Integer> occurrence : occurrences)
			if (matches(choice, indices, occurrence, data))
				occurrencesOut.add(occurrence);
		
		System.out.print(occurrencesOut.size());

		// * Copy the subgraph and mask out the 1s
		DGraph<String> copy = new MapDTGraph<String, Object>();
		
		Iterator<String> it = choice.iterator();
		for (int j : Series.series(sub.size()))
			if (mask.get(j))
				copy.add(MotifVar.VARIABLE_SYMBOL);
			else
				copy.add(it.next());

		for (DLink<String> link : sub.links())
		{
			int from = link.from().index(), to = link.to().index();

			copy.get(from).connect(copy.get(to));
		}

		return copy;
	}

	private static boolean matches(List<String> choice, List<Integer> indices,
			List<Integer> occurrence, DGraph<String> data)
	{
		for (int i : Series.series(choice.size()))
		{
			int indexOcc = indices.get(i);
			int indexGraph = occurrence.get(indexOcc);

			String graphLabel = data.get(indexGraph).label();
			String choiceLabel = choice.get(i);

			if (!choiceLabel.equals(graphLabel))
				return false;
		}

		return true;
	}
}
