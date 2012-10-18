package org.lilian.experiment;

import static org.lilian.util.Series.series;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.List;

import org.lilian.data.real.Datasets;
import org.lilian.data.real.MVN;
import org.lilian.data.real.PCAEco;
import org.lilian.data.real.Point;
import org.lilian.util.Pair;
import org.lilian.util.Series;

public class Eigenfaces extends AbstractExperiment
{
	private File directory;
	private List<Point> data;
	private int width, height;
	private int dim, size;
	private boolean gray;
	private int cutoff;
	
	public PCAEco pca;
	public double error;
	public Point p, simp, rec;
	public MVN simpMVN;
	private List<Point> simplified;

	
	public Eigenfaces(
			@Parameter(name="directory") File directory,
			@Parameter(name="gray") boolean gray,
			@Parameter(name="cutoff") int cutoff) 
		throws IOException
	{
		this.directory = directory;
		this.gray = gray;
		
		data = Datasets.readImages(directory, gray);
		Pair<Integer, Integer> dims = Datasets.size(directory);
		
		width = dims.first();
		height = dims.second();
		
		this.cutoff = cutoff == -1 ? data.size() : cutoff;
	}

	@Override
	protected void setup()
	{
		dim = data.get(0).dimensionality();
		size = data.size();
	}

	@Override
	protected void body()
	{
		pca = new PCAEco(data);
		p = data.get(0);
		simp = pca.simplify(p, cutoff);
		rec = pca.mapBack(simp);
		
		error = 0;
		for(int i : series(dim))
		{
			double e = p.get(i) - rec.get(i); 
			error += e * e;
		}
		
		simplified = pca.simplify(cutoff);
		simpMVN = MVN.find(simplified);
	}
	
	@Result(name="error")
	protected double error()
	{
		return error;
	}
	
	@Result(name="simplified")
	protected List<Point> simplified()
	{
		return simplified;
	}
	
	@Result(name="original")
	protected BufferedImage original()
	{
		BufferedImage im =  Datasets.toImage(p, width, height, gray);
		return im;
	}
	
	@Result(name="original (values)")
	protected List<Double> originalValues()
	{
		return p.subList(0, 100);
	}
	
	@Result(name="reconstructed (values)")
	protected List<Double> reconstructedValues()
	{
		return rec.subList(0, 100);
	}

	@Result(name="reconstructed")
	protected BufferedImage rec()
	{
		return Datasets.toImage(rec, width, height, gray);
	}
	
	@Result(name="eigenvector 0")
	protected BufferedImage ev0()
	{
		return Datasets.toImage(pca.eigenVector(0), width, height, gray);
	}		
	
	@Result(name="eigenvector 1")
	protected BufferedImage ev1()
	{
		return Datasets.toImage(pca.eigenVector(1), width, height, gray);
	}		
	
	@Result(name="eigenvector 2")
	protected BufferedImage ev2()
	{
		return Datasets.toImage(pca.eigenVector(2), width, height, gray);
	}		

	@Result(name="random 1")
	protected BufferedImage random1()
	{
		Point r = simpMVN.generate();
		logger.info("r: " + r);
		return Datasets.toImage(pca.mapBack(r), width, height, gray);
	}
	
	@Result(name="random 2")
	protected BufferedImage random2()
	{
		Point r = simpMVN.generate();
		logger.info("r: " + r);
		return Datasets.toImage(pca.mapBack(r), width, height, gray);
	}
	
	@Result(name="random 3")
	protected BufferedImage random3()
	{		
		Point r = simpMVN.generate();
		logger.info("r: " + r);
		return Datasets.toImage(pca.mapBack(r), width, height, gray);
	}
	
	
	@Result(name="eigenvalues")
	protected List<Double> eigenvalues()
	{
		return pca.eigenValues();
	}
}
