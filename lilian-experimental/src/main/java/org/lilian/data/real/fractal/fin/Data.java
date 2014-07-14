package org.lilian.data.real.fractal.fin;

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.data2semantics.platform.annotation.In;
import org.data2semantics.platform.annotation.Main;
import org.data2semantics.platform.annotation.Module;
import org.lilian.data.real.Datasets;
import org.lilian.data.real.Generators;
import org.lilian.data.real.Histogram2D;
import org.lilian.data.real.Point;
import org.lilian.data.real.Similitude;
import org.lilian.data.real.fractal.IFS;
import org.lilian.data.real.fractal.IFSs;

public class Data
{
	@Module(name="sierpinski")
	public static class Sierpinski
	{
		@In(name="p0") public double p0;
		@In(name="p1") public double p1;
		@In(name="p2") public double p2;
		
		@In(name="depth", description="the depth to which to evaluate te data. If negative, the chaos game is used (which is a simulation of infinite depth)") 
		public double depth;
		
		@In(name="size")
		public int size;
		
		@Main(print=false)
		public List<Point> body()
		{
			IFS<Similitude> model = IFSs.sierpinskiOffSim(p0, p1, p2);
			
			if(depth < 0.0)
				return model.generator().generate(size);
			return model.generator(depth).generate(size);
		}
	}
	
	@Module(name="koch2")
	public static class Koch2
	{
		@In(name="p0") public double p0;
		@In(name="p1") public double p1;
		
		@In(name="depth", description="the depth to which to evaluate te data. If negative, the chaos game is used (which is a simulation of infinite depth)") 
		public double depth;
		
		@In(name="size")
		public int size;
		
		@Main(print=false)
		public List<Point> body()
		{
			IFS<Similitude> model = IFSs.koch2SimOff(p0, p1);
			
			if(depth < 0.0)
				return model.generator().generate(size);
			return model.generator(depth).generate(size);
		}
	}
	
	@Module(name="koch4")
	public static class Koch4
	{
		@In(name="p0") public double p0;
		@In(name="p1") public double p1;
		@In(name="p2") public double p2;
		@In(name="p3") public double p3;		
		
		@In(name="depth", description="the depth to which to evaluate te data. If negative, the chaos game is used (which is a simulation of infinite depth)") 
		public double depth;
		
		@In(name="size")
		public int size;
		
		@Main(print=false)
		public List<Point> body()
		{
			IFS<Similitude> model = IFSs.koch4SimOff(p0, p1, p2, p3);
			
			if(depth < 0.0)
				return model.generator().generate(size);
			return model.generator(depth).generate(size);
		}
	}
	
	@Module(name="image")
	public static class FromImage
	{
		@In(name="file") public String file;
		@In(name="size") public int size;
		
		@Main(print=false)
		public List<Point> body() throws IOException
		{
			Histogram2D hist = Histogram2D.fromImage(new File(file));
			return hist.generate(size);
		}
	}
	
	@Module(name="sphere")
	public static class Sphere
	{
		@In(name="dim") public int dim;
		@In(name="size") public int size;
		
		@Main(print=false)
		public List<Point> body()
		{
			return Datasets.sphere(dim).generate(size);
		}
	}
	
	@Module(name="ball")
	public static class Ball
	{
		@In(name="dim") public int dim;
		@In(name="size") public int size;
		
		@Main(print=false)
		public List<Point> body()
		{
			return Datasets.ball(dim).generate(size);
		}
	}
}
