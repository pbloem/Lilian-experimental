package org.lilian.platform.graphs;

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.data2semantics.platform.annotation.In;
import org.data2semantics.platform.annotation.Main;
import org.data2semantics.platform.annotation.Module;
import org.data2semantics.platform.annotation.Out;
import org.data2semantics.platform.util.Series;
import org.nodes.DGraph;
import org.nodes.DTGraph;
import org.nodes.DTNode;
import org.nodes.Graph;
import org.nodes.Graphs;
import org.nodes.Link;
import org.nodes.Subgraph;
import org.nodes.TGraph;
import org.nodes.algorithms.Nauty;
import org.nodes.data.Data;
import org.nodes.data.GML;
import org.nodes.data.RDF;
import org.nodes.random.RandomGraphs;
import org.nodes.random.SimpleSubgraphGenerator;
import org.nodes.random.SubgraphGenerator;
import org.nodes.util.FrequencyModel;
import org.lilian.util.Functions;
import org.nodes.util.Order;
           
@Module(name="Motif extraction", description="Detects frequent subgraphs in networks")
public class DMotifs
{
	
	private DGraph<String> data;
	private DGraph<String> top, second, third;
	private int samples;
	private int depth;
	private double topFreq, secondFreq, thirdFreq;
	private boolean correct, blank;
	
	
	public DMotifs(
			@In(name="samples") int samples,
			@In(name="depth") int depth,
			@In(name="correct frequency") boolean correct,
			@In(name="blank") boolean blank) throws IOException
	{
		// this.data = Data.edgeListDirected(new File("/Users/Peter/Documents/datasets/graphs/ecoli-makse/cellular.dat"));
		
		// this.data = RDF.readTurtle(new File("/Users/Peter/Documents/datasets/graphs/molecules/enzymes.ttl"));
		// this.data = Data.edgeListDirectedUnlabeled(new File("/Users/Peter/Documents/datasets/graphs/www-makse/www.dat"), true);
		this.data = (DGraph<String>)GML.read(new File("/Users/Peter/Documents/datasets/graphs/neural/newman/celegans.gml") );
		System.out.println(data.size());
		
		this.samples = samples;
		this.depth = depth;
		this.correct = correct;
		this.blank = blank;
	}
	
	@Main()
	public void body()
	{	
		if(blank)
			data = Graphs.blank(data, "x");
		
		FrequencyModel<DGraph<String>> fm = 
				new FrequencyModel<DGraph<String>>();
		SubgraphGenerator<String> gen = new SubgraphGenerator<String>(data, depth);
		
		for(int i : Series.series(samples))
		{
			if(i % 1000 == 0)
				System.out.println(i);

			SubgraphGenerator<String>.Result result = gen.generate();
			DGraph<String> sub = Subgraph.dSubgraphIndices(data, result.indices());
			
			Order canOrder = Nauty.order(sub, new Functions.NaturalComparator<String>());
			sub = Graphs.reorder(sub, canOrder);
			
			fm.add(sub, correct ? result.invProbability() : 1.0);
		}
		
		List<DGraph<String>> tokens = fm.sorted(); 
		
		// top = RDF.simplify(tokens.get(0));
		top = tokens.get(0);
		topFreq = fm.frequency(top);
		
		second = tokens.get(1);
		secondFreq = fm.frequency(second);

		third = tokens.get(2);
		thirdFreq = fm.frequency(third);
	}
	
	@Out(name="top graph", description="The most frequent subgraph found. Labels are simplified from RDF.")
	public DGraph<String> top()
	{
		return top;
	}
	
	@Out(name="top graph frequency", description="The frequency of the top graph")
	public double topFreq()
	{
		return topFreq;
	}
	
	@Out(name="second graph", description="")
	public DGraph<String> second()
	{
		return second;
	}
	
	@Out(name="second graph frequency", description="")
	public double secondFreq()
	{
		return secondFreq;
	}
	
	@Out(name="third graph", description="")
	public DGraph<String> third()
	{
		return third;
	}
	
	@Out(name="third graph frequency", description="")
	public double thirdFreq()
	{
		return thirdFreq;
	}	
}
