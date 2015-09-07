package org.lilian.platform.graphs;

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.data2semantics.platform.annotation.In;
import org.data2semantics.platform.annotation.Main;
import org.data2semantics.platform.annotation.Module;
import org.nodes.DGraph;
import org.nodes.DTGraph;
import org.nodes.data.Data;


@Module(name="Load graph")
public class LoadDGraph
{

	@In(name="file")
	public String file;
	
	@In(name="fast")
	public boolean fast;
	
	@In(name="blank")
	public boolean blank;

	@Main(name="data", print=false)
	public DGraph<String> load() throws IOException
	{
		if(! fast)
			return Data.edgeListDirected(new File(file), blank);
		
		return Data.edgeListDirectedUnlabeled(new File(file), true);
	}
	
}
