package org.lilian.motifs;

import static org.nodes.util.Series.series;
import static org.lilian.util.Functions.log2;
import static org.nodes.models.USequenceModel.CIMethod;
import static org.nodes.models.USequenceModel.CIType;
import static org.nodes.motifs.MotifCompressor.exDegree;

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
import org.lilian.util.Functions.NaturalComparator;
import org.nodes.DGraph;
import org.nodes.DLink;
import org.nodes.DNode;
import org.nodes.Graph;
import org.nodes.Graphs;
import org.nodes.Node;
import org.nodes.Subgraph;
import org.nodes.UGraph;
import org.nodes.UNode;
import org.nodes.algorithms.Nauty;
import org.nodes.compression.BinomialCompressor;
import org.nodes.compression.EdgeListCompressor;
import org.nodes.compression.NeighborListCompressor;
import org.nodes.data.Data;
import org.nodes.models.USequenceModel;
import org.nodes.motifs.DPlainMotifExtractor;
import org.nodes.motifs.MotifCompressor;
import org.nodes.motifs.UPlainMotifExtractor;
import org.nodes.random.RandomGraphs;
import org.nodes.random.SubgraphGenerator;
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

/**
 * Compares the code length under the motifs to that under a given null-model
 * 
 * For directed data.
 * 
 * @author Peter
 */

@Module(name="Test confidence intervals")
public class DCompare
{
	@In(name="motif samples")
	public int motifSamples;
	
	@In(name="motif min size", description="minimum motif size (inclusive)")
	public int motifMinSize;
	
	@In(name="motif max size", description="maximum motif size (inclusive!)")
	public int motifMaxSize;
		
	@In(name="data")
	public DGraph<String> data;
	
	@In(name="data name")
	public String dataName;
	
	@In(name="max motifs")
	public int maxMotifs;
	
	@In(name="null model")
	public String nullModelIn;
	public NullModel nullModel;
	
	@In(name="min freq")
	public int minFreq;
	
	public static enum NullModel{ER, EDGELIST, NEIGHBORLIST}
	
	private NaturalComparator<String> comparator;
 	
	@Main(print=false)
	public void main() throws IOException
	{		
		data = Graphs.toSimpleDGraph(data);
		data = Graphs.blank(data, "");
		
		nullModel = NullModel.valueOf(nullModelIn.toUpperCase());
		double baseline = size(data);
				
		Global.log().info("Computing motif code lengths");
		
		DPlainMotifExtractor<String> ex 
			= new DPlainMotifExtractor<String>(data, motifSamples, motifMinSize, motifMaxSize, minFreq);
		
		List<Double> diffs = new ArrayList<Double>(ex.subgraphs().size());
		List<Double> frequencies = new ArrayList<Double>(ex.subgraphs().size());
		List<DGraph<String>> subs = new ArrayList<DGraph<String>>(ex.subgraphs());
		
		if(subs.size() > maxMotifs)
			subs = new ArrayList<DGraph<String>>(subs.subList(0, maxMotifs));
		
		for(DGraph<String> sub : subs)
		{
			Global.log().info("Analysing sub " + sub);
			double size = size(data, sub, ex.occurrences(sub), true); 

			double profit = baseline - size; 
			double subFrequency = ex.frequency(sub);
			 
			System.out.println("baseline: " + baseline);
			System.out.println("motif code: " + size);

			System.out.println("profit: " + profit);
			System.out.println("freq: " + subFrequency);
			
			diffs.add(profit);
			frequencies.add(subFrequency);
		}
		
		Comparator<Double> comp = Functions.natural();
		org.lilian.util.Functions.sort(diffs, Collections.reverseOrder(comp), (List) frequencies, (List) subs);
		
		File numbersFile = new File(Global.getWorkingDir(), "numbers.csv");
		
		BufferedWriter numbersWriter = new BufferedWriter(new FileWriter(numbersFile));
		for(int i : series(subs.size()))
			numbersWriter.write(diffs.get(i) + ", " + frequencies.get(i) + "\n");		
		numbersWriter.close();

		int i = 0;
		for(DGraph<String> sub : subs)
		{
			File graphFile = new File(Global.getWorkingDir(), String.format("motif.%03d.edgelist", i));
			Data.writeEdgeList(sub, graphFile);
			
			i++;
		}
		
		BufferedWriter titleWriter = new BufferedWriter(new FileWriter(new File(Global.getWorkingDir(), "title.txt")));
		titleWriter.write("data: "+dataName+", null: "+nullModel.toString().toLowerCase()+"");
		titleWriter.close();
		
		// * signal that these are directed graphs
		File directed = new File(Global.getWorkingDir(), "directed.txt");
		directed.createNewFile();
	}
	
	public double size(DGraph<String> graph)
	{
		switch (nullModel) {
		case ER:
			return BinomialCompressor.directed(graph);
		case EDGELIST:
			return EdgeListCompressor.directed(graph);
		case NEIGHBORLIST:
			return NeighborListCompressor.directed(graph); 
		default:
			throw new IllegalStateException();
		}
	}
	
	public double size(DGraph<String> graph, DGraph<String> sub,
			List<List<Integer>> occurrences, boolean resetWiring)
	{
		List<List<Integer>> wiring = new ArrayList<List<Integer>>();
		DGraph<String> subbed = MotifCompressor.subbedGraph(graph, sub,
				occurrences, wiring);
		
		// * the beta model can only store simple graphs, so we translate subbed
		//   to a simple graph and store the multiple edges separately
		FrequencyModel<Pair<Integer, Integer>> removals = null;
		if(isSimpleGraphCode(nullModel))
		{
			removals = new FrequencyModel<Pair<Integer,Integer>>();
			subbed = Graphs.toSimpleDGraph(subbed, removals);
		}
			
		// * The rest of the graph (for which we can compute the code length 
		//   directly) 
		double bits = 0.0;
		
		bits += size(sub);
		bits += size(subbed);
		
		// * Store the labels (0=no label, 1= motif node)
		OnlineModel<Integer> model = new OnlineModel<Integer>(Arrays.asList(
			new Integer(0), new Integer(1)));

		for (DNode<String> node : subbed.nodes())
			bits += - Functions.log2(model.observe(node.label().equals(
				MotifCompressor.MOTIF_SYMBOL) ? 0 : 1));
		
		// * multi edges
		// * Any node pairs with multiple links
		if(isSimpleGraphCode(nullModel))
		{
			double multiEdgeBits = 0.0;
			int maxRemovals =(int)(removals.frequency(removals.maxToken()));
			OnlineModel<Integer> frequencies = new OnlineModel<Integer>(Series.series(maxRemovals + 1));
			for(DLink<String> link : subbed.links())
			{
				Pair<Integer, Integer> pair = new Pair<Integer, Integer>(
						link.from().index(), link.to().index());
				int freq = (int) removals.frequency(pair);
				multiEdgeBits += - Functions.log2(frequencies.observe(freq));
			}
			
			bits += multiEdgeBits;
		}
			
		// * Store the rewiring information
		bits += wiringBits(sub, wiring, resetWiring);
		
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
	
	public boolean isSimpleGraphCode(NullModel nm)
	{
		switch(nm){
			case EDGELIST: 
				return false;
			case NEIGHBORLIST:
				return false;
			case ER:
				return true;
		}
		
		return false;
	}
}
