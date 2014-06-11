package org.lilian.platform.graphs;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.data2semantics.platform.annotation.In;
import org.data2semantics.platform.annotation.Main;
import org.data2semantics.platform.annotation.Module;
import org.data2semantics.platform.annotation.Out;
import org.data2semantics.platform.util.Series;
import org.nodes.DTGraph;
import org.nodes.DTNode;
import org.nodes.Global;
import org.nodes.Graph;
import org.nodes.Graphs;
import org.nodes.MapDTGraph;
import org.nodes.Node;
import org.nodes.Subgraph;
import org.nodes.boxing.Boxing;
import org.nodes.boxing.CBBBoxer;
import org.nodes.data.GML;

@Module(name = "Boxing")
public class GraphBoxing
{
	private CBBBoxer<String> boxer;
	
	private DTGraph<String, String> graph;
	private Graph<Integer> post;
	
	private int lb;
	
	public int numBoxes;
	public double propBoxes;
	public double meanMass;
	public int uncovered;
	public int overCovered;
	public List<Integer> boxSizes;
	public List<Set<Node<String>>> mapping1, mapping2, mapping3, mapping4;
	// public GraphMeasures<V, E> e0;
	// public GraphMeasures<Integer, Integer> e1, e2, e3, e4; 
	
	public GraphBoxing(
			@In(name="graph", print=false) DTGraph<String, String> graph,
			@In(name="lb") int lb)
	{
		this.graph = graph;
		this.lb = lb;

		boxer = new CBBBoxer<String>(graph);
		boxSizes = new ArrayList<Integer>();
	}

	@Main
	public void body()
	{
		Boxing<String> boxing =  boxer.box(lb);
		
		numBoxes = boxing.size();
		propBoxes = numBoxes / (double) graph.numLinks();
		meanMass = graph.numLinks() / (double)numBoxes;
		
		uncovered = boxing.uncovered().size();
		overCovered = boxing.overCovered().size();
		
		post = boxing.postGraph();
		DTGraph<String, String> boxes1 = boxes(boxing);
		mapping1 = boxing;
		
		for(Set<Node<String>> box : boxing)
			boxSizes.add(box.size());
		try
		{
			GML.write(post,  new File("post1.gml"));
			GML.write(boxes1, new File("boxes1.gml"));
		
			
			
			
			Global.log().info("Starting 2");
			Boxing<Integer> iBoxing = new CBBBoxer<Integer>(post).box(lb);
			System.out.print(".");
			
			Graph<Integer> post2 = iBoxing.postGraph();
			System.out.print(".");
	
			mapping2 = merge(iBoxing, mapping1);
			System.out.print(".");
			
			for(Set<Node<String>> set : mapping2)
				System.out.println(set.size());
	
			DTGraph<String, String> boxes2 = boxes(mapping2);
			System.out.print(".");
			
			GML.write(boxes2, new File("boxes2.gml"));
			GML.write(post2, new File("post2.gml"));

			
	
			Global.log().info("Starting 3");
			iBoxing = new CBBBoxer<Integer>(post2).box(lb);
			System.out.print(".");
	
			Graph<Integer> post3 = iBoxing.postGraph();
			System.out.print(".");
	
			mapping3 = merge(iBoxing, mapping2);
			System.out.print(".");
	
			DTGraph<String, String> boxes3 = boxes(mapping3);
			System.out.print(".");
			
			for(Set<Node<String>> set : mapping3)
				System.out.println(set.size());
			
			
			GML.write(boxes3, new File("boxes3.gml"));
			GML.write(post3, new File("post3.gml"));
			
			
			
	
	
			Global.log().info("Starting 4");
			iBoxing = new CBBBoxer<Integer>(post3).box(lb);
			System.out.print(".");
	
			Graph<Integer> post4 = iBoxing.postGraph();
			System.out.print(".");
	
			mapping4 = merge(iBoxing, mapping3);
			System.out.print(".");
	
			DTGraph<String, String> boxes4 = boxes(mapping4);
			System.out.print(".");
			
			for(Set<Node<String>> set : mapping4)
				System.out.println(set.size());

			GML.write(boxes4, new File("boxes4.gml"));
			GML.write(post4, new File("post4.gml"));
			
		} catch(IOException e)
		{
			throw new RuntimeException(e);
		}
		
	}
	
	@Out(name="number of boxes")
	public int numBoxes()
	{
		return numBoxes;
	}
	
	@Out(name="Mean box mass")
	public double meanMass()
	{
		return meanMass;
	}
	
	@Out(name="box proportion")
	public double propBoxes()
	{
		return propBoxes;
	}
	
	@Out(name="Uncovered", description="Number of uncovered vertices")
	public int uncovered()
	{
		return uncovered;
	}
	
	@Out(name="Overcovered", description="Number of vertices covered more than once")
	public int overCovered()
	{
		return overCovered;
	}
	
	@Out(name="Box sizes")
	public List<Integer> boxSizes()
	{
		return boxSizes;
	}
	
	public DTGraph<String, String> boxes(List<Set<Node<String>>> boxing)
	{
		DTGraph<String, String> boxes = new MapDTGraph<String, String>();
		
		for(Set<Node<String>> box : boxing)
		{
			List<Integer> indices = new ArrayList<Integer>(box.size());
			for(Node<String> node : box)
				indices.add(node.index());
			
			DTGraph<String, String> sub = Subgraph.dtSubgraphIndices(graph, indices);
			
			Graphs.add(boxes, sub);
		}
		
		return boxes;
	}
	
	public List<Set<Node<String>>> merge(List<Set<Node<Integer>>> latest, List<Set<Node<String>>> base)
	{
		List<Set<Node<String>>> result = new ArrayList<Set<Node<String>>>(latest.size());
		
		for(int i : Series.series(latest.size()))
			result.add(new HashSet<Node<String>>());
		
		for(int i : Series.series(latest.size()))
		{
			Set<Node<Integer>> box = latest.get(i);
			Set<Node<String>> resBox = result.get(i);
			
			for(Node<Integer> member : box)
			{
				Set<Node<String>> baseBox = base.get(member.label());
				resBox.addAll(baseBox);
			}
		}
		
		return result;
	}
	
//	@Result(name="Graph plot")
//	public BufferedImage plot0()
//	{
//		return e0.visualization();
//	}
//	
//	@Result(name="Graph plot: first reduction")
//	public BufferedImage plot1()
//	{
//		return e1.visualization();
//	} 
//	
//	@Result(name="Graph plot: second reduction")
//	public BufferedImage plot2()
//	{
//		return e2.visualization();
//	} 
//	
//	@Result(name="Graph plot: third reduction")
//	public BufferedImage plot3()
//	{
//		return e3.visualization();
//	}
//	
//	@Result(name="Graph plot: fourth reduction")
//	public BufferedImage plot4()
//	{
//		return e4.visualization();
//	}	
//	
//	@Result(name="reduced mean degree")
//	public double reducedMean()
//	{
//		return e1.meanDegree();
//	}
//
//	@Result(name="reduced degree sample standard deviation")
//	public double reducedStd()
//	{
//		return e1.stdDegree();
//	}	
//	
}
