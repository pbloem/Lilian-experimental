package org.lilian.experiment;

import static java.util.Collections.reverseOrder;

import java.awt.image.BufferedImage;
import java.io.File;
import java.util.AbstractList;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.data2semantics.tools.graphs.Edge;
import org.data2semantics.tools.graphs.Vertex;
import org.lilian.models.BasicFrequencyModel;
import org.lilian.util.Series;
import org.lilian.util.graphs.jung.Graphs;
import org.openrdf.rio.RDFFormat;

import edu.uci.ics.jung.graph.Graph;

/**
 * Calculates and outputs several straightforward graph measures on some input 
 * graph
 *
 * @author Peter
 *
 */
public class GraphMeasures<V, E> extends AbstractExperiment
{
	private Graph<V, E> graph;
	
	public @State List<Pair> pairs;  
	public @State List<Integer> degrees;  
	
	public @State List<Pair> vertexLabelFrequencies;
	public @State List<Pair> edgeLabelFrequencies;
	
	public @State BufferedImage image;
	
	@Factory
	public static GraphMeasures<Vertex<String>, Edge<String>> fromFile(
			@Parameter(name="data") File file)
	{
		return new GraphMeasures<Vertex<String>, Edge<String>>(org.data2semantics.tools.graphs.Graphs.graphFromRDF(file));
	}
	
	@Factory
	public static GraphMeasures<Vertex<String>, Edge<String>> fromFile(
			@Parameter(name="data") File file,
			@Parameter(name="edge whitelist") List<String> edgeWhiteList)
			
	{
		return new GraphMeasures<Vertex<String>, Edge<String>>(org.data2semantics.tools.graphs.Graphs.graphFromRDF(file, RDFFormat.RDFXML, null, edgeWhiteList));
	}
	
	/**
	 * 
	 */
	@Factory
	public static GraphMeasures<Integer, Integer> abRandom(
			@Parameter(name="number of nodes") int nodes,
			@Parameter(name="number to attach") int toAttach)
	{
		return new GraphMeasures<Integer, Integer>(Graphs.abRandom(nodes, 3, toAttach));
	}
	
	/**
	 * For a basic random graph (Erdos-Renyi random)
	 * 
	 * @param nodes
	 */
	@Factory
	public static GraphMeasures<Integer, Integer> erRandom(
			@Parameter(name="number of nodes") int nodes,
			@Parameter(name="edge probability") double edgeProb)
	{	
		return new GraphMeasures<Integer, Integer>(Graphs.random(nodes, edgeProb));
	}
	
	private GraphMeasures(Graph<V, E> graph)
	{	
		this.graph = graph;
	}
	
	@Override
	protected void setup()
	{		
		pairs = new ArrayList<Pair>(graph.getVertexCount());	
		degrees = new ArrayList<Integer>(graph.getVertexCount());
			
	}

	@Override
	protected void body()
	{
		// * Collect degrees 
		for(V node : graph.getVertices())
		{
			degrees.add(graph.degree(node));
			pairs.add(
					new Pair(graph.degree(node), node.toString()));
		}
		
		Collections.sort(degrees, reverseOrder());
		Collections.sort(pairs, reverseOrder());
		
		// * Collect vertex labels
		BasicFrequencyModel<String> vertexModel = new BasicFrequencyModel<String>();
		for(V vertex : graph.getVertices())
			vertexModel.add(vertex.toString());
		
		List<String> tokens = vertexModel.sorted();
		vertexLabelFrequencies = new ArrayList<Pair>(tokens.size());
		for(String token : tokens)
			vertexLabelFrequencies.add(new Pair((int)vertexModel.frequency(token), token));
			
		// * Collect edge labels
		BasicFrequencyModel<String> edgeModel = new BasicFrequencyModel<String>();
		for(E edge : graph.getEdges())
			edgeModel.add(edge.toString());
		
		tokens = edgeModel.sorted();
		edgeLabelFrequencies = new ArrayList<Pair>(tokens.size());
		for(String token : tokens)
			edgeLabelFrequencies.add(new Pair((int)edgeModel.frequency(token), token));
		
		// * Draw visualization
		image = org.data2semantics.tools.graphs.Graphs.image(graph, 800, 494);
	}

	@Result(name="Number of nodes (vertices)")
	public int numNodes()
	{
		return graph.getVertexCount();
	}
	
	@Result(name="Number of links (edges)")
	public int numLinks()
	{
		return graph.getEdgeCount();
	}
	
	@Result(name="Degrees")
	public List<Integer> degrees()
	{
		return Collections.unmodifiableList(degrees);		
	}
	
	@Result(name="Visualization")
	public BufferedImage vizualization()
	{
		return image;
	}
	
//	@Result(name="Degrees with labels")
//	public List<Pair> degreesWithLabels()
//	{
//		return Collections.unmodifiableList(pairs).subList(0, 100);		
//	}
	
	@Result(name="Vertex labels")
	public List<Pair> vertexLabels()
	{
		return Collections.unmodifiableList(vertexLabelFrequencies);
	}
	
	@Result(name="Edge labels")
	public List<Pair> edgeLabels()
	{
		return Collections.unmodifiableList(edgeLabelFrequencies);
	}
	
	private class Pair extends AbstractList<Object> implements Comparable<Pair>
	{
		int degree;
		String label;
		
		public Pair(int degree, String node)
		{
			this.degree = degree;
			this.label = node;
		}

		@Override
		public int compareTo(Pair that)
		{
			return Double.compare(this.degree, that.degree);
		}
		
		public String toString()
		{
			return degree + " " + label;
		}

		@Override
		public Object get(int index)
		{
			if(index == 0)
				return degree;
			if(index == 1)
				return label;
			throw new IndexOutOfBoundsException();
		}

		@Override
		public int size()
		{
			return 2;
		}
	}
}
