package org.lilian.platform.graphs;

import java.io.File;
import java.io.IOException;
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
import org.nodes.data.Dot;
import org.nodes.data.GML;
import org.nodes.data.RDF;
import org.lilian.graphs.grammar.Clustering;
import org.lilian.graphs.grammar.Induction;
import org.nodes.random.RandomGraphs;
import org.nodes.random.SubgraphGenerator;

@Module(name="Motif extraction", description="Detects frequent subgraphs in networks")
public class ClusteringExp
{
	
	private DTGraph<String, String> data;
	private DTGraph<String, String> top;
	private int samples;
	private boolean blank;
	
	public ClusteringExp(
			@In(name="samples") int samples,
			@In(name="blank") boolean blank) throws IOException
	{
		this.data = RDF.readTurtle(new File("/Users/Peter/Documents/datasets/graphs/molecules/mutag.ttl"));

		
		// this.data = (DTGraph<String, String>)GML.read(new File("/Users/Peter/Documents/datasets/graphs/neural/newman/celegans.gml"));
		
		// this.data = RandomGraphs.randomDirected(20, 0.2);
		this.samples = samples;
		this.blank = blank;
	}
	
	@Main()
	public void body()
	{
		System.out.println("DATA " + data.size() + " " + data.numLinks());
		
		if(blank)
			data = Graphs.blank(data, "t");
		
		Clustering clust = new Clustering(data);
		
		int i = 0;
		while(clust.learn(samples))
		{
			i++; 
			
			try
			{
				GML.write(clust.current(), new File(String.format("generation.%03d.gml", i)));
			} catch (IOException e)
			{
				throw new RuntimeException(e);
			}
			
			
		}
		
	}
	
}
