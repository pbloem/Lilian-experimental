package org.lilian.motifs;

import static org.nodes.util.Functions.log2;
import static org.nodes.util.Functions.log2Choose;
import static org.nodes.util.Functions.log2Factorial;
import static org.nodes.util.Functions.logFactorial;
import static org.nodes.util.Series.series;
import static org.lilian.util.Functions.log2;
import static org.nodes.compression.Functions.log2;
import static org.nodes.compression.Functions.prefix;
import static org.nodes.models.USequenceModel.CIMethod;
import static org.nodes.models.USequenceModel.CIType;
import static org.nodes.motifs.MotifCompressor.exDegree;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.data2semantics.platform.Global;
import org.data2semantics.platform.annotation.In;
import org.data2semantics.platform.annotation.Main;
import org.data2semantics.platform.annotation.Module;
import org.data2semantics.platform.annotation.Out;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.lilian.util.Functions.NaturalComparator;
import org.nodes.DGraph;
import org.nodes.DLink;
import org.nodes.Graph;
import org.nodes.Graphs;
import org.nodes.Link;
import org.nodes.Node;
import org.nodes.Subgraph;
import org.nodes.TGraph;
import org.nodes.TLink;
import org.nodes.UGraph;
import org.nodes.ULink;
import org.nodes.UNode;
import org.nodes.algorithms.Nauty;
import org.nodes.compression.BinomialCompressor;
import org.nodes.compression.EdgeListCompressor;
import org.nodes.compression.NeighborListCompressor;
import org.nodes.data.Data;
import org.nodes.models.DSequenceModel;
import org.nodes.models.USequenceModel;
import org.nodes.motifs.DPlainMotifExtractor;
import org.nodes.motifs.MotifCompressor;
import org.nodes.motifs.UPlainMotifExtractor;
import org.nodes.random.RandomGraphs;
import org.nodes.random.SimpleSubgraphGenerator;
import org.nodes.util.FrequencyModel;
import org.nodes.util.Functions;
import org.nodes.util.Generator;
import org.nodes.util.Generators;
import org.nodes.util.OnlineModel;
import org.nodes.util.Order;
import org.nodes.util.Pair;
import org.nodes.util.Series;
import org.nodes.util.bootstrap.BCaCI;
import org.nodes.util.bootstrap.LogBCaCI;
import org.nodes.util.bootstrap.LogNormalCI;

/**
 * Compares the code length under the motifs to that under a given null-model
 * 
 * For undirected data.
 * 
 * @author Peter
 */

@Module(name="Test confidence intervals")
public class CompareLarge
{
	private static final int BS_SAMPLES = 10000;

	@In(name="motif samples")
	public int motifSamples;
	
	@In(name="motif min size", description="minimum motif size (inclusive)")
	public int motifMinSize;
	
	@In(name="motif max size", description="maximum motif size (inclusive!)")
	public int motifMaxSize;
		
	@In(name="data")
	public DGraph<String> data;
	
	@In(name="data name")
	public String dataName = "";
	
	@In(name="max motifs")
	public int maxMotifs;
	
	@In(name="minimum frequency")
	public int minFreq;
	
	public static enum NullModel{ER, EDGELIST}
	
	private boolean directed;

	private boolean resets = true;
	
	@Main(print=false)
	public void main() throws IOException
	{
		org.nodes.Global.secureRandom(42);
		
		Global.log().info("Computing motif code lengths");
		
		List<? extends DGraph<String>> subs;
		List<Double> frequencies;
		List<List<List<Integer>>> occurrences;

		DPlainMotifExtractor<String> ex 
		= new DPlainMotifExtractor<String>(
				(DGraph<String>)data, motifSamples, motifMinSize, motifMaxSize, minFreq);
	
		subs = new ArrayList<DGraph<String>>(ex.subgraphs());
		frequencies = new ArrayList<Double>(subs.size());
		for(Graph<String> sub : subs)
			frequencies.add(ex.frequency((DGraph<String>)sub));
		
		occurrences = new ArrayList<List<List<Integer>>>(subs.size());
		for(Graph<String> sub : subs)
			occurrences.add(ex.occurrences((DGraph<String>)sub));
	
		if(subs.size() > maxMotifs)
		{
			subs = new ArrayList<DGraph<String>>(subs.subList(0, maxMotifs));
			frequencies = new ArrayList<Double>(frequencies.subList(0, maxMotifs));
		}
		
		System.out.println(frequencies);
			
		List<Double> factorsER = new ArrayList<Double>(subs.size());
		List<Double> factorsEL = new ArrayList<Double>(subs.size());
				
		double baselineER = size(data, NullModel.ER, false);
		double baselineEL = size(data, NullModel.EDGELIST, false);
		
		for(int i : series(subs.size()))
		{
			DGraph<String> sub = subs.get(i);
			List<List<Integer>> occs = occurrences.get(i);
			
			Global.log().info("Analysing sub ("+ (i+1) +" of " + subs.size() + "): " + sub);
			Global.log().info("freq: " + frequencies.get(i));

			Global.log().info("null model: ER");
			{
				double sizeER = size(data, sub, occs, NullModel.ER, resets); 
				double factorER = baselineER - sizeER;
				factorsER.add(factorER);
				 
				Global.log().info("ER baseline: " + baselineER);
				Global.log().info("ER motif code: " + sizeER);
				Global.log().info("ER factor: " + factorER);
			}

			Global.log().info("null model: EL");
			{
				double sizeEL = size(data, sub, occs, NullModel.EDGELIST, resets); 
				double factorEL = baselineEL - sizeEL;
				factorsEL.add(factorEL);
			 
				Global.log().info("EL baseline: " + baselineEL);
				Global.log().info("EL motif code: " + sizeEL);
				Global.log().info("EL factor: " + factorEL);
			}

		}
		
		Comparator<Double> comp = Functions.natural();
		org.lilian.util.Functions.sort(
				frequencies, Collections.reverseOrder(comp), 
				(List) factorsER, 
				(List) factorsEL, 
				(List) subs);
		
		File numbersFile = new File(Global.getWorkingDir(), "numbers.csv");
		
		BufferedWriter numbersWriter = new BufferedWriter(new FileWriter(numbersFile));
		for(int i : series(subs.size()))
			numbersWriter.write(frequencies.get(i) + ", " + factorsER.get(i) + ", " + factorsEL.get(i) + "\n");		
		numbersWriter.close();

		int i = 0;
		for(Graph<String> sub : subs)
		{
			File graphFile = new File(Global.getWorkingDir(), String.format("motif.%03d.edgelist", i));
			Data.writeEdgeList(sub, graphFile);
			
			i++;
		}

		JSONObject obj = new JSONObject();
		obj.put("data", dataName);
		obj.put("directed", true);
		obj.put("baseline er", baselineER);
		obj.put("baseline el", baselineEL);
		Functions.write(obj.toString(), new File(Global.getWorkingDir(), "metadata.json"));
		
		try
		{
			org.data2semantics.platform.util.Functions.python(Global.getWorkingDir(), "motifs/plot.large.py");
		} catch (InterruptedException e)
		{
			System.out.println("Failed to run plot script. " + e);
		}
	}

	public static double size(DGraph<String> graph, NullModel nullModel, boolean withPrior)
	{		
		switch (nullModel) {
		case ER:
			return BinomialCompressor.directed((DGraph<String>)graph, true, withPrior);
		case EDGELIST:
			return EdgeListCompressor.directed((DGraph<String>) graph, withPrior);	
		default:
			throw new IllegalStateException("Null model not recognized");
		}
	}
	
	public static double size(DGraph<String> graph, DGraph<String> sub,
			List<List<Integer>> occurrences, NullModel nullModel, boolean resetWiring)
	{
		List<List<Integer>> wiring = new ArrayList<List<Integer>>();
		
		double bits = 0.0;
		
		bits += size(sub, nullModel, true);
		bits += (nullModel == NullModel.ER) ? 
				sizeSubbedER(graph, sub, occurrences) : 
				sizeSubbedEL(graph, sub, occurrences);
		
		// * Store the rewiring information
		bits += wiringBits(sub, wiring, resetWiring);
		
		// * Store the insertion order, to preserve the precise ordering of the
		//   nodes in the data
		int subbedSize = graph.size() - (sub.size() - 1) * occurrences.size(); 
		bits += log2Factorial(graph.size()) - log2Factorial(subbedSize);

		return bits;
	}
	
	private static double sizeSubbedEL(DGraph<String> graph, DGraph<String> sub,
			List<List<Integer>> occurrences)
	{
		double bits = 0.0;
		
		// - This list holds the index of the occurrence the node belongs to
		List<Integer> inOccurrence = new ArrayList<Integer>(graph.size());
		for (int i : Series.series(graph.size()))
			inOccurrence.add(null);

		for (int occIndex : Series.series(occurrences.size()))
			for (Integer i : occurrences.get(occIndex))
				inOccurrence.set(i, occIndex);

		OnlineModel<Integer> source = new OnlineModel<Integer>(Collections.EMPTY_LIST);
		OnlineModel<Integer> target = new OnlineModel<Integer>(Collections.EMPTY_LIST);

		// - observe all symbols
		for (Node<String> node : graph.nodes())
			if (inOccurrence.get(node.index()) == null)
			{
				source.add(node.index(), 0.0);
				target.add(node.index(), 0.0);
			}

		// - negative numbers represent symbol nodes
		for (int i : Series.series(1, occurrences.size() + 1))
		{
			source.add(-i, 0.0);
			target.add(-i, 0.0);
		}

		int subbedNumLinks = 0;
		for (Link<String> link : graph.links())
		{
			Integer firstOcc = inOccurrence.get(link.first().index());
			Integer secondOcc = inOccurrence.get(link.second().index());

			if ((firstOcc == null && secondOcc == null)
					|| firstOcc != secondOcc)
				subbedNumLinks++;
		}

		// * Size of the subbed graph
		bits += prefix(graph.size() - (sub.size() - 1) * occurrences.size());
		// * Num links in the subbed graph
		bits += prefix(subbedNumLinks);

		for (Link<String> link : graph.links())
		{
			Integer firstOcc = inOccurrence.get(link.first().index());
			Integer secondOcc = inOccurrence.get(link.second().index());

			if ((firstOcc == null && secondOcc == null)
					|| firstOcc != secondOcc)
			{
				int first = link.first().index();
				int second = link.second().index();

				first = inOccurrence.get(first) == null ? first
						: -(inOccurrence.get(first) + 1);
				second = inOccurrence.get(second) == null ? second
						: -(inOccurrence.get(second) + 1);

				double p = source.observe(first) * target.observe(second);
				bits += -Functions.log2(p);
			}
		}

		bits -= logFactorial(subbedNumLinks, 2.0);
		
		return bits;
	}

	private static double sizeSubbedER(DGraph<String> graph, Graph<String> sub,
			List<List<Integer>> occurrences)
	{
		int subbedSize = graph.size() - (sub.size() - 1) * occurrences.size();
		int subbedLinks = 0;
		
		// * records which node is in which occurrence (if any)
		Map<Integer, Integer> nodeInOccurrence = new HashMap<Integer, Integer>();
		
		for(int occurrenceIndex : Series.series(occurrences.size()))
			for(int nodeIndex : occurrences.get(occurrenceIndex))
				nodeInOccurrence.put(nodeIndex, occurrenceIndex);
		
		FrequencyModel<Pair<Integer, Integer>> nodeToInstance = 
				new FrequencyModel<Pair<Integer,Integer>>();
		FrequencyModel<Pair<Integer, Integer>> instanceToNode = 
				new FrequencyModel<Pair<Integer,Integer>>();
		FrequencyModel<Pair<Integer, Integer>> instanceToInstance = 
				new FrequencyModel<Pair<Integer,Integer>>();
		
		for(DLink<String> link : graph.links())
		{
			int fromInstance = nodeInOccurrence.get(link.from().index()) == null ? -1 : nodeInOccurrence.get(link.from().index()); 
			int toInstance =   nodeInOccurrence.get(link.to().index()) == null ? -1 : nodeInOccurrence.get(link.to().index()); 
		
			if(fromInstance == -1 && toInstance == -1)
			{
				subbedLinks++;
				continue;
			}
			
			if(fromInstance == -1)
			{
				Pair<Integer, Integer> n2i = Pair.p(link.from().index(), toInstance);
				if(nodeToInstance.frequency(n2i) == 0.0)
					subbedLinks++;
				nodeToInstance.add(n2i);
				continue;
			}
			
			if(toInstance == -1)
			{
				Pair<Integer, Integer> i2n = Pair.p(fromInstance, link.to().index());
				if(instanceToNode.frequency(i2n) == 0.0)
					subbedLinks++;
				instanceToNode.add(i2n);
				continue;
			}
			
			{
				Pair<Integer, Integer> i2i = Pair.p(fromInstance, toInstance);
				if(instanceToInstance.frequency(i2i) == 0.0)
					subbedLinks++;
				instanceToInstance.add(i2i);	
			}
		}
		
		double bits = 0.0;

		// * size of the subbed graph under the binomial compressor
		double n = subbedSize;
		double t = n * n - n;
		
		bits += org.nodes.compression.Functions.prefix((int)n) + Functions.log2(t) + log2Choose(subbedLinks, t);
		
		List<Integer> additions = new ArrayList<Integer>(graph.size());
		for(Pair<Integer, Integer> token : nodeToInstance.tokens())
			additions.add((int)nodeToInstance.frequency(token) - 1);
		for(Pair<Integer, Integer> token : instanceToNode.tokens())
			additions.add((int)instanceToNode.frequency(token) - 1);
		for(Pair<Integer, Integer> token : instanceToInstance.tokens())
			additions.add((int)instanceToInstance.frequency(token) - 1);
		
		bits += OnlineModel.storeSequence(additions); 
		
		return bits;
	}

	public static double wiringBits(Graph<String> sub, List<List<Integer>> wiring,
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
				bits += - Functions.log2(om.observe(wire));
		}

		return bits;
	}
	
	public static boolean isSimpleGraphCode(NullModel nm)
	{
		switch(nm){
			case EDGELIST: 
				return false;
			case ER:
				return true;
		}
		
		return false;
	}
}
