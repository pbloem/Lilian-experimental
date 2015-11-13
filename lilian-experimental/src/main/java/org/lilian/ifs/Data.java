package org.lilian.ifs;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import org.data2semantics.platform.annotation.In;
import org.data2semantics.platform.annotation.Main;
import org.data2semantics.platform.annotation.Module;
import org.lilian.data.real.Datasets;
import org.lilian.data.real.Generators;
import org.lilian.data.real.Histogram2D;
import org.lilian.data.real.MOG;
import org.lilian.data.real.Point;
import org.lilian.data.real.Similitude;
import org.lilian.data.real.classification.Classification;
import org.lilian.data.real.classification.Classified;
import org.lilian.data.real.fractal.old.IFS;
import org.lilian.data.real.fractal.old.IFSs;
import org.lilian.experiment.Name;
import org.lilian.experiment.Resource;
import org.lilian.search.Builder;

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
	
	@Module(name="csv")
	public static class CSV
	{
		@In(name="file") public String file;
		
		@Main(print=false)
		public List<Point> read()
			throws IOException
		{
			return Datasets.readCSV(new File(file));
		}
	}
	
	@Module(name="class points")
	public static class ClassPoints
	{
		@In(name="file") public String file;
		@In(name="class") public int cls;
		
		@Main(print=false)
		public List<Point> read()
			throws IOException
		{
			Classified<Point> data = Classification.readCSV(new File(file));
			return data.points(cls);
		}
	}
	
	@Module(name="three")
	public static class Three
	{
		@In(name="size") public int size;
		
		@Main(print=false)
		public List<Point> read()
			throws IOException
		{
			
			Builder<IFS<Similitude>> builder = 
					IFS.builder(3, Similitude.similitudeBuilder(2));
			IFS<Similitude> ifs = builder.build(Arrays.asList(
					0.1,  0.0, 0.5, 0.0, 1.0, 
					0.1,  0.5,-0.5, 0.0, 1.0, 
					0.1, -0.5,-0.5, 0.0, 1.0
				));
			
			MOG mog = null;
			for(Similitude map : ifs)
				if(mog == null)
					mog = new MOG(map, 1.0);
				else
					mog.addMap(map, 1.0);
				
			return mog.generate(size);
		}
	}
	
	@Module(name="sample")
	public static class Sample
	{
		@In(name="in")
		public List<Point> data;
		@In(name="size")
		public int size;
		
		@Main(print=false)
		public List<Point> sample()
		{
			return Datasets.sampleWithoutReplacement(data, size); 
		}
	}
}
