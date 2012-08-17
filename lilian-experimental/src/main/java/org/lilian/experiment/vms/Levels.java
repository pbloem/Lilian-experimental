package org.lilian.experiment.vms;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.lilian.adios.Adios;
import org.lilian.corpora.AdiosCorpus;
import org.lilian.corpora.SequenceCorpus;
import org.lilian.corpora.WesternCorpus;
import org.lilian.corpora.wrappers.Characters;
import org.lilian.experiment.AbstractExperiment;
import org.lilian.experiment.Parameter;
import org.lilian.experiment.Result;
import org.lilian.experiment.State;
import org.lilian.level.LevelStatisticsModel;
import org.lilian.models.BasicFrequencyModel;
import org.lilian.util.Series;

public class Levels extends AbstractExperiment 
{
	private File data;
	private boolean adios = false;
	private int minFreq;
	private double minC;

	public @State LevelStatisticsModel<String> model;
	public @State List<String> tokens;
	
	public Levels(
			@Parameter(name="data") File data,
			@Parameter(name="min freq") int minFreq)
	{
		this.minFreq = minFreq;
		this.minC = minC;
		this.data = data;
	}
	
	public Levels(
			@Parameter(name="data") File data,
			@Parameter(name="adios") boolean adios,
			@Parameter(name="min freq") int minFreq)
	{
		this.data = data;
		this.adios = adios;
		this.minFreq = minFreq;
		this.minC = minC;
		
	}	
	
	@Override
	protected void setup() 
	{
	}

	@Override
	protected void body() 
	{
		SequenceCorpus<String> gold; 
		if(adios)
			gold = new AdiosCorpus(data);
		else
			gold = new WesternCorpus(data, false, true);

		model = new LevelStatisticsModel<String>(gold);
		
		tokens = model.tokens(minFreq);
		Collections.sort(tokens, 
				Collections.reverseOrder(model.new RelevanceComparator()));
	}
	
	@Result(name="Most relevant")
	public List<List<Object>> mostRelevant()
	{
		int n = 40;
		
		 List<List<Object>> most = new ArrayList<List<Object>>();
		 
		 for(int i : Series.series(n))
			 most.add(Arrays.asList(
					 (Object)tokens.get(i),
					 (Object)model.relevance(tokens.get(i))));
		 
		 return most;
	}

}
