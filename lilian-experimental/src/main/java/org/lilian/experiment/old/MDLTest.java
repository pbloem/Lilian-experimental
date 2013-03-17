package org.lilian.experiment.old;

import java.util.Collection;

import org.lilian.experiment.AbstractExperiment;
import org.lilian.experiment.Factory;
import org.lilian.experiment.Parameter;
import org.lilian.experiment.Result;
import org.lilian.util.graphs.old.BaseGraph;
import org.lilian.util.graphs.old.Graph;
import org.lilian.util.graphs.old.Node;
import org.lilian.util.graphs.old.algorithms.GraphMDL;
import org.lilian.util.graphs.old.algorithms.Subdue;

public class MDLTest<L, N extends Node<L, N>> extends AbstractExperiment
{
	private Graph<L, N> graph;
	private int iterations;
	private int width;
	private boolean sparse;
	
	public double compressedSize;
	public double modelSize;
	public BaseGraph<Subdue.Token> model;
	public Collection<Subdue<L,N>.Substructure> subs;
	
	public static @Factory <L, N extends Node<L, N>> MDLTest<L, N> make(
			@Parameter(name="data") Graph<L, N> graph, 
			@Parameter(name="iterations") int iterations,
			@Parameter(name="width") int width,
			@Parameter(name="sparse") boolean sparse)
	{
		MDLTest<L, N> mdlTest = new MDLTest<L, N>();
		
		mdlTest.graph = graph;
		mdlTest.iterations = iterations;
		mdlTest.width = width;
		mdlTest.sparse = sparse;
		
		return mdlTest;
	}
	

	@Override
	protected void setup()
	{
	}

	@Override
	protected void body()
	{
		Subdue<L,N> subdue = new Subdue<L, N>(graph, sparse);
		subs = subdue.search(iterations, width, width, -1);
		Subdue<L,N>.Substructure sub = subs.iterator().next();
		
		for(Subdue<L,N>.Substructure s : subs )
			System.out.println(s);
		
		model = sub.subgraph();
		compressedSize = sub.score();
		modelSize = GraphMDL.mdl(model);
	}
	
	@Result(name="model")
	public BaseGraph<Subdue.Token> model()
	{
		return model;
	}
	
	@Result(name="graph")
	public Graph<L, N> graph()
	{
		return graph;
	}
	
	@Result(name="compressed size (in bits)")
	public double compressedSize()
	{
		return compressedSize;
	}
	
	@Result(name="model size (in bits)")
	public double modelSize()
	{
		return modelSize;
	}
	
	@Result(name="graph size (in nodes)")
	public double graphSize()
	{
		return graph.size();
	}
	
	@Result(name="number of edges")
	public double numEdges()
	{
		return graph.numEdges();
	}
	
	@Result(name="substructures")
	public Collection<Subdue<L, N>.Substructure> substructures()
	{
		return subs;
	}

	@Result(name="density")
	public double density()
	{
		double n = graph.size();
		double e = graph.numEdges();
		
		return e/((n*n - n ) / 2.0);
	}
}
