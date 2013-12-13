package org.lilian.experiment.old;

import java.awt.image.BufferedImage;

import org.lilian.experiment.Result;
import org.lilian.experiment.State;

import edu.uci.ics.jung.graph.Graph;

public class SmallGraph<V, E> extends LargeGraph<V, E>
{
	public @State BufferedImage image;
	public @State double globalClusteringCoefficient;

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
		image = null;
		
		logger.info("Calculating global Clustering Coefficient");
		// globalClusteringCoefficient = Graphs.clusteringCoefficient(graph);
	}	
	
	@Result(name="Visualization")
	public BufferedImage visualization()
	{
		return image;
	}
	
	@Result(name="Global clustering coefficient")
	public double globalClusteringCoefficient()
	{
		return globalClusteringCoefficient;
	}
	
}
