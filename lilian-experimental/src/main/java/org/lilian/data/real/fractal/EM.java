//package org.lilian.data.real.fractal;
//
//import java.util.*;
//
//import org.lilian.data.real.AffineMap;
//import org.lilian.data.real.Point;
//
//
///**
// * A two part search for an IFS model
// * @author Peter
// *
// */
//public class EM {
//	
//	private List<Point> data;
//	
//	private HashMap<List<Integer>, List<Point>> distribution;
//
//	private List<List<Integer>> codes;	
//	private List<AffineMap> transformations;
//	private List<Double> probabilities;
//	public  IFS<AffineMap> model;	
//		
//	private int num;
//	private int dim;
//	private int depth;
//	
//	public EM(int numComponents, int dimension, int depth, List<Point> data)
//	{
//		this.num = numComponents;
//		this.dim = dimension;
//		this.data = data;
//		this.depth = depth;
//		
//		// Create random AffineMaps.
//		int np = dim*dim + dim;
//		double dev = Conf.current.getIfsemInitialStdDev();
//
//		List<Double> parameters = new ArrayList<Double>();
//		for(int i = 0; i < np; i++)
//			parameters.add(Global.random.nextGaussian() * dev);
//		
//		model = new IFSDensityModel(new AffineMap(parameters), 1.0);
//		for(int i = 1; i < num; i++)
//		{
//			parameters.clear();
//			for(int j = 0; j < np; j++)
//				parameters.add(Global.random.nextGaussian() * dev);
//			model.addOperation(new AffineMap(parameters), 1.0);
//		}
//	}
//	
//	public EM(IFSDensityModel initial, List<Point> data)
//	{
//		this.num = initial.numberOfOperations();
//		this.dim = initial.dimension();
//		this.data = data;
//		this.depth = (int)initial.getDepth();
//		
//		this.model = initial;
//	}	
//	
//	public IFSDensityModel model()
//	{
//		return model;
//	}
//	
//	public HashMap<List<Integer>, List<Point>> distribution()
//	{
//		return distribution;
//	}
//	
//	public List<List<Integer>> codes()
//	{
//		return codes;
//	}
//	
//	public List<AffineMap> transformations()
//	{
//		return transformations;
//	}
//
//	public List<Double> probabilities()
//	{
//		return probabilities;
//	}
//	
//	
//	public void step()
//	{
//		distributePoints();
//		getTransformations();
//		findIFS();
//	}
//	
//	/**
//	 * Assigns each each point to the leaf-distribution of the current IFS that
//	 * is most likely to have generated it.  
//	 * 
//	 * @param data
//	 * @param model
//	 * @return
//	 */
//	public void distributePoints()
//	{
//		int sampleSize = Conf.current.getIfsemDataSubSample();
//		List<Point> sample =  Datasets.randomSubset(data, sampleSize);
//		
//		distribution = new LinkedHashMap<List<Integer>, List<Point>>();
//		model.setDepth(depth);
//		
//		for(Point point : sample)
//		{
//			List<Integer> code = model.findComponent(point);
//			if(!distribution.containsKey(code))
//				distribution.put(code, new ArrayList<Point>());				
//			
//			distribution.get(code).add(point);
//		}
//	}
//	
//	/**
//	 * Converts the point set for each leaf to a mean and a convariance matrix,
//	 * and then converts that to a transformation. 
//	 *
//	 */
//	public void getTransformations()
//	{
//		codes = new ArrayList<List<Integer>>();
//		transformations = new ArrayList<AffineMap>();
//		probabilities = new ArrayList<Double>();
//		
//		for(java.util.Map.Entry<List<Integer>, List<Point>> entry : 
//			distribution.entrySet())
//		{
//			List<Integer> code = entry.getKey();
//			List<Point> points = entry.getValue();
//			
//			int dim = points.get(0).dimensionality();
//			
//			// * find the mean
//			Vector mean = new DenseVector(dim);
//			for(Point point : points)
//				mean.add(point.getVector());
//			mean.scale(1.0/points.size());
//		
//			// * find the covariance matrix
//			Matrix cov = new DenseMatrix(dim, dim);
//			for(Point point : points)
//			{
//				Vector diff = point.getVector().copy();
//				diff.add(-1.0, mean);
//				cov.rank1(diff, diff);
//			}
//			
//			cov.scale(1.0/points.size());
//			
//			// * find the transformation matrix from the cov matrix
//			DenseCholesky decomp = DenseCholesky.factorize(cov);
//			Matrix transformation = null;			
//			try {
//				transformation = decomp.getU();
//			} catch (Exception e) {
//				System.out.println(Functions.toString(cov));
//				
//				e.printStackTrace();
//				System.exit(9);
//			}
//			
//			AffineMap map = new AffineMap(transformation, mean);
//			
//			codes.add(code);
//			transformations.add(map);
//			probabilities.add(points.size() / (double) data.size());
//		}
//	}
//	
//	public void findIFS()
//	{
//		int n = Conf.current.getEsPopulationSize();
//		List<EMStepESAgent> pop = new ArrayList<EMStepESAgent>(n);
//		for(int i = 0; i < Conf.current.getEmPopulationSize(); i++)
//			pop.add(new EMStepESAgent(dim, num, codes, transformations, probabilities));
//	
//		ES<EMStepESAgent> es = new ES<EMStepESAgent>(pop);
//		
//		int steps =  Conf.current.getEmMaxIterations();
//		
//		for(int i = 0; i < steps; i++)
//		{
//			if(i % (steps/5) == 0)
//				System.out.println("("+i+"|_) - " + es.best().fitness());
//			
//			es.breed();
//		}
//		
//		model = es.best().model();
//		model.setDepth(depth);
//	}
//	
//	/**
//	 * Returns a mixture of gaussians model based on the induced trasformations
//	 * 
//	 * @return
//	 */
//	public MOGDensityModel mog() 
//	{
//		MOGDensityModel mog = new MOGDensityModel(transformations.get(0), 
//														probabilities.get(0));
//		for(int i = 1; i < transformations.size(); i++)
//			mog.addOperation(transformations.get(i), 
//					probabilities.get(i));
//		
//		return mog;
//	}
//}
