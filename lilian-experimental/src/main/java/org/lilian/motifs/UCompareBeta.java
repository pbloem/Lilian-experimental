package org.lilian.motifs;

import static org.nodes.util.Series.series;
import static org.nodes.models.USequenceModel.CIMethod;
import static org.nodes.models.USequenceModel.CIType;
import static org.nodes.motifs.MotifCompressor.exDegree;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.data2semantics.platform.Global;
import org.data2semantics.platform.annotation.In;
import org.data2semantics.platform.annotation.Main;
import org.data2semantics.platform.annotation.Module;
import org.data2semantics.platform.annotation.Out;
import org.data2semantics.platform.util.FrequencyModel;
import org.lilian.util.Functions.NaturalComparator;
import org.nodes.DGraph;
import org.nodes.Graph;
import org.nodes.Graphs;
import org.nodes.Subgraph;
import org.nodes.UGraph;
import org.nodes.UNode;
import org.nodes.algorithms.Nauty;
import org.nodes.compression.BinomialCompressor;
import org.nodes.compression.EdgeListCompressor;
import org.nodes.compression.NeighborListCompressor;
import org.nodes.models.USequenceModel;
import org.nodes.motifs.MotifCompressor;
import org.nodes.motifs.UPlainMotifExtractor;
import org.nodes.random.RandomGraphs;
import org.nodes.random.SubgraphGenerator;
import org.nodes.util.Generator;
import org.nodes.util.Generators;
import org.nodes.util.Order;
import org.nodes.util.Pair;
import org.nodes.util.Series;

/**
 * Compares the code length under the motifs to that under a given null-model
 * 
 * For undirected data.
 * 
 * @author Peter
 */

@Module(name="Test confidence intervals")
public class UCompareBeta
{
	@In(name="motif samples")
	public int motifSamples;
	
	@In(name="motif min size", description="minimum motif size (inclusive)")
	public int motifMinSize;
	
	@In(name="motif max size", description="maximum motif size (inclusive!)")
	public int motifMaxSize;
	
	@In(name="beta ceiling", description="An indicator for the number of iterations to use for the beta model. The number of iteration is the size of the graph divided by the ceiling.")
	public int betaCeiling;
	
	@In(name="beta alpha", description="The alpha value to use in the lower bound of the beta model.")
	public int betaAlpha;
	
	@In(name="data")
	public Graph<String> dataIn;
	public UGraph<String> data;
	
	private NaturalComparator<String> comparator;
 	
	@Main(print=false)
	public void main() throws IOException
	{		
		data = Graphs.toSimpleUGraph(data);
		data = Graphs.blank(data, "");

		Global.log().info("Computing beta model code length");
		int its = (int) (betaCeiling / (double) data.size()); 
		Global.log().info("-- using " +its+ " iterations");
		USequenceModel<String> model = new USequenceModel<String>(data);
		
		betaCodeLengthEstimate = model.logNumGraphs();
		betaCodeLengthLowerbound = model.confidence(betaAlpha, CIMethod.BCA, CIType.LOWER_BOUND).first();
	
		Global.log().info("Computing motif code lengths");
		
		UPlainMotifExtractor<String> extractor 
			= new UPlainMotifExtractor<String>(data, motifSamples, motifMinSize, motifMaxSize);
		
		// ...
		
	}
	
	@Out(name="motif codelengths")
	public double motifCodeLength;
	
	@Out(name="degree codelength estimate)")
	public double betaCodeLengthEstimate;
	
	@Out(name="degree codelength lower")
	public double betaCodeLengthLowerbound;

}
