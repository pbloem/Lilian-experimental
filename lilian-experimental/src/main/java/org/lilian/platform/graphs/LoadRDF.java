package org.lilian.platform.graphs;

import java.io.File;
import java.util.List;

import org.data2semantics.platform.annotation.In;
import org.data2semantics.platform.annotation.Main;
import org.data2semantics.platform.annotation.Module;
import org.lilian.graphs.DTGraph;
import org.lilian.graphs.MapDTGraph;
import org.lilian.graphs.data.RDF;

@Module(name="Load RDF")
public class LoadRDF
{

	@In(name="file")
	public String file;
	
	@In(name="type")
	public String type;
	
	@In(name="whitelist")
	public List<String> nodeWhitelist;
	
	@Main(name="data", print=false)
	public DTGraph<String, String> load()
	{
		if(type.toLowerCase().equals("turtle"))
			return RDF.readTurtle(new File(file), nodeWhitelist);
		
		if(type.toLowerCase().equals("xml"))
			return RDF.read(new File(file), nodeWhitelist);
		
		throw new RuntimeException("RDF type "+type+" not recognized");
	}
	
}
