package org.lilian.experiment.old;

import java.util.ArrayList;
import java.util.List;

import org.lilian.experiment.AbstractExperiment;
import org.lilian.experiment.BasicResults;
import org.lilian.experiment.Environment;
import org.lilian.experiment.Experiment;
import org.lilian.experiment.Parameter;
import org.lilian.experiment.Result;
import org.lilian.experiment.Results;

import edu.uci.ics.jung.graph.DirectedGraph;
import edu.uci.ics.jung.graph.Graph;

public class GraphMeasures<V, E> extends AbstractExperiment
{
	protected Graph<V, E> graph;
	protected String size;
	
	private boolean directed;
	public List<Experiment> experiments = new ArrayList<Experiment>();
	
	public GraphMeasures(
			@Parameter(name="data") Graph<V, E> graph)
	{
		this(
			graph,
			graph.getVertexCount() < 5000 ? "small" : 
				graph.getVertexCount() < 100000 ? "large" : "huge");
	}
	
	public GraphMeasures(
			@Parameter(name="data") Graph<V, E> graph, 
			@Parameter(name="size") String size)
	{
		this.graph = graph;
		this.size = size;
	}

	@Override
	protected void setup()
	{
		directed = graph instanceof DirectedGraph<?, ?>;
		
		if(size.equals("huge"))
			experiments.add(new HugeGraph<V, E>(graph));
		else if(size.equals("large"))
			experiments.add(new LargeGraph<V, E>(graph));
		else if(size.equals("small"))
			experiments.add(new SmallGraph<V, E>(graph));
		else
			throw new RuntimeException("Size parameter ("+size+") not understood.");
		
		if(directed)
		{
			if(size.equals("huge"))
				experiments.add(new HugeDirectedGraph<V, E>(graph));
			else if(size.equals("large"))
				experiments.add(new LargeDirectedGraph<V, E>(graph));
			else if(size.equals("small"))
				experiments.add(new SmallDirectedGraph<V, E>(graph));
			else
				throw new RuntimeException("Size parameter ("+size+") not understood.");
		}
	}

	@Override
	protected void body()
	{
		for(Experiment experiment : experiments)
			Environment.current().child(experiment);
	}
	
	@Result(name="directed")
	public boolean directed()
	{
		return directed;
	}
	
	@Result(name="result")
	public Results allResults()
	{
		BasicResults results = new BasicResults();
		for(Experiment experiment : experiments)
			results.addAll(experiment);
		return results;
	}
}