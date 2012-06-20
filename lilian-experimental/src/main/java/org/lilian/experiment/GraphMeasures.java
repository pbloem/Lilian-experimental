package org.lilian.experiment;

import static java.util.Collections.reverseOrder;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.AbstractList;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.data2semantics.tools.graphs.Edge;
import org.data2semantics.tools.graphs.GML;
import org.data2semantics.tools.graphs.Vertex;
import org.jfree.util.Log;
import org.lilian.models.BasicFrequencyModel;
import org.lilian.util.Series;
import org.lilian.util.graphs.jung.Graphs;
import org.lilian.util.graphs.jung.Measures;
import org.openrdf.rio.RDFFormat;

import edu.uci.ics.jung.algorithms.cluster.WeakComponentClusterer;
import edu.uci.ics.jung.algorithms.shortestpath.DistanceStatistics;
import edu.uci.ics.jung.graph.DirectedGraph;
import edu.uci.ics.jung.graph.Graph;
import edu.uci.ics.jung.graph.UndirectedGraph;

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
	private boolean large;
	
	public @State List<Pair> pairs;  
	public @State List<Integer> degrees;
	
	public @State double meanDegree;
	public @State double stdDegree;
	
	public @State double meanInDegree = Double.NaN;
	public @State double stdInDegree = Double.NaN;
	
	public @State double meanOutDegree = Double.NaN;
	public @State double stdOutDegree = Double.NaN;
	
	public @State List<Pair> vertexLabelFrequencies;
	public @State List<Pair> edgeLabelFrequencies;
	
	public @State BufferedImage image;
	
	public @State double assortativity = Double.NaN;
	public @State boolean directed;
	
	public @State double diameter = Double.NaN;	
	
	public @State double largestComponentSize = Double.NaN;
	
	@Factory
	public static GraphMeasures<Vertex<String>, Edge<String>> fromFile(
			@Parameter(name="data") File file,
			@Parameter(name="large") boolean visualize)
	{
		return new GraphMeasures<Vertex<String>, Edge<String>>(
				org.data2semantics.tools.graphs.Graphs.graphFromRDF(file), visualize);
	}
	
	@Factory
	public static GraphMeasures<Vertex<Integer>, Edge<Integer>> intFromFile(
			@Parameter(name="data") File file,
			@Parameter(name="int", description="Marker parameter, value is ignored") boolean shrt) 
					throws IOException
			
	{	
		return new GraphMeasures<Vertex<Integer>, Edge<Integer>>(
				org.data2semantics.tools.graphs.Graphs
					.intGraphFromTSV(file), true);
	}	
	
	@Factory
	public static GraphMeasures<Vertex<Integer>, Edge<Integer>> intDirectedFromFile(
			@Parameter(name="data") File file,
			@Parameter(name="directed", description="Marker parameter, value is ignored") boolean directed,
			@Parameter(name="large", description="Whether this is a large network (and expensive methods should be skipped)") 
				boolean large) 
					throws IOException
			
	{	
		return new GraphMeasures<Vertex<Integer>, Edge<Integer>>(
				org.data2semantics.tools.graphs.Graphs
					.intDirectedGraphFromTSV(file), large);
	}		
	
	@Factory 
	public static GraphMeasures<GML.LVertex, Edge<String>> gml(			
			@Parameter(name="data") 
				File file,
			@Parameter(name="gml") 
				boolean gml,
			@Parameter(name="large") 
				boolean large) throws IOException
	{
		return new GraphMeasures<GML.LVertex, Edge<String>>(
				org.data2semantics.tools.graphs.GML.read(file),
				large);
	}
	
	@Factory
	public static GraphMeasures<Vertex<String>, Edge<String>> fromFile(
			@Parameter(name="data") File file,
			@Parameter(name="format", description="one of: rdf, txt") 
				String format,
			@Parameter(name="edge whitelist", description="only used if format is RDF") 
				List<String> edgeWhiteList,
			@Parameter(name="large") 
				boolean large) 
					throws IOException
			
	{
		if(format.trim().toLowerCase().equals("rdf"))
			return new GraphMeasures<Vertex<String>, Edge<String>>(
					org.data2semantics.tools.graphs.Graphs
					.graphFromRDF(file, RDFFormat.RDFXML, null, edgeWhiteList), large);
		
		return new GraphMeasures<Vertex<String>, Edge<String>>(
				org.data2semantics.tools.graphs.Graphs
					.graphFromTSV(file), large);
	}
	
	/**
	 * 
	 */
	@Factory
	public static GraphMeasures<Integer, Integer> abRandom(
			@Parameter(name="number of nodes") int nodes,
			@Parameter(name="number to attach") int toAttach,
			@Parameter(name="large") boolean large)
	{
		return new GraphMeasures<Integer, Integer>(Graphs.abRandom(nodes, 3, toAttach), large);
	}
	
	/**
	 * For a basic random graph (Erdos-Renyi random)
	 * 
	 * @param nodes
	 */
	@Factory
	public static GraphMeasures<Integer, Integer> erRandom(
			@Parameter(name="number of nodes") int nodes,
			@Parameter(name="edge probability") double edgeProb,
			@Parameter(name="large") boolean large)
	{	
		return new GraphMeasures<Integer, Integer>(Graphs.random(nodes, edgeProb), large);
	}
	
	private GraphMeasures(Graph<V, E> graph, boolean large)
	{	
		this.graph = graph;
		this.large = large;
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
		logger.info("Calculating degree mean and std dev");
		
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
		
		logger.info("Calculating diameter");
		
		if(!large) 
		{		
			diameter = DistanceStatistics.diameter(graph);

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
			
			// * Rendering visualization
			logger.info("Rendering visualization.");
			image = org.data2semantics.tools.graphs.Graphs.image(graph, 800, 494);		
			
			
			// * Calculate the size of the largest component
			WeakComponentClusterer<V, E> clust = 
					new WeakComponentClusterer<V, E>();
			
			Set<Set<V>> clusters = clust.transform(graph);
			
			largestComponentSize = Double.NEGATIVE_INFINITY;
			for(Set<V> set : clusters)
				largestComponentSize = Math.max(largestComponentSize, set.size());
			
			
		}
				
		// * Calculate assortivity
		directed = graph instanceof DirectedGraph<?, ?>;
		
		if(directed) 
		{
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
		
		} else
		{
			assortativity = Measures.assortativity((UndirectedGraph<?, ?>)graph);
		}
	}
	
	@Result(name="diameter", description="Longest shortest path")
	public double diameter()
	{
		return diameter;
	}
	
	@Result(name="directed")
	public boolean directed()
	{
		return directed;
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
		if(! large)
			return Collections.unmodifiableList(degrees);
		return null;
	}
	
	@Result(name="Visualization")
	public BufferedImage vizualization()
	{
		return image;
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
	
//	@Result(name="Degrees with labels")
//	public List<Pair> degreesWithLabels()
//	{
//		return Collections.unmodifiableList(pairs).subList(0, 100);		
//	}
	
	@Result(name="Vertex labels")
	public List<Pair> vertexLabels()
	{
		if(!large)
			return Collections.unmodifiableList(vertexLabelFrequencies);
		return null;
	}
	
	@Result(name="Edge labels")
	public List<Pair> edgeLabels()
	{
		if(! large)
			return Collections.unmodifiableList(edgeLabelFrequencies);
		return null;
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
