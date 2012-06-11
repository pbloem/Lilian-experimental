package org.lilian.experiment;

import static java.util.Collections.reverseOrder;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.lilian.util.Series;
import org.lilian.util.graphs.jung.Graphs;

import edu.uci.ics.jung.graph.Graph;

/**
 * Calculates and outputs several straigtforward graoh measures on some input 
 * graph
 *
 * @author Peter
 *
 */
public class GraphMeasures<V, E> extends AbstractExperiment
{
	private Graph<V, E> graph;
	
	public @State List<Integer> degrees;  
	
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
		degrees = new ArrayList<Integer>(graph.getVertexCount());
	}

	@Override
	protected void body()
	{
		// * Collect degrees 
		for(V node : graph.getVertices())
			degrees.add(graph.degree(node));
		Collections.sort(degrees, reverseOrder());
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
		
}
