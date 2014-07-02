//package org.lilian.platform.graphs.motifs;
//
//import java.awt.Color;
//import java.awt.geom.AffineTransform;
//import java.awt.geom.Rectangle2D;
//import java.awt.image.BufferedImage;
//import java.io.ByteArrayInputStream;
//import java.io.ByteArrayOutputStream;
//import java.io.File;
//import java.io.FileOutputStream;
//import java.io.IOException;
//import java.io.OutputStreamWriter;
//import java.util.ArrayList;
//import java.util.Collections;
//import java.util.LinkedHashMap;
//import java.util.List;
//import java.util.Map;
//
//import javax.xml.transform.Transformer;
//import javax.xml.transform.TransformerConfigurationException;
//import javax.xml.transform.TransformerException;
//import javax.xml.transform.TransformerFactory;
//import javax.xml.transform.dom.DOMSource;
//import javax.xml.transform.stream.StreamResult;
//
//import org.apache.batik.bridge.BridgeContext;
//import org.apache.batik.bridge.DocumentLoader;
//import org.apache.batik.bridge.GVTBuilder;
//import org.apache.batik.bridge.UserAgent;
//import org.apache.batik.bridge.UserAgentAdapter;
//import org.apache.batik.dom.svg.SAXSVGDocumentFactory;
//import org.apache.batik.dom.svg.SVGOMSVGElement;
//import org.apache.batik.gvt.GraphicsNode;
//import org.apache.batik.util.XMLResourceDescriptor;
//import org.data2semantics.platform.Global;
//import org.data2semantics.platform.annotation.In;
//import org.data2semantics.platform.annotation.Main;
//import org.data2semantics.platform.annotation.Module;
//import org.data2semantics.platform.annotation.Out;
//import org.data2semantics.platform.util.Series;
//import org.gephi.graph.api.DirectedGraph;
//import org.gephi.graph.api.Edge;
//import org.gephi.graph.api.GraphController;
//import org.gephi.graph.api.GraphModel;
//import org.gephi.graph.api.Node;
//import org.gephi.graph.api.UndirectedGraph;
//import org.gephi.io.exporter.api.ExportController;
//import org.gephi.io.exporter.preview.SVGExporter;
//import org.gephi.layout.plugin.forceAtlas2.ForceAtlas2;
//import org.gephi.layout.plugin.forceAtlas2.ForceAtlas2Builder;
//import org.gephi.preview.api.PreviewController;
//import org.gephi.preview.api.PreviewModel;
//import org.gephi.preview.api.PreviewProperty;
//import org.gephi.preview.types.DependantColor;
//import org.gephi.preview.types.EdgeColor;
//import org.gephi.project.api.ProjectController;
//import org.gephi.project.api.Workspace;
//import org.nodes.DGraph;
//import org.nodes.DNode;
//import org.nodes.DTGraph;
//import org.nodes.DTNode;
//import org.nodes.DegreeComparator;
//import org.nodes.Graph;
//import org.nodes.Graphs;
//import org.nodes.Link;
//import org.nodes.MapDTGraph;
//import org.nodes.Subgraph;
//import org.nodes.TGraph;
//import org.nodes.TLink;
//import org.nodes.algorithms.Nauty;
//import org.nodes.algorithms.SlashBurn;
//import org.nodes.compression.EdgeListCompressor;
//import org.nodes.compression.MotifCompressor;
//import org.nodes.data.Data;
//import org.nodes.data.GML;
//import org.nodes.data.RDF;
//import org.nodes.draw.Draw;
//import org.nodes.gephi.Gephi;
//import org.nodes.random.RandomGraphs;
//import org.nodes.random.SubgraphGenerator;
//import org.nodes.util.FrequencyModel;
//import org.lilian.util.Functions;
//import org.lilian.util.Functions.NaturalComparator;
//import org.nodes.util.Order;
//import org.openide.util.Lookup;
//import org.w3c.dom.Document;
//import org.w3c.dom.svg.SVGDocument;
//import org.w3c.dom.svg.SVGRect;
//           
//@Module(name="Slashburn Motif extraction", description="Tests different methods of Motif extraction to avoid overlap")
//public class SBMotifs
//{
//	
//	private DGraph<String> data;
//	private DGraph<String> top, second, third;
//	private int samples;
//	private int maxSize, minSize;
//	private double topFreq, secondFreq, thirdFreq;
//	private boolean correct, blank;
//	private int numMotifs;
//	private int remHubs;
//	
//	private NaturalComparator<String> comp;
//	
//	private List<Double> overlaps, ratios;
//	private List<List<Double>> degrees;
//	private List<Integer> sizes, frequencies;
//	
//	private List<DGraph<String>> sampleOccurrences = new ArrayList<DGraph<String>>();
//	
//	private BufferedImage topMotif, dataImage, silhouette;
//	
//	public SBMotifs(
//			@In(name="data", print=false) DGraph<String> data,
//			@In(name="max size", description="the max size for which we check isomorphism of subgraphs") int maxSize,
//			@In(name="min size", description="the min size of subgraph we consider") int minSize,
//			@In(name="blank") boolean blank,
//			@In(name="num motifs") int numMotifs) throws IOException
//	{
//		this.data = data;
//		this.samples = samples;
//		this.maxSize = maxSize;
//		this.minSize = minSize;
//		
//		this.correct = correct;
//		this.blank = blank;
//		this.numMotifs = numMotifs;
//		
//		comp = new Functions.NaturalComparator<String>();
//	}
//	
//	@Main()
//	public void body() throws TransformerException, IOException
//	{	
//		System.out.println("data size:" + data.size());
//		
//		DGraph<String> copy = MapDTGraph.copy(data);
//
//		if(blank)
//			data = Graphs.blank(data, "x");
//
//		FrequencyModel<DGraph<String>> fm = 
//				new FrequencyModel<DGraph<String>>();
//		Map<DGraph<String>, List<List<Integer>>> occurrences = 
//				new LinkedHashMap<DGraph<String>, List<List<Integer>>>();
//		
//		List<List<Integer>> islands = new ArrayList<List<Integer>>();
//		
//		SlashBurn<String> sb = new SlashBurn<String>(data, 1, islands);
//		sb.finish();
//		
//		sizes = new ArrayList<Integer>();
//		for(List<Integer> island : islands)
//			sizes.add(island.size());
//		
//		System.out.println(islands.size());
//		for(List<Integer> island : islands)
//			if(island.size() >= minSize)
//			{
//				DGraph<String> sub = Subgraph.dSubgraphIndices(data, island);
//				System.out.println(sub.size());
//				
//				// * Canonize the subgraph
//				if(sub.size() <= maxSize)
//					sub = Graphs.reorder(sub, Nauty.order(sub, comp));
//				
//				// * Count
//				fm.add(sub);
//				
//				// * Record occurrences
//				if(! occurrences.containsKey(sub))
//					occurrences.put(sub, new ArrayList<List<Integer>>());
//				
//				occurrences.get(sub).add(island);			
//			}
//		
//		List<DGraph<String>> tokens = fm.sorted();
//		tokens = tokens.subList(0, numMotifs);
//		
//		Global.log().info("Starting compression test");
//		// * Compression ratios
//		ratios = new ArrayList<Double>(numMotifs);
//		for(DGraph<String> sub : tokens)
//			ratios.add(StandardMotifs.compressionRatio(data, sub, occurrences.get(sub)));
//
//		Global.log().info("Starting overlap test");
//		// * Overlap numbers
//		overlaps = new ArrayList<Double>(numMotifs);
//		for(DGraph<String> sub : tokens)
//			overlaps.add((double)StandardMotifs.overlap(data, occurrences.get(sub)));
//		
//		degrees = new ArrayList<List<Double>>(numMotifs);
//		for(DGraph<String> sub : tokens)
//			degrees.add(StandardMotifs.degrees(data, sub, occurrences.get(sub)));
//		
//		frequencies = new ArrayList<Integer>(numMotifs);
//		for(DGraph<String> sub : tokens)
//			frequencies.add((int)fm.frequency(sub));
//		
//		for(DGraph<String> sub : tokens)
//			sampleOccurrences.add(
//				Subgraph.dSubgraphIndices(copy, Functions.choose(occurrences.get(sub))));
//		
//		silhouette = plot(tokens, occurrences);
//	}
//	
//	@Out(name="ratios")
//	public List<Double> ratios()
//	{
//		return ratios;
//	}
//	
//	@Out(name="overlaps")
//	public List<Double> overlaps()
//	{
//		return overlaps;
//	}
//	
//	@Out(name="degrees")
//	public List<List<Double>> degrees()
//	{
//		return degrees;
//	}
//	
//	@Out(name="sizes")
//	public List<Integer> sizes()
//	{
//		return sizes;
//	}
//	
//	@Out(name="frequencies")
//	public List<Integer> frequencies()
//	{
//		return frequencies;
//	}
//	
//	@Out(name="sample occurrence of each motif")
//	public List<DGraph<String>> mostFreq()
//	{
//		return sampleOccurrences;
//	}
//	
//	@Out(name="top motif")
//	public BufferedImage topMotif()
//	{
//		return topMotif;
//	}
//
//	@Out(name="silhouette")
//	public BufferedImage silhouette()
//	{
//		return silhouette;
//	}
//		
//	
//	@Out(name="data")
//	public BufferedImage data()
//	{
//		return dataImage;
//	}
//	
//	public BufferedImage plot(List<DGraph<String>> subs, Map<DGraph<String>, List<List<Integer>>> occurrences)
//	{
//		DGraph<String> silGraph = MotifCompressor.subbedGraphMulti(
//				data, subs, 
//				occurrences,
//				new ArrayList<Integer>());
//		
//		Workspace workspace;
//		
//		ProjectController pc = Lookup.getDefault().lookup(ProjectController.class);
//		pc.newProject();
//		workspace = pc.getCurrentWorkspace();
//		
//		boolean directed = data instanceof DGraph<?>; 
//		
//		GraphModel graphModel = Lookup.getDefault().lookup(GraphController.class).getModel();
//		
//		
//		List<Node> gephiNodes = new ArrayList<Node>(data.size());
//		List<Edge> gephiEdges = new ArrayList<Edge>(data.numLinks());
//		
//		// * Add all nodes
//		for(org.nodes.Node<String> node : data.nodes())
//		{
//			Node gNode = graphModel.factory().newNode(node.index() + "");
//			
//			gNode.getNodeData().setLabel(org.nodes.util.Functions.toString(node.label()));
//			gNode.getNodeData().setSize(10f);
//			
//			if(node.label().startsWith("%S"))
//			{
//				gNode.getNodeData().setColor(0, 0, 1.0f);
//				gNode.getNodeData().setSize(50f);
//			}
//						
//			gephiNodes.add(gNode);
//		}
//		
//		for(Link<String> link : data.links())
//		{
//			int i = link.first().index(), j = link.second().index();
//			Node iNode = gephiNodes.get(i), jNode = gephiNodes.get(j);
//			
//			Edge edge = graphModel.factory().newEdge(iNode,  jNode, 1f, directed);
//			
//			if(link instanceof TLink<?, ?>)
//			{
//				Object tag = ((TLink<?, ?>)link).tag();
//				edge.getEdgeData().setLabel(tag == null ? "" : tag.toString());
//			}
//			
//			gephiEdges.add(edge);
//		}
//		
//		for(DGraph<String> sub : subs)	
//		{
//			for(org.nodes.Node<String> node : sub.nodes())
//			{
//				Node gNode = graphModel.factory().newNode(node.index() + "");
//				
//				gNode.getNodeData().setLabel(org.nodes.util.Functions.toString(node.label()));
//				gNode.getNodeData().setSize(10f);
//							
//				gephiNodes.add(gNode);
//			}
//			
//			for(Link<String> link : sub.links())
//			{
//				int i = link.first().index(), j = link.second().index();
//				Node iNode = gephiNodes.get(i), jNode = gephiNodes.get(j);
//				
//				Edge edge = graphModel.factory().newEdge(iNode,  jNode, 1f, directed);
//				
//				if(link instanceof TLink<?, ?>)
//				{
//					Object tag = ((TLink<?, ?>)link).tag();
//					edge.getEdgeData().setLabel(tag == null ? "" : tag.toString());
//				}
//				
//				gephiEdges.add(edge);
//			}
//		}
//		
//		org.gephi.graph.api.Graph out = null;
//		if(directed)
//			out = graphModel.getDirectedGraph();
//		else 
//			out = graphModel.getUndirectedGraph();
//		
//		for(Node node : gephiNodes)
//			out.addNode(node);
//		for(Edge edge : gephiEdges)
//			out.addEdge(edge);
//		
//		System.out.println("Graph has " + out.getEdgeCount() + " edges");
//				
//		SVGDocument svg = Gephi.svg(out);
//		return Draw.draw(svg, 800);
//	}
//}
