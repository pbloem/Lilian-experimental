package org.lilian.motifs;

import static org.nodes.util.Functions.log2;
import static org.nodes.util.Series.series;
import static org.nodes.models.USequenceEstimator.CIMethod;
import static org.nodes.models.USequenceEstimator.CIType;
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
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.data2semantics.platform.Global;
import org.data2semantics.platform.annotation.In;
import org.data2semantics.platform.annotation.Main;
import org.data2semantics.platform.annotation.Module;
import org.data2semantics.platform.annotation.Out;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.lilian.motifs.Compare.NullModel;
import org.lilian.util.Functions.NaturalComparator;
import org.nodes.DGraph;
import org.nodes.DLink;
import org.nodes.DNode;
import org.nodes.Graph;
import org.nodes.Graphs;
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
import org.nodes.models.DSequenceEstimator;
import org.nodes.models.ERSimpleModel;
import org.nodes.models.EdgeListModel;
import org.nodes.models.MotifModel;
import org.nodes.models.MotifSearchModel;
import org.nodes.models.USequenceEstimator;
import org.nodes.motifs.DPlainMotifExtractor;
import org.nodes.motifs.MotifCompressor;
import org.nodes.motifs.UPlainMotifExtractor;
import org.nodes.random.RandomGraphs;
import org.nodes.random.SimpleSubgraphGenerator;
import org.nodes.util.Functions;
import org.nodes.util.Generator;
import org.nodes.util.Generators;
import org.nodes.util.OnlineModel;
import org.nodes.util.Order;
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
		
	@In(name="data", print=false)
	public DGraph<String> data;
	
	@In(name="data name")
	public String dataName = "";
	
	@In(name="max motifs")
	public int maxMotifs;
	
	@In(name="minimum frequency")
	public int minFreq;
	
	@In(name="search depth")
	public int searchDepth;
		
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
		List<Double> maxFactors = new ArrayList<Double>(subs.size());

		double baselineER = (new ERSimpleModel(false)).codelength(data);
		double baselineEL = (new EdgeListModel(false)).codelength(data);
		
		for(int i : series(subs.size()))
		{
			DGraph<String> sub = subs.get(i);
			List<List<Integer>> occs = occurrences.get(i);
			
			Global.log().info("Analysing sub ("+ (i+1) +" of " + subs.size() + "): " + sub);
			Global.log().info("freq: " + frequencies.get(i));
			
			double max = Double.NEGATIVE_INFINITY;

			Global.log().info("null model: ER");
			{
				double sizeER = MotifSearchModel.sizeER(data, sub, occs, resets, searchDepth); 
				double factorER = baselineER - sizeER;
				factorsER.add(factorER);
				 
				Global.log().info("ER baseline: " + baselineER);
				Global.log().info("ER motif code: " + sizeER);
				Global.log().info("ER factor: " + factorER);
				
				max = Math.max(max, factorER);
			}

			Global.log().info("null model: EL");
			{
				double sizeEL = MotifSearchModel.sizeEL(data, sub, occs, resets, searchDepth); 
				double factorEL = baselineEL - sizeEL;
				factorsEL.add(factorEL);
			 
				Global.log().info("EL baseline: " + baselineEL);
				Global.log().info("EL motif code: " + sizeEL);
				Global.log().info("EL factor: " + factorEL);
				
				max = Math.max(max, factorEL);
			}

			maxFactors.add(max);
		}
		
		Comparator<Double> comp = Functions.natural();
		org.lilian.util.Functions.sort(
				factorsEL, Collections.reverseOrder(comp), 
				(List) frequencies,
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
}
