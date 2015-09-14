package org.lilian.motifs;

import static org.junit.Assert.*;

import java.io.File;
import java.io.IOException;

import org.junit.Test;
import org.lilian.motifs.Compare.NullModel;
import org.nodes.DGraph;
import org.nodes.data.Data;
import org.nodes.motifs.DPlainMotifExtractor;

public class CompareLargeTest
{

	@Test
	public void test()
		throws IOException
	{
		DGraph<String> data = 
				Data.edgeListDirectedUnlabeled(
						new File("/Users/Peter/Documents/datasets/graphs/physicians/physicians.txt"),
						true);
		
		DPlainMotifExtractor<String> ex = new DPlainMotifExtractor<String>(data, 1000, 2, 7, 1);
		
		System.out.println("EL");
		for(DGraph<String> sub : ex.subgraphs())
		{
			System.out.println(sub);
			double sizeSlow = Compare.size(data, sub, ex.occurrences(sub), NullModel.EDGELIST, true);
			double sizeFast = CompareLarge.size(data, sub, ex.occurrences(sub), NullModel.EDGELIST, true);
			
			assertEquals(sizeSlow, sizeFast, 0.000001);
		}
		
		System.out.println("ER");
		for(DGraph<String> sub : ex.subgraphs())
		{
			System.out.println(sub);
			double sizeSlow = Compare.size(data, sub, ex.occurrences(sub), NullModel.ER, true);
			double sizeFast = CompareLarge.size(data, sub, ex.occurrences(sub), NullModel.ER, true);
			
			System.out.println("--- " + sizeSlow + " " + sizeFast);
			assertEquals(sizeSlow, sizeFast, 0.000001);
		}
		
	}

}
