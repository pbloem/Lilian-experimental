package org.lilian.motifs;

import static org.data2semantics.platform.util.Functions.python;
import static org.nodes.motifs.MotifCompressor.exDegree;
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
import org.nodes.models.DegreeSequenceModel;
import org.nodes.models.DegreeSequenceModel.Margin;
import org.nodes.models.DegreeSequenceModel.Prior;
import org.nodes.models.ERSimpleModel;
import org.nodes.models.EdgeListModel;
import org.nodes.models.MotifModel;
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
	
	@In(name="null model")
	public String nullModelString;
	
	public NullModel nullModel;

	@Main(print=false)
	public void main() throws IOException, InterruptedException
	{
		nullModel = NullModel.valueOf(nullModelString.toUpperCase());
		
		DPlainMotifExtractor<String> ex = new DPlainMotifExtractor<String>(
				data, motifSamples, motifMinSize, motifMaxSize, 1);
	
		File file = new File(Global.getWorkingDir(), "out.csv");
		BufferedWriter writer = new BufferedWriter(new FileWriter(file)); 

		File fileRel = new File(Global.getWorkingDir(), "relation.csv");
		BufferedWriter writerRel = new BufferedWriter(new FileWriter(fileRel)); 
		
		for(int s : Series.series(instanceSamples))
		{
			DGraph<String> sub = Functions.choose(ex.subgraphs());
			
			List<List<Integer>> instances = ex.occurrences(sub);
			
			int instance = Global.random().nextInt(instances.size());
			double exDegree = exDegree(data, instances.get(instance));
			
			double sizeWith = Double.NaN, 
					sizeWithout = Double.NaN, 
					baseline = Double.NaN;
						
			if(nullModel == NullModel.BETA)
			{
				baseline = new DegreeSequenceModel(betaIterations, betaAlpha, Prior.ML, Margin.LOWERBOUND).codelength(data);
				sizeWith = MotifModel.sizeER(data, sub, instances,  true);
				sizeWithout = MotifModel.sizeER(data, sub, Functions.minList(instances, instance), true);
			} else if(nullModel == NullModel.ER)
			{
				baseline = new ERSimpleModel(false).codelength(data);
				sizeWith = MotifModel.sizeER(data, sub, instances,  true);
				sizeWithout = MotifModel.sizeER(data, sub, Functions.minList(instances, instance), true);
			} else if(nullModel == NullModel.EDGELIST)
			{
				baseline = new EdgeListModel(false).codelength(data);
				sizeWith = MotifModel.sizeEL(data, sub, instances,  true);
				sizeWithout = MotifModel.sizeEL(data, sub, Functions.minList(instances, instance), true);
			}

			double profit = sizeWithout - sizeWith;
						
			double numNodes = sub.size();
			double numLinks = sub.numLinks();
			
			double sumExdegrees = 0;
			for(List<Integer> occ : ex.occurrences(sub))
				sumExdegrees += MotifCompressor.exDegree(data, occ);

			writer.write(exDegree + ", " + numNodes + ", " + numLinks + ", " + profit + "\n");
			
			writerRel.write(sumExdegrees + ", " + numNodes + ", " + numLinks + ", " + sizeWith + "\n");
			
			dot(s, instanceSamples);
		}

		writer.close();
		writerRel.close();
		
		python(Global.getWorkingDir(), "motifs/plot.heuristic.py");
	}
}
