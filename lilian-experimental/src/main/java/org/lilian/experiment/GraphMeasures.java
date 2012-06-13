package org.lilian.experiment;

import static java.util.Collections.reverseOrder;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.data2semantics.tools.graphs.Edge;
import org.data2semantics.tools.graphs.Node;
import org.lilian.util.Series;
import org.lilian.util.graphs.jung.Graphs;

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
	
	
	@Factory
	public static GraphMeasures<Node<String>, Edge<String>> fromFile(
			@Parameter(name="data") File file)
	{
		return new GraphMeasures<Node<String>, Edge<String>>(org.data2semantics.tools.graphs.Graphs.graphFromRDF(file));
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
	
	@Result(name="Degrees with labels")
	public List<Pair> degreesWitLabels()
	{
		return Collections.unmodifiableList(pairs).subList(0, 100);		
	}
	
	private class Pair implements Comparable<Pair>
	{
		int degree;
		String node;
		
		public Pair(int degree, String node)
		{
			this.degree = degree;
			this.node = node;
		}

		@Override
		public int compareTo(Pair that)
		{
			return Double.compare(this.degree, that.degree);
		}
		
		public String toString()
		{
			return degree + " " + node;
		}
	}
}
