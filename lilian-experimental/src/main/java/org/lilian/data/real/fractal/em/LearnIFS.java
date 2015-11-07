package org.lilian.data.real.fractal.em;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.List;

import javax.imageio.ImageIO;

import org.data2semantics.platform.Global;
import org.data2semantics.platform.annotation.In;
import org.data2semantics.platform.annotation.Main;
import org.lilian.data.real.Draw;
import org.lilian.data.real.MVN;
import org.lilian.data.real.Map;
import org.lilian.data.real.Point;
import org.lilian.data.real.Similitude;
import org.lilian.data.real.fractal.EM;
import org.lilian.data.real.fractal.IFS;
import org.lilian.data.real.fractal.IFSs;
import org.lilian.search.Parametrizable;
import org.lilian.util.Series;

public class LearnIFS 
{

	@In(name="high quality")
	public boolean highQuality;
	
	@Main
	public void run()
		throws IOException
	{
		int iterations = 30;
		
		List<Point> data = IFSs.sierpinskiSim().generator().generate(100000);
		
		IFS<Similitude> initial = IFSs.sierpinskiSim();
		
		EM em = new EM(data, 128, initial, 6);
		
		for(int i : Series.series(iterations))
		{
			Global.log().info("" + i);
			
			em.iterate();
			
			if(data.get(0).size() == 2)
				write(em, Global.getWorkingDir(), String.format("generation%04d", i));
		
			
		}
	}
	 
	private <M extends Map & Parametrizable> void write(EM em, File dir, String name) throws IOException
	{		
		int div = highQuality ? 1 : 4;
		int its = highQuality ? (int) 10000000 : 10000;
		
		File genDir = new File(dir, "generations");
		genDir.mkdirs();
		
		BufferedImage image;

		image= Draw.draw(em.model().generator(em.depths()), its, 1000/div, true);
		ImageIO.write(image, "PNG", new File(genDir, name+".png"));
	}
}
