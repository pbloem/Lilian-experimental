package org.lilian.platform.graphs;

import java.io.File;
import java.util.Arrays;

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
import org.nodes.algorithms.Nauty;
import org.nodes.data.RDF;
import org.lilian.graphs.grammar.Induction;
import org.nodes.random.SimpleSubgraphGenerator;
import org.lilian.models.BasicFrequencyModel;
import org.lilian.util.Functions;
import org.lilian.util.Order;

@Module(name="Motif extraction", description="Detects frequent subgraphs in networks")
public class InductionRDF
{
	
	private DTGraph<String, String> data;
	private DTGraph<String, String> top;
	private int samples;
	private boolean blank;
	
	public InductionRDF(
			@In(name="samples") int samples,
			@In(name="blank") boolean blank)
	{
		this.data = RDF.readTurtle(new File("/Users/Peter/Documents/datasets/graphs/molecules/mutag.tll"));
		this.samples = samples;
		this.blank = blank;
	}
	
	@Main()
	public void body()
	{
		System.out.println("DATA " + data.size() + " " + data.numLinks());
		
		if(blank)
			data = Graphs.blank(data, "blank");
		
		Induction induction = new Induction(data);
		for(int i : Series.series(10))
			induction.learn(samples);
	}
}
