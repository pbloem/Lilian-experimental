package org.lilian.motifs;

import static org.nodes.util.Functions.log2Choose;
import static org.nodes.util.Pair.p;
import static org.nodes.util.Series.series;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

import org.data2semantics.platform.annotation.In;
import org.data2semantics.platform.annotation.Main;
import org.json.simple.JSONObject;
import org.data2semantics.platform.Global;
import org.lilian.motifs.Compare.NullModel;
import org.lilian.util.Series;
import org.nodes.Graph;
import org.nodes.Graphs;
import org.nodes.Link;
import org.nodes.Node;
import org.nodes.UGraph;
import org.nodes.UNode;
import org.nodes.UTGraph;
import org.nodes.algorithms.Nauty;
import org.nodes.compression.BinomialCompressor;
import org.nodes.data.Data;
import org.nodes.models.MotifSearchModel;
import org.nodes.motifs.UPlainMotifExtractor;
import org.nodes.random.RandomGraphs;
import org.nodes.util.FrequencyModel;
import org.nodes.util.Functions;
import org.nodes.util.Pair;

public class Synthetic
{
	
	@In(name="n")
	public int n;
	
	@In(name="m")
	public int m;
	
	@In(name="n prime")
	public int nPrime;
	
	@In(name="m prime")
	public int mPrime;
	
	@In(name="num instances")
	public List<Integer> numsInstances;
	
	@In(name="max degree")
	public int maxDegree;
	
	@In(name="motif samples")
	public int motifSamples;
	
	@In(name="runs")
	public int runs;

	private UGraph<String> sub;
	
	private Map<Pair<UGraph<String>,Integer>, List<Run>> all = 
			new LinkedHashMap<Pair<UGraph<String>,Integer>, List<Run>>();
	
	// * encountered subgraphs in the correct order
	private List<UGraph<String>> subs;
	
	// * sums of all factors seen for each subgraph
	private Map<Integer, FrequencyModel<UGraph<String>>> sumFactors = 
			new LinkedHashMap<Integer, FrequencyModel<UGraph<String>>>();
	
	@Main
	public void main() throws IOException
	{
		subs = Graphs.allIsoConnected(nPrime, "");
		System.out.println(subs.size()  + " " + subs);
		
		// * Sample the subgraph
		sub = RandomGraphs.random(nPrime, mPrime);
		sub = Graphs.blank(sub, "");
		
		for(int numInstances : numsInstances)
			sumFactors.put(numInstances, new FrequencyModel<UGraph<String>>());
		
		// * perform the experiments
		for(int numInstances : numsInstances)
			for(int run : series(runs))
				run(numInstances, run);
		
		sub = (UGraph<String>)Nauty.canonize(sub);
		
		// * sort the subgraphs
		sort();
		
		// * collate the data and print to CSVs
		out();
		
		try
		{
			org.data2semantics.platform.util.Functions.python(Global.getWorkingDir(), "motifs/plot.synthetic.py");
		} catch (InterruptedException e)
		{
			System.out.println("Failed to run plot script. " + e);
		}
	}

	private void sort()
	{
		List<Double> maxFactors = new ArrayList<Double>(subs.size());
		for(UGraph<String> sub : subs)
		{
			List<Double> sums = new ArrayList<Double>(numsInstances.size());
			for(int numInstances : numsInstances)
				sums.add(sumFactors.get(numInstances).frequency(sub));
			maxFactors.add(absMax(sums));
		}

		Comparator<Double> natural = Functions.natural();
		org.lilian.util.Functions.sort(maxFactors, Collections.reverseOrder(natural), subs);
	}
	
	/**
	 * Returns the element in the list with the greatest absolute magnitude
	 * @param values
	 * @return
	 */
	private double absMax(List<Double> values)
	{
		double maxMag = Double.NEGATIVE_INFINITY;
		double elem = -1.0;
		
		for(double value : values)
			if(Math.abs(value) > maxMag)
			{
				maxMag = Math.abs(value);
				elem = value;
			}
		
		return elem;
	}

	public void run(int numInstances, int run)
	{	
		// * Sample the compressed graph
		UGraph<String> graph = RandomGraphs.random(n - numInstances * (sub.size() - 1), m - numInstances * sub.numLinks());
		
		Global.log().info("Finished sampling subbed graph");
		
		List<UNode<String>> candidates = new ArrayList<UNode<String>>(graph.size());
		for(UNode<String> node : graph.nodes())
			candidates.add(node);
		
		// * remove all candidates with too high degree
		Iterator<UNode<String>> it = candidates.iterator();
		while(it.hasNext())
			if(it.next().degree() > maxDegree)
				it.remove();
		
		// * Sample a random subset of instances
		List<UNode<String>> instances = Functions.subset(candidates, numInstances);
		
		// * Sample a random multinomial
		List<Double> probs = Functions.randomMultinomial(nPrime);
		
		// * Wire in the instances
		for(Node<String> instance : instances)
		{
			List<Node<String>> newNodes = new ArrayList<Node<String>>();
			// * Add motif nodes
			for(Node<String> subNode : sub.nodes())
				newNodes.add(graph.add(subNode.label()));
			
			// * Wire them up internally
			for(Link<String> link : sub.links())
			{
				int i = link.first().index();
				int j = link.second().index();
				
				newNodes.get(i).connect(newNodes.get(j));
			}
			
			// * Wire them up externally
			for(Link<String> link : instance.links())
			{
				Node<String> other = link.other(instance);
				// - choose a random node inside the motif
				int inside = Functions.choose(probs, 1.0);
				other.connect(newNodes.get(inside));
			}
			
			// * remove the instance node
			instance.remove();
		}
		
		// - Not really necessary...
		graph = Graphs.blank(graph, "");
		
		Global.log().info("Graph reconstructed. nodes: " + graph.size() + ", links: " + graph.numLinks());
		
		// * Perform the motif search
		UPlainMotifExtractor<String> ex = new UPlainMotifExtractor<String>(graph, motifSamples, nPrime);
		
		double baseline = BinomialCompressor.undirected(graph, true);
		Global.log().info("baseline " + baseline);
		
		int nn = graph.size(), nl = graph.numLinks();
		Global.log().info("choose: " + log2Choose(nl, (nn*nn-nn)/2 ));
		
		for(UGraph<String> s : subs)
		{
			List<List<Integer>> occurrences =  ex.occurrences(s);
			if(occurrences == null)
				occurrences = Collections.emptyList();
			
			Global.log().info("Analysing sub: " + s);
			double motifSize = MotifSearchModel.sizeER(graph, s, occurrences, true);
			Global.log().info("motif size: " + motifSize);
			double factor = baseline - motifSize;
			
			sumFactors.get(numInstances).add(s, factor);
			
			new Run(s, numInstances, run, ex.frequency(s), factor);
		}
	}

	private class Run
	{
		UGraph<String> sub;
		int instances;
		int run;
		
		double frequency;
		double factor;
		
		public Run(UGraph<String> sub, int instances, int run, double frequency, double factor)
		{
			super();
			this.sub = sub;
			this.instances = instances;
			this.run = run;
			
			this.frequency = frequency;
			this.factor = factor;
			
			Pair<UGraph<String>, Integer> pair = Pair.p(sub, instances);
			if(! all.containsKey(pair))
			{
				List<Run> list = new ArrayList<Run>(runs);
				for(int i : series(runs))
					list.add(null);
				all.put(pair, list);
			}
			
			all.get(pair).set(run, this);
		}
		
		public int instances()
		{
			return instances;
		}
		
		public UGraph<String> sub()
		{
			return sub;
		}
		public int run()
		{
			return run;
		}
		
		public double frequency()
		{
			return frequency;
		}
		
		public double factor()
		{
			return factor;
		}

		@Override
		public String toString()
		{
			return  frequency + "_" + factor;
		}
	}

	private void out() throws IOException
	{
		File frequenciesFile = new File(Global.getWorkingDir(), "frequencies.csv");
		File factorsFile = new File(Global.getWorkingDir(), "factors.csv");
		File meansFile = new File(Global.getWorkingDir(), "means.csv");
		
		BufferedWriter frequencies = new BufferedWriter(new FileWriter(frequenciesFile));
		BufferedWriter factors = new BufferedWriter(new FileWriter(factorsFile));
		BufferedWriter means = new BufferedWriter(new FileWriter(meansFile));
		
		System.out.println(subs);
		
		int i = 0, subIndex = -1;
		for(UGraph<String> sub : subs)
		{
			System.out.println(sub);
			
			if(sub.equals(this.sub))
				subIndex = i;

			// * Write the subgraph
			File graphFile = new File(Global.getWorkingDir(), String.format("motif.%03d.edgelist", i));
			Data.writeEdgeList(sub, graphFile);
			
			// * Write the frequencies, 
			FrequencyModel<Integer> freqSums = new FrequencyModel<Integer>();
			FrequencyModel<Integer> factSums = new FrequencyModel<Integer>();
			
			int c = 0;
			for(int numInstances : numsInstances)
				for(int runIndex : series(runs))
				{
					Run run = all.get(p(sub, numInstances)).get(runIndex);
					frequencies.write((c==0 ? "" : ", ") + run.frequency());
					factors.write((c++==0 ? "" : ", ") + run.factor());
					
					freqSums.add(numInstances, run.frequency());
					factSums.add(numInstances, run.factor());
				}
			
			frequencies.write("\n");
			factors.write("\n");
			
			c = 0;
			for(int numInstances : numsInstances)
				means.write((c++==0 ? "" : ", ") + (freqSums.frequency(numInstances) / (double)runs));
			
			for(int numInstances : numsInstances)
				means.write( ", " + (factSums.frequency(numInstances) / (double)runs));
			means.write("\n");
			
			i++;
		}
		
		JSONObject obj = new JSONObject();
		obj.put("subindex", subIndex);
		obj.put("nums instances", numsInstances);
		obj.put("motif size", nPrime);

		Functions.write(obj.toString(), new File(Global.getWorkingDir(), "metadata.json"));
		
		frequencies.close();
		means.close();
		factors.close();
	}
}
