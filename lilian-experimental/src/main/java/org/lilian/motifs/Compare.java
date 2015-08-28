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
	
	@In(name="beta ceiling", description="An indicator for the number of iterations to use for the beta model. The number of iteration is the size of the graph divided by the ceiling.")
	public int betaCeiling;
	
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

	public static enum NullModel{ER, EDGELIST, NEIGHBORLIST}
	
	private NaturalComparator<String> comparator;
	private boolean directed;

	private boolean resets = true;
	
	@Main(print=false)
	public void main() throws IOException
	{
		directed = data instanceof DGraph<?>;
		Global.log().info("Is data directed? : " + directed + " ("+data.getClass()+")");
		
		if(directed)
			data = Graphs.toSimpleDGraph((DGraph<String>)data);
		else
			data = Graphs.toSimpleUGraph(data);
		data = Graphs.blank(data, "");
		
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
			
		List<Double> factorsER = new ArrayList<Double>(subs.size());
		List<Double> factorsEL = new ArrayList<Double>(subs.size());
		List<Double> factorsBeta =  new ArrayList<Double>(subs.size());
				
		double baselineER = size(data, NullModel.ER);
		double baselineEL = size(data, NullModel.EDGELIST);
		double baselineBeta = sizeBeta(data).lowerBound(betaAlpha);
		
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
	
	private int iterations(int size)
	{
		return (int)(betaCeiling / size);
	}
	
	public double size(Graph<String> graph, NullModel nullModel)
	{
		switch (nullModel) {
		case ER:
			return directed ?
				 BinomialCompressor.directed((DGraph<String>)graph) :
				 BinomialCompressor.undirected((UGraph<String>)graph);
		case EDGELIST:
			return directed ?
				EdgeListCompressor.undirected((DGraph<String>)graph) :	
				EdgeListCompressor.undirected((UGraph<String>)graph);
		case NEIGHBORLIST:
			return directed ?
					NeighborListCompressor.undirected((DGraph<String>)graph) : 	
				NeighborListCompressor.undirected((UGraph<String>)graph); 
		default:
			throw new IllegalStateException("Null model not recognized");
		}
	}
	
	public LogNormalCI sizeBeta(Graph<String> data)
	{
		Global.log().info("Computing beta model code length");
		int its = iterations(data.size());
		Global.log().info("-- beta model: using " + its + " iterations");
		
		if(directed)
		{
			DSequenceModel<String> model = new DSequenceModel<String>((DGraph<String>)data, its);
			return new LogNormalCI(model.logSamples(), BS_SAMPLES);
		}	
			
		USequenceModel<String> model = new USequenceModel<String>(data, its);
		return new LogNormalCI(model.logSamples(), BS_SAMPLES);
	}
	
	public double size(Graph<String> graph, Graph<String> sub,
			List<List<Integer>> occurrences, NullModel nullModel, boolean resetWiring)
	{
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
		
		double bits = 0.0;
		
		bits += size(sub, nullModel);
		bits += size(subbed, nullModel);
		
		// * Store the labels
		OnlineModel<Integer> model = new OnlineModel<Integer>(Arrays.asList(
			new Integer(0), new Integer(1)));

		for (Node<String> node : subbed.nodes())
			bits += - Functions.log2(model.observe(node.label().equals(
				MotifCompressor.MOTIF_SYMBOL) ? 0 : 1));
		
		// * Any node pairs with multiple links
		if(isSimpleGraphCode(nullModel))
		{
			int maxRemovals = (int)(removals.frequency(removals.maxToken()));
			OnlineModel<Integer> frequencies = new OnlineModel<Integer>(Series.series(maxRemovals + 1));
			for(Link<String> link : subbed.links())
			{
				Pair<Integer, Integer> pair;
				
				if(directed)
				{
					pair = new Pair<Integer, Integer>(link.first().index(), link.second().index());
				} else
				{
					int minor = Math.min(link.first().index(), link.second().index());
					int major = Math.max(link.first().index(), link.second().index());
					pair = new Pair<Integer, Integer>(minor, major);
				}
				
				int freq = (int) removals.frequency(pair);
				bits += - Functions.log2(frequencies.observe(freq));
			}
		}
		
		// * Store the rewiring information
		bits += wiringBits(sub, wiring, resetWiring);
		
		return bits;
	}
	
	/**
	 * 
	 * @param graph
	 * @param sub
	 * @param occurrences
	 * @param resetWiring
	 * @return A pair containing a logbca model over the uncertain element of 
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

		int its = iterations(subbed.size());
		List<Double> samples = new ArrayList<Double>(its);
		if(directed)
		{
			DSequenceModel<String> motifModel = new DSequenceModel<String>((DGraph<String>)sub, its);
			DSequenceModel<String> subbedModel = new DSequenceModel<String>((DGraph<String>)subbed, its);
			
			for(int i : series(its))
				samples.add(motifModel.logSamples().get(i) + subbedModel.logSamples().get(i));
		} else
		{
			USequenceModel<String> motifModel = new USequenceModel<String>((UGraph<String>)sub, its);
			USequenceModel<String> subbedModel = new USequenceModel<String>((UGraph<String>)subbed, its);
			
			for(int i : series(its))
				samples.add(motifModel.logSamples().get(i) + subbedModel.logSamples().get(i));
		}
		
		LogNormalCI ci = new LogNormalCI(samples, BS_SAMPLES);
		
		// * The rest of the graph (for which we can compute the code length 
		//   directly) 
		double rest = 0.0;
		
		// * the size of the motif
		rest += org.nodes.compression.Functions.prefix(sub.size());
		// * degree sequence of the motif
		if(directed)
			rest += degreesDSize(DSequenceModel.sequence((DGraph<String>)sub));
		else
			rest += degreesUSize(Graphs.degrees(sub));
		
		
		// * size of the subbed graph
		rest += org.nodes.compression.Functions.prefix(subbed.size());
		// * degree sequence of subbed
		if(directed)
			rest += degreesDSize(DSequenceModel.sequence((DGraph<String>)subbed));
		else
			rest += degreesUSize(Graphs.degrees(subbed));
		
//		System.out.println("degrees and sizes: " + rest);
		
		// * Store the labels
		OnlineModel<Integer> model = new OnlineModel<Integer>(Arrays.asList(
			new Integer(0), new Integer(1)));

		double labelBits = 0.0;
		for (Node<String> node : subbed.nodes())
			labelBits += - Functions.log2(model.observe(node.label().equals(
				MotifCompressor.MOTIF_SYMBOL) ? 0 : 1));
//		System.out.println("labels : " + labelBits);
		rest += labelBits;
		
		// * Any node pairs with multiple links
		
		double remBits = 0.0;
		int maxRemovals =(int)(removals.frequency(removals.maxToken()));
		OnlineModel<Integer> frequencies = new OnlineModel<Integer>(Series.series(maxRemovals + 1));
		for(Link<String> link : subbed.links())
		{
			Pair<Integer, Integer> pair;
			
			if(directed)
			{
				pair = new Pair<Integer, Integer>(link.first().index(), link.second().index());
			} else
			{
				int minor = Math.min(link.first().index(), link.second().index());
				int major = Math.max(link.first().index(), link.second().index());
				pair = new Pair<Integer, Integer>(minor, major);
			}
			
			int freq = (int) removals.frequency(pair);
			remBits += - Functions.log2(frequencies.observe(freq));
		}
		
//		System.out.println("removals: " + remBits);
		rest += remBits;
		
		// * Store the rewiring information
		double wiringBits = wiringBits(sub, wiring, resetWiring);
//		System.out.println("wiring: " + wiringBits);
		
		return new Pair<LogNormalCI, Double>(ci, rest);
	}
	
	public double degreesDSize(List<DSequenceModel.D> degrees)
	{
		double sum = 0.0;
		
		for(DSequenceModel.D degree : degrees)
			sum += 
				org.nodes.compression.Functions.prefix(degree.in()) + 
				org.nodes.compression.Functions.prefix(degree.out());
		
		return sum;
	}
	
	public double degreesUSize(List<Integer> degrees)
	{
		double sum = 0.0;
		
		for(int degree : degrees)
			sum += org.nodes.compression.Functions.prefix(degree);
		return sum;
	}
	
	public double wiringBits(Graph<String> sub, List<List<Integer>> wiring,
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
