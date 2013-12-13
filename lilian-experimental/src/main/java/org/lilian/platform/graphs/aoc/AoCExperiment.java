package org.lilian.platform.graphs.aoc;

import static org.lilian.util.Functions.choose;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.data2semantics.platform.annotation.In;
import org.data2semantics.platform.annotation.Main;
import org.data2semantics.platform.util.Series;
import org.lilian.Global;
import org.lilian.graphs.DTGraph;
import org.lilian.graphs.DTLink;
import org.lilian.graphs.DTNode;
import org.lilian.graphs.MapDTGraph;
import org.lilian.models.BasicFrequencyModel;
import org.lilian.util.Functions;

public class AoCExperiment
{
	@In(name="max")
	public int maxEdges;
	@In(name="loops")
	public int loops;
	@In(name="data size")
	public int dataSize = 1000;

	@Main
	public void main() throws IOException
	{
		BufferedWriter writer = new BufferedWriter(new FileWriter(new File("aoc.csv")));
		
		int finite = 0, total = 0;
		
		for(int loop : Series.series(loops))
		{
			Global.log().info("loop " +  loop);
			
			DTGraph<String, Boolean> graph = new MapDTGraph<String, Boolean>();
			graph.add("start");
			
			BasicFrequencyModel<DTNode<String, Boolean>> states = new BasicFrequencyModel<DTNode<String, Boolean>>();
			states.add(graph.get(0));
			
			for(int edge : Series.series(maxEdges))
			{
				// * Add a random edge to the graph
				DTNode<String, Boolean> from = choose(graph.nodes());
				
				DTNode<String, Boolean> to;
				if(Global.random.nextInt(graph.size() + 1) == 0)
					to = graph.add("s");
				else
					to = choose(graph.nodes());
				
				boolean tag = Global.random.nextBoolean();
				if(! from.connected(to, tag))
					from.connect(to, tag);
				
				// * Get data estimates
				states = new BasicFrequencyModel<DTNode<String, Boolean>>();
				BasicFrequencyModel<Boolean> bits = new BasicFrequencyModel<Boolean>();

				
				double bitsPerSymbol = 0.0;
				double bitsPerState = 0.0;
				
				boolean finished = false;
				DTNode<String, Boolean> current = graph.node("start");
				// System.out.println(graph);
				
				int step = 0;
				while(step < dataSize)
				{
					step++;
					
					List<DTLink<String, Boolean>> outLinks = new ArrayList<DTLink<String,Boolean>>(current.linksOut());
					
					if(outLinks.isEmpty())
						break;
					
					// * record the entropy of the out links
					BasicFrequencyModel<Boolean> model = new BasicFrequencyModel<Boolean>();
					for(DTLink<String, Boolean> link : outLinks)
						model.add(link.tag());
					
					bitsPerSymbol += model.entropy();
					
					// * record the state
					states.add(current);
					
					DTLink<String, Boolean> link = choose(outLinks);
					bits.add(link.tag());
					
					current = link.to();
				}

				if(step == dataSize)
				{
					bitsPerSymbol /= (double) dataSize;
					writer.write(states.entropy() + ", " + bits.entropy() + ", " + graph.size() + ", " + graph.numLinks() + "\n");
				} else {
					finite++;
				}
				total ++;
			}
		}
		
		writer.close();
		Global.log().info("Finite: " + finite + ", total: " + total);
	}
}
