package org.lilian.experiment.graphs.compression;

import org.lilian.data.real.classification.Classified;
import org.lilian.experiment.AbstractExperiment;
import org.lilian.experiment.Result;
import org.lilian.experiment.State;
import org.lilian.graphs.UTGraph;

public class CompressionDistance extends AbstractExperiment
{
	public enum Compressor {ZIP, SUBDUE};
	
	private Classified<UTGraph<String, String>> data;

	@State
	public double error;
	
	@Override
	protected void body()
	{
		// * Calculate distances
		
		// * Perform Clustering
		
		// * Optimize mapping 
		
	}
	
	@Result(name = "Error")
	public double error()
	{
		return error;
	}

}
