package org.lilian.platform.graphs.motifs;

import static java.util.Collections.reverseOrder;
import static org.nodes.util.Series.series;

import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.data2semantics.platform.annotation.In;
import org.data2semantics.platform.annotation.Main;
import org.data2semantics.platform.annotation.Module;
import org.data2semantics.platform.annotation.Out;
import org.gephi.graph.api.Edge;
import org.gephi.graph.api.GraphController;
import org.gephi.graph.api.GraphModel;
import org.gephi.graph.api.Node;
import org.gephi.project.api.ProjectController;
import org.gephi.project.api.Workspace;
import org.lilian.Global;
import org.lilian.util.Functions;
import org.nodes.DGraph;
import org.nodes.DNode;
import org.nodes.DegreeComparator;
import org.nodes.Graphs;
import org.nodes.Link;
import org.nodes.MapDTGraph;
import org.nodes.Subgraph;
import org.nodes.TLink;
import org.nodes.algorithms.Nauty;
import org.nodes.algorithms.SlashBurn;
import org.nodes.compression.MotifCompressor;
import org.nodes.draw.Draw;
import org.nodes.gephi.Gephi;
import org.nodes.random.SubgraphGenerator;
import org.nodes.util.AbstractGenerator;
import org.nodes.util.FrequencyModel;
import org.nodes.util.Generator;
import org.nodes.util.Series;
import org.openide.util.Lookup;
import org.w3c.dom.svg.SVGDocument;


@Module(name="Iterative Motif extraction", description="Iterative hierarchichal motif extraction")
public class Iterative
{
	public static final int MIN_SIZE = 3;
	public static final int MAX_SIZE = 8;
	
	private Comparator<String> comp = new Functions.NaturalComparator<String>();

	private String method;
	private int hubRem;
	private int iterations;
	private DGraph<String> data, copy, silhouette;
	private DGraph<String> sub;
	private boolean blank;
	private int samples;
	
	private List<DGraph<String>> subs = new ArrayList<DGraph<String>>();
	List<BufferedImage> images = new ArrayList<BufferedImage>();
	
	public Iterative(
			@In(name="data")
			DGraph<String> data,
			@In(name="method", description="'sb' or 'freq'")
			String method,
			@In(name="hubs to remove")
			int hubRem, 
			@In(name="iterations")
			int iterations,
			@In(name="blank")
			boolean blank,
			@In(name="samples")
			int samples)
	{
		this.data = data;
		this.method = method;
		this.hubRem = hubRem;
		this.iterations = iterations;
		this.blank = blank;
		this.samples = samples;
	}
	
	@Main()
	public void body()
	{
		System.out.println("data size:" + data.size());
		
		copy = MapDTGraph.copy(data);

		if(blank)
			data = Graphs.blank(data, "x");
		
		for(int i : series(iterations))
		{
			Global.log().info("Starting iteration " + i);
			
			// * Find the best motif and its occurrences
			DGraph<String> subgraph = null;
			List<List<Integer>> subgraphOccurrences = null;
			
			if(method.equals("sb"))
			{
				FrequencyModel<DGraph<String>> fm = 
						new FrequencyModel<DGraph<String>>();
				Map<DGraph<String>, List<List<Integer>>> occurrences = 
						new LinkedHashMap<DGraph<String>, List<List<Integer>>>();
				
				List<List<Integer>> islands = new ArrayList<List<Integer>>();
				
				SlashBurn<String> sb = new SlashBurn<String>(data, 1, islands);
				sb.finish();
				
				List<Integer> sizes = new ArrayList<Integer>();
				for(List<Integer> island : islands)
					sizes.add(island.size());
				
				System.out.println(islands.size());
				for(List<Integer> island : islands)
					if(island.size() >= MIN_SIZE)
					{
						DGraph<String> sub = Subgraph.dSubgraphIndices(data, island);
						System.out.println(sub.size());
						
						// * Canonize the subgraph
						if(sub.size() <= MAX_SIZE)
							sub = Graphs.reorder(sub, Nauty.order(sub, comp));
						
						// * Count
						fm.add(sub);
						
						// * Record occurrences
						if(! occurrences.containsKey(sub))
							occurrences.put(sub, new ArrayList<List<Integer>>());
						
						occurrences.get(sub).add(island);			
					}
				
				subgraph = fm.maxToken();
				subgraphOccurrences = occurrences.get(subgraph);
				
			} else if(method.equals("freq"))
			{
				FrequencyModel<DGraph<String>> fm = 
						new FrequencyModel<DGraph<String>>();
				Map<DGraph<String>, List<List<Integer>>> occurrences = 
						new LinkedHashMap<DGraph<String>, List<List<Integer>>>();
				
				// * Collect the hubs to remove
				List<DNode<String>> hubs = new ArrayList<DNode<String>>(hubRem);
				if(hubRem > 0)
				{
					List<DNode<String>> nodes = new ArrayList<DNode<String>>(data.nodes());
					Collections.sort(nodes, reverseOrder(new DegreeComparator<String>()));
					
					hubs.addAll(nodes.subList(0, hubRem));
				}
				
				for(DNode<String> node : hubs)
					System.out.println("HUB " + node + "\t" + node.degree());
				
				Generator<Integer> ints = new UniformGenerator(MIN_SIZE, MAX_SIZE);
				SubgraphGenerator<String> gen = 
						new SubgraphGenerator<String>(data, ints, hubs);
				
				for(int j : Series.series(samples))
				{
					if(j % 1000 == 0)
						System.out.println("Samples finished: " + i);

					SubgraphGenerator<String>.Result result = gen.generate();
					DGraph<String> sub = Subgraph.dSubgraphIndices(data, result.indices());
					
					// * Reorder nodes to canonical ordering
					sub = Graphs.reorder(sub, Nauty.order(sub, comp));
					
					fm.add(sub, result.invProbability());
				
					if(! occurrences.containsKey(sub))
						occurrences.put(sub, new ArrayList<List<Integer>>());
					
					occurrences.get(sub).add(result.indices());
				}
				
				subgraph = fm.maxToken();
				subgraphOccurrences = occurrences.get(subgraph);
				
				subs.add(subgraph);
			}
			
			// * Extract motif
			List<Integer> wiring = new ArrayList<Integer>(); 
			data = MotifCompressor.subbedGraph(data, subgraph, subgraphOccurrences, wiring);
			
			if((i/10) > 0 && i % (i/10) == 0)
			{
				BufferedImage image = plot(subs, data);
				images.add(image);
			}
		}
	}
	
	
	@Out(name="image0")
	public BufferedImage image0()
	{
		if(images.size() > 1)
			return images.get(0);
		return null;
	}
	
	@Out(name="image1")
	public BufferedImage image1()
	{
		if(images.size() > 2)
			return images.get(1);
		return null;
	}
	@Out(name="image2")
	public BufferedImage image2()
	{
		if(images.size() > 3)
			return images.get(2);
		return null;
	}
	@Out(name="image3")
	public BufferedImage image3()
	{
		if(images.size() > 4)
			return images.get(3);
		return null;
	}
	@Out(name="image4")
	public BufferedImage image4()
	{
		if(images.size() > 5)
			return images.get(4);
		return null;
	}
	@Out(name="image5")
	public BufferedImage image5()
	{
		if(images.size() > 6)
			return images.get(5);
		return null;
	}
	@Out(name="image6")
	public BufferedImage image6()
	{
		if(images.size() > 7)
			return images.get(6);
		return null;
	}
	@Out(name="image8")
	public BufferedImage image8()
	{
		if(images.size() > 9)
			return images.get(8);
		return null;
	}
	@Out(name="image9")
	public BufferedImage image9()
	{
		if(images.size() > 10)
			return images.get(9);
		return null;
	}
	
	
	public static class UniformGenerator extends AbstractGenerator<Integer>
	{
		private int lower, upper;

		public UniformGenerator(int lower, int upper)
		{
			this.lower = lower;
			this.upper = upper;
		}
		
		@Override
		public Integer generate()
		{	
			return Global.random.nextInt(upper - lower) + lower;
		}
	}
	
	public static BufferedImage plot(List<DGraph<String>> subs, DGraph<String> silhouette)
	{
		Workspace workspace;
		
		ProjectController pc = Lookup.getDefault().lookup(ProjectController.class);
		pc.newProject();
		workspace = pc.getCurrentWorkspace();
		
		boolean directed = silhouette instanceof DGraph<?>; 
		
		GraphModel graphModel = Lookup.getDefault().lookup(GraphController.class).getModel();
		
		
		List<Node> gephiNodes = new ArrayList<Node>(silhouette.size());
		List<Edge> gephiEdges = new ArrayList<Edge>(silhouette.numLinks());
		
		// * Add all nodes
		for(org.nodes.Node<String> node : silhouette.nodes())
		{
			Node gNode = graphModel.factory().newNode(node.index() + "");
			
			gNode.getNodeData().setLabel(org.nodes.util.Functions.toString(node.label()));
			gNode.getNodeData().setSize(10f);
			
			if(node.label().startsWith("%S"))
			{
				gNode.getNodeData().setColor(0, 0, 1.0f);
				gNode.getNodeData().setSize(50f);
			}
						
			gephiNodes.add(gNode);
		}
		
		for(Link<String> link : silhouette.links())
		{
			int i = link.first().index(), j = link.second().index();
			Node iNode = gephiNodes.get(i), jNode = gephiNodes.get(j);
			
			Edge edge = graphModel.factory().newEdge(iNode,  jNode, 1f, directed);
			
			if(link instanceof TLink<?, ?>)
			{
				Object tag = ((TLink<?, ?>)link).tag();
				edge.getEdgeData().setLabel(tag == null ? "" : tag.toString());
			}
			
			gephiEdges.add(edge);
		}
		
		for(DGraph<String> sub : subs)	
		{
			for(org.nodes.Node<String> node : sub.nodes())
			{
				Node gNode = graphModel.factory().newNode(node.index() + "");
				
				gNode.getNodeData().setLabel(org.nodes.util.Functions.toString(node.label()));
				gNode.getNodeData().setSize(10f);
							
				gephiNodes.add(gNode);
			}
			
			for(Link<String> link : sub.links())
			{
				int i = link.first().index(), j = link.second().index();
				Node iNode = gephiNodes.get(i), jNode = gephiNodes.get(j);
				
				Edge edge = graphModel.factory().newEdge(iNode,  jNode, 1f, directed);
				
				if(link instanceof TLink<?, ?>)
				{
					Object tag = ((TLink<?, ?>)link).tag();
					edge.getEdgeData().setLabel(tag == null ? "" : tag.toString());
				}
				
				gephiEdges.add(edge);
			}
		}
		
		org.gephi.graph.api.Graph out = null;
		if(directed)
			out = graphModel.getDirectedGraph();
		else 
			out = graphModel.getUndirectedGraph();
		
		for(Node node : gephiNodes)
			out.addNode(node);
		for(Edge edge : gephiEdges)
			out.addEdge(edge);
		
		System.out.println("Graph has " + out.getEdgeCount() + " edges");
				
		SVGDocument svg = Gephi.svg(out);
		return Draw.draw(svg, 800);
	}
}
