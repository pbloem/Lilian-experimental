package org.lilian.data.real;


import java.awt.image.BufferedImage;
import java.util.List;

import org.lilian.data.real.Draw;
import org.lilian.data.real.PCA;
import org.lilian.data.real.PCAEco;
import org.lilian.data.real.PCAIterative;
import org.lilian.data.real.Point;
import org.lilian.experiment.AbstractExperiment;
import org.lilian.experiment.Parameter;
import org.lilian.experiment.Result;


public class PCAExperiment extends AbstractExperiment
{
	private static final int RES = 500;
	private List<Point> data, iterative, eco, plain;

	public PCAExperiment(
			@Parameter(name="data") List<Point> data)
	{
		this.data = data;
	}


	@Override
	protected void body()
	{
		PCA plainPCA = new PCA(data);
		plain = plainPCA.simplify(2);
		
//		PCAEco ecoPCA = new PCAEco(data);
//		eco = ecoPCA.simplify(2);
		
		PCAIterative itPCA = new PCAIterative(data, 2, 100);
		iterative = itPCA.simplify(2);
	}
	
	@Result(name="data")
	public BufferedImage data()
	{
		return Draw.draw(data, RES, true, true);
	}
	
	@Result(name="plain")
	public BufferedImage plain()
	{
		return Draw.draw(plain, RES, true, true);
	}
	
//	@Result(name="eco")
//	public BufferedImage eco()
//	{
//		return Draw.draw(eco, RES, true, true);
//	}
	
	@Result(name="iterative")
	public BufferedImage iterative()
	{
		return Draw.draw(iterative, RES, true, true);
	}

}
