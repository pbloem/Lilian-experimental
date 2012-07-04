package org.lilian.experiment.graphs;

import java.awt.image.BufferedImage;

import org.lilian.experiment.Result;
import org.lilian.experiment.State;

import edu.uci.ics.jung.graph.Graph;

public class SmallGraph<V, E> extends LargeGraph<V, E>
{
	public @State BufferedImage image;

	public SmallGraph(Graph<V, E> graph)
	{
		super(graph);
	}

	@Override
	protected void setup()
	{
		super.setup();
	}

	@Override
	protected void body()
	{
		super.body();
		
		// * Rendering visualization
		logger.info("Rendering visualization.");
		image = org.data2semantics.tools.graphs.Graphs.image(graph, 800, 494);	
	}	
	
	@Result(name="Visualization")
	public BufferedImage visualization()
	{
		return image;
	}
	
}
