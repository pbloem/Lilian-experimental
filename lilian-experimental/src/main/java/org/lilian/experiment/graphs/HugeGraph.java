package org.lilian.experiment.graphs;


import org.lilian.experiment.AbstractExperiment;
import org.lilian.experiment.Result;
import org.lilian.experiment.State;
import org.lilian.graphs.Graph;
import org.lilian.graphs.Measures;
import org.lilian.graphs.Node;

/**
 * Graphs measures that will work on huge graphs. Generally, these are linear in 
 * the number of edges.
 *  
 * @author Peter
 *
 */
public class HugeGraph<N> extends AbstractExperiment
{
	protected Graph<N> graph;

	public @State double meanDegree;
	public @State double stdDegree;
	
	public @State double assortativity = Double.NaN;
	public @State double meanLocalClusteringCoefficient = Double.NaN;
	
	public HugeGraph(Graph<N> graph)
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
		for(Node<N> node : graph.nodes())
			meanDegree += node.degree();
		meanDegree /= graph.size();
		
		// * Calculate degree std
		double varSum = 0.0;
		for(Node<N> node : graph.nodes())
		{
			double v = node.degree();
			
			double diff = meanDegree - v;
			varSum += diff * diff;
		}

		double variance = varSum/(graph.size() - 1);
		stdDegree = Math.sqrt(variance);
		
		logger.info("Calculating assortativity");
		assortativity = Measures.assortativity(graph);
		logger.info("finished");
		
//		logger.info("Calculating mean local clustering coefficient");
//		Map<V, Double> map = Metrics.clusteringCoefficients(graph);
//		meanLocalClusteringCoefficient = 0.0;
//		for(V vertex : graph.getVertices())
//			meanLocalClusteringCoefficient += map.get(vertex);
//		meanLocalClusteringCoefficient /= (double) graph.getVertexCount();
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
		return graph.size();
	}
	
	@Result(name="Number of links (edges)")
	public int numLinks()
	{
		return graph.numLinks();
	}
	
	@Result(name="Assortivity")
	public double assortivity()
	{
		return assortativity;
	}
	
	@Result(name="Mean local clustering coefficient")
	public double meanLocalClusteringCoefficient()
	{
		return meanLocalClusteringCoefficient;
	}
}
