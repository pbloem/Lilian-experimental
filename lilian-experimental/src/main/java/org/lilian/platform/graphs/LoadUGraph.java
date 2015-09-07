package org.lilian.platform.graphs;

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.data2semantics.platform.Global;
import org.data2semantics.platform.annotation.In;
import org.data2semantics.platform.annotation.Main;
import org.data2semantics.platform.annotation.Module;
import org.nodes.DGraph;
import org.nodes.DTGraph;
import org.nodes.UGraph;
import org.nodes.data.Data;


@Module(name="Load undirected graph")
public class LoadUGraph
{

	@In(name="file")
	public String file;
	
	@In(name="bipartite")
	public boolean bipartite;
	
	@In(name="blank")
	public boolean blank;

	@Main(name="data", print=false)
	public UGraph<String> load() throws IOException
	{
		UGraph<String> data =  Data.edgeList(new File(file), bipartite, blank);
		
		Global.log().info("Loaded data ("+data.size()+" nodes, "+data.numLinks()+" links).");
		
		return data;
	}
}
