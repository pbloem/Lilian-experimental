package org.lilian.experiment.graphs;

import java.util.Map;

import org.lilian.experiment.AbstractExperiment;
import org.lilian.experiment.Result;
import org.lilian.experiment.State;
import org.lilian.util.graphs.jung.Measures;

import edu.uci.ics.jung.algorithms.metrics.Metrics;
import edu.uci.ics.jung.graph.Graph;
import edu.uci.ics.jung.graph.UndirectedGraph;

/**
 * Graphs measures that will work on huge graphs. Generally, these are linear in 
 * the number of edges.
 *  
 * @author Peter
 *
 */
public class HugeGraph<V, E> extends AbstractExperiment
{
	protected Graph<V, E> graph;

	public @State double meanDegree;
	public @State double stdDegree;
	
	public @State double assortativity = Double.NaN;
	public @State double meanLocalClusteringCoefficient;
	
	public HugeGraph(Graph<V, E> graph)
	{
		this.graph = graph;
	}

	@Override
	protected void setup()
	{
		
	}

	@Override
	protected void body()
	{
		// * Calculate mean degree
		meanDegree = 0.0;
		for(V node : graph.getVertices())
			meanDegree += graph.degree(node);
		meanDegree /= graph.getVertexCount();
		
		// * Calculate degree std
		double varSum = 0.0;
		for(V node : graph.getVertices())
		{
			double v = graph.degree(node);
			
			double diff = meanDegree - v;
			varSum += diff * diff;
		}

		double variance = varSum/(graph.getVertexCount() - 1);
		stdDegree = Math.sqrt(variance);
		
		assortativity = Measures.assortativity(graph);
		
		logger.info("Calculating mean local clustering coefficient");
		Map<V, Double> map = Metrics.clusteringCoefficients(graph);
		meanLocalClusteringCoefficient = 0.0;
		for(V vertex : graph.getVertices())
			meanLocalClusteringCoefficient += map.get(vertex);
		meanLocalClusteringCoefficient /= (double) graph.getVertexCount();
	}
	
	@Result(name="Mean degree")
	public double meanDegree()
	{
		return meanDegree;
	}	
	
	@Result(name="Degree (sample) standard deviation")
	public double stdDegree()
	{
		return stdDegree;
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
	
	@Result(name="Mean local clustering coefficient")
	public double meanLocalClusteringCoefficient()
	{
		return meanLocalClusteringCoefficient;
	}
}
