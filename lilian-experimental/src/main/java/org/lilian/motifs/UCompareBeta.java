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
import org.data2semantics.platform.util.FrequencyModel;
import org.lilian.util.Functions.NaturalComparator;
import org.nodes.DGraph;
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
import org.nodes.models.USequenceModel;
import org.nodes.motifs.MotifCompressor;
import org.nodes.motifs.UPlainMotifExtractor;
import org.nodes.random.RandomGraphs;
import org.nodes.random.SubgraphGenerator;
import org.nodes.util.Functions;
import org.nodes.util.FunctionsTest;
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
public class UCompareBeta
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
	public UGraph<String> data;
	
	@In(name="max motifs")
	public int maxMotifs;
	
	private NaturalComparator<String> comparator;
 	
	@Main(print=false)
	public void main() throws IOException
	{		
		data = Graphs.toSimpleUGraph(data);
		data = Graphs.blank(data, "");

		Global.log().info("Computing beta model code length");
		int its = iterations(data.size());
		Global.log().info("-- beta model: using " +its+ " iterations");
		
		USequenceModel<String> model = new USequenceModel<String>(data, its);
		
		double baselineEstimate = model.logNumGraphs();
		double baselineLowerBound = model.confidence(betaAlpha, CIMethod.BCA, CIType.LOWER_BOUND).first();
	
		Global.log().info("Computing motif code lengths");
		
		UPlainMotifExtractor<String> ex 
			= new UPlainMotifExtractor<String>(
					data, motifSamples, motifMinSize, motifMaxSize);
		
		List<Double> estimates = new ArrayList<Double>(ex.subgraphs().size());
		List<Double> cis = new ArrayList<Double>(ex.subgraphs().size());
		List<Double> frequencies = new ArrayList<Double>(ex.subgraphs().size());
		List<UGraph<String>> subs = new ArrayList<UGraph<String>>(ex.subgraphs());
		
		if(subs.size() > maxMotifs)
			subs = new ArrayList<UGraph<String>>(subs.subList(0, maxMotifs));
		
		for(UGraph<String> sub : subs)
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
		File graphsFile = new File(Global.getWorkingDir(), "graphs.csv");
		
		BufferedWriter numbersWriter = new BufferedWriter(new FileWriter(numbersFile));
		BufferedWriter graphsWriter  = new BufferedWriter(new FileWriter(graphsFile));
		
		for(int i : series(subs.size()))
		{
			numbersWriter.write(estimates.get(i) + ", "  + cis.get(i) + ", " + frequencies.get(i) + "\n");
			graphsWriter.write(subs.get(i).toString() + "\n");
		}
		
		numbersWriter.close();
		graphsWriter.close();
	}

	private int iterations(int size)
	{
		return (int)(betaCeiling / size);
	}

	public Pair<Double, Double> size(UGraph<String> graph, UGraph<String> sub,
			List<List<Integer>> occurrences, boolean resetWiring)
	{
		List<List<Integer>> wiring = new ArrayList<List<Integer>>();
		UGraph<String> subbed = MotifCompressor.subbedGraph(graph, sub,
				occurrences, wiring);
		
		// * The estimate cost of storing the structure of the motif and the 
		//   structure of the subbed graph. 

		int its = iterations(subbed.size());
		USequenceModel<String> motifModel = new USequenceModel<String>(sub, its);
		USequenceModel<String> subbedModel = new USequenceModel<String>(subbed, its);
		
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
		rest += degreesSize(Graphs.degrees(sub));
		
		// * size of the subbed graph
		rest += org.nodes.compression.Functions.prefix(subbed.size());
		// * degree sequence of subbed
		rest += degreesSize(Graphs.degrees(subbed));
		
		// * Store the labels
		OnlineModel<Integer> model = new OnlineModel<Integer>(Arrays.asList(
			new Integer(0), new Integer(1)));

		for (UNode<String> node : subbed.nodes())
			rest += - Functions.log2(model.observe(node.label().equals(
				MotifCompressor.MOTIF_SYMBOL) ? 0 : 1));
		
		// * Store the rewiring information
		rest += wiringBits(sub, wiring, resetWiring);
		
		return new Pair<Double, Double>(rest + betaEstimate, rest + betaUpperBound);
	}
	
	public double degreesSize(List<Integer> degrees)
	{
		double sum = 0.0;
		
		for(int degree : degrees)
			sum += org.nodes.compression.Functions.prefix(degree);
		return sum;
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
}
