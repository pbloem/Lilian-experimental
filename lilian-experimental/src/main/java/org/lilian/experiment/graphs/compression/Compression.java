package org.lilian.experiment.graphs.compression;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.lilian.Global;
import org.lilian.experiment.AbstractExperiment;
import org.lilian.experiment.Parameter;
import org.lilian.experiment.Result;
import org.lilian.experiment.State;
import org.lilian.util.BitString;
import org.lilian.util.Compressor;
import org.lilian.util.GZIPCompressor;
import org.lilian.util.Series;
import org.lilian.util.graphs.jung.Graphs;

import edu.uci.ics.jung.graph.Graph;

public class Compression<V, E> extends AbstractExperiment
{

	private int runs;
	private Graph<V, E> graph;
	public @State double mean, min, max;
	
	public @State Compressor<BitString> comp = new GZIPCompressor<BitString>(128);
	public @State BitString bitString;
	
	public Compression(
		@Parameter(name="data") Graph<V, E> graph,
		@Parameter(name="runs") int runs)
	{
		super();
		this.graph = graph;
		this.runs = runs;
	}

	@Override
	protected void setup()
	{
	}

	@Override
	protected void body()
	{
		List<V> list = new ArrayList<V>(graph.getVertices());
		
		mean = 0.0;
		min = Double.POSITIVE_INFINITY;
		max = Double.NEGATIVE_INFINITY;
		
		for(int i : Series.series(runs))
		{
//			if(i%(runs/10) == 0)
//				logger.info("run " + i);
			Collections.shuffle(list, Global.random);
			bitString = Graphs.toBits(graph, list);
			double c = comp.ratio(bitString);
			
			mean += c;
			min = Math.min(min, c);
			max = Math.max(max, c);
		}
		
		mean /= runs;
	}

	@Result(name="Mean compression ratio")
	public double ratio()
	{
		return mean;
	}
	
	@Result(name="Min")
	public double min()
	{
		return min;
	}
	
	@Result(name="max")
	public double max()
	{
		return max;
	}
	
	// @Result(name="BitString")
	public String string()
	{
		StringBuffer bf = new StringBuffer();
		
		int i = 0;
		for(boolean bit : bitString)
		{
			bf.append(bit ? '1' : '0');

			if(i++ % 80 == 0)
				bf.append('\n');
		}
		
		return bf.toString();
	}	
}
