package org.lilian.experiment.graphs;

import static java.util.Collections.reverseOrder;

import java.util.AbstractList;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.lilian.experiment.Result;
import org.lilian.experiment.State;
import org.lilian.models.BasicFrequencyModel;

import edu.uci.ics.jung.algorithms.cluster.WeakComponentClusterer;
import edu.uci.ics.jung.algorithms.shortestpath.DistanceStatistics;
import edu.uci.ics.jung.graph.Graph;

/**
 * Graph measures for large graphs. These methods are generally a low polynomial 
 * in the number of edges or vertices
 *  
 * @author Peter
 *
 * @param <V>
 * @param <E>
 */
public class LargeGraph<V, E> extends HugeGraph<V, E>
{
	
	public @State double diameter = Double.NaN;	
	public @State double largestComponentSize = Double.NaN;
	
	public @State List<Pair> pairs;  
	public @State List<Integer> degrees;

	public @State List<Pair> vertexLabelFrequencies;
	public @State List<Pair> edgeLabelFrequencies;	

	public LargeGraph(Graph<V, E> graph)
	{
		super(graph);
	}
	
	@Override
	protected void setup()
	{
		super.setup();
		
		pairs = new ArrayList<Pair>(graph.getVertexCount());	
		degrees = new ArrayList<Integer>(graph.getVertexCount());		
	}

	@Override
	protected void body()
	{
		super.body();
		
		logger.info("Calculating diameter");
		diameter = DistanceStatistics.diameter(graph);
		
		logger.info("Calculating size of the largest component");
		WeakComponentClusterer<V, E> clust = 
				new WeakComponentClusterer<V, E>();
		
		Set<Set<V>> clusters = clust.transform(graph);
		
		largestComponentSize = Double.NEGATIVE_INFINITY;
		for(Set<V> set : clusters)
			largestComponentSize = Math.max(largestComponentSize, set.size());
		
		
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
		
		
	}
	
	@Result(name="diameter", description="Longest shortest path")
	public double diameter()
	{
		return diameter;
	}
	
	
	@Result(name="Assortivity")
	public double assortivity()
	{
		return assortativity;
	}
	
	@Result(name="Size of the largest component")
	public double lCompSize()
	{
		return largestComponentSize;
	}	
	
	@Result(name="Proportion of the largest component")
	public double lCompProp()
	{
		return largestComponentSize / (double) numNodes();
	}
	
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
	
	@Result(name="Degrees")
	public List<Integer> degrees()
	{
		return Collections.unmodifiableList(degrees);
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
