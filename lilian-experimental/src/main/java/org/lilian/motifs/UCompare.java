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
import org.lilian.motifs.DCompare.NullModel;
import org.lilian.util.Functions.NaturalComparator;
import org.nodes.DGraph;
import org.nodes.Graph;
import org.nodes.Graphs;
import org.nodes.Node;
import org.nodes.Subgraph;
import org.nodes.UGraph;
import org.nodes.ULink;
import org.nodes.UNode;
import org.nodes.algorithms.Nauty;
import org.nodes.compression.BinomialCompressor;
import org.nodes.compression.EdgeListCompressor;
import org.nodes.compression.NeighborListCompressor;
import org.nodes.data.Data;
import org.nodes.models.USequenceModel;
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
 * For undirected data.
 * 
 * @author Peter
 */

@Module(name="Test confidence intervals")
public class UCompare
{
	@In(name="motif samples")
	public int motifSamples;
	
	@In(name="motif min size", description="minimum motif size (inclusive)")
	public int motifMinSize;
	
	@In(name="motif max size", description="maximum motif size (inclusive!)")
	public int motifMaxSize;
		
	@In(name="data")
	public UGraph<String> data;
	
	@In(name="data name")
	public String dataName;
	
	@In(name="max motifs")
	public int maxMotifs;
	
	@In(name="null model")
	public String nullModelIn;
	public NullModel nullModel;
	
	public static enum NullModel{ER, EDGELIST, NEIGHBORLIST}
	
	private NaturalComparator<String> comparator;
 	
	@Main(print=false)
	public void main() throws IOException
	{		
		data = Graphs.toSimpleUGraph(data);
		data = Graphs.blank(data, "");
		
		nullModel = NullModel.valueOf(nullModelIn.toUpperCase());
		double baseline = size(data);
				
		Global.log().info("Computing motif code lengths");
		
		UPlainMotifExtractor<String> ex 
			= new UPlainMotifExtractor<String>(
					data, motifSamples, motifMinSize, motifMaxSize);
		
		List<Double> diffs = new ArrayList<Double>(ex.subgraphs().size());
		List<Double> frequencies = new ArrayList<Double>(ex.subgraphs().size());
		List<UGraph<String>> subs = new ArrayList<UGraph<String>>(ex.subgraphs());
		
		if(subs.size() > maxMotifs)
			subs = new ArrayList<UGraph<String>>(subs.subList(0, maxMotifs));
		
		for(UGraph<String> sub : subs)
		{
			System.out.println("Analysing sub " + sub);
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
		for(UGraph<String> sub : subs)
		{
			File graphFile = new File(Global.getWorkingDir(), String.format("motif.%03d.edgelist", i));
			Data.writeEdgeList(sub, graphFile);
			
			i++;
		}
		
		BufferedWriter titleWriter = new BufferedWriter(new FileWriter(new File(Global.getWorkingDir(), "title.txt")));
		titleWriter.write("data: "+dataName+", null: "+nullModel.toString().toLowerCase()+"");
		titleWriter.close();
		
		// * signal that these are directed graphs
		File directed = new File(Global.getWorkingDir(), "undirected.txt");
		directed.createNewFile();
	}
	
	public double size(UGraph<String> graph)
	{
		switch (nullModel) {
		case ER:
			return BinomialCompressor.undirected(graph);
		case EDGELIST:
			return EdgeListCompressor.undirected(graph);
		case NEIGHBORLIST:
			return NeighborListCompressor.undirected(graph); 
		default:
			throw new IllegalStateException();
		}
	}
	
	public double size(UGraph<String> graph, UGraph<String> sub,
			List<List<Integer>> occurrences, boolean resetWiring)
	{
		List<List<Integer>> wiring = new ArrayList<List<Integer>>();
		UGraph<String> subbed = MotifCompressor.subbedGraph(graph, sub,
				occurrences, wiring);
		
		FrequencyModel<Pair<Integer, Integer>> removals = null;
		
		if(isSimpleGraphCode(nullModel))
		{
			removals = new FrequencyModel<Pair<Integer,Integer>>();
			subbed = Graphs.toSimpleUGraph(subbed, removals);
		}
		
		// * The rest of the graph (for which we can compute the code length 
		//   directly) 
		double bits = 0.0;
		
		bits += size(sub);
		bits += size(subbed);
		
		// * Store the labels
		OnlineModel<Integer> model = new OnlineModel<Integer>(Arrays.asList(
			new Integer(0), new Integer(1)));

		for (UNode<String> node : subbed.nodes())
			bits += - Functions.log2(model.observe(node.label().equals(
				MotifCompressor.MOTIF_SYMBOL) ? 0 : 1));
		
		// * Any node pairs with multiple links
		if(isSimpleGraphCode(nullModel))
		{
			int maxRemovals = (int)(removals.frequency(removals.maxToken()));
			OnlineModel<Integer> frequencies = new OnlineModel<Integer>(Series.series(maxRemovals + 1));
			for(ULink<String> link : subbed.links())
			{
				int minor = Math.min(link.first().index(), link.second().index());
				int major = Math.max(link.first().index(), link.second().index());
				Pair<Integer, Integer> pair = new Pair<Integer, Integer>(minor, major);
				
				int freq = (int) removals.frequency(pair);
				bits += - Functions.log2(frequencies.observe(freq));
			}
		}
		
		// * Store the rewiring information
		bits += wiringBits(sub, wiring, resetWiring);
		
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
