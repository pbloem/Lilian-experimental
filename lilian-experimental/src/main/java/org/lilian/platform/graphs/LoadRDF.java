package org.lilian.platform.graphs;

import java.io.File;
import java.util.List;

import org.data2semantics.platform.annotation.In;
import org.data2semantics.platform.annotation.Main;
import org.data2semantics.platform.annotation.Module;
import org.nodes.DTGraph;
import org.nodes.data.RDF;

@Module(name="Load RDF")
public class LoadRDF
{

	@In(name="file")
	public String file;
	
	@In(name="type")
	public String type;
	
	@In(name="whitelist")
	public List<String> nodeWhitelist;
	
	@In(name="simplify")
	public boolean simplify = true;
	
	@Main(name="data", print=false)
	public DTGraph<String, String> load()
	{
		DTGraph<String, String> graph; 
		
		if(type.toLowerCase().equals("turtle"))
			graph = RDF.readTurtle(new File(file), nodeWhitelist);		
		else if(type.toLowerCase().equals("xml"))
			graph = RDF.read(new File(file), nodeWhitelist);
		else
			throw new RuntimeException("RDF type "+type+" not recognized");
		
		if(simplify)
			graph = RDF.simplify(graph);
		
		return graph;
	}
	
}
