package org.lilian.platform.graphs;

import java.io.File;
import java.util.List;

import org.data2semantics.platform.annotation.In;
import org.data2semantics.platform.annotation.Main;
import org.data2semantics.platform.annotation.Module;
import org.nodes.DGraph;
import org.nodes.DTGraph;
import org.nodes.Graph;
import org.nodes.Graphs;
import org.nodes.data.RDF;

@Module(name="Load RDF")
public class ToyGraphs
{

	@In(name="name")
	public String name;
	
	@Main(name="data", print=false)
	public DGraph<String> load()
	{
		if(name.equals("jbc directed"))
			return Graphs.jbcDirected();
		
		throw new IllegalArgumentException("Name "+name+" not recognized.");
	}
	
}
