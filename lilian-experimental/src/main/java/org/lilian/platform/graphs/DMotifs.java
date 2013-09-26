package org.lilian.platform.graphs;

import org.data2semantics.platform.annotation.In;
import org.data2semantics.platform.annotation.Main;
import org.data2semantics.platform.annotation.Module;
import org.data2semantics.platform.annotation.Out;
import org.data2semantics.platform.util.Series;
import org.lilian.graphs.DGraph;
import org.lilian.graphs.DTGraph;
import org.lilian.graphs.DTNode;
import org.lilian.graphs.Graph;
import org.lilian.graphs.Graphs;
import org.lilian.graphs.Link;
import org.lilian.graphs.Subgraph;
import org.lilian.graphs.algorithms.Nauty;
import org.lilian.graphs.data.RDF;
import org.lilian.graphs.motifs.DCensus;
import org.lilian.graphs.random.SubgraphGenerator;
import org.lilian.models.BasicFrequencyModel;
import org.lilian.util.Functions;
import org.lilian.util.Order;

@Module(name="Motif extraction", description="Detects frequent subgraphs in networks")
public class DMotifs
{
	
	private DTGraph<String, String> data;
	private DTGraph<String, String> top;
	private int samples;
	private int depth;
	private double topFreq;
	private boolean correct, blank;
	
	
	public DMotifs(
			@In(name="data", print=false) DTGraph<String, String> data, 
			@In(name="samples") int samples,
			@In(name="depth") int depth,
			@In(name="correct frequency") boolean correct,
			@In(name="blank") boolean blank)
	{
		this.data = data;
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
		
		BasicFrequencyModel<DTGraph<String, String>> fm = 
				new BasicFrequencyModel<DTGraph<String,String>>();
		SubgraphGenerator<String> gen = new SubgraphGenerator<String>(data, depth);
		
		for(int i : Series.series(samples))
		{
			SubgraphGenerator<String>.Result result = gen.generate();
			DTGraph<String, String> sub = Subgraph.dtSubgraphIndices(data, result.indices());
			
			Order canOrder = Nauty.order(sub, new Functions.NaturalComparator<String>());
			sub = Graphs.reorder(sub, canOrder);
			
			fm.add(sub, correct ? result.invProbability() : 1.0);
		}
		
		top = RDF.simplify(fm.maxToken());
		topFreq = fm.frequency(top);
		
//		DTNode<String, String> node = data.node("http://www.aifb.uni-karlsruhe.de/Forschungsgebiete/viewForschungsgebietOWL/id58instance");
//				
//		for(Link<String> link : node.links())		
//			System.out.println(link);
		// System.exit(1);
	}
	
	@Out(name="top graph", description="The most frequent subgraph found. Labels are simplified from RDF.")
	public DTGraph<String, String> top()
	{
		return top;
	}
	
	@Out(name="top graph frequency", description="The frequency of the top graph")
	public double topFreq()
	{
		return topFreq;
	}
	
}
