package org.lilian;


import java.io.File;
import java.io.IOException;

import org.nodes.DGraph;


public class Quick
{

	public static void main(String[] args) 
		throws IOException
	{
	
		DGraph<String> data = org.nodes.data.Data.edgeListDirectedUnlabeled(
				new File("/Users/Peter/Documents/datasets/graphs/email/email.eu.txt"), 
				true);
		
		System.out.println(data.size());
		System.out.println(data.numLinks());

		
	}

}
