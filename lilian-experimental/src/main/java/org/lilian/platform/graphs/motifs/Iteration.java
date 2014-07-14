package org.lilian.platform.graphs.motifs;

import static org.lilian.util.Functions.tic;
import static org.lilian.util.Functions.toc;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.data2semantics.platform.annotation.In;
import org.data2semantics.platform.annotation.Main;
import org.data2semantics.platform.annotation.Module;
import org.lilian.util.Functions;
import org.lilian.util.Functions.NaturalComparator;
import org.nodes.DGraph;
import org.nodes.DTGraph;
import org.nodes.Global;
import org.nodes.compression.MotifExtractor;
import org.nodes.data.GML;
import org.nodes.util.Generator;

@Module(name = "Masking Motif extraction")

/**
 * TODO: Maintain replacement rules, and replace any sample that fits an existing rule. 
 * @author Peter
 *
 */
public class Iteration
{
	private static int MIN_SIZE = 25;
	
	private DTGraph<String, String> data, current;
	private int samples;
	private int minSize, maxSize;
	private int numMotifs;

	public Iteration(
			@In(name = "data", print = false) DTGraph<String, String> data,
			@In(name = "samples") int samples,
			@In(name = "min size") int minSize,
			@In(name = "max size") int maxSize)
	{
		this.data = data;
		this.current = data;
		
		this.samples = samples;
		
		this.minSize = minSize;
		this.maxSize = maxSize;
	}
	
	@Main
	public void body() throws IOException
	{
		int iteration = 0;
		while(true)
		{
			tic();
			
			Global.log().info("Starting interation " + iteration);
			MotifExtractor me = new MotifExtractor(current, samples, minSize, maxSize);
			
			me.run();
			if(me.result() == null)
				break;
			
			current = me.result().subbedGraph(new ArrayList<Integer>());
			
			Global.log().info("Finished interation " + iteration + " " + toc() + " seconds");
			
			// * write the result graph
			File outGraph = new File(org.data2semantics.platform.Global.getWorkingDir(), String.format("graph%04d.gml", iteration));
			GML.write(current, outGraph);
			
			DTGraph<String, String> motif = me.result().motifGraph(); 
			File outMotif = new File(org.data2semantics.platform.Global.getWorkingDir(), String.format("motif%04d.gml", iteration));
			GML.write(motif, outMotif);
			
			if(current.size() < MIN_SIZE)
				break;
			
			iteration ++;
		}
	}
}
