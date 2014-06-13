package org.lilian.platform.graphs;

import org.data2semantics.platform.annotation.In;
import org.data2semantics.platform.annotation.Main;
import org.data2semantics.platform.annotation.Module;
import org.nodes.DTGraph;
import org.nodes.random.RandomGraphs;

@Module(name="random graph")
public class DRandom
{
	@In(name="size")
	public int n;
	@In(name="probability")
	public double p;
	
	@Main(print=false)
	public DTGraph<String, String> randomGraph()
	{
		return RandomGraphs.randomDirected(n, p);
	}

}
