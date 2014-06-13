package org.lilian.experiment.vms;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.data2semantics.platform.annotation.In;
import org.data2semantics.platform.annotation.Main;
import org.data2semantics.platform.annotation.Module;
import org.lilian.adios.Adios;
import org.lilian.corpora.SequenceCorpus;
import org.lilian.corpora.SequenceIterator;
import org.lilian.corpora.tagged.mapping.MappingWrapper;
import org.lilian.corpora.wrappers.Characters;
import org.lilian.level.LevelStatisticsModel;
import org.lilian.models.BasicFrequencyModel;
import org.lilian.pos.BasicTaggedCorpus;
import org.lilian.pos.SimpleTagSet;
import org.lilian.pos.Tag;
import org.lilian.pos.TagSet;
import org.lilian.pos.TaggedWord;
import org.lilian.pos.brown.BrownCorpus;
import org.lilian.pos.brown.BrownToSimple;
import org.lilian.pos.penn.PennTagSet;
import org.lilian.pos.penn.PennToSimple;
import org.lilian.util.Functions;
import org.lilian.util.Series;
import org.lilian.models.FrequencyModel;

import au.com.bytecode.opencsv.CSVWriter;

@Module(name="tagging")
public class SupervisedTagging
{
	public File corpus1;
	public String name;

	public SupervisedTagging(
			@In(name="corpus1")
			String corpus1, 
			@In(name="name")
			String name)
	{
		this.corpus1 = new File(corpus1);
		this.name = name;
	}

	@Main()
	public void body() throws IOException
	{
		
		TagSet penn = new PennTagSet();
		
		SequenceCorpus<TaggedWord> c1 = new BasicTaggedCorpus(penn, corpus1);
		c1 = MappingWrapper.wrap(c1, new PennToSimple());
		
		SequenceIterator<TaggedWord> it1 = c1.iterator();
		
		while(it1.hasNext())
		{	
			TaggedWord word = it1.next();
		}
		
		Map<String, BasicFrequencyModel<Tag>> tagProb = new LinkedHashMap<String, BasicFrequencyModel<Tag>>();
		BasicFrequencyModel<String> frequencies = new BasicFrequencyModel<String>();
		LevelStatisticsModel<String> levels = new LevelStatisticsModel<String>();
		
		it1 = c1.iterator();
		
		while(it1.hasNext())
		{
			TaggedWord word = it1.next();
			
			if(! tagProb.containsKey(word.word()))
				tagProb.put(word.word(), new BasicFrequencyModel<Tag>());
			tagProb.get(word.word()).add(word.tag());

			frequencies.add(word.word());
			
			levels.add(word.word());
		}
		
		List<Tag> tags = new ArrayList<Tag>(new SimpleTagSet());
		
		File out = new File(name + ".features.csv");
		CSVWriter writer = new CSVWriter(new BufferedWriter(new FileWriter(out)), ',', ' ');
		
		for(String word : frequencies.sorted())
		{
			if(frequencies.frequency(word) < 4)
				break;
			
			for(Tag tag : tagProb.get(word).tokens())
			{
				double prob = tagProb.get(word).probability(tag);
				double logProb = - Functions.log2(frequencies.probability(word));
				
				double rel = levels.relevance(word);
				double relSigma = levels.relevanceSigma(word);
				double c = levels.cValue(word, 0.01);


				int tagIndex = tags.indexOf(tag);
				
				// if(tagProb.get(word).maxToken().equals(tag))
				writer.writeNext(new String[]{tagIndex+"", prob+"", logProb+"", rel+"", relSigma+"", c+""});
			}
		}
		
		writer.close();
	}
}
