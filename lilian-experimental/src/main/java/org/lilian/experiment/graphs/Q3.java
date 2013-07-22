package org.lilian.experiment.graphs;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.lilian.experiment.AbstractExperiment;
import org.lilian.experiment.Environment;
import org.lilian.experiment.Experiment;
import org.lilian.experiment.Resources;
import org.lilian.graphs.DGraph;
import org.lilian.graphs.Graph;
import org.lilian.graphs.data.Data;
import org.lilian.graphs.data.RDF;
import org.lilian.graphs.random.RandomGraphs;

public class Q3 extends AbstractExperiment
{

	private static String base = "/Users/Peter/Documents/datasets/graphs/";
	
	@SuppressWarnings("unchecked")
	@Override
	protected void body()
	{
		System.out.println("...");
		try
		{
			/**
			 * Load datasets one at a time to let the GC clean up in between 
			 */
			
			Graph<?> data;
			
			int n = 10000;
	
//			
			data = null;
			data = RandomGraphs.preferentialAttachment(n, 2);
			int e = data.numLinks();
			logger.info("Loaded BA graph");
			go((Graph<Object>)data);		
			
			data = null;
			data = RandomGraphs.random(n, e);
			logger.info("Loaded ER graph");
			go((Graph<Object>)data);				
						
			data = null;
			data = Resources.gmlGraph(new File(base + "internet-newman/as.gml"));
			logger.info("Loaded internet");
			go((Graph<Object>)data);
			
			data = null;
			data = RDF.readTurtle(new File(base + "commit/commit-contacts.ttl"));
			logger.info("Loaded commit (n="+data.size()+", l="+data.numLinks()+")");
			go((Graph<Object>)data);

			data = null;
			data = RDF.read(new File(base + "aifb/aifb.owl"));
			logger.info("Loaded aifb (n="+data.size()+", l="+data.numLinks()+")");
			go((Graph<Object>)data);
		
			data = null;
			data = Data.edgeListDirectedUnlabeled(new File(base + "epinions/epinions.txt"), false);
			logger.info("Loaded epinions (n="+data.size()+", l="+data.numLinks()+")");
			go((Graph<Object>)data);				

			data = null;
			data = Data.edgeListDirectedUnlabeled(new File(base + "www-barabasi/www.dat"), true);
			logger.info("Loaded web (n="+data.size()+", l="+data.numLinks()+")");
			go((Graph<Object>)data);

//			data = null;
//			data = Data.edgeListDirectedUnlabeled(new File(base + "internet/as-skitter.txt"), false);
//			logger.info("Loaded internet big (n="+data.size()+", l="+data.numLinks()+")");
//			go((Graph<Object>)data);
			
//			data = null;
//			data = Data.edgeListDirectedUnlabeled(new File(base + "patents/cit-Patents.txt"), false);
//			logger.info("Loaded patent citations (n="+data.size()+", l="+data.numLinks()+")");
//			go((Graph<Object>)data);
			
//			data = null;
//			data = Data.edgeListDirectedUnlabeled(new File(base + "social-pokec/soc-pokec.txt"), false);
//			logger.info("Loaded pokec (n="+data.size()+", l="+data.numLinks()+")");
//			go((Graph<Object>)data);
			
		} catch (Exception e)
		{
			throw new RuntimeException(e);
		}

	}
	
	private void go(Graph<Object> data)
	{
		Experiment exp = new GraphMeasures<Object>(data, "huge");
		Environment.current().child(exp);
	}
}
