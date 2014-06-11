package org.lilian.experiment.vms;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

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
import org.lilian.pos.TagSet;
import org.lilian.pos.TaggedWord;
import org.lilian.pos.brown.BrownCorpus;
import org.lilian.pos.brown.BrownToSimple;
import org.lilian.pos.penn.PennTagSet;
import org.lilian.pos.penn.PennToSimple;
import org.lilian.util.Series;

@Module(name="tagging")
public class SupervisedTagging
{
	public File corpus1;
	public File corpus2;

	public SupervisedTagging(
			@In(name="corpus1")
			String corpus1, 
			@In(name="corpus2")
			String corpus2)
	{
		this.corpus1 = new File(corpus1);
		this.corpus2 = new File(corpus2);
		
		System.out.println("++++++++++++" + corpus1);
	}

	@Main()
	public void body()
	{
		
		TagSet penn = new PennTagSet();
		
		SequenceCorpus<TaggedWord> c1 = new BasicTaggedCorpus(penn, corpus1);
		c1 = MappingWrapper.wrap(c1, new PennToSimple());
		
		SequenceIterator<TaggedWord> it1 = c1.iterator();
		
		while(it1.hasNext())
		{
			if(it1.atSequenceEnd())
				System.out.println();
			
			TaggedWord word = it1.next();
			System.out.print(word.word() + "/" + word.tag() + " ");	
		}

	}
}
