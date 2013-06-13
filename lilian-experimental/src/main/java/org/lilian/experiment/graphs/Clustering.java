package org.lilian.experiment.graphs;

import org.lilian.data.real.classification.Classified;
import org.lilian.data.real.clustering.SpectralClustering;
import org.lilian.experiment.AbstractExperiment;
import org.lilian.graphs.Graph;
import org.lilian.graphs.Node;
import org.lilian.graphs.clustering.Clusterer;
import org.lilian.graphs.clustering.GraphSpectral;

public class Clustering<N> extends AbstractExperiment
{
	private String type = "spectral";
	private int k; 
	private Graph<N> data;
	
	@Override
	protected void body()
	{
		
		Clusterer<N> clusterer = new GraphSpectral<N>(k);
		Classified<Node<N>> clusters = clusterer.cluster(data);
		
		
	}

}
