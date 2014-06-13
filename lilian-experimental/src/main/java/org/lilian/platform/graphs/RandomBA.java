package org.lilian.platform.graphs;

import org.data2semantics.platform.annotation.In;
import org.data2semantics.platform.annotation.Main;
import org.data2semantics.platform.annotation.Module;
import org.nodes.UGraph;
import org.nodes.random.RandomGraphs;

@Module(name="random graph")
public class RandomBA
{
	@In(name="size")
	public int n;
	@In(name="to attach")
	public int toAttach;
	
	@Main(print=false)
	public UGraph<String> randomGraph()
	{
		return RandomGraphs.preferentialAttachment(n, toAttach);
	}

}
