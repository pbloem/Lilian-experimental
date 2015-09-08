package org.lilian.platform.graphs;

import static java.lang.String.format;
import static java.util.Collections.reverseOrder;
import static org.data2semantics.platform.util.Series.series;
import static org.lilian.util.Functions.choose;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.channels.OverlappingFileLockException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;
import java.util.regex.Pattern;

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
import org.nodes.classification.Classification;
import org.nodes.classification.Classified;
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
import org.lilian.util.Functions;
import org.nodes.random.RandomGraphs;
import org.nodes.random.SimpleSubgraphGenerator;
import org.nodes.rdf.InformedAvoidance;
import org.nodes.util.FrequencyModel;
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
	private Pattern instanceMask;
	private Classified<Node<String>> instances;
	
	public @Out(name="final") BufferedImage matrixSlashBurn;
	public @Out(name="one") BufferedImage matrixSlashBurn1;
	public @Out(name="two") BufferedImage matrixSlashBurn2;
	
	private String affiliationRelation = "http://swrc.ontoware.org/ontology#affiliation";
	
	public SlashBurnTest() throws IOException
	{
		// this.graph = RDF.readTurtle(new File("/Users/Peter/Documents/datasets/graphs/molecules/enzymes.ttl"));
		this.graph = RDF.read(new File("/Users/Peter/Documents/datasets/graphs/aifb/aifb.owl"));		
	}
	
	@Main()
	public void body()
		throws IOException
	{
		
		instances = HubAvoidanceTest.getInstances(graph, affiliationRelation);
		
		System.out.println(instances.size());
		System.out.println(instances.numClasses());

		int top = 50;
		int depth = 2;
		int viability = 3;
		
		System.out.println(graph.size() + " " + graph.numLinks());
		
		int k = (int)(0.005 * graph.size());
		k = Math.max(k, 1);
		
		InformedAvoidance ia = new InformedAvoidance(graph, instances, 4);
		
		MaxObserver<Node<String>> moDeg = 
				new MaxObserver<Node<String>>(top, new DegreeComparator<String>());
		for(Node<String> node : graph.nodes())
			if(! node.label().contains("viewPersonOWL"))
				moDeg.observe(node);	
		
		List<Node<String>> topDegree = moDeg.elements();

		MaxObserver<DTNode<String, String>> moInformed = 
				new MaxObserver<DTNode<String, String>>(top, ia.informedComparator(depth));
		
		for(DTNode<String, String> node : graph.nodes())
			if(! node.label().contains("viewPersonOWL"))
				if(ia.viableHub(node, depth, viability))
					moInformed.observe(node);
		
		List<DTNode<String, String>> topInformed = moInformed.elements();
		
		MaxObserver<DTNode<String, String>> moUninformed = 
				new MaxObserver<DTNode<String, String>>(top, ia.uninformedComparator(depth));
		
		for(DTNode<String, String> node : graph.nodes())
			if(! node.label().contains("viewPersonOWL"))
				if(ia.viableHub(node, depth, 3))
					moUninformed.observe(node);
		List<DTNode<String, String>> topUninformed = moUninformed.elements();	
		
		System.out.println("\n\nTop " + top + " hubs by degree ");
		for(Node<String> node : topDegree)
			System.out.println(node);

		System.out.println("\n\nTop " + top + " hubs by informed analysis ");
		for(Node<String> node : topInformed)
			System.out.println(node);
		
		System.out.println("\n\nTop " + top + " hubs by uninformed analysis ");
		for(Node<String> node : topUninformed)
			System.out.println(node);
		
	}
	
	public String cd(Node<String> node, int depth, InformedAvoidance ia)
	{
		String out = "[";
				
		for(int cls : series(instances.numClasses()))
			out += " " + f(ia.pClass(cls, node, depth)) + " ";
		
		return out + "]";
	}
	
	public String lik(Node<String> node, int depth, InformedAvoidance ia)
	{
		String out = "[";
				
		for(int cls : series(instances.numClasses()))
			out += " " + f(ia.p(node, cls, depth)) + " ";
		
		return out + "]";
	}
	
	public String c(InformedAvoidance ia)
	{
		String out = "[";
				
		for(int cls : series(instances.numClasses()))
			out += " " + f(ia.p(cls)) + " ";
		
		return out + "]";
	}

	public String f(double x)
	{
		return String.format("%.3f", x);
	}
	
	private double summedMarginal(InformedAvoidance ia, Node<String> node, int depth)
	{
		double sum = 0.0;
		
		for(int cls : series(instances.numClasses()))
			sum+= ia.p(cls) * ia.p(node, cls, depth);
		
		return sum;
	}
	
}
