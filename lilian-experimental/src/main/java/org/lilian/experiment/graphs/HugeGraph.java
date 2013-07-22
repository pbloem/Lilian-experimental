package org.lilian.experiment.graphs;


import static org.lilian.util.Functions.tic;
import static org.lilian.util.Functions.toc;
import static org.lilian.util.Series.series;

import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.lilian.experiment.AbstractExperiment;
import org.lilian.experiment.Result;
import org.lilian.experiment.State;
import org.lilian.graphs.Graph;
import org.lilian.graphs.Measures;
import org.lilian.graphs.Node;
import org.lilian.graphs.algorithms.SlashBurn;
import org.lilian.graphs.clustering.ConnectionClusterer.ConnectionClustering;
import org.lilian.graphs.compression.AbstractGraphCompressor;
import org.lilian.graphs.compression.BinomialCompressor;
import org.lilian.graphs.compression.BinomialRowCompressor;
import org.lilian.graphs.compression.EdgeListCompressor;
import org.lilian.graphs.compression.MatrixZIPCompressor;
import org.lilian.graphs.compression.NeighborListCompressor;
import org.lilian.graphs.compression.UniformCompressor;
import org.lilian.graphs.draw.Draw;
import org.lilian.graphs.random.LinkGenerators;
import static org.lilian.graphs.random.LinkGenerators.LinkGenerator;
import org.lilian.util.Compressor;
import org.lilian.util.Functions;
import org.lilian.util.Series;

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
	public @State BufferedImage matrixNatural, matrixDegree, matrixRandom, 
		matrixSlashBurn, matrixSlashBurn1, matrixSlashBurn2;
	
	public @State double wingWidthRatio;
	public @State int gccSize; 
	
	public @State double uniformCompression, binomialCompression, binomialRowCompression;
	public @State double elCompression, nlCompression, amCompression;
	public @State List<Double> randCompression;
	public @State List<Double> degCompression, revDegCompression; 
	public @State List<Double> sbCompression; 

	public @State double tMakeGenerator;
	public @State double tSample;
	
	public @State List<Integer> orderRand, orderDeg, orderSB;

	
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
//		int n = graph.size();
//		
//		// * Calculate mean degree
//		meanDegree = 0.0;
//		for(Node<N> node : graph.nodes())
//			meanDegree += node.degree();
//		meanDegree /= graph.size();
//		
//		// * Calculate degree std
//		double varSum = 0.0;
//		for(Node<N> node : graph.nodes())
//		{
//			double v = node.degree();
//			
//			double diff = meanDegree - v;
//			varSum += diff * diff;
//		}
//
//		double variance = varSum/(graph.size() - 1);
//		stdDegree = Math.sqrt(variance);
//		
//		logger.info("Calculating assortativity");
//		assortativity = Measures.assortativity(graph);
//		logger.info("finished");
		
//		logger.info("Calculating mean local clustering coefficient");
//		Map<V, Double> map = Metrics.clusteringCoefficients(graph);
//		meanLocalClusteringCoefficient = 0.0;
//		for(V vertex : graph.getVertices())
//			meanLocalClusteringCoefficient += map.get(vertex);
//		meanLocalClusteringCoefficient /= (double) graph.getVertexCount();
		
//		logger.info("Starting connection clustering.");
//		ConnectionClustering<N> c = new ConnectionClustering<N>(graph);
//		gccSize = c.clusterSize(c.largestClusterIndex());
//		logger.info("Finished connection clustering.");
//
//		
//		logger.info("Starting adjacency natural.");
//		matrixNatural = Draw.matrix(graph, 500, 500);
//		logger.info("Finished adjacency natural.");
//
//		logger.info("Starting adjacency degree.");
//		orderDeg = Draw.degreeOrdering(graph);
//		matrixDegree = Draw.matrix(graph, 500, 500, orderDeg);
//		logger.info("Finished adjacency degree. ");
//
//		logger.info("Starting adjacency random.");
//		List<Integer> orderRand = new ArrayList<Integer>(series(graph.size()));
//		Collections.shuffle(orderRand);	
//		
//		orderDeg = Draw.degreeOrdering(graph);
//		matrixRandom = Draw.matrix(graph, 500, 500, orderRand);
//		logger.info("Finished adjacency random. ");		
		
//		logger.info("Starting adjacency slashburn.");
//		
//		int k = (int)(0.005 * graph.size());
//		k = Math.max(k, 1);
//		
//		SlashBurn<N> sb = new SlashBurn<N>(graph, k);
//
//		matrixSlashBurn1 = Draw.matrix(graph, 500, 500, sb.order());
//		sb.iterate();
//
//		matrixSlashBurn2 = Draw.matrix(graph, 500, 500, sb.order());
//		
//		sb.finish();		
//		matrixSlashBurn = Draw.matrix(graph, 500, 500, sb.order());
//		orderSB = sb.order();
//
//		wingWidthRatio = sb.wingWidthRatio();
//		logger.info("-- Slashburn finished in "+sb.iterations()+" iterations (k at "+k+")");
//
//		logger.info("Finished adjacency slashburn. ");	
		
//		AbstractGraphCompressor<N> el = new EdgeListCompressor<N>(),
//		                           nl = new NeighborListCompressor<N>(),
//		                           am = new MatrixZIPCompressor<N>(),
//		                           uniform = new UniformCompressor<N>(),
//		                           binom = new BinomialCompressor<N>(),
//				                   binomRow = new BinomialRowCompressor<N>();
//
//								
//		uniformCompression = uniform.compressedSize(graph);
//		
//		binomialCompression = binom.compressedSize(graph);
//		
//		binomialRowCompression = binomRow.compressedSize(graph);
//		
//		logger.info("EL Compression. ");	
//		elCompression = el.compressedSize(graph);
//		
//		logger.info("NL Compression. ");	
//		nlCompression = nl.compressedSize(graph);
//		
//		logger.info("AM compression. ");	
//		amCompression = n > 5000 ? 0.0 : am.compressedSize(graph);
//		
//		logger.info("random ordering");
//		
//		randCompression = Arrays.asList(
//				el.structureBits(graph, orderRand),
//				nl.structureBits(graph, orderRand),
//				n > 5000 ? 0.0 : am.structureBits(graph, orderRand),
//				binomRow.structureBits(graph, orderRand)
//		);
//		
//		logger.info("degree ordering");
//
//		degCompression = Arrays.asList(
//				el.structureBits(graph, orderDeg),
//				nl.structureBits(graph, orderDeg),
//				n > 5000 ? 0.0 : am.structureBits(graph, orderDeg),
//				binomRow.structureBits(graph, orderDeg)
//		);
//		
//		logger.info("Reverse degree ordering");
//		
//		Collections.reverse(orderDeg);
//
//		revDegCompression = Arrays.asList(
//				el.structureBits(graph, orderDeg),
//				nl.structureBits(graph, orderDeg),
//				n > 5000 ? 0.0 : am.structureBits(graph, orderDeg),
//				binomRow.structureBits(graph, orderDeg)
//		);
//		
//		logger.info("sb ordering");
//
//		sbCompression = Arrays.asList(
//				el.structureBits(graph, orderSB),
//				nl.structureBits(graph, orderSB),
//				n > 50000 ? 0.0 : am.structureBits(graph, orderSB),
//				binomRow.structureBits(graph, orderSB)
//		);
		
		logger.info("Link sampling test");
		
		tic();
		LinkGenerator<N> gen = new LinkGenerator<N>(graph);
		tMakeGenerator = toc();
		
		int sampleSize = 5000;
		tic();
		for(int i : Series.series(sampleSize))
			gen.generate();
		tSample = toc()/(double)sampleSize;
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
	
	@Result(name="Assortativity")
	public double assortivity()
	{
		return assortativity;
	}
	
	@Result(name="Adjacency matrix (given ordering)")
	public BufferedImage matrixNatural()
	{
		return matrixNatural;
	}
	
	@Result(name="Adjacency matrix (degree ordering)")
	public BufferedImage matrixDegree()
	{
		return matrixDegree;
	}
	
	@Result(name="Adjacency matrix (slashburn ordering)")
	public BufferedImage matrixSlashBurn()
	{
		return matrixSlashBurn;
	}
	
	@Result(name="Adjacency matrix (random ordering)")
	public BufferedImage matrixRandom()
	{
		return matrixRandom;
	}
	
	// @Result(name="Adjacency matrix (slashburn ordering, 1 iterations)")
	public BufferedImage matrixSlashBurn1()
	{
		return matrixSlashBurn1;
	}
	
	// @Result(name="Adjacency matrix (slashburn ordering, 2 iterations)")
	public BufferedImage matrixSlashBurn2()
	{
		return matrixSlashBurn2;
	}	
	
	@Result(name="Mean local clustering coefficient")
	public double meanLocalClusteringCoefficient()
	{
		return meanLocalClusteringCoefficient;
	}
	
	@Result(name="Wing width ratio")
	public double wingWidthRatio()
	{
		return wingWidthRatio;
	}
	
	@Result(name="GCC size")
	public int gccSize()
	{
		return gccSize;
	}
	
	@Result(name="Uniform Compression")
	public double uniformCompression()
	{
		return uniformCompression;
	}
	
	@Result(name="Binomial Compression")
	public double binomialCompression()
	{
		return binomialCompression;
	}
	
	@Result(name="Binomial Row Compression")
	public double binomialRowCompression()
	{
		return binomialRowCompression;
	}
	
	@Result(name="EL Compression")
	public double elCompression()
	{
		return elCompression;
	}
	
	@Result(name="NL Compression")
	public double nlCompression()
	{
		return nlCompression;
	}
	@Result(name="AM Compression")
	public double amCompression()
	{
		return amCompression;
	}
	
	@Result(name="Random order compression")
	public List<Double> randCompression()
	{
		return randCompression;
	}
	
	@Result(name="Degree order compression")
	public List<Double> degCompression()
	{
		return degCompression;
	}
	
	@Result(name="Reverse degree order compression")
	public List<Double> revDegCompression()
	{
		return revDegCompression;
	}
	
	@Result(name="SB order compression")
	public List<Double> sbCompression()
	{
		return sbCompression;
	}
	
	@Result(name="Time to create generator (seconds)")
	public double tMakeGenerator()
	{
		return tMakeGenerator;
	}
	
	@Result(name="Time per sample (seconds)")
	public double tSample()
	{
		return tSample;
	}

}
