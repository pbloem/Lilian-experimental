package org.lilian.experiment.old;

import org.lilian.experiment.AbstractExperiment;
import org.lilian.experiment.Result;
import org.lilian.experiment.State;

import edu.uci.ics.jung.graph.Graph;

/**
 * Graphs measures that work on huge directed graphs. Generally, there are 
 * linear in the number of edges.
 * 
 * @author Peter
 *
 */
public class HugeDirectedGraph<V, E> extends AbstractExperiment
{	
	protected Graph<V, E> graph;
	
	public @State double meanInDegree = Double.NaN;
	public @State double stdInDegree = Double.NaN;
	
	public @State double meanOutDegree = Double.NaN;
	public @State double stdOutDegree = Double.NaN;
	
	
	public HugeDirectedGraph(Graph<V, E> graph)
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
		double varSum, variance;
		
		// * Calculate mean in degree
		meanInDegree = 0.0;
		for(V node : graph.getVertices())
			meanInDegree += graph.inDegree(node);
		meanInDegree /= graph.getVertexCount();
		
		// * Calculate in degree std
		varSum = 0.0;
		for(V node : graph.getVertices())
		{
			double v = graph.inDegree(node);
			
			double diff = meanInDegree - v;
			varSum += diff * diff;
		}

		variance = varSum/(graph.getVertexCount() - 1);
		stdInDegree = Math.sqrt(variance);	
		
		// * Calculate out mean degree
		meanOutDegree = 0.0;
		for(V node : graph.getVertices())
			meanOutDegree += graph.outDegree(node);
		meanOutDegree /= graph.getVertexCount();
		
		// * Calculate out degree std
		varSum = 0.0;
		for(V node : graph.getVertices())
		{
			double v = graph.outDegree(node);
			
			double diff = meanOutDegree - v;
			varSum += diff * diff;
		}		
		
		variance = varSum/(graph.getVertexCount() - 1);
		stdOutDegree = Math.sqrt(variance);			
	}

	@Result(name="Mean in degree")
	public double meanInDegree()
	{
		return meanInDegree;
	}	
	
	@Result(name="In degree (sample) standard deviation")
	public double stdInDegree()
	{
		return stdInDegree;
	}

	@Result(name="Mean out degree")
	public double meanOutDegree()
	{
		return meanOutDegree;
	}	
	
	@Result(name="Out Degree (sample) standard deviation")
	public double stdOutDegree()
	{
		return stdOutDegree;
	}
	
}
