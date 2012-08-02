package org.lilian.data.real.fractal.em;

import static org.lilian.util.Series.series;

import java.io.PrintStream;
import java.io.Serializable;
import java.util.*;

import org.apache.commons.math.linear.ArrayRealVector;
import org.apache.commons.math.linear.RealVector;
import org.lilian.Global;
import org.lilian.data.real.AffineMap;
import org.lilian.data.real.Datasets;
import org.lilian.data.real.MVN;
import org.lilian.data.real.Point;
import org.lilian.data.real.Similitude;
import org.lilian.data.real.fractal.IFS;
import org.lilian.data.real.fractal.IFSTarget;
import org.lilian.data.real.fractal.IFSs;
import org.lilian.data.real.fractal.Tools;
import org.lilian.experiment.Reportable;
import org.lilian.models.BasicFrequencyModel;
import org.lilian.models.FrequencyModel;
import org.lilian.search.Builder;
import org.lilian.search.Parameters;
import org.lilian.search.Parametrizable;
import org.lilian.search.evo.ES;
import org.lilian.search.evo.Target;
import org.lilian.util.MatrixTools;
import org.lilian.util.Series;

/**
 * An EM-style algorithm for learning iterated function systems
 * 
 * @author Peter
 *
 */
public class EM implements Serializable
{
	
	public static enum ProbMode {NONE, OLD, NEW} // How to determine the component priors
												 // NEW is better
	
	@Reportable
	public static final ProbMode probMode = ProbMode.NEW;
	@Reportable
	// * Invert the similitudes if they are not contractive
	//   TODO: NOT SURE IF ture IS BSET. MAKE EXP. PARAM AND TEST
	public static final boolean INVERT_IF_NOT_CONTRACTIVE = false;
	// * If the relative frequency of a component among the code drops below this,
	//   we drop it, and distribute its symbol over the symbols of the most 
	//   frequent component
	public static final double DROP_PROBABILITY = 0.02;
	
	public Target<IFS<Similitude>> target;
	public Builder<IFS<Similitude>> builder;
	
	private static final double DEV = 0.8;
	private static final double PERTURB_VAR = 0.1;
	private static final double THRESHOLD = 0.00000000001;
	private static final int PAIR_SAMPLE_SIZE = 20;
	private static final double PAIR_SAMPLE_VAR  = 0.01;
	
	private List<Point> data;
	
	// private HashMap<List<Integer>, List<Point>> distribution;

	private List<List<Integer>> codes;	
	
	public  IFS<Similitude> model;	
	public double modelPerformance;
		
	// * If true, we do not just consider the means of the points with matching codes
	//   but also the variance (by mapping a small amount of random points to 
	//   both of the matching codes)
	private boolean considerVariance;
		
	private int num;
	private int dim;

	// * The root of a backwards trie of codes 
	private Node root;
	
	// * Frequencies of components over all codes
	private BasicFrequencyModel<Integer> priors = new BasicFrequencyModel<Integer>();
	
	/**
	 * Sets up the EM algorithm with a random initial transformations
	 * @param numComponents
	 * @param dimension
	 * @param depth
	 * @param data
	 * @param target If not null, then the EM algorithm will only update it's 
	 * model if the new model has a score equal to or 
	 */
	public EM(IFS<Similitude> initial, List<Point> data, boolean considerVariance, Target<IFS<Similitude>> target)
	{
		this.num = initial.size();
		this.dim = initial.dimension();
		this.data = data;
		this.target = target;
		this.considerVariance = considerVariance;
		
		if(dim != data.get(0).dimensionality())
			throw new IllegalArgumentException("Data dimension ("+data.get(0).dimensionality()+") must match initial model argument ("+dim+")");
		
		model = initial;
		
		if(target != null)
			modelPerformance = target.score(model);
		
		root = new Node(-1, null);
		
		Builder<Similitude> sb = Similitude.similitudeBuilder(dim);
		builder = IFS.builder(num, sb);
	}
//	
//	public EM(int numComponents, int initialDepth, int dimension, List<Point> data, boolean considerVariance)
//	{
//		this.num = numComponents;
//		this.dim = dimension;
//		this.data = data;
//		this.considerVariance = considerVariance;
//
//		root = new Node(-1, null);
//		
//		for(Point point : data)
//		{
//			List<Integer> code = new ArrayList<Integer>(initialDepth);
//			for(int i : Series.series(initialDepth))
//				code.add(Global.random.nextInt(numComponents));
//			
//			root.show(code, point);
//		}
//	}
////	
//	public EM(IFS<Similitude> initial, List<Point> data, boolean considerVariance)
//	{
//		this.num = initial.size();
//		this.dim = initial.dimension();
//		this.data = data;
//		this.considerVariance = considerVariance;
//
//		
//		this.model = initial;
//
//		root = new Node(-1, null);
//	}	
	
	public IFS<Similitude> model()
	{
		return model;
	}
	
	public List<List<Integer>> codes()
	{
		return codes;
	}
	
	
	public void print(PrintStream out)
	{
		root.print(out, 0);
	}

	/**
	 * Assigns each each point to the leaf-distribution of the current IFS that
	 * is most likely to have generated it.  
	 * 
	 * @param data
	 * @param model
	 * @return
	 */
	public void distributePoints(int sampleSize, int depth, int beamWidth)
	{
		List<Point> sample =  
			sampleSize	== -1 ? data : Datasets.sample(data, sampleSize);

		root = new Node(-1, null); // Challenging for the GC, but should be fine...
				
		for(Point point : sample)
		{
			List<Integer> code = null; 
			if(beamWidth == -1)
				code = IFS.code(model, point, depth);
			else	
				code = Tools.search(point, model, depth, beamWidth);
			
			if(code == null)
				throw new RuntimeException("Could not find a code for the point ("+point+").");
				
			root.show(code, point);
		}
	}
	
	public void findIFS()
	{
		findIFS(false, 0.0);
	}
	
	public void findIFS(boolean greedy, double noise)
	{	
		Maps maps = findMaps();
		
		priors = new BasicFrequencyModel<Integer>();
		root.count(priors);
		
		IFS<Similitude> lastModel = model;
		double lastPerformance = modelPerformance;
		model = null;
		
		List<Similitude> trans = new ArrayList<Similitude>(num);
		for(int i : Series.series(num))
			trans.add(null);

		List<Double> weights = new ArrayList<Double>(num);
		for(int i : Series.series(num))
			weights.add(1.0/num);
		
		List<Integer> assigned = new ArrayList<Integer>(num);
		List<Integer> unassigned = new ArrayList<Integer>(num);
		
		for(int i : Series.series(num))
		{
			int n = maps.size(i);
			
			Similitude map;
			if(n != 0)
			{
				map = org.lilian.data.real.Maps.findMap(
							maps.from(i), maps.to(i));
				
				double det = MatrixTools.getDeterminant(
						map.getTransformation());
				
				
				if(Double.isNaN(det))
					Global.log().warning("Map with NaN determinant" + map);
				
				// * If the map contracts too much, we perturb it slightly
				if(Math.abs(det) < THRESHOLD || Double.isNaN(det))
					map = Parameters.perturb(map, Similitude.similitudeBuilder(dim), PERTURB_VAR);
			
				if(INVERT_IF_NOT_CONTRACTIVE && map.scalar() > 1.0)
					map = map.inverse();
				
				trans.set(i, map);
				if(probMode == ProbMode.OLD) 
					weights.set(i, priors.probability(i));
				else
					weights.set(i, findScalar(maps.fromWeights(i), maps.toWeights(i)));
				
				assigned.add(i);
			} else {
				unassigned.add(i);
			}
			
		}
		
		if(assigned.isEmpty())
			throw new IllegalStateException("No points were assigned to any components");
		
		/**
		 * For each unassigned component, take a random assigned component and 
		 * perturb it slightly.
		 */
		for(int i : unassigned)
		{
			System.out.println("-------"+i);
			
			int j = assigned.get(Global.random.nextInt(assigned.size()));
			
			Similitude source = trans.get(j);
			Similitude perturbed0 = 
					Parameters.perturb(source,
						Similitude.similitudeBuilder(dim), 
						PERTURB_VAR);

			Similitude perturbed1 = 
					Parameters.perturb(source,
						Similitude.similitudeBuilder(dim), 
						PERTURB_VAR);
			
			perturbed0 = perturbed0.scalar() > 1.0 ? perturbed0.inverse() : perturbed0;
			perturbed1 = perturbed1.scalar() > 1.0 ? perturbed1.inverse() : perturbed1;
			
			
			trans.set(i, perturbed0);
			trans.set(j, perturbed1);
		}
		
		model = new IFS<Similitude>(trans.get(0), weights.get(0));
		for(int i = 1; i < num; i++)
			model.addMap(trans.get(i), weights.get(i));
		
		if(target != null)
			modelPerformance = target.score(model);

		if(greedy && lastPerformance > modelPerformance)
		{
			// * new model is no better than last, don't change
			model = lastModel;
			// ** but recompute score so that we don't get stuck on "lucky" models
			modelPerformance = target.score(model); 
		}
		
		if(noise > 0.0)
			model = IFSs.perturb(model, builder, noise);
	}
	
	
	/**
	 * Findst the optimal scalar c so that sum (x - c * y)^2 is minimized.
	 * 
	 * @param x
	 * @param y
	 * @return
	 */
	public static double findScalar(List<Double> x, List<Double> y)
	{
		double sumYY = 0.0;
		double sumYX = 0.0;
		
		for(int i = 0; i < x.size(); i++)
		{
			sumYY += y.get(i) * y.get(i);
			sumYX += y.get(i) * x.get(i);
		}
		
		return sumYX / sumYY;
	}
	
	/**
	 * Returns, for each component, a set of domain and range points that 
	 * define the map. The points are means of subsets of the data.
	 *  
	 * @return
	 */
	public Maps findMaps()
	{
		Maps maps = new Maps();
		root.findPairs(maps);
		
		return maps;
	}
		
	private class Node implements Serializable
	{
		MVN mvn = null;
		
		Node parent;
		Map<Integer, Node> children;
		int depth = 0;
		
		List<Integer> code;
		//double frequency;
		
		boolean isLeaf = false;
		// RealVector pointSum = new ArrayRealVector(dim);
		List<Point> points = new ArrayList<Point>();
		
		public Node(int symbol, Node parent)
		{
			this.parent = parent;
			code = new ArrayList<Integer>( parent != null ? parent.code().size() + 1: 1);
			
			if(parent != null)
				code.addAll(parent.code());
			if(symbol >= 0)
				code.add(symbol);
			
			if(parent != null)
				depth = parent.depth + 1;
			
			children = new HashMap<Integer, Node>();
		}
		
		public void count(BasicFrequencyModel<Integer> model)
		{
			if(parent != null)
				model.add(symbol());
			
			for(int i : children.keySet())
				children.get(i).count(model);
		}

		public int depth()
		{
			return depth;
		}
		
		public List<Integer> code()
		{
			return code;
		}
		
		public int symbol()
		{
			return code.get(code.size() - 1);
		}
		
		public void show(List<Integer> code, Point point)
		{
			// pointSum = pointSum.add(point.getVector());
			points.add(point);
			mvn = null; // signal that the mvn needs to be recomputed
			
			if(code.size() == 0)
			{
				isLeaf = true;
				return;
			}
			
			int symbol = code.get(0);
			if(!children.containsKey(symbol))
				children.put(symbol, new Node(symbol, this));				
			
			children.get(symbol).show(code.subList(1, code.size()), point);
		}
		
		public List<Point> points()
		{
			return points;
		}
		
		public double frequency()
		{
			return points.size();
		}
		
		public boolean isLeaf()
		{
			return isLeaf;
		}
		
		public MVN mvn()
		{
			if(mvn == null)
				mvn = MVN.find(points);
			
			return mvn;
		}
				
		public void print(PrintStream out, int indent)
		{
			String ind = "";
			for(int i : series(indent))
				ind += "\t";
			
			String code = "";
			for(int i : code())
				code += i;
			
			out.println(ind + code + " f:" + frequency() + ", p: " + points());
			for(int symbol : children.keySet())
				children.get(symbol).print(out, indent+1);
		}
		
		/**
		 * Returns the node for the given code (starting from this node).
		 * 
		 * @param code
		 * @return
		 */
		public Node find(List<Integer> code)
		{
			if(code.size() == 0)
				return this;
			
			int symbol = code.get(0);
			if(!children.containsKey(symbol))
				return null;
			
			return children.get(symbol).find(code.subList(1, code.size()));
		}
		
		/**
		 * start at the leaves of this node, take its code and find the longest 
		 * code that that shares its tail, save for the last symbol t. The 
		 * transformation t should map that node onto this.
		 * 
		 * 
		 * @param maps
		 */
		public void findPairs(Maps maps)
		{
			if(children.size() > 0) // Recurse
			{
				for(int i : children.keySet())
					children.get(i).findPairs(maps);	
			}
			
			if(code().size() > 0) // Execute for this node
			{
				List<Integer> codeFrom = new ArrayList<Integer>(code);
				int t = codeFrom.remove(0);
				
				Node nodeFrom = root.find(codeFrom);
				if(nodeFrom != null)
				{
					int m = Math.min(nodeFrom.points.size(), this.points.size());

					
					MVN from = nodeFrom.mvn(), to = mvn();
//					Global.log().info(considerVariance + " ");
					
					if(m < 3 || ! considerVariance) // not enough points to consider covariance
					{
						maps.add(t, from.mean(), to.mean());
					} else
					{
						// Consider the covariance by taking not just the means, 
						// but points close to zero mapped to both distributions
						
						for(int i = 0; i < PAIR_SAMPLE_SIZE; i++)
						{
							Point p = Point.random(dim, PAIR_SAMPLE_VAR); //normal.generate();
							
							Point pf = from.map().map(p);
							Point pt = to.map().map(p);
							
							maps.add(t, pf, pt);
						}
					}
					
					// Register the drop in frequency as the symbol t gets added to the code
					maps.weight(t, nodeFrom.frequency(), this.frequency());
				}
			}
		}
		
		public String toString()
		{
			String code = depth() + ") ";
			for(int i : code())
				code += i;
			return code;
		}
	}
	
	/**
	 * Small helper class for storing a to and from list for each component map
	 * 
	 * @author Peter
	 *
	 */
	public class Maps
	{
		private List<List<Point>> from = new ArrayList<List<Point>>();
		private List<List<Point>> to = new ArrayList<List<Point>>();
		
		private List<List<Double>> fromWeights = new ArrayList<List<Double>>();
		private List<List<Double>> toWeights = new ArrayList<List<Double>>();
		
		public int size(int i)
		{
			ensure(i);
			return from.get(i).size();
		}
		
		public void add(int component, Point from, Point to)
		{
			ensure(component);
			this.from.get(component).add(from);
			this.to.get(component).add(to);
		}
		
		public void weight(int component, double from, double to)
		{
			ensure(component);
			this.fromWeights.get(component).add(from);
			this.toWeights.get(component).add(to);
		}

		private void ensure(int component)
		{
			while(from.size() <= component)
				from.add(new ArrayList<Point>());
			while(to.size() <= component)
				to.add(new ArrayList<Point>());
			
			while(fromWeights.size() <= component)
				fromWeights.add(new ArrayList<Double>());
			while(toWeights.size() <= component)
				toWeights.add(new ArrayList<Double>());
		}
		
		public List<Point> from(int component)
		{
			if(component < from.size())
				return from.get(component);
			
			return Collections.emptyList();
		}
		
		public List<Point> to(int component)
		{
			if(component < to.size())
				return to.get(component);
			
			return Collections.emptyList();
		}
		
		public List<Double> fromWeights(int component)
		{
			if(component < fromWeights.size())
				return fromWeights.get(component);
			
			return Collections.emptyList();
		}
		
		public List<Double> toWeights(int component)
		{
			if(component < toWeights.size())
				return toWeights.get(component);
			
			return Collections.emptyList();
		}
		
		
		public String toString()
		{
			String out = "";
			
			for(int i: Series.series(from.size()))
			{
				out += i + ":" + from(i).size() + "_" + to(i).size() + " ";  
			}
			
			return out;
		}
	}
	
	/**
	 * Produces a simple initial model from a a set of random double variables.
	 * 
	 * @param dim
	 * @param comp
	 * @param var
	 * @return
	 */
	public static IFS<Similitude> initialRandom(int dim, int comp, double var)
	{
		// Create random Similitudes
		int np = Similitude.similitudeBuilder(dim).numParameters();


		List<Double> parameters = new ArrayList<Double>();
		for(int i = 0; i < np; i++)
			parameters.add(Global.random.nextGaussian() * var);	
		
		IFS<Similitude> model = new IFS<Similitude>(new Similitude(parameters), 1.0);
		for(int i = 1; i < comp; i++)
		{
			parameters.clear();
			for(int j = 0; j < np; j++)
				parameters.add(Global.random.nextGaussian() * DEV);
			model.addMap(new Similitude(parameters), 1.0);
		}
		
		return model;
	}

	/**
	 * Produces an initial model such that the given points are the fixed points 
	 * of each component. The components do not rotate and have the given scaling parameter
	 * 
	 * @param comp
	 * @param points
	 * @return
	 */
	public static IFS<Similitude> initialPoints(double scale, List<Point> points)
	{
		int dim = points.get(0).dimensionality();
		IFS<Similitude> model = null;
		double prior = 1.0/points.size();
		
		for(Point point : points)
		{
			RealVector translation = point.getVector().mapMultiply(1.0 - scale);
			Similitude map = new Similitude(scale, new Point(translation), (List<Double>)new Point((dim * dim - dim)/2));
			
			if(model == null)
				model = new IFS<Similitude>(map, prior);
			else
				model.addMap(map, prior);
		}
		
		return model;
		
	}
	
	/**
	 * Produces an initial model such that the given points are the fixed points 
	 * of each component. The components do not rotate and have the given scaling parameter
	 * 
	 * @param comp
	 * @param points
	 * @return
	 */
	public static IFS<Similitude> initialSphere(int dim, int comp, double radius, double scale)
	{
		List<Point> points = Datasets.sphere(dim, radius).generate(comp);
		return initialPoints(scale, points);
	}	
	
	/**
	 * Produces an initial model with fixed points all at a fixed distance from 
	 * the origin and a maximal angle between any two fixed points.
	 * 
	 * This method uses a search rather than an analytical approach.
	 * 
	 * @param dim
	 * @param comp
	 * @return
	 */
	public static IFS<Similitude> initialSpread(int dim, int comp, double radius, double scale)
	{
		Builder<PointList> b = new PointListBuilder(dim, comp);
		ES<PointList> es = new ES<PointList>(
				b, new SpreadTarget(radius), ES.initial(100, b.numParameters(), 0.6));
		
		for(int i : Series.series(100))
			es.breed();
		
		PointList list = es.best().instance();
		List<Point> points = new ArrayList<Point>(list.size());
		for(Point point : list)
		{
			RealVector v = point.getVector();
			v.unitize();
			v.mapMultiplyToSelf(radius);
			Point nw = new Point(v);
			points.add(nw);
		}
		
		return initialPoints(scale, points);
	}
	
	/**
	 * Provides an initial IFS that is n slightly perturbed variants of the 
	 * identity transform.
	 * 
	 * @return
	 */
	public static IFS<Similitude> initialIdentity(int dim, int num, double var)
	{
		Similitude source = Similitude.identity(dim);
				
		IFS<Similitude> ifs = null;
		for(int i : Series.series(num))
		{
			Similitude perturbed = 
					Parameters.perturb(source,
							Similitude.similitudeBuilder(dim), 
							PERTURB_VAR);
			if(perturbed.scalar() > 1.0)
				perturbed = perturbed.inverse();
			
			if(ifs == null)
				ifs = new IFS<Similitude>(perturbed, 1.0);
			else
				ifs.addMap(perturbed, 1.0);
		}
		
		return ifs;
	}
	
	private static class SpreadTarget implements Target<List<Point>>
	{
		double radius;
		
		public SpreadTarget(double radius)
		{
			this.radius = radius;
		}

		@Override
		public double score(List<Point> points)
		{
//			double penSum = 0.0;
//			
//			for(Point p : points)
//			{
//				double pen = Math.abs(radius - p.getVector().getNorm());
//				penSum += pen;
//			}
			
			double min = Double.POSITIVE_INFINITY;
			
			for(int i = 0; i < points.size(); i++)
				for(int j = i + 1; j < points.size(); j++)
				{
					RealVector a = points.get(i).getVector(),
					           b = points.get(j).getVector();
					
					double dot = a.dotProduct(b);
					double prd = a.getNorm() * b.getNorm();
					
					double angle = Math.acos(dot/prd);
					
					min = Math.min(min, angle);
				}
					
			return min;
		}
	}
	
	private static class PointList extends AbstractList<Point> implements Parametrizable
	{
		public List<Point> master;
		
		public PointList(List<Point> master)
		{
			this.master = master;
		}

		@Override
		public List<Double> parameters()
		{
			List<Double> parameters = new ArrayList<Double>();
			for(Point p : master)
				parameters.addAll(p);
			
			return parameters;
		}

		@Override
		public Point get(int index)
		{
			return master.get(index);
		}

		@Override
		public int size()
		{
			return master.size();
		}
	}
	
	private static class PointListBuilder implements Builder<PointList>
	{
		private int num;
		private int dim;
		
		public PointListBuilder(int dim, int num)
		{
			this.num = num;
			this.dim = dim;
		}

		@Override
		public PointList build(List<Double> parameters)
		{
			List<Point> points = new ArrayList<Point>();
			for(int i = 0; i < num*dim; i += dim)
				points.add(new Point(parameters.subList(i, i + dim)));
				
			return new PointList(points);
		}

		@Override
		public int numParameters()
		{
			return num * dim;
		}
	}
}
