package org.lilian.experiment;

import static org.lilian.util.Series.series;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.collections15.Factory;
import org.apache.commons.collections15.functors.ConstantFactory;
import org.lilian.Global;
import org.lilian.util.Series;

import edu.uci.ics.jung.algorithms.generators.GraphGenerator;
import edu.uci.ics.jung.algorithms.generators.random.ErdosRenyiGenerator;
import edu.uci.ics.jung.algorithms.shortestpath.DijkstraShortestPath;
import edu.uci.ics.jung.graph.Graph;
import edu.uci.ics.jung.graph.UndirectedGraph;
import edu.uci.ics.jung.graph.UndirectedSparseGraph;
import edu.uci.ics.jung.graph.util.Graphs;

/**
 * This simple experiment goes as follows
 * 
 * - Start with a simple graph
 * - Repeat until no more edges remain:
 * 		- Choose a random pair of nodes
 * 		- Find the shortest path between nodes
 * 		- If such a path exists, remove the edges
 * 
 * We are interested in the scatter/density plot of the path length and the time
 * 
 * @author Peter
 *
 */
public class GraphExperiment extends AbstractExperiment
{
	public enum Structure {FULL, RANDOM, SCALE_FREE};
	
	protected int nodes;
	protected Structure structure;
	protected boolean repetition;
	protected double p;
	protected int maxTries;
	
	// * State variables
	public @State Graph<Integer, Integer> graph;
	public @State List<Double> length;
	public @State long currentGeneration;
	public @State IntegerFactory integerFactory;
	public @State List<Integer> pathLengths;
	public @State List<Integer> triesList;
	public @State boolean maxTriesTriggered;

	public GraphExperiment(
			@Parameter(name="nodes", description="The number of nodes in the graph.")
			int nodes,
			@Parameter(name="structure", description="The initial structure of the graph. One of: full, random, scale-free.")
			String structure,
			@Parameter(name="with repetition", description="Whether a given pair of nodes can be chosen twice.")
			boolean repetition,
			@Parameter(name="er edge probability", description="The parameter p for the random network.")
			double p,
			@Parameter(name="max tries", description="The maximum number of tries to find a connected par of vertices until the experiment halts.")
			int maxTries)
	{
		this.nodes = nodes;
		if(structure.toLowerCase().trim().equals("full"))
			this.structure = Structure.FULL;
		else if(structure.toLowerCase().trim().equals("random"))
			this.structure = Structure.RANDOM;
		else if(structure.toLowerCase().trim().equals("scale-free"))
			this.structure = Structure.SCALE_FREE;
		else
			throw new IllegalArgumentException("Argument " + structure + " for parameter 'structure' not understood.");
		
		this.repetition = repetition;
		if(!repetition)
			throw new IllegalStateException("Not yet implemented");
		
		this.p = p;
		this.maxTries = maxTries;
	}
	
	@Override
	protected void setup()
	{		
		currentGeneration = 0;
		integerFactory = new IntegerFactory();
		
		pathLengths = new ArrayList<Integer>();
		triesList = new ArrayList<Integer>();
		maxTriesTriggered = false;
		
		switch(structure)
		{
			case FULL:
				throw new IllegalStateException("'Full' not yet implemented");
			case RANDOM:
				GraphGenerator<Integer, Integer> gen =
					new ErdosRenyiGenerator<Integer, Integer>(
						new GraphFactory(),
						integerFactory, 
						new IntegerFactory(), 
						nodes, p);
				graph = gen.create();
				break;
		}
	}

	@Override
	protected void body()
	{
		while(graph.getEdgeCount() > 0 && ! maxTriesTriggered)
		{
			currentGeneration++;
			logger.info("Generation " + currentGeneration + " edge count " + graph.getEdgeCount());
			
			List<Integer> path = new ArrayList<Integer>();
			int tries = 0;
			
			
			// System.out.println("ec " + graph.getEdgeCount());
			// System.out.println(graph);
			while(path.size() == 0 && !maxTriesTriggered)
			{
				// * draw random pair of edges
				int v1 = Global.random.nextInt(integerFactory.max()+1), 
					v2 = Global.random.nextInt(integerFactory.max()+1);				
				
				// * Find the shortest path
				DijkstraShortestPath<Integer, Integer> sp = 
						new DijkstraShortestPath<Integer, Integer>(graph);
				path = sp.getPath(v1, v2);
				tries++;
				
				// System.out.println(path + " for "+ v1 + " " + v2);
				if(tries > maxTries)
					maxTriesTriggered = true;
			}
			

			triesList.add(tries);

			if(!maxTriesTriggered)
			{
				pathLengths.add(path.size());
				for(int edge : path)
					graph.removeEdge(edge);
			}
		}
	}
	
	@Result(name="Remaining nodes")
	public int remainingNodes()
	{
		return graph.getVertexCount();
	}
	
	@Result(name="max tries triggered")
	public boolean maxTriesTriggered()
	{
		return maxTriesTriggered;
	}
	
	@Result(name="path lengths")
	public List<Integer> pathLentghs()
	{
		return pathLengths;
	}
	
	@Result(name="tries")
	public List<Integer> tries()
	{
		return triesList;
	}
	
	private class GraphFactory implements Factory<UndirectedGraph<Integer, Integer>>
	{
		@Override
		public UndirectedGraph<Integer, Integer> create()
		{
			return new UndirectedSparseGraph<Integer, Integer>();
		}	
	}
	
	private class IntegerFactory implements Factory<Integer>
	{
		private int i = 0;

		@Override
		public Integer create()
		{
			return i++;
		}
		
		public int max()
		{
			return i-1;
		}
		
	}

}
