//package org.lilian.platform.graphs.motifs;
//
//import static java.util.Collections.reverseOrder;
//import static org.data2semantics.platform.util.Series.series;
//
//import java.io.File;
//import java.io.FileWriter;
//import java.io.IOException;
//import java.util.ArrayList;
//import java.util.Collections;
//import java.util.HashSet;
//import java.util.Iterator;
//import java.util.LinkedHashMap;
//import java.util.List;
//import java.util.Map;
//import java.util.Set;
//
//import org.data2semantics.platform.Global;
//import org.data2semantics.platform.annotation.In;
//import org.data2semantics.platform.annotation.Main;
//import org.data2semantics.platform.annotation.Module;
//import org.data2semantics.platform.annotation.Out;
//import org.data2semantics.platform.util.Series;
//import org.nodes.DGraph;
//import org.nodes.DLink;
//import org.nodes.DNode;
//import org.nodes.DTGraph;
//import org.nodes.DTLink;
//import org.nodes.DTNode;
//import org.nodes.DegreeComparator;
//import org.nodes.Graph;
//import org.nodes.Graphs;
//import org.nodes.LightDGraph;
//import org.nodes.Link;
//import org.nodes.MapDTGraph;
//import org.nodes.Node;
//import org.nodes.Subgraph;
//import org.nodes.TGraph;
//import org.nodes.algorithms.Nauty;
//import org.nodes.compression.EdgeListCompressor;
//import org.nodes.compression.MotifCompressor;
//import org.nodes.compression.MotifVar;
//import org.nodes.compression.MotifVarTags;
//import org.nodes.compression.NeighborListCompressor;
//import org.nodes.data.Data;
//import org.nodes.data.GML;
//import org.nodes.data.RDF;
//import org.nodes.random.RandomGraphs;
//import org.nodes.random.SubgraphGenerator;
//import org.nodes.util.BitString;
//import org.nodes.util.Compressor;
//import org.nodes.util.FrequencyModel;
//import org.nodes.util.Generator;
//import org.lilian.util.Functions;
//import org.lilian.util.Functions.NaturalComparator;
//import org.nodes.util.Order;
//
//import au.com.bytecode.opencsv.CSVWriter;
//
//@Module(name = "Masking Motif extraction")
//public class Masking
//{	
//	private static final int MIN_OCCURRENCES = 10;
//
//	private DTGraph<String, String> data;
//	private DGraph<String> top, second, third;
//	private int samples;
//	private int minSize, maxSize;
//	private double topFreq, secondFreq, thirdFreq;
//	private boolean correct;
//	private int numMotifs;
//	private int remHubs;
//	private boolean specifySubs; 
//
//	private NaturalComparator<String> comp;
//
//	private List<Double> overlaps, ratios;
//	private List<List<Double>> degrees;
//	private List<Integer> frequencies;
//
//	private List<DGraph<String>> tokens;
//
//	private Generator<Integer> intGen;
//
//	public Masking(
//			@In(name = "data", print = false) DTGraph<String, String> data,
//			@In(name = "samples") int samples,
//			@In(name = "min size") int minSize,
//			@In(name = "max size") int maxSize,
//			@In(name = "correct frequency", description = "whether to correct the sampling prob") boolean correct,
//			@In(name = "num motifs") int numMotifs,
//			@In(name = "remove hubs") int remHubs,
//			@In(name = "specify subs") boolean specifySubs) throws IOException
//	{
//		this.data = data;
//		this.samples = samples;
//		this.correct = correct;
//		this.numMotifs = numMotifs;
//		this.remHubs = remHubs;
//
//		this.minSize = minSize;
//		this.maxSize = maxSize;
//
//		this.specifySubs = specifySubs;
//		
//		comp = new Functions.NaturalComparator<String>();
//		intGen = new Iterative.UniformGenerator(minSize, maxSize);
//	}
//
//	@Main()
//	public void body() throws IOException
//	{
//		System.out.println("data size:" + data.size());
//
//		FrequencyModel<DGraph<String>> fm = new FrequencyModel<DGraph<String>>();
//		
//		// * Places in the graph where each motif occurs
//		Map<DGraph<String>, List<List<Integer>>> occurrences = new LinkedHashMap<DGraph<String>, List<List<Integer>>>();
//		// * Those nodes that have been taken by one of the occurrences. If a new occurrence contains one of these,
//		//   it will not be added
//		Map<DGraph<String>, Set<Integer>> taken = new LinkedHashMap<DGraph<String>, Set<Integer>>();
//
//		SubgraphGenerator<String> gen = new SubgraphGenerator<String>(data,
//				intGen, Collections.EMPTY_LIST);
//
//		Functions.tic();
//
//		// * Sampling
//		for (int i : Series.series(samples))
//		{
//			if (i % 1000 == 0)
//				System.out.println("Samples finished: " + i);
//			
//			// * Sample a subgraph
//			SubgraphGenerator<String>.Result result = gen.generate();
//			DTGraph<String, String> subLabeled = Subgraph.dtSubgraphIndices(data,
//					result.indices());
//			DGraph<String> subBlanked = Graphs.blank(subLabeled, "");
//			
//			// * Check if any of the nodes of the occurrence have been used already
//			boolean overlaps;
//			if(! taken.containsKey(subBlanked))
//				overlaps = false;
//			else
//				overlaps = Functions.overlap(taken.get(subBlanked), result.indices()) > 0;
//			
//			// * Add it as an occurrence	
//			if(! overlaps)
//			{	
//				// * Reorder nodes to canonical ordering
//				Order canonical = Nauty.order(subBlanked, comp);
//				subBlanked = Graphs.reorder(subBlanked, canonical);
//				
//				List<Integer> indices = canonical.apply(result.indices());
//				
//				fm.add(subBlanked, correct ? result.invProbability() : 1.0);
//	
//				if (! occurrences.containsKey(subBlanked))
//					occurrences.put(subBlanked, new ArrayList<List<Integer>>());
//	
//				occurrences.get(subBlanked).add(indices);
//				
//				if (! taken.containsKey(subBlanked))
//					taken.put(subBlanked, new HashSet<Integer>());
//				
//				taken.get(subBlanked).addAll(indices);
//			}
//		}
//
//		tokens = fm.sorted();
//		
//		for(DGraph<String> token : tokens.subList(0, 10))
//			System.out.println("--- " + token);
//
//		System.out.println("Finished sampling. "+tokens.size()+ " tokens found. Time taken: " + Functions.toc()
//				+ " seconds.");
//
//		// * Masking
//		
//		double topBits = Double.MAX_VALUE;
//		DTGraph<String, String> topMotif = null;
//		List<List<Integer>> topOccurrences = null;
//		List<List<String>> topLabels = null;
//		
//		CSVWriter writer = new CSVWriter(new FileWriter(
//				new File(Global.getWorkingDir(), "motifs.csv")));
//		
//		int i  = 0;
//		for (DGraph<String> sub : tokens)
//		{
//			System.out.println("Starting motif (" + fm.frequency(sub) + ")" + sub);
//			int nMask = sub.size() + sub.numLinks();
//			
//			if(fm.frequency(sub) < MIN_OCCURRENCES)
//				break;
//			
//			double currentTopBits = Double.MAX_VALUE;
//			DTGraph<String, String> motif = null;
//			List<List<Integer>> occOut = null;
//			List<List<String>> labels = null;
//			double bits = -1.0;
//			
//			DTGraph<String, String> currentTopMotif = null;
//			List<List<Integer>> currentTopOccurrences = null;
//			List<List<String>> currentTopLabels = null;
//			
//			for(BitString mask : BitString.all(nMask))
//			{				
//				System.out.print(".");
//				
//				occOut = new ArrayList<List<Integer>>();
//				labels = new ArrayList<List<String>>();
//
//				motif = mask(sub, mask, data,
//						occurrences.get(sub), occOut, labels);
//				
//				MotifVarTags mv = new MotifVarTags(data, motif,occOut, specifySubs);
//				bits = mv.size();
//				
//				if(bits < currentTopBits)
//				{
//					currentTopBits = bits;
//					currentTopMotif = motif;
//					currentTopOccurrences = occOut;
//					currentTopLabels = labels;
//				}				
//			}
//			
//			System.out.println("\nfinished: best size " + currentTopBits);
//			System.out.println("          best motif " + currentTopMotif);
//
//
//			
//			List<Integer> occChoice = Functions.choose(currentTopOccurrences);
//			DGraph<String> randomOcc = Subgraph.dSubgraphIndices(data, occChoice);
//			
//			writer.writeNext(new String[]{i+"", currentTopOccurrences.size()+"", currentTopBits+"", currentTopMotif+"", randomOcc+""});
//			
//			// * Write the occurrences to a CSV file
//			CSVWriter motifWriter = new CSVWriter(new FileWriter(
//					new File(Global.getWorkingDir(), "occurrences.motif"+i+".csv")));
//			
//			List<String> lbs = labels(currentTopMotif);
//			
//			motifWriter.writeNext(lbs.toArray(new String[0]));
//			
//			for(List<Integer> occurrenceIndices : currentTopOccurrences)
//			{
//				DTGraph<String, String> occurrence = Subgraph.dtSubgraphIndices(data, occurrenceIndices);
//
//				List<String> occLabels = labels(occurrence);
//				motifWriter.writeNext(occLabels.toArray(new String[0]));
//			}
//			
//			motifWriter.close();
//			
//			System.out.println();
//			
//			if(currentTopBits < topBits)
//			{
//				topBits = currentTopBits;
//				topMotif = currentTopMotif;
//				topOccurrences = currentTopOccurrences;
//				topLabels = currentTopLabels;
//			}	
//			
//			i++;
//		}
//		writer.close();
//		
//		Global.log().info("top motif " + topMotif);
//		
//		MotifVarTags mv = new MotifVarTags(data, topMotif, topOccurrences, specifySubs);
//		Global.log().info("size with motif: " + mv.size());
//		
//		Compressor<Graph<String>> compressor = new EdgeListCompressor<String>();
//		
//		Global.log().info("baseline (edge list) " + compressor.compressedSize(data));
//		
//		compressor = new NeighborListCompressor<String>();
//		
//		Global.log().info("baseline (neighbor list)" + compressor.compressedSize(data));
//	}
//
//	@Out(name = "tokens")
//	public List<DGraph<String>> tokens()
//	{
//		return tokens;
//	}
//
//	@Out(name = "ratios")
//	public List<Double> ratios()
//	{
//		return ratios;
//	}
//
//	@Out(name = "overlaps")
//	public List<Double> overlaps()
//	{
//		return overlaps;
//	}
//
//	@Out(name = "degrees")
//	public List<List<Double>> degrees()
//	{
//		return degrees;
//	}
//
//	@Out(name = "frequencies")
//	public List<Integer> frequencies()
//	{
//		return frequencies;
//	}
//
//	/**
//	 * Determines the compression ratio between the graph compressed plainly,
//	 * and the graph compressed with the given occurrences replaced with a
//	 * symbol node.
//	 * 
//	 * @param graph
//	 * @param occurrences
//	 * @return
//	 */
//	public static double compressionRatio(DGraph<String> graph,
//			DGraph<String> sub, List<List<Integer>> occurrences)
//	{
//		EdgeListCompressor<String> comp = new EdgeListCompressor<String>();
//		double baseline = comp.compressedSize(graph);
//
//		double compressed = MotifCompressor.size(graph, sub, occurrences, comp);
//
//		return compressed / baseline;
//	}
//
//	/**
//	 * Computes the number of times the most frequently occurring node occurs in
//	 * the given motif occurrences.
//	 * 
//	 * @param graph
//	 * @param occurrences
//	 * @return
//	 */
//	public static int overlap(DGraph<String> graph,
//			List<List<Integer>> occurrences)
//	{
//		FrequencyModel<Integer> fm = new FrequencyModel<Integer>();
//		for (List<Integer> indices : occurrences)
//			fm.add(indices);
//
//		return (int) fm.frequency(fm.maxToken());
//	}
//	
//	/**
//	 * Returns the labels of the given occurrence in canonical order
//	 * 
//	 * @param graph
//	 * @param indices
//	 * @return
//	 */
//	private static List<String> labels(DTGraph<String, String> graph, List<Integer> indices)
//	{
//		List<String> labels = new ArrayList<String>();
//		
//		DTGraph<String, String> sub = Subgraph.dtSubgraphIndices(graph, indices);
//				
//		for(Node<String> node : sub.nodes())
//			labels.add(node.label());
//		
//		// * We create our own loop through the links, to make sure that it doesn't 
//		//   depend on the graph implementation.
//		for(DTNode<String, String> node : sub.nodes())
//		{
//			int from = node.index();
//			
//			Set<Integer> tos = new HashSet<Integer>();
//			for(DTLink<String, String> link : node.linksOut())
//				tos.add(link.to().index());
//			
//			List<Integer> listTos = new ArrayList<Integer>(tos);
//			Collections.sort(listTos);
//			
//			for(int to : listTos)
//			{
//				List<String> tags = new ArrayList<String>();
//				for(DTLink<String, String> link : sub.get(from).linksOut(sub.get(to)))
//					tags.add(link.tag());
//				
//				Collections.sort(tags);
//				
//				labels.addAll(tags);
//			}	
//		}
//		
//		return labels;
//	}
//	
//	private static List<String> labels(DTGraph<String, String> graph)
//	{
//		List<String> labels = new ArrayList<String>();
//						
//		for(Node<String> node : graph.nodes())
//			labels.add(node.label());
//		
//		// * We create our own loop through the links, to make sure that it doesn't 
//		//   depend on the graph implementation.
//		for(DTNode<String, String> node : graph.nodes())
//		{
//			int from = node.index();
//			
//			Set<Integer> tos = new HashSet<Integer>();
//			for(DTLink<String, String> link : node.linksOut())
//				tos.add(link.to().index());
//			
//			List<Integer> listTos = new ArrayList<Integer>(tos);
//			Collections.sort(listTos);
//			
//			for(int to : listTos)
//			{
//				List<String> tags = new ArrayList<String>();
//				for(DTLink<String, String> link : graph.get(from).linksOut(graph.get(to)))
//					tags.add(link.tag());
//				
//				Collections.sort(tags);
//				
//				labels.addAll(tags);
//			}	
//		}
//		
//		return labels;
//	}	
//	
//	private static DTGraph<String, String> motif(DGraph<String> subgraph, List<String> labels)
//	{	
//		DTGraph<String, String> copy = new MapDTGraph<String, String>();
//		int i = 0;
//		
//		for(Node<String> node : subgraph.nodes())
//			copy.add(labels.get(i++));
//	
//		for(DNode<String> node : subgraph.nodes())
//		{
//			int from = node.index();
//			Set<Integer> tos = new HashSet<Integer>();
//			for(DLink<String> link : node.linksOut())
//				tos.add(link.to().index());
//			
//			List<Integer> listTos = new ArrayList<Integer>(tos);
//			Collections.sort(listTos);
//			
//			for(int to : listTos)
//				for(DLink<String> link : subgraph.get(from).linksOut(subgraph.get(to)))
//					copy.get(from).connect(copy.get(to), labels.get(i++));
//		}
//		
//		return copy;
//	}
//
//
//	/**
//	 * Applies mask to substring.
//	 * 
//	 * For the nonmasked nodes and links, it chooses the labels from the occurrences that
//	 * maximize the number of occurrences for the masked motif.
//	 * 
//	 * @param sub
//	 * @param mask
//	 *            1s (true) are masked
//	 * @param occurrences
//	 * @param occurrencesOut
//	 * @param The labels of matching occurrence will be added to this
//	 * @return
//	 */
//	public static DTGraph<String, String> mask(DGraph<String> sub, BitString mask,
//			DTGraph<String, String> data, List<List<Integer>> occurrences,
//			List<List<Integer>> occurrencesOut, List<List<String>> labels)
//	{
//		
//		FrequencyModel<List<String>> fm = new FrequencyModel<List<String>>();
//		for (List<Integer> occurrence : occurrences)
//		{
//			List<String> sequence = labels(data, occurrence);
//						
//			assert(sequence.size() == mask.size());
//			for(int i : series(mask.size()))
//				if(mask.get(i))
//					sequence.set(i, MotifVar.VARIABLE_SYMBOL);
//			
//			fm.add(sequence);
//		}
//
//		List<String> choice = fm.maxToken();
//		
//		// * Filter out the non-matching occurrences
//		for (List<Integer> occurrence : occurrences)
//		{
//			List<String> sequence = labels(data, occurrence);
//			
//			boolean matches = true;
//			for(int i : series(sequence.size()))
//				if(! (choice.get(i).equals(MotifVar.VARIABLE_SYMBOL) || choice.get(i).equals(sequence.get(i))))
//				{
//					matches = false;
//					break;
//				}
//			
//			if(matches)
//			{
//				occurrencesOut.add(occurrence);
//				labels.add(sequence);
//			}
//		}
//		
//		System.out.print(occurrencesOut.size());
//
//		// * Copy the subgraph and mask out the 1s
//		return motif(sub, choice);
//	}
//
//	/**
//	 * Computes the number of times the most frequently occurring node occurs in
//	 * the given motif occurrences.
//	 * 
//	 * @param graph
//	 * @param occurrences
//	 * @return
//	 */
//	public static List<Double> degrees(DGraph<String> graph,
//			DGraph<String> sub, List<List<Integer>> occurrences)
//	{
//		List<Double> means = new ArrayList<Double>(sub.size());
//	
//		for (int i : Series.series(sub.size()))
//		{
//			double sum = 0.0;
//			for (List<Integer> occ : occurrences)
//				sum += graph.get(occ.get(i)).degree();
//	
//			means.add(sum / (double) occurrences.size());
//		}
//	
//		return means;
//	}
//
//
//}
