package org.lilian.platform.graphs;

import java.util.Random;

import org.data2semantics.platform.annotation.In;
import org.data2semantics.platform.annotation.Main;
import org.data2semantics.platform.annotation.Module;
import org.lilian.graphs.DGraph;
import org.lilian.graphs.UGraph;
import org.lilian.graphs.random.RandomGraphs;

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
