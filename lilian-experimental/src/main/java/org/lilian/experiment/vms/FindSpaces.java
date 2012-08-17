package org.lilian.experiment.vms;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Vector;

import org.lilian.adios.Adios;
import org.lilian.adios.MexFunctions;
import org.lilian.adios.MexGraph;
import org.lilian.corpora.AdiosCorpus;
import org.lilian.corpora.SequenceCorpus;
import org.lilian.corpora.SequenceIterator;
import org.lilian.corpora.WesternCorpus;
import org.lilian.corpora.wrappers.Characters;
import org.lilian.corpora.wrappers.TokenMask;
import org.lilian.experiment.AbstractExperiment;
import org.lilian.experiment.Parameter;
import org.lilian.experiment.Result;
import org.lilian.experiment.State;
import org.lilian.experiment.Tools;
import org.lilian.models.BasicFrequencyModel;
import org.lilian.util.Series;

public class FindSpaces extends AbstractExperiment 
{
	private boolean adios = false;
	
	private File data;
	private List<Double> alphas;
	private double mu;
	private boolean context;
	
	private BasicFrequencyModel<String> original, reconstructed;
	
	public @State List<Double> errors = new ArrayList<Double>();
	
	public FindSpaces(
			@Parameter(name="data") File data,
			@Parameter(name="alphas") List<Double> alphas,
			@Parameter(name="mu") double mu,
			@Parameter(name="context", description="Whether to run the algorithm in context sensistive mode")
				boolean context)
	{
		this.data = data;
		this.alphas = alphas;
		this.mu = mu;
		this.context = context;
	}
	
	public FindSpaces(
			@Parameter(name="data") File data,
			@Parameter(name="alphas") List<Double> alphas,
			@Parameter(name="mu") double mu,
			@Parameter(name="context", description="Whether to run the algorithm in context sensistive mode")
				boolean context,
			@Parameter(name="adios")
				boolean adios)
	{
		this.data = data;
		this.alphas = alphas;
		this.mu = mu;
		this.context = context;
		this.adios = adios;
	}	
	
	@Override
	protected void setup() 
	{

	}

	@Override
	protected void body() 
	{
		try 
		{
			SequenceCorpus<String> gold; 
			if(adios)
				gold = new AdiosCorpus(data);
			else
				gold = new WesternCorpus(data, false, true);
			
			original = new BasicFrequencyModel<String>(gold);
			
			SequenceCorpus<String> corpus = 
					Characters.wrap(gold);
			
			Adios<String> graph = new Adios<String>(corpus);
			
			int n, denom = -1;
			graph.writeResults(dir, "result.segmentation");
	
			// * Count the total number of characters in the gold corpus
			denom = 0;
			for(String current : gold)
				denom += current.length();
	
			// * Initial pass
			n = MexFunctions.checkWrongSpaces(graph.stringCorpus(), gold);
			System.out.println("  number of incorrect spaces:" + n);
			System.out.println("  proportion:" + (double)n / (double)denom);
			
			errors.add(new Double( ((double)n)/denom));
	
			
			logger.info("  Training Model");
			// * Pass for each alpha
			for(double alpha : alphas)
			{
				while(graph.patternDistillation(mu, alpha, context))
					System.out.println(".");
				System.out.println();			
		
				System.out.println("  Finished. Writing Results.");
				graph.writeResults(dir, "result.segmentation." + alpha);
		
				System.out.println("  Finished. Checking against Gold Corpus.");
		
				if(gold != null)
				{
					n = MexFunctions.checkWrongSpaces(graph.stringCorpus(), gold);
					System.out.println("  number of incorrect spaces:" + n);
					System.out.println("  proportion:" + n / (double)denom);				
					errors.add( n / (double)denom );
				}	
			}
			
			reconstructed = new BasicFrequencyModel<String>(graph.stringCorpus());
			
		} catch(IOException e)
		{
			throw new RuntimeException(e);
		}
	}
	
	@Result(name = "error")
	public List<Double> error()
	{
		return errors;
	}

	@Result(name = "best")
	public double best()
	{
		return Tools.min(errors);
	}
	
	@Result(name = "tokens original")
	public List<List<Object>> tokensOriginal()
	{
		return top(original);
	}		

	@Result(name = "tokens reconstructed")
	public List<List<Object>> tokensReconstructed()
	{
		return top(reconstructed);
	}			
	
	public List<List<Object>> top(BasicFrequencyModel<String> model)
	{
		int n = 40;
		List<List<Object>> result = new ArrayList<List<Object>>(n);
		List<String> sorted = original.sorted();
		for(int i : Series.series(n))
			result.add(Arrays.asList((Object)sorted.get(i), (Object)model.frequency(sorted.get(i))));
		
		return result;
	}	
}
