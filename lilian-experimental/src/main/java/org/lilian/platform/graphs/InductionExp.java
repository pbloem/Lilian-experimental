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
import org.lilian.graphs.grammar.Induction;
import org.lilian.graphs.motifs.DCensus;
import org.lilian.graphs.random.SubgraphGenerator;
import org.lilian.models.BasicFrequencyModel;
import org.lilian.util.Functions;
import org.lilian.util.Order;

@Module(name="Motif extraction", description="Detects frequent subgraphs in networks")
public class InductionExp
{
	
	private DTGraph<String, String> data;
	private DTGraph<String, String> top;
	private int samples;
	private boolean blank;
	
	public InductionExp(
			@In(name="data", print=false) DTGraph<String, String> data, 
			@In(name="samples") int samples,
			@In(name="blank") boolean blank)
	{
		this.data = data;
		this.samples = samples;
		this.blank = blank;
	}
	
	@Main()
	public void body()
	{
		System.out.println("DATA" + data.size());
		
		if(blank)
			data = Graphs.blank(data, "blank");
		
		Induction induction = new Induction(data);
		for(int i : Series.series(100))
			induction.learn(samples);
	}
	
}
