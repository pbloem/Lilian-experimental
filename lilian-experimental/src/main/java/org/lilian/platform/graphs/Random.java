package org.lilian.platform.graphs;

import org.data2semantics.platform.annotation.In;
import org.data2semantics.platform.annotation.Main;
import org.data2semantics.platform.annotation.Module;
import org.nodes.DTGraph;
import org.nodes.random.RandomGraphs;

public class Random
{
	@Module(name="random graph")
	public static class DRandom
	{
		@In(name="size")
		public int n;
		@In(name="probability")
		public double p;
		
		@Main(name="data", print=false)
		public DTGraph<String, String> randomGraph()
		{
			return RandomGraphs.randomDirected(n, p);
		}
	}	
	
	@Module(name="pa graph")
	public static class PARandom
	{
		@In(name="size")
		public int n;
		@In(name="to attach")
		public int ta;		
		
		@Main(name="data", print=false)
		public DTGraph<String, String> randomGraph()
		{
			return RandomGraphs.preferentialAttachmentDirected(n, ta);
		}
	}

}
