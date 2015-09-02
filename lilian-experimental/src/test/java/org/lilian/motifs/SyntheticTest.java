package org.lilian.motifs;

import static org.junit.Assert.*;
import static org.nodes.motifs.MotifCompressor.MOTIF_SYMBOL;
import static org.nodes.util.Functions.log2Choose;
import static org.nodes.util.Series.series;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.data2semantics.platform.Global;
import org.junit.Test;
import org.lilian.motifs.Compare.NullModel;
import org.nodes.UGraph;
import org.nodes.UNode;
import org.nodes.compression.BinomialCompressor;
import org.nodes.data.Data;
import org.nodes.motifs.MotifCompressor;
import org.nodes.motifs.UPlainMotifExtractor;
import org.nodes.random.RandomGraphs;
import org.nodes.util.Series;

public class SyntheticTest
{

	@Test
	public void test()
		throws IOException
	{
		File dir = new File("/Users/Peter/Documents/motif/");
		dir.mkdirs();
		
		int n = 100;
		
		//org.nodes.Global.secureRandom(42);
		
		UGraph<String> graph = RandomGraphs.random(n, 0.05);
		int m = graph.numLinks();
		
		UPlainMotifExtractor<String> ex = new UPlainMotifExtractor<String>(graph, 1000, 5);

		double baseline = log2Choose(m, (n*n-n)/2);

		UGraph<String> sub = ex.subgraphs().get(0);
		System.out.println("frequency: " + ex.frequency(sub));
		double motifSize = Compare.size(graph, sub, ex.occurrences(sub), NullModel.EDGELIST, false);
		
		System.out.println(baseline + " " + motifSize);
		
//		List<List<Integer>> wiring = new ArrayList<List<Integer>>();
//		UGraph<String> subbed = MotifCompressor.subbedGraph(graph, sub, ex.occurrences(sub), wiring);
//		
//		// * store the graph
//		File graphFile = new File(dir, "graph.edgelist");
//		Data.writeEdgeList(graph, graphFile);
//		
//		// * store the node colors
//		List<Integer> nc = new ArrayList<Integer>(graph.size());
//		for(int i : series(graph.size()))
//			nc.add(0);
//		
//		int col = 1;
//		for(List<Integer> occ : ex.occurrences(sub))
//		{
//			for(int i : occ)
//			{
//				nc.set(i, col);
//			}
//			col++;
//		}
//		
//		File ncFile = new File(dir, "graph-colors.csv");
//		BufferedWriter ncWriter = new BufferedWriter(new FileWriter(ncFile));
//		
//		for(int color : nc)
//			ncWriter.write(color + "\n");
//		ncWriter.close();
//		
//		// * store the subbed graph
//		File subbedFile = new File(dir, "subbed.edgelist");
//		Data.writeEdgeList(subbed, subbedFile);
//		
//		// * store the labels
//		File scFile = new File(dir, "subbed-colors.csv");
//		BufferedWriter scWriter = new BufferedWriter(new FileWriter(scFile));
//		
//		for(UNode<String> node : subbed.nodes())
//			scWriter.write((node.label().equals(MOTIF_SYMBOL) ? 0 : 1) + "\n");
//		scWriter.close();
	}

}
