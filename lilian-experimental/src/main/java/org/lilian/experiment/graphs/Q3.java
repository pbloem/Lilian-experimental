package org.lilian.experiment.graphs;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.lilian.experiment.AbstractExperiment;
import org.lilian.experiment.Resources;
import org.lilian.graphs.Graph;
import org.lilian.graphs.data.Data;

public class Q3 extends AbstractExperiment
{

	private static String base = "/Users/Peter/Documents/datasets/graphs/";
	private List<Graph<?>> datasets = new ArrayList<Graph<?>>();
	
	
	
	@Override
	protected void setup()
	{
		try
		{
			Graph<?> internet = Resources.gmlGraph(new File(base + "internet-newman/as.gml"));
			logger.info("Loaded internet");
			
			Graph<?> web = Data.edgeListDirectedUnlabeled(new File(base + "www-barabasi/www.dat"));
			logger.info("Loaded web (n="+web.size()+", l="+web.numLinks()+")");
			
			
			Graph<?> internetBig = Data.edgeListDirectedUnlabeled(new File(base + "internet/as-skitter.txt"));
			logger.info("Loaded internet big (n="+internetBig.size()+", l="+internetBig.numLinks()+")");
			
			
			datasets.add(internet);
			datasets.add(web);
			datasets.add(internetBig);
			
			
		} catch (IOException e)
		{
			throw new RuntimeException(e);
		}

	}



	@Override
	protected void body()
	{
		
		
		
	}

}
