package org.lilian.experiment;

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.data2semantics.tools.graphs.Edge;
import org.data2semantics.tools.graphs.Vertex;
import org.lilian.util.graphs.jung.CPPBoxer;
import org.openrdf.rio.RDFFormat;

import edu.uci.ics.jung.graph.Graph;

public class GraphDimension<V, E> extends AbstractExperiment
{
	private CPPBoxer<V, E> boxer;
	private Graph<V, E> graph;
	private int lb;
	
	public @State int numBoxes;
	
	@Factory
	public static GraphDimension<Vertex<String>, Edge<String>> fromFile(
			@Parameter(name="data") File file,
			@Parameter(name="format", description="one of: rdf, txt") 
				String format,
			@Parameter(name="edge whitelist", description="only used if format is RDF") 
				List<String> edgeWhiteList,
			@Parameter(name="lb")
				int lb	) 
					throws IOException
			
	{
		if(format.trim().toLowerCase().equals("rdf"))
			return new GraphDimension<Vertex<String>, Edge<String>>(
					org.data2semantics.tools.graphs.Graphs
					.graphFromRDF(file, RDFFormat.RDFXML, null, edgeWhiteList), lb);
		
		return new GraphDimension<Vertex<String>, Edge<String>>(
				org.data2semantics.tools.graphs.Graphs
					.graphFromTSV(file), lb);
	}	
	
	public GraphDimension(
			@Parameter(name="graph") 
				Graph<V, E> graph,
			@Parameter(name="lb")
				int lb)
	{
		this.graph = graph;
		this.lb = lb;
	}

	@Override
	protected void setup()
	{
		boxer = new CPPBoxer<V, E>(graph);
	}

	@Override
	protected void tearDown()
	{
		super.tearDown();
		boxer = null;
		graph = null;
	}

	@Override
	protected void body()
	{
		numBoxes = boxer.box(lb).size();
	}
	
	@Result(name="number of boxes")
	public int numBoxes()
	{
		return numBoxes;
	}

}
