package org.lilian.conditional;

import static org.junit.Assert.*;

import java.util.Arrays;
import java.util.List;

import org.junit.Test;
import org.lilian.models.markov.MarkovModel;
import org.lilian.models.markov.TrieMarkovModel;
import org.lilian.util.BitString;
import org.lilian.util.Functions;

public class ConditionalCompression
{

	@Test
	public void test()
	{
		List<Boolean> data = BitString.random(1000000, 0.6);
		
		MarkovModel<Boolean> model = new TrieMarkovModel<Boolean>(6);
		for(Boolean bit : data)
			model.add(bit);
		
		System.out.println(model.model(6).probability(Arrays.asList(
				true, true, true, true, true, true)));
		System.out.println(
				Math.ceil(- Functions.log2(model.model(6).probability(Arrays.asList(
				true, true, true, true, true, true)))));
	}

}
