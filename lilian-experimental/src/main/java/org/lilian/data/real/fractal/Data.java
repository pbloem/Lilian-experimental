package org.lilian.data.real.fractal;

import java.util.List;

import org.data2semantics.platform.annotation.In;
import org.data2semantics.platform.annotation.Main;
import org.data2semantics.platform.annotation.Module;
import org.lilian.data.real.Point;
import org.lilian.data.real.Similitude;

@Module(name="Data")
public class Data
{

	
	@In(name="size")
	public int size;
	
	@Main(name="data", print=false)
	public List<Point> body()
	{
		IFS<Similitude> ifs = IFSs.sierpinskiSim();
		
		return ifs.generator().generate(size);
	}

}
