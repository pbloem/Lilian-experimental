package org.lilian.platform.graphs;

import java.util.Random;

import org.data2semantics.platform.annotation.In;
import org.data2semantics.platform.annotation.Main;
import org.data2semantics.platform.annotation.Module;
import org.lilian.graphs.DGraph;
import org.lilian.graphs.random.RandomGraphs;

@Module(name="random graph")
public class DRandom
{
	@In(name="size")
	public int n;
	@In(name="probability")
	public double p;
	
	@Main(print=false)
	public DGraph<String> randomGraph()
	{
		return RandomGraphs.randomDirected(n, p);
	
	}

}
