package org.lilian.platform.graphs;

import static java.lang.String.format;
import static org.data2semantics.platform.util.Series.series;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.logging.Logger;

import org.data2semantics.platform.annotation.In;
import org.data2semantics.platform.annotation.Main;
import org.data2semantics.platform.annotation.Module;
import org.data2semantics.platform.annotation.Out;
import org.data2semantics.platform.util.Series;
import org.nodes.DGraph;
import org.nodes.DLink;
import org.nodes.DNode;
import org.nodes.DTGraph;
import org.nodes.DTLink;
import org.nodes.DTNode;
import org.nodes.DegreeComparator;
import org.nodes.Graph;
import org.nodes.Graphs;
import org.nodes.Link;
import org.nodes.Node;
import org.nodes.Subgraph;
import org.nodes.algorithms.Nauty;
import org.nodes.algorithms.SlashBurn;
import org.nodes.clustering.ConnectionClusterer;
import org.nodes.clustering.ConnectionClusterer.ConnectionClustering;
import org.nodes.data.Dot;
import org.nodes.data.GML;
import org.nodes.data.RDF;
import org.nodes.draw.Draw;
import org.lilian.Global;
import org.lilian.experiment.State;
import org.lilian.graphs.grammar.Clustering;
import org.lilian.graphs.grammar.Induction;
import org.lilian.rdf.SBSimplifier;
import org.nodes.random.RandomGraphs;
import org.nodes.random.SubgraphGenerator;
import org.nodes.util.MaxObserver;
import org.nodes.util.Order;

@Module(name="Motif extraction", description="Detects frequent subgraphs in networks")
public class SlashBurnTest
{
	
	private DTGraph<String, String> graph;
	private DTGraph<String, String> top;
	private int samples;
	private boolean blank;
	private int k = 20;
	
	public @Out(name="final") BufferedImage matrixSlashBurn;
	public @Out(name="one") BufferedImage matrixSlashBurn1;
	public @Out(name="two") BufferedImage matrixSlashBurn2;
	
	public SlashBurnTest() throws IOException
	{
		this.graph = RDF.readTurtle(new File("/Users/Peter/Documents/datasets/graphs/commit/commit-contacts.ttl"));
		
	}
	
	@Main()
	public void body()
		throws IOException
	{
		System.out.println(graph.size() + " " + graph.numLinks());
		
		int k = (int)(0.005 * graph.size());
		k = Math.max(k, 1);
		
		SBSimplifier<String, String> simp = new SBSimplifier<String, String>(graph, k, false);
		for(int i : series(100))
		{
			simp.iterate();
			System.out.println(simp.graph().size() + " " + simp.graph().numLinks());
			GML.write(RDF.simplify(simp.graph()), new File(format("generation.%03d.gml", i)));
		}
	}
		
		
}
