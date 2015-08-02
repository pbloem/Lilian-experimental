package org.lilian.platform.graphs;

import static org.nodes.util.Series.series;
import static org.nodes.models.USequenceModel.CIMethod;
import static org.nodes.models.USequenceModel.CIType;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.data2semantics.platform.Global;
import org.data2semantics.platform.annotation.In;
import org.data2semantics.platform.annotation.Main;
import org.data2semantics.platform.annotation.Module;
import org.data2semantics.platform.annotation.Out;
import org.data2semantics.platform.util.FrequencyModel;
import org.nodes.Graph;
import org.nodes.Link;
import org.nodes.MapUTGraph;
import org.nodes.Node;
import org.nodes.UGraph;
import org.nodes.models.USequenceModel;
import org.nodes.random.RandomGraphs;
import org.nodes.util.Pair;
import org.nodes.util.Series;

@Module(name="Test confidence intervals")
public class CIs
{
	@In(name="single sample")
	public int singleSample;

	
	@In(name="big sample")
	public int bigSample;

	@In(name="coverage sample")
	public int covSample;
	
	@In(name="step size")
	public int stepSize;
	
	@In(name="alpha")
	public double alpha;
	
	@In(name="gold standard")
	public double gold;
	
	@In(name="coverage sizes")
	public List<Integer> covSizes;
	
	@In(name="data")
	public Graph<String> data;
	
	@Main(print=false)
	public void samples() throws IOException
	{
		// * transform the data into a simple, undirected graph
		data = toSimpleUGraph(data);
		
		// * Construct a single sample to plot
		Global.log().info("Sampling a single set of log probabilites.");
		
		File file = new File(Global.getWorkingDir(), "single-sample.csv");
		BufferedWriter writer = new BufferedWriter(new FileWriter(file));
		
		USequenceModel<String> model = new USequenceModel<String>(data);
		
		for(int i : series(singleSample))
		{
			model.nonuniform();
			if(i % (singleSample/100) == 0)
				System.out.print(".");
		}
		System.out.println();
		
		if(gold < 0.0)
			gold = model.logNumGraphs(); 
			
		
		for(double value: model.logSamples())
			writer.write(value + "\n");
			
		writer.close();
		
		// * Estimate coverage
		Global.log().info("Estimating coverage for small samples.");

		file = new File(Global.getWorkingDir(), "coverage.csv");
		writer = new BufferedWriter(new FileWriter(file));
		
		for(int n : covSizes)
		{
			FrequencyModel<CIMethod> hits = new FrequencyModel<USequenceModel.CIMethod>();
			for(int i : series(covSample))
			{
				model = new USequenceModel<String>(data, n);
				
				for(CIMethod method : CIMethod.values())
				{
					Pair<Double, Double> interval = model.confidence(alpha, method, CIType.LOWER_BOUND);
					
					if(gold >= interval.first())
						hits.add(method);
				}
			}
			
			writer.write(n);
			for(CIMethod method : CIMethod.values())
			{
				// * estimate of coverage
				double est = hits.frequency(method) / (double) covSample;
				System.out.println(n + " coverage error for " + method + "\t: " + ((1.0 - alpha) - est));
				
				writer.write(", " + ((1.0 - alpha) - est));
			}
			
			writer.write("\n");
			writer.flush();
		}
		
		writer.close();
		
		// * Perform a convergence test.  
		Global.log().info("Performing convergence test.");

		file = new File(Global.getWorkingDir(), "convergence.csv");
		writer = new BufferedWriter(new FileWriter(file));
		
		model = new USequenceModel<String>(data);

		for(int samples : series(bigSample+1))
		{
			model.nonuniform();
			if(samples % stepSize == 0)
			{
				System.out.println("Convergence: " + samples + " samples");
				
				if(samples > 0 && samples < 100)
					System.out.println(model.logSamples());
				
				writer.write(samples + ", " +
						+ model.effectiveSampleSize() + ", " + 
						+ model.logNumGraphs() + ", " 
						+ model.logNormalMean());
				
				for(CIMethod method : CIMethod.values())
				{
					Pair<Double, Double> interval = model.confidence(alpha, method, CIType.LOWER_BOUND);
					
					writer.write(", " + interval.first());
				}
				
				writer.write("\n");
				writer.flush();
			}
		}
		
		writer.close();
		
	}

	private Graph<String> toSimpleUGraph(Graph<String> data)
	{
		MapUTGraph<String, String> result = new MapUTGraph<String, String>();
		
		for(Node<String> node : data.nodes())
			result.add("");
		
		for(Link<String> link : data.links())
		{
			Node<String> a = result.get(link.first().index()),
			             b = result.get(link.second().index());
			
			if(!a.connected(b))
				a.connect(b);
		}
		
		return result;
	}
	
	

}
