//package org.lilian.data.real.fractal.fin;
//
//import static java.lang.Math.ceil;
//import static java.lang.Math.max;
//import static java.lang.Math.min;
//import static org.data2semantics.platform.util.Series.series;
//import static org.data2semantics.platform.util.Statistics.mean;
//import static org.data2semantics.platform.util.Statistics.toArray;
//import static org.lilian.util.Functions.log2;
//import static org.lilian.util.Functions.tic;
//import static org.lilian.util.Functions.toc;
//
//import java.awt.image.BufferedImage;
//import java.awt.image.RenderedImage;
//import java.io.File;
//import java.io.IOException;
//import java.util.ArrayList;
//import java.util.List;
//import java.util.Random;
//
//import javax.imageio.ImageIO;
//
//import org.apache.commons.math3.stat.inference.TTest;
//import org.data2semantics.platform.Global;
//import org.data2semantics.platform.annotation.In;
//import org.data2semantics.platform.annotation.Main;
//import org.data2semantics.platform.annotation.Module;
//import org.data2semantics.platform.annotation.Out;
//import org.data2semantics.platform.util.Series;
//import org.data2semantics.platform.util.Statistics;
//import org.lilian.data.real.AffineMap;
//import org.lilian.data.real.Datasets;
//import org.lilian.data.real.Draw;
//import org.lilian.data.real.MVN;
//import org.lilian.data.real.Map;
//import org.lilian.data.real.MappedList;
//import org.lilian.data.real.Maps;
//import org.lilian.data.real.Point;
//import org.lilian.data.real.Similitude;
//import org.lilian.data.real.fractal.AffineEM;
//import org.lilian.data.real.fractal.EMOld;
//import org.lilian.data.real.fractal.EMOne;
//import org.lilian.data.real.fractal.EMSimple;
//import org.lilian.data.real.fractal.IFS;
//import org.lilian.data.real.fractal.IFSs;
//import org.lilian.data.real.fractal.MCEM;
//import org.lilian.data.real.fractal.SimEM;
//import org.lilian.experiment.Parameter;
//import org.lilian.experiment.Reportable;
//import org.lilian.search.Parametrizable;
//import org.lilian.util.Functions;
//
//@Module(name="Monte Carlo")
//public class MonteCarlo
//{
//	private static final double RADIUS = 0.7;
//	private static final double SCALE = 0.5;
//	
//	@In(name="model")
//	public String modelName;
//	
//	@In(name="generations")
//	public int generations;
//	
//	public int numComponents;
//
//	@In(name="depth")
//	public double depth;
//	
//	@In(name="sample size")
//	public int sampleSize;	
//	
//	@In(name="branching")
//	public int branching;	
//	
//	@In(name="high quality")
//	public boolean highQuality;
//	
//	public IFS<Similitude> target;
//	
//	@Main()
//	public void main() throws IOException
//	{
//		long seed = new Random().nextLong();
//		System.out.println("seed " + seed);
//		
//		org.lilian.Global.random = new Random(seed);
//				
//		if(modelName.equals("sierpinski"))
//		{
//			target = IFSs.sierpinskiSim();
//		} else if(modelName.equals("sierpinski-off"))
//		{
//			target = IFSs.sierpinskiOffSim(1.0, 4.0, 16.0);
//		} else if(modelName.equals("sierpinski-small"))
//		{
//			target = IFSs.sierpinskiSmall();
//		} else if(modelName.equals("koch-two"))
//		{
//			target = IFSs.koch2Sim();
//		} else if(modelName.equals("koch-two-off"))
//		{
//			target = IFSs.koch2SimOff(1.0, 16.0);
//		} else if(modelName.equals("koch-four"))
//		{
//			target = IFSs.koch4Sim();
//		} else if(modelName.equals("koch-four-off"))
//		{
//			target = IFSs.koch4SimOff(1.0, 2.0, 4.0, 8.0);
//		} 
//		else 
//			throw new IllegalArgumentException("Model name ("+modelName+") not recognized");
//		
//		numComponents = target.size();
//		
//		List<Point> data = target.generator().generate(100000);
////		
//////		AffineMap map = CENTER_UNIFORM ? 
//////			Maps.centerUniform(data) :
//////			Maps.centered(data) ;
////			
////		data = new MappedList(data, map);
//
//		BufferedImage image = Draw.draw(data, 1000, true);
//		// imagesDeep.add(image);
//		ImageIO.write(image, "PNG", new File(Global.getWorkingDir(), "data.png"));
//		
////		image = Draw.drawCodes(target, new double[]{-1.0, 1.0}, new double[]{-1.0, 1.0}, 100, 6, -1);
////		ImageIO.write(image, "PNG", new File(Global.getWorkingDir(), "codes.png"));
////
////		
//		Global.log().info("Data size: " + data.size());
//		
//		int dim = data.get(0).dimensionality();
//	
//		IFS<Similitude> initial = IFSs.initialSphere(dim, numComponents, RADIUS, SCALE, true);
//		
//		MCEM em = new MCEM(data, target.size(), sampleSize, (int)depth);
//		
//		// * BODY
//		tic();
//		for(int generation : Series.series(generations))
//		{			
//			if(dim == 2)
//				write(em.model(), Global.getWorkingDir(), String.format("generation%04d", generation), depth, em.basis());
//			
//			tic();
//			em.iterate(branching);
//			Global.log().info(generation + ") finished ("+toc() +" seconds, total samples: "+sampleSize+")");
//		}
//	}
//
//	
//	private <M extends Map & Parametrizable> void write(IFS<M> ifs, File dir, String name, double currentDepth, MVN basis) throws IOException
//	{		
//		if(ifs == null)
//			return;
//		
//		int div = highQuality ? 1 : 4;
//		int its = highQuality ? (int) 500000000 : 10000;
//		
//		File genDir = new File(dir, "generations");
//		genDir.mkdirs();
//		
//		BufferedImage image;
//		
//		image= Draw.draw(ifs, its, new double[]{-1.1, 1.1}, new double[]{-1.1, 1.1}, 1000/div, 1000/div, true, currentDepth, basis);
//		ImageIO.write(image, "PNG", new File(genDir, name+"png"));
//		
//		image= Draw.draw(ifs.generator(), its, 1000/div, true);
//		ImageIO.write(image, "PNG", new File(genDir, name+".deep.png"));
//	}
//}
