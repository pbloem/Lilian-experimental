package org.lilian.motifs;

import static org.data2semantics.platform.util.Functions.python;
import static org.nodes.util.Functions.dot;
import static org.nodes.util.Series.series;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.data2semantics.platform.Global;
import org.data2semantics.platform.annotation.In;
import org.data2semantics.platform.annotation.Main;
import org.lilian.motifs.Compare.NullModel;
import org.nodes.DGraph;
import org.nodes.Graph;
import org.nodes.motifs.DPlainMotifExtractor;
import org.nodes.motifs.MotifCompressor;
import org.nodes.util.Functions;
import org.nodes.util.Pair;
import org.nodes.util.Series;
import org.nodes.util.bootstrap.LogNormalCI;

public class Heuristic
{
	@In(name="instance samples")
	public int instanceSamples;
	
	@In(name="motif samples")
	public int motifSamples;
	
	@In(name="motif min size", description="minimum motif size (inclusive)")
	public int motifMinSize;
	
	@In(name="motif max size", description="maximum motif size (inclusive!)")
	public int motifMaxSize;
	
	@In(name="beta iterations", description="Number of iteration to use for the beta model.")
	public int betaIterations;
	
	@In(name="beta alpha", description="The alpha value to use in the lower bound of the beta model.")
	public double betaAlpha;
		
	@In(name="data")
	public DGraph<String> data;
	
	@In(name="data name")
	public String dataName;

	@Main(print=false)
	public void main() throws IOException, InterruptedException
	{
		DPlainMotifExtractor<String> ex = new DPlainMotifExtractor<String>(
				data, motifSamples, motifMinSize, motifMaxSize, 1);
	
		File file = new File(Global.getWorkingDir(), "out.csv");
		BufferedWriter writer = new BufferedWriter(new FileWriter(file)); 
				
		for(int s : Series.series(instanceSamples))
		{
			DGraph<String> sub = Functions.choose(ex.subgraphs());
			
			List<List<Integer>> instances = ex.occurrences(sub);
			int instance = Global.random().nextInt(instances.size());
			
			Compare comp = new Compare();
			comp.betaIterations = betaIterations;
			comp.directed = true;
			
			Pair<LogNormalCI, Double> pair = comp.sizeBeta(data, sub, instances, true);
			double sizeWith = pair.first().upperBound(0.05) + pair.second();
			
			pair = comp.sizeBeta(data, sub, Functions.minList(instances, instance), true);
			double sizeWithout = pair.first().upperBound(0.05) + pair.second();
			double profit = sizeWithout - sizeWith;
			
			System.out.println(profit + " (" + sizeWith + ", " + sizeWithout + ")");
			
			double numNodes = sub.size();
			double numLinks = sub.numLinks();
			double exDegree = MotifCompressor.exDegree(data, instances.get(instance));

			writer.write(exDegree + ", " + numNodes + ", " + numLinks + ", " + profit + "\n");
			
			dot(s, instanceSamples);
		}

		writer.close();
		
		python(Global.getWorkingDir(), "motifs/plot.heuristic.py");
	}
}
