package org.lilian.experiment;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.data2semantics.tools.graphs.Edge;
import org.data2semantics.tools.graphs.Vertex;
import org.lilian.experiment.graphs.GraphMeasures;
import org.lilian.util.graphs.jung.Boxing;
import org.lilian.util.graphs.jung.CPPBoxer;
import org.openrdf.rio.RDFFormat;

import edu.uci.ics.jung.graph.Graph;

public class GraphDimension<V, E> extends AbstractExperiment
{
	private CPPBoxer<V, E> boxer;
	
	private Graph<V, E> graph;
	private Graph<Integer, Integer> post;
	
	private int lb;
	
	public @State int numBoxes;
	public @State double propBoxes;
	public @State double meanMass;
	public @State int uncovered;
	public @State int overCovered;
	public @State List<Integer> boxSizes;
	public @State GraphMeasures<V, E> e0;
	public @State GraphMeasures<Integer, Integer> e1, e2, e3, e4; 
	
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
		boxSizes = new ArrayList<Integer>();
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
		Boxing<V, E> boxing =  boxer.box(lb);
		
		numBoxes = boxing.size();
		propBoxes = numBoxes/(double)graph.getVertexCount();
		meanMass = graph.getVertexCount()/(double)numBoxes;
		
		uncovered = boxing.uncovered().size();
		overCovered = boxing.overCovered().size();
		
		post = boxing.postGraph();
		
		for(Set<V> box : boxing)
			boxSizes.add(box.size());
		
		e0 = new GraphMeasures<V, E>(graph, "small");
		Environment.current().child(e0);
		
		e1 = new GraphMeasures<Integer, Integer>(post, "small");
		Environment.current().child(e1);

		Boxing<Integer, Integer> iBoxing = new CPPBoxer<Integer, Integer>(post).box(lb);
		Graph<Integer, Integer> post2 = iBoxing.postGraph();
		
		e2 = new GraphMeasures<Integer, Integer>(post2, "small");
		Environment.current().child(e2);
		
		iBoxing = new CPPBoxer<Integer, Integer>(post2).box(lb);
		Graph<Integer, Integer> post3 = iBoxing.postGraph();
		
		e3 = new GraphMeasures<Integer, Integer>(post3, "small");
		Environment.current().child(e3);		
		
		iBoxing = new CPPBoxer<Integer, Integer>(post3).box(lb);
		Graph<Integer, Integer> post4 = iBoxing.postGraph();
		
		e4 = new GraphMeasures<Integer, Integer>(post4, "small");
		Environment.current().child(e4);		
		
	}
	
	@Result(name="number of boxes")
	public int numBoxes()
	{
		return numBoxes;
	}
	
	@Result(name="Mean box mass")
	public double meanMass()
	{
		return meanMass;
	}
	
	@Result(name="box proportion")
	public double propBoxes()
	{
		return propBoxes;
	}
	
	@Result(name="Uncovered", description="Number of uncovered vertices")
	public int uncovered()
	{
		return uncovered;
	}
	
	@Result(name="Overcovered", description="Number of vertices covered more than once")
	public int overCovered()
	{
		return overCovered;
	}
	
	@Result(name="Box sizes")
	public List<Integer> boxSizes()
	{
		return boxSizes;
	}
	
//	@Result(name="Graph plot")
//	public BufferedImage plot0()
//	{
//		return e0.visualization();
//	}
//	
//	@Result(name="Graph plot: first reduction")
//	public BufferedImage plot1()
//	{
//		return e1.visualization();
//	} 
//	
//	@Result(name="Graph plot: second reduction")
//	public BufferedImage plot2()
//	{
//		return e2.visualization();
//	} 
//	
//	@Result(name="Graph plot: third reduction")
//	public BufferedImage plot3()
//	{
//		return e3.visualization();
//	}
//	
//	@Result(name="Graph plot: fourth reduction")
//	public BufferedImage plot4()
//	{
//		return e4.visualization();
//	}	
//	
//	@Result(name="reduced mean degree")
//	public double reducedMean()
//	{
//		return e1.meanDegree();
//	}
//
//	@Result(name="reduced degree sample standard deviation")
//	public double reducedStd()
//	{
//		return e1.stdDegree();
//	}	
//	
}
