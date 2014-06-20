package org.lilian.data.real.fractal.fin;

import java.util.List;

import org.data2semantics.platform.annotation.In;
import org.data2semantics.platform.annotation.Main;
import org.data2semantics.platform.annotation.Module;
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
	
}
