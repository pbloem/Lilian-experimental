package org.lilian.platform.graphs;

import static org.data2semantics.platform.util.Series.series;
import static org.nodes.util.Functions.log2;
import static org.nodes.util.Functions.tic;
import static org.nodes.util.Functions.toc;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import org.data2semantics.platform.annotation.In;
import org.data2semantics.platform.annotation.Main;
import org.data2semantics.platform.annotation.Module;
import org.data2semantics.platform.annotation.Out;
import org.data2semantics.platform.util.Series;
import org.nodes.DNode;
import org.nodes.DTGraph;
import org.nodes.DTLink;
import org.nodes.DTNode;
import org.nodes.Global;
import org.nodes.Graph;
import org.nodes.Graphs;
import org.nodes.MapDTGraph;
import org.nodes.Node;
import org.nodes.Subgraph;
import org.nodes.boxing.Boxing;
import org.nodes.boxing.CBBBoxer;
import org.nodes.classification.Classification;
import org.nodes.classification.Classified;
import org.nodes.data.GML;
import org.nodes.rdf.DepthScorer;
import org.nodes.rdf.FlatInstances;
import org.nodes.rdf.InformedAvoidance;
import org.nodes.rdf.Instances;
import org.nodes.util.FrequencyModel;
import org.nodes.util.Functions;
import org.nodes.util.MaxObserver;

@Module(name = "Hub Avoidance")
public class HubAvoidanceTest
{
	private DTGraph<String, String> graph;
	private String instanceRelation;
	private int maxDepth, numInstances;
	
	private List<FrequencyModel<Node<String>>> counts;
	private int instanceSize;
	private int beamWidth;
	
	private boolean directed;

	public HubAvoidanceTest(
			@In(name="graph", print=false) 
				DTGraph<String, String> graph,
			@In(name="instance relation", description="Any node with this relation (outgoing) is an instance") 
				String instanceRelation,
			@In(name="max depth") 
				int maxDepth,
			@In(name="instance size") 
				int instanceSize,
			@In(name="beam width") 
				int beamWidth,
			@In(name="directed")
				boolean directed)
	{
		this.graph = graph;
		this.instanceRelation = instanceRelation;
		this.maxDepth = maxDepth;
		this.instanceSize = instanceSize;
		this.beamWidth = beamWidth;
		this.directed = directed;
	}

	@Main
	public void body()
	{
		Classified<Node<String>> instances = getInstances(graph, instanceRelation);			
		numInstances = instances.size();
		
		InformedAvoidance inst = new InformedAvoidance(graph, instances, maxDepth);
		
		FlatInstances searcherDepth = new FlatInstances(graph, instanceSize, maxDepth, new DepthScorer());
		FlatInstances searcherSmart = new FlatInstances(graph, instanceSize, maxDepth, inst);


//		for(int depth : series(1, maxDepth))
//		{
//			System.out.println("** Depth " +  depth + ":");
//			System.out.println(" * top 10");
//			
//			MaxObserver<Node<String>> mo = new MaxObserver<Node<String>>(500, inst.entropyComparator(depth));
//			mo.observe(graph.nodes());
//					
//			for(Node<String> node : mo.elements())
//				System.out.println(inst.p(node, depth) + "  " + node);
//		}
		tic();
		
		int i = 0;
		for(Node<String> iNode : instances)
		{
//			System.out.println("Extracting (depth) for instance " + iNode + " (degree "+iNode.degree()+") ");
//			
//			tic();
//			
//			for(Node<String> node : searcherDepth.instance(iNode))
//				System.out.println("           " + node.index() + " " + node);
//			
//			System.out.println("Time taken: " + toc() + " seconds");
			
			System.out.println("Extracting (smart) for instance " + i + " " + iNode + " (degree "+iNode.degree()+") ");
						
			searcherSmart.instance((DNode<String>)iNode);
			// for(Node<String> node : searcherSmart.instance((DNode<String>)iNode))
			//	System.out.print(".");
			
			// System.out.println("\n** Time taken: " + toc() + " seconds");
			System.out.println(i);
			i++;
		}
		
		System.out.println("\n** Time taken: " + toc() + " seconds");

	}
	
	public static Classified<Node<String>> getInstances(DTGraph<String, String> graph, String relation)
	{
		Classified<Node<String>> instances = Classification.empty();
		List<Node<String>> affList = new ArrayList<Node<String>>();
		Set<Node<String>> affiliations = new LinkedHashSet<Node<String>>();
		
		for(DTNode<String, String> node : graph.nodes())
		{
			// * Check if node has the hasAffiliation relation
			Node<String> affiliation = getAffiliation(node, relation);
			if(affiliation == null)
				continue;
			
			instances.add(node, 0);
			affiliations.add(affiliation);
			affList.add(affiliation);
		}
		
		List<Node<String>> classNumbers = new ArrayList<Node<String>>(affiliations);
		
		for(int i : series(instances.size()))
		{
			int cls = classNumbers.indexOf(affList.get(i));
			instances.setClass(i, cls);
		}
		
		return instances;
	}

	private static Node<String> getAffiliation(DTNode<String, String> node, String relation)
	{
		for(DTLink<String, String> link : node.linksOut())
		{
			if(link.tag().equals(relation))
				return link.other(node);
		}
			
		return null;
	}
}
