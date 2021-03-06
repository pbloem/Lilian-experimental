package org.lilian.experiment.graphs;

import static org.lilian.util.Functions.tic;
import static org.lilian.util.Functions.toc;

import org.data2semantics.platform.Global;
import org.data2semantics.platform.annotation.In;
import org.data2semantics.platform.annotation.Main;
import org.data2semantics.platform.annotation.Module;
import org.data2semantics.platform.annotation.Out;
import org.lilian.util.Functions;
import org.nodes.Graph;
import org.nodes.compression.BinomialCompressor;
import org.nodes.compression.BinomialRowCompressor;
import org.nodes.compression.EdgeListCompressor;
import org.nodes.compression.NeighborListCompressor;
import org.nodes.util.Compressor;

@Module(name="compression test")
public class CompressionTest
{

	@In(name="graph", print=false)
	public Graph<String> graph;
	
	@Out(name="neighbour list compression")
	public double nlCompression;
	
	@Out(name="edge list comprssion")
	public double elCompression;
	
	@Out(name="Binomial (row) compression")
	public double binRowCompression;
	
	@Out(name="Binomial compression")
	public double binCompression;

	
	@Main
	public void body()
	{
		Compressor<Graph<String>> comp;
		
		tic();
		comp = new EdgeListCompressor<String>();
		elCompression = comp.compressedSize(graph);
		Global.log().info("Finished edge list ("+toc()+" seconds)");
		
		tic();
		comp = new NeighborListCompressor<String>();
		nlCompression = comp.compressedSize(graph);
		Global.log().info("Finished neighbor list ("+toc()+" seconds)");
		
		tic();
		comp = new BinomialRowCompressor<String>();
		binRowCompression = comp.compressedSize(graph);
		Global.log().info("Finished binomial row ("+toc()+" seconds)");

		
		tic();
		comp = new BinomialCompressor<String>();
		binCompression = comp.compressedSize(graph);
		Global.log().info("Finished binomial ("+toc()+" seconds)");

	}

}
