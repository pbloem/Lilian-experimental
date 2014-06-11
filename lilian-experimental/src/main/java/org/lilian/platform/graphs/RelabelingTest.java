package org.lilian.platform.graphs;

import static org.data2semantics.platform.util.Series.series;
import static org.nodes.data.RDF.simplify;
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
import org.nodes.data.RDF;
import org.nodes.rdf.DepthScorer;
import org.nodes.rdf.FlatInstances;
import org.nodes.rdf.InformedAvoidance;
import org.nodes.rdf.InformedLabels;
import org.nodes.rdf.Instances;
import org.nodes.util.FrequencyModel;
import org.nodes.util.Functions;
import org.nodes.util.MaxObserver;

@Module(name = "Relabeling")
public class RelabelingTest
{
	private DTGraph<String, String> graph;
	private String instanceRelation;
	private int maxDepth, numInstances;
	
	private List<FrequencyModel<Node<String>>> counts;
	private int instanceSize;
	private int beamWidth;
	
	private boolean directed;

	public RelabelingTest(
			@In(name="graph", print=false) 
				DTGraph<String, String> graph,
			@In(name="instance relation", description="Any node with this relation (outgoing) is an instance") 
				String instanceRelation,
			@In(name="depth") 
				int maxDepth)
	{
		this.graph = graph;
		this.instanceRelation = instanceRelation;
		this.maxDepth = maxDepth;

	}

	@Main
	public void body()
	{
		Classified<Node<String>> instances = HubAvoidanceTest.getInstances(graph, instanceRelation);			
		numInstances = instances.size();
		
		System.out.println("Building ("+numInstances+")");
		tic();
		InformedLabels inst = new InformedLabels(graph, instances, maxDepth);
		System.out.println("\n** Time taken: " + toc() + " seconds");

		tic();
		
		int i = 0, u = 0;
		for(DTNode<String, String> node : graph.nodes())
		{
			String nodeLabel = node.label();
			String informed = inst.chooseLabelInformed(node, maxDepth);
			String uninformed =  inst.chooseLabelUninformed(node, maxDepth);
						
			if(! nodeLabel.equals(informed)) i++;
			if(! nodeLabel.equals(uninformed)) u++;
			
			if((! nodeLabel.equals(informed)) || (!nodeLabel.equals(informed)))
				System.out.println(String.format("%04d", node.degree()) + " " + simplify(nodeLabel) + "\t" + informed + "\t" + uninformed);

		}
		
		System.out.println("informed " + i + ", uninformed " + u);
		
		System.out.println("\n** Time taken: " + toc() + " seconds");

	}
}
