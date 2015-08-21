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
public class DCompareBeta
{
	@In(name="motif samples")
	public int motifSamples;
	
	@In(name="motif min size", description="minimum motif size (inclusive)")
	public int motifMinSize;
	
	@In(name="motif max size", description="maximum motif size (inclusive!)")
	public int motifMaxSize;
	
	@In(name="beta ceiling", description="An indicator for the number of iterations to use for the beta model. The number of iteration is the size of the graph divided by the ceiling.")
	public int betaCeiling;
	
	@In(name="beta alpha", description="The alpha value to use in the lower bound of the beta model.")
	public double betaAlpha;
	
	@In(name="data")
	public DGraph<String> data;
	
	@In(name="data name")
	public String dataName;
	
	@In(name="max motifs")
	public int maxMotifs;
	
	@In(name="min freq")
	public int minFreq;
	
	private NaturalComparator<String> comparator;
 	
	@Main(print=false)
	public void main() throws IOException
	{		
		data = Graphs.toSimpleDGraph(data);
		data = Graphs.blank(data, "");

		Global.log().info("Computing beta model code length");
		int its = iterations(data.size());
		Global.log().info("-- beta model: using " +its+ " iterations");
		
		DSequenceModel<String> model = new DSequenceModel<String>(data, its);
		
		double baselineEstimate = model.logNumGraphs();
		LogBCaCI ci = new LogBCaCI(model.logSamples(), USequenceModel.BOOTSTRAP_SAMPLES);
		double baselineLowerBound = ci.lowerBound(betaAlpha);
	
		Global.log().info("Computing motif code lengths");
		
		DPlainMotifExtractor<String> ex 
			= new DPlainMotifExtractor<String>(data, motifSamples, motifMinSize, motifMaxSize, minFreq);
		
		List<Double> estimates = new ArrayList<Double>(ex.subgraphs().size());
		List<Double> cis = new ArrayList<Double>(ex.subgraphs().size());
		List<Double> frequencies = new ArrayList<Double>(ex.subgraphs().size());
		List<DGraph<String>> subs = new ArrayList<DGraph<String>>(ex.subgraphs());
		
		if(subs.size() > maxMotifs)
			subs = new ArrayList<DGraph<String>>(subs.subList(0, maxMotifs));
		
		for(DGraph<String> sub : subs)
		{
			System.out.println("Analysing sub " + sub);
			Pair<Double, Double> size = size(data, sub, ex.occurrences(sub), true); 

			double profitEstimate = baselineEstimate - size.first(); 
			double profitCI = baselineLowerBound - size.second();
			double subFrequency = ex.frequency(sub);
			 
			System.out.println("baseline: " + baselineEstimate + "(> "+ baselineLowerBound +")");
			System.out.println("motif code: " + size.first() + "(< "+ size.second() +")");

			System.out.println("profit: " + profitEstimate + "(> "+ profitCI +")");
			System.out.println("freq: " + subFrequency);
			
			estimates.add(profitEstimate);
			cis.add(profitCI);
			frequencies.add(subFrequency);
		}
		
		Comparator<Double> comp = Functions.natural();
		org.lilian.util.Functions.sort(estimates, Collections.reverseOrder(comp), (List) cis, (List) frequencies, (List) subs);
		
		File numbersFile = new File(Global.getWorkingDir(), "numbers.csv");

		
		BufferedWriter numbersWriter = new BufferedWriter(new FileWriter(numbersFile));
		for(int i : series(subs.size()))
			numbersWriter.write(estimates.get(i) + ", " + frequencies.get(i) + ", " + cis.get(i) + "\n");		
		numbersWriter.close();

		int i = 0;
		for(DGraph<String> sub : subs)
		{
			File graphFile = new File(Global.getWorkingDir(), String.format("motif.%03d.edgelist", i));
			Data.writeEdgeList(sub, graphFile);
			
			i++;
		}
		
		BufferedWriter titleWriter = new BufferedWriter(new FileWriter(new File(Global.getWorkingDir(), "title.txt")));
		titleWriter.write("data: "+dataName+", null: beta");
		titleWriter.close();
		
		// * signal that these are directed graphs
		File directed = new File(Global.getWorkingDir(), "directed.txt");
		directed.createNewFile();
	}

	private int iterations(int size)
	{
		return (int)(betaCeiling / size);
	}

	public Pair<Double, Double> size(DGraph<String> graph, DGraph<String> sub,
			List<List<Integer>> occurrences, boolean resetWiring)
	{
		List<List<Integer>> wiring = new ArrayList<List<Integer>>();
		DGraph<String> subbed = MotifCompressor.subbedGraph(graph, sub,
				occurrences, wiring);
		
		// * the beta model can only store simple graphs, so we translate subbed
		//   to a simple graph and store the multiple edges separately 
		FrequencyModel<Pair<Integer, Integer>> removals = new FrequencyModel<Pair<Integer,Integer>>();
		subbed = Graphs.toSimpleDGraph(subbed, removals);
		
		// * The estimate cost of storing the structure of the motif and the 
		//   structure of the subbed graph. 
		int its = iterations(subbed.size());
		DSequenceModel<String> motifModel = new DSequenceModel<String>(sub, its);
		DSequenceModel<String> subbedModel = new DSequenceModel<String>(subbed, its);
		
		System.out.println("motif  size: " + motifModel.logNumGraphs());
		System.out.println("subbed size: " + subbedModel.logNumGraphs());
		
		List<Double> samples = new ArrayList<Double>(its);
		for(int i : series(its))
			samples.add(motifModel.logSamples().get(i) + subbedModel.logSamples().get(i));
		
		LogBCaCI bca = new LogBCaCI(samples, USequenceModel.BOOTSTRAP_SAMPLES);
		
		double betaEstimate = bca.logMean(); 
		double betaUpperBound = bca.upperBound(betaAlpha);
		
		// * The rest of the graph (for which we can compute the code length 
		//   directly) 
		double rest = 0.0;
				
		// * the size of the motif
		rest += org.nodes.compression.Functions.prefix(sub.size());
		// * degree sequence of the motif
		rest += degreesSize(DSequenceModel.sequence(sub));
		System.out.println("motif degree size: " + degreesSize(DSequenceModel.sequence(sub)));
		
		// * size of the subbed graph
		rest += org.nodes.compression.Functions.prefix(subbed.size());
		// * degree sequence of subbed
		rest += degreesSize(DSequenceModel.sequence(subbed));
		System.out.println("subbed degree size: " + degreesSize(DSequenceModel.sequence(subbed)));
		
		// * Any node pairs with multiple links
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
		rest += multiEdgeBits;
		
		System.out.println("removals " + multiEdgeBits);
		
		// * Store the labels
		double labelBits = 0.0;
		OnlineModel<Integer> model = new OnlineModel<Integer>(Arrays.asList(
			new Integer(0), new Integer(1)));

		for (DNode<String> node : subbed.nodes())
			labelBits += - Functions.log2(model.observe(node.label().equals(
				MotifCompressor.MOTIF_SYMBOL) ? 0 : 1));
		
		System.out.println("labels " + labelBits);
		
		rest += labelBits;
		
		// * Store the rewiring information
		double wiringBits = wiringBits(sub, wiring, resetWiring);
		System.out.println("wiring " + wiringBits);
		
		rest += wiringBits;
		
		return new Pair<Double, Double>(rest + betaEstimate, rest + betaUpperBound);
	}
	
	public double degreesSize(List<DSequenceModel.D> degrees)
	{
		double sum = 0.0;
		
		for(DSequenceModel.D degree : degrees)
			sum += 
				org.nodes.compression.Functions.prefix(degree.in()) + 
				org.nodes.compression.Functions.prefix(degree.out());
		
		return sum;
	}

	public double wiringBits(DGraph<String> sub, List<List<Integer>> wiring,
			boolean reset)
	{
		OnlineModel<Integer> om = new OnlineModel<Integer>(Series.series(sub
				.size()));

		double bits = 0.0;
		for (List<Integer> motifWires : wiring)
		{
			// System.out.println("s"+motifWires.size());
			if (reset)
				om = new OnlineModel<Integer>(Series.series(sub.size()));

			for (int wire : motifWires)
				bits += -log2(om.observe(wire));
		}

		return bits;
	}
}
