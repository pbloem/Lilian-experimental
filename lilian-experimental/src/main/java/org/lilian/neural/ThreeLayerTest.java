package org.lilian.neural;

import static org.junit.Assert.*;

import org.junit.Test;
import org.lilian.data.real.Point;
import org.lilian.search.Builder;

public class ThreeLayerTest
{

	@Test
	public void testParameters()
	{
		Builder<ThreeLayer> builder = ThreeLayer.builder(2, 3, Activations.sigmoid());
		
		ThreeLayer map1 = 
				builder.build(Point.random(builder.numParameters(), 0.5));
		
		ThreeLayer map2 = 
				builder.build(map1.parameters());
		
		Point x = Point.random(2, 1.0),
		      y1 = map1.map(x),
		      y2 = map2.map(x);
		
		assertEquals(y1.get(0), y2.get(0), 0.0);
		assertEquals(y1.get(1), y2.get(1), 0.0);
		
	}

}
