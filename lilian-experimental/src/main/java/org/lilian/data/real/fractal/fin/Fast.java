//package org.lilian.data.real.fractal.fin;
//
//import static java.lang.Math.ceil;
//import static java.lang.Math.max;
//import static java.lang.Math.min;
//import static org.lilian.util.Functions.tic;
//import static org.lilian.util.Functions.toc;
//
//import java.awt.image.BufferedImage;
//import java.awt.image.RenderedImage;
//import java.io.File;
//import java.io.IOException;
//import java.util.ArrayList;
//import java.util.List;
//
//import javax.imageio.ImageIO;
//
//import org.data2semantics.platform.Global;
//import org.data2semantics.platform.annotation.In;
//import org.data2semantics.platform.annotation.Main;
//import org.data2semantics.platform.annotation.Module;
//import org.data2semantics.platform.annotation.Out;
//import org.data2semantics.platform.util.Series;
//import org.lilian.data.real.AffineMap;
//import org.lilian.data.real.Datasets;
//import org.lilian.data.real.Draw;
//import org.lilian.data.real.MVN;
//import org.lilian.data.real.Map;
//import org.lilian.data.real.MappedList;
//import org.lilian.data.real.Maps;
//import org.lilian.data.real.Point;
//import org.lilian.data.real.Similitude;
//import org.lilian.data.real.fractal.old.EMOld;
//import org.lilian.data.real.fractal.old.IFS;
//import org.lilian.data.real.fractal.old.IFSs;
//import org.lilian.data.real.fractal.old.SimEM;
//import org.lilian.experiment.Parameter;
//import org.lilian.experiment.Reportable;
//import org.lilian.search.Parametrizable;
//import org.lilian.util.Functions;
//
//@Module(name="Visual")
//public class Fast
//{
//	private static final boolean CENTER_UNIFORM = true;
//
//	private static final double RADIUS = 0.7;
//	private static final double SCALE = 0.1;
//	private static final int NUM_SOURCES = 1;
//	
//	@In(name="data", print=false)
//	public List<Point> data;	
//	
//	@In(name="generations")
//	public int generations;
//	
//	@In(name="num components", description="The number of components in the IFS model")
//	public int numComponents;
//
//	@In(name="depth")
//	public double depth;
//	
//	@In(name="sample size")
//	public int sampleSize;	
//	
//	@In(name="spanning variance")
//	public double spanningVariance;
//	
//	@In(name="high quality")
//	public boolean highQuality;
//	
//	@Main()
//	public void main() throws IOException
//	{
//		AffineMap map = CENTER_UNIFORM ? 
//			Maps.centerUniform(data) :
//			Maps.centered(data) ;
//			
//		data = new MappedList(data, map);
//
//		BufferedImage image= Draw.draw(data, 1000, true);
//		// imagesDeep.add(image);
//		ImageIO.write(image, "PNG", new File(Global.getWorkingDir(), "data.png"));
//		
//		Global.log().info("Data size: " + data.size());
//		
//		int dim = data.get(0).dimensionality();
//		
//		// * We use the "sphere" initialization strategy
//		IFS<Similitude> model = null;
//		model = IFSs.initialSphere(dim, numComponents, RADIUS, SCALE, true);
//			
//		EMOld<Similitude> em = new SimEM(model, data, NUM_SOURCES, 
//					Similitude.similitudeBuilder(dim), spanningVariance);
//								
//		// * BODY
//		tic();
//		for(int generation : Series.series(generations))
//		{			
//			model = em.model();
//
//			if(dim == 2)
//				write(em.model(), Global.getWorkingDir(), String.format("generation%04d", generation), depth, em.basis());
//			
//			tic();
//			em.iterate(sampleSize, depth);
//			Global.log().info(generation + ") finished ("+toc() +" seconds, total samples: "+sampleSize+")");
//
//		}
//	}
//	
//	private <M extends Map & Parametrizable> void write(IFS<M> ifs, File dir, String name, double currentDepth, MVN basis) throws IOException
//	{		
//		int div = highQuality ? 1 : 4;
//		int its = highQuality ? (int) 10000000 : 10000;
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
