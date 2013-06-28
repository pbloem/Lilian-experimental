package org.lilian.experiment.graphs.draw;

import static org.lilian.util.Series.series;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileOutputStream;

import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;


import org.lilian.Global;
import org.lilian.experiment.AbstractExperiment;
import org.lilian.experiment.Factory;
import org.lilian.experiment.Parameter;
import org.lilian.experiment.Result;
import org.lilian.graphs.Graph;
import org.lilian.graphs.Graphs;
import org.lilian.graphs.MapUTGraph;
import org.lilian.graphs.Node;
import org.lilian.graphs.UTGraph;
import org.lilian.graphs.draw.CircleLayout;
import org.lilian.graphs.draw.SpectralLayout;
import org.lilian.util.Series;
import org.w3c.dom.Document;

public class Draw<L> extends AbstractExperiment
{
	private static final int WIDTH = 800;

	private static final int HEIGHT = 560;

	private Graph<L> data;
	
	private BufferedImage circleLayout;
	
	private BufferedImage spectralLayout = null;
	
//	@Factory
//	public static <L> Draw<L> draw(Graph<L> data)
//	{
//		return new Draw<L>(data);	
//	}
	
	public Draw(@Parameter(name="data") Graph<L> data)
	{
		this.data = data;
	}
	
	@Override
	protected void body()
	{
		data = toUndirected(data);
		
		circleLayout = org.lilian.graphs.draw.Draw.draw(data, new CircleLayout<L>(data), WIDTH, HEIGHT);
		
		spectralLayout = org.lilian.graphs.draw.Draw.draw(data, new SpectralLayout<L>(data), WIDTH, HEIGHT);

	}
	
	@Result(name="Circle layout")
	public BufferedImage circleLayout()
	{
		return circleLayout;
	}
	
	@Result(name="Spectral layout")
	public BufferedImage spectralLayout()
	{
		return spectralLayout;
	}
	
	@Factory
	public static Draw<String> draw(@Parameter(name="test") String test)
	{
		int n = 10;
		
		UTGraph<String, String> c = Graphs.k(n, "x");
		Graphs.add(c, Graphs.k(n, "x"));
		
		for(int i : series(1))
		{
			int j = Global.random.nextInt(2 * n);
			int k = Global.random.nextInt(2 * n);
			
			c.nodes().get(j).connect(c.nodes().get(k));
		}
		
		return new Draw<String>(c);
	}
	
	private void write(Document svg, File file)
	{		
		TransformerFactory transformerFactory = TransformerFactory.newInstance();
		Transformer transformer;
		try
		{
			transformer = transformerFactory.newTransformer();

			DOMSource source = new DOMSource(svg);
			StreamResult result = new StreamResult(new FileOutputStream(file));
	
			transformer.transform(source, result);
			
		} catch (Exception e)
		{
			throw new RuntimeException(e);
		}			
	}
	
	/**
	 * Creates a copy of the graph which removes directions and tags 
	 * @param graph
	 * @return
	 */
	private UTGraph<L, Object> toUndirected(Graph<L> graph)
	{
		if(graph instanceof UTGraph<?, ?>)
			return (UTGraph<L, Object>)graph;
		
		UTGraph<L, Object> copy = new MapUTGraph<L, Object>();
		
		for(Node<L> node : graph.nodes())
			copy.add(node.label());
		
		for(int i : Series.series(graph.size()))
			for(int j : Series.series(i, graph.size()))
				if(graph.nodes().get(i).connected(graph.nodes().get(j)) 
						|| graph.nodes().get(j).connected(graph.nodes().get(i)))
					copy.nodes().get(i).connect(copy.nodes().get(j));
		
		return copy;
	}
}
