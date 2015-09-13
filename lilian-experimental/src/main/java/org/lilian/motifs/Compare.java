package org.lilian.motifs;

import static org.nodes.util.Functions.log2Choose;
import static org.nodes.util.Functions.log2Factorial;
import static org.nodes.util.OnlineModel.storeSequence;
import static org.nodes.util.OnlineModel.storeSequenceML;
import static org.nodes.util.Series.series;
import static org.lilian.util.Functions.log2;
import static org.nodes.Graphs.degrees;
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
import org.nodes.Graph;
import org.nodes.Graphs;
import org.nodes.Link;
import org.nodes.Node;
import org.nodes.Subgraph;
import org.nodes.UGraph;
import org.nodes.ULink;
import org.nodes.UNode;
import org.nodes.algorithms.Nauty;
import org.nodes.clustering.ConnectionClusterer;
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
public class Compare
{
	private static final int BS_SAMPLES = 10000;

	@In(name="motif samples")
	public int motifSamples;
	
	@In(name="motif min size", description="minimum motif size (inclusive)")
	public int motifMinSize;
	
	@In(name="motif max size", description="maximum motif size (inclusive!)")
	public int motifMaxSize;
	
	@In(name="beta iterations", description="Number of iteration to use for the beta model.")
	public int betaIterations;
	
	@In(name="beta alpha", description="The alpha value to use in the lower bound of the beta model.")
	public double betaAlpha;
		
	@In(name="data")
	public Graph<String> data;
	
	@In(name="data name")
	public String dataName = "";
	
	@In(name="max motifs")
	public int maxMotifs;
	
	@In(name="minimum frequency")
	public int minFreq;

	@In(name="blank", description="Whether to 'blank' the data (ie. set all labels to a common value).")
	public boolean blank;
	
	@In(name="simplify", description="Whether to remove multiple edges and self-loops.")
	public boolean simplify;
	
	public static final int NUM_THREADS = Runtime.getRuntime().availableProcessors();;
	
	public static enum NullModel{ER, EDGELIST}
	
	private boolean directed;

	private boolean resets = true;
	
	@Main(print=false)
	public void main() throws IOException
	{
		org.nodes.Global.secureRandom(42);
		
		directed = data instanceof DGraph<?>;
		Global.log().info("Is data directed? : " + directed + " ("+data.getClass()+")");
		if(simplify)
		{
			if(directed)
				data = Graphs.toSimpleDGraph((DGraph<String>)data);
			else
				data = Graphs.toSimpleUGraph(data);
		}
		
		if(blank)
		{
			Global.log().info("Blanking.");
			data = Graphs.blank(data, "");
			Global.log().info("Blanked.");
		}
		
		Global.log().info("data nodes: " + data.size());
		Global.log().info("data links: " + data.numLinks());
		
		List<Integer> degrees = Graphs.degrees(data);
		Collections.sort(degrees, Collections.reverseOrder());
		
		Global.log().info("Computing motif code lengths");
		
		List<? extends Graph<String>> subs;
		List<Double> frequencies;
		List<List<List<Integer>>> occurrences;

		if(directed)
		{
			DPlainMotifExtractor<String> ex 
			= new DPlainMotifExtractor<String>(
					(DGraph<String>)data, motifSamples, motifMinSize, motifMaxSize, minFreq);
		
			subs = new ArrayList<Graph<String>>(ex.subgraphs());
			
			Collections.reverse(subs);
			
			frequencies = new ArrayList<Double>(subs.size());
			for(Graph<String> sub : subs)
				frequencies.add(ex.frequency((DGraph<String>)sub));
			
			occurrences = new ArrayList<List<List<Integer>>>(subs.size());
			for(Graph<String> sub : subs)
				occurrences.add(ex.occurrences((DGraph<String>)sub));
		} else
		{	
			UPlainMotifExtractor<String> ex 
				= new UPlainMotifExtractor<String>(
						(UGraph<String>)data, motifSamples, motifMinSize, motifMaxSize, minFreq);
			
			subs = new ArrayList<Graph<String>>(ex.subgraphs());
			frequencies = new ArrayList<Double>(subs.size());
			for(Graph<String> sub : subs)
				frequencies.add(ex.frequency((UGraph<String>)sub));
			
			occurrences = new ArrayList<List<List<Integer>>>(subs.size());
			for(Graph<String> sub : subs)
				occurrences.add(ex.occurrences((UGraph<String>)sub));
		}
		
		if(subs.size() > maxMotifs)
		{
			subs = new ArrayList<Graph<String>>(subs.subList(0, maxMotifs));
			frequencies = new ArrayList<Double>(frequencies.subList(0, maxMotifs));
		}
		
		
		System.out.println(frequencies);
			
		List<Double> factorsER = new ArrayList<Double>(subs.size());
		List<Double> factorsEL = new ArrayList<Double>(subs.size());
		List<Double> factorsBeta =  new ArrayList<Double>(subs.size());
				
		double baselineER = size(data, NullModel.ER, false);
		double baselineEL = size(data, NullModel.EDGELIST, false);
		Pair<LogNormalCI, Double> pairBeta = sizeBeta(data);
		double baselineBeta = pairBeta.first().lowerBound(betaAlpha) + pairBeta.second();
		
		System.out.println("Difference between estimate and lowerbound: " + (pairBeta.first().mlMean() - pairBeta.first().lowerBound(betaAlpha)));
		System.out.println("Cost of storing degrees: " + pairBeta.second());

		
		for(int i : series(subs.size()))
		{
			Graph<String> sub = subs.get(i);
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

			Global.log().info("null model: Beta");
			{
				Pair<LogNormalCI, Double> pair = sizeBeta(data, sub, occs, resets); 
				
				System.out.println("Difference between estimate and upperbound: " + (pair.first().upperBound(betaAlpha) - pair.first().mlMean()));
				
				double sizeBeta = pair.first().upperBound(betaAlpha) + pair.second();
				double factorBeta = baselineBeta - sizeBeta;
				factorsBeta.add(factorBeta);
			 
				Global.log().info("Beta baseline: " + baselineBeta);
				Global.log().info("Beta motif code: " + sizeBeta);
				Global.log().info("Beta factor: " + factorBeta);
			}
		}
		
		Comparator<Double> comp = Functions.natural();
		org.lilian.util.Functions.sort(
				frequencies, Collections.reverseOrder(comp), 
				(List) factorsER, 
				(List) factorsEL, 
				(List) factorsBeta, 
				(List) subs);
		
		File numbersFile = new File(Global.getWorkingDir(), "numbers.csv");
		
		BufferedWriter numbersWriter = new BufferedWriter(new FileWriter(numbersFile));
		for(int i : series(subs.size()))
			numbersWriter.write(frequencies.get(i) + ", " + factorsER.get(i) + ", " + factorsEL.get(i) + ", " + factorsBeta.get(i) + "\n");		
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
		obj.put("directed", directed);
		obj.put("baseline er", baselineER);
		obj.put("baseline el", baselineEL);
		obj.put("baseline beta", baselineBeta);
		Functions.write(obj.toString(), new File(Global.getWorkingDir(), "metadata.json"));
		
		try
		{
			org.data2semantics.platform.util.Functions.python(Global.getWorkingDir(), "motifs/plot.py");
		} catch (InterruptedException e)
		{
			System.out.println("Failed to run plot script. " + e);
		}
	}

	public static double size(Graph<String> graph, NullModel nullModel, boolean withPrior)
	{
		boolean directed = (graph instanceof DGraph<?>); 
		
		switch (nullModel) {
		case ER:
			return directed ?
				 BinomialCompressor.directed((DGraph<String>)graph, true, withPrior) :
				 BinomialCompressor.undirected((UGraph<String>)graph, true, withPrior);
		case EDGELIST:
			return directed ?
				EdgeListCompressor.directed((DGraph<String>) graph, withPrior) :	
				EdgeListCompressor.undirected((UGraph<String>) graph, withPrior);
		default:
			throw new IllegalStateException("Null model not recognized");
		}
	}
	
	public Pair<LogNormalCI, Double> sizeBeta(Graph<String> data)
	{
		Global.log().info("Computing beta model code length");
		Global.log().info("-- beta model: using " + betaIterations + " iterations");

		LogNormalCI ci;		
		double rest;
		
		if(directed)
		{
			DSequenceModel<String> model = new DSequenceModel<String>((DGraph<String>)data);
			model.nonuniform(betaIterations, NUM_THREADS);

			ci =  new LogNormalCI(model.logSamples(), BS_SAMPLES);
			rest = storeSequenceML(Graphs.inDegrees((DGraph<?>)data)) + storeSequenceML(Graphs.outDegrees((DGraph<?>)data));
			
			System.out.println("Difference between ML and KT " + (storeSequence(Graphs.inDegrees((DGraph<?>)data)) + storeSequence(Graphs.outDegrees((DGraph<?>)data)) - storeSequenceML(Graphs.inDegrees((DGraph<?>)data)) - storeSequenceML(Graphs.outDegrees((DGraph<?>)data))) );
		} else 
		{
			USequenceModel<String> model = new USequenceModel<String>(data);
			model.nonuniform(betaIterations, NUM_THREADS);
			
			ci =  new LogNormalCI(model.logSamples(), BS_SAMPLES);
			rest = storeSequenceML(degrees(data));
		
			System.out.println("Difference between ML and KT " + (storeSequence(degrees(data)) - storeSequenceML(degrees(data))));
		}
		
		return Pair.p(ci, rest);
	}
	
	public static double size(Graph<String> graph, Graph<String> sub,
			List<List<Integer>> occurrences, NullModel nullModel, boolean resetWiring)
	{
		boolean directed = (graph instanceof DGraph<?>); 

		List<List<Integer>> wiring = new ArrayList<List<Integer>>();
		Graph<String> subbed;
		if(directed)
			subbed = MotifCompressor.subbedGraph((DGraph<String>) graph, (DGraph<String>)sub, occurrences, wiring);
		else
			subbed = MotifCompressor.subbedGraph((UGraph<String>) graph, (UGraph<String>)sub, occurrences, wiring);
		
		FrequencyModel<Pair<Integer, Integer>> removals = new FrequencyModel<Pair<Integer,Integer>>();
		
		if(isSimpleGraphCode(nullModel))
		{
			if(directed)
				subbed = Graphs.toSimpleDGraph((DGraph<String>)subbed, removals);
			else
				subbed = Graphs.toSimpleUGraph((UGraph<String>)subbed, removals);
		}
		
		FrequencyModel<String> bits = new FrequencyModel<String>();
		
		bits.add("sub", size(sub, nullModel, true));

		bits.add("subbed", size(subbed, nullModel, true));
		
		// * Any node pairs with multiple links
		if(isSimpleGraphCode(nullModel))
		{
			List<Integer> additions = new ArrayList<Integer>((int)removals.distinct());
			for(Pair<Integer, Integer> pair : removals.tokens())
				additions.add((int)removals.frequency(pair));
			bits.add("multiple-edges", prefix(additions.isEmpty() ? 0 : Functions.max(additions)));
			bits.add("multiple-edges", OnlineModel.storeSequence(additions)); 
		}
		
		// * Store the rewiring information
		bits.add("wiring", wiringBits(sub, wiring, resetWiring));
		
		// * Store the insertion order, to preserve the precise ordering of the
		//   nodes in the data 
		bits.add("insertions", log2Factorial(graph.size()) - log2Factorial(subbed.size()));

		System.out.println("bits: ");
		bits.print(System.out);
		
		return bits.total();
	}
	
	/**
	 * 
	 * @param graph
	 * @param sub
	 * @param occurrences
	 * @param resetWiring
	 * @return A pair containing a logNormalCI model over the uncertain element of 
	 * the code  a double representing the rest. If p is the resulting value, 
	 * then p.first().logMean() + p.second() is the best estimate of the total 
	 * code length. 
	 */
	public Pair<LogNormalCI, Double> sizeBeta(Graph<String> graph, Graph<String> sub,
			List<List<Integer>> occurrences, boolean resetWiring)
	{
		List<List<Integer>> wiring = new ArrayList<List<Integer>>();
		Graph<String> subbed;
		if(directed)
			subbed = MotifCompressor.subbedGraph((DGraph<String>) graph, (DGraph<String>)sub, occurrences, wiring);
		else
			subbed = MotifCompressor.subbedGraph((UGraph<String>) graph, (UGraph<String>)sub, occurrences, wiring);
				
		// * the beta model can only store simple graphs, so we translate subbed
		//   to a simple graph and store the multiple edges separately 
		FrequencyModel<Pair<Integer, Integer>> removals = new FrequencyModel<Pair<Integer,Integer>>();
		if(directed)
			subbed = Graphs.toSimpleDGraph((DGraph<String>)subbed, removals);
		else
			subbed = Graphs.toSimpleUGraph((UGraph<String>)subbed, removals);
		
		// * The estimated cost of storing the structure of the motif and the 
		//   structure of the subbed graph. 

		List<Double> samples = new ArrayList<Double>(betaIterations);
		if(directed)
		{
			DSequenceModel<String> motifModel = new DSequenceModel<String>((DGraph<String>)sub);
			DSequenceModel<String> subbedModel = new DSequenceModel<String>((DGraph<String>)subbed);
			motifModel.nonuniform(betaIterations, NUM_THREADS);
			subbedModel.nonuniform(betaIterations, NUM_THREADS);
			
			for(int i : series(betaIterations))
				samples.add(motifModel.logSamples().get(i) + subbedModel.logSamples().get(i));
		} else
		{
			USequenceModel<String> motifModel = new USequenceModel<String>((UGraph<String>)sub);
			USequenceModel<String> subbedModel = new USequenceModel<String>((UGraph<String>)subbed);
			motifModel.nonuniform(betaIterations, NUM_THREADS);
			subbedModel.nonuniform(betaIterations, NUM_THREADS);
			
			for(int i : series(betaIterations))
				samples.add(motifModel.logSamples().get(i) + subbedModel.logSamples().get(i));
		}
		
		LogNormalCI ci = new LogNormalCI(samples, BS_SAMPLES);
		
		// * The rest of the graph (for which we can compute the code length 
		//   directly) 
		FrequencyModel<String> rest = new FrequencyModel<String>();
		
		// * the size of the motif
		rest.add("sub", prefix(sub.size()));
		// * degree sequence of the motif
		if(directed)
			rest.add("sub", degreesDSize(DSequenceModel.sequence((DGraph<String>)sub)));
		else
			rest.add("sub", degreesUSize(Graphs.degrees(sub)));
		
		// * size of the subbed graph
		rest.add("subbed", prefix(subbed.size()));
		// * degree sequence of subbed
		if(directed)
			rest.add("subbed", degreesDSize(DSequenceModel.sequence((DGraph<String>)subbed)));
		else
			rest.add("subbed", degreesUSize(Graphs.degrees(subbed)));
		
		// * Store the labels
		rest.add("labels", log2Choose(occurrences.size(), subbed.size())); 
		
		// * Any node pairs with multiple links
		List<Integer> additions = new ArrayList<Integer>((int)removals.distinct());
		for(Pair<Integer, Integer> pair : removals.tokens())
			additions.add((int)removals.frequency(pair));
		
		rest.add("multi-edges", prefix(additions.isEmpty() ? 0 : Functions.max(additions)));
		rest.add("multi-edges", OnlineModel.storeSequence(additions)); 
				
		// * Store the rewiring information
		rest.add("wiring", wiringBits(sub, wiring, resetWiring));
		
		// * Store the insertion order, to preserve the precise ordering of the
		//   nodes in the data 
		rest.add("insertions", log2Factorial(graph.size()) - log2Factorial(subbed.size()));
		
		System.out.println(ci.mlMean() + " " + ci.upperBound(betaAlpha));
		System.out.println("rest: ");
		rest.print(System.out);
		
		return new Pair<LogNormalCI, Double>(ci, rest.total());
	}
	
	public double degreesDSize(List<DSequenceModel.D> degrees)
	{
		double sumKT = 0.0;
		
		int maxIn = Integer.MIN_VALUE, maxOut = Integer.MIN_VALUE;
		for(DSequenceModel.D degree : degrees)
		{
			maxIn = Math.max(maxIn, degree.in());
			maxOut = Math.max(maxOut, degree.out());
		}
		
		sumKT += org.nodes.compression.Functions.prefix(maxIn);
		sumKT += org.nodes.compression.Functions.prefix(maxOut);
		
		OnlineModel<Integer> modelIn = new OnlineModel<Integer>(Series.series(maxIn + 1)); 
		OnlineModel<Integer> modelOut = new OnlineModel<Integer>(Series.series(maxOut + 1)); 
		
		for(DSequenceModel.D degree : degrees)
		{
			sumKT += - Functions.log2(modelIn.observe(degree.in()));
			sumKT += - Functions.log2(modelOut.observe(degree.out()));
		}
		
		return sumKT;
	}
	
	public double degreesUSize(List<Integer> degrees)
	{
		double sumKT = 0.0;
		
		int max = Integer.MIN_VALUE;
		for(int degree : degrees)
			max = Math.max(max, degree);
		
		sumKT += org.nodes.compression.Functions.prefix(max);
		
		OnlineModel<Integer> model = new OnlineModel<Integer>(Series.series(max + 1)); 
		
		for(int degree : degrees)
			sumKT += - Functions.log2(model.observe(degree));
		
		return sumKT;
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
				bits += -log2(om.observe(wire));
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
