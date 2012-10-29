package org.lilian.experiment.classification;

import static org.lilian.util.Series.series;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Map;

import javax.imageio.ImageIO;

import libsvm.LibSVM;

import net.sf.javaml.classification.Classifier;
import net.sf.javaml.classification.KNearestNeighbors;
import net.sf.javaml.classification.evaluation.EvaluateDataset;
import net.sf.javaml.classification.evaluation.PerformanceMeasure;
import net.sf.javaml.core.Dataset;
import net.sf.javaml.core.DefaultDataset;
import net.sf.javaml.core.DenseInstance;
import net.sf.javaml.core.Instance;
import net.sf.javaml.tools.weka.WekaClassifier;

import org.lilian.data.real.Point;
import org.lilian.data.real.classification.Classified;
import org.lilian.experiment.AbstractExperiment;
import org.lilian.experiment.Parameter;
import org.lilian.experiment.Resources;
import org.lilian.experiment.Result;
import org.lilian.experiment.State;
import org.lilian.util.Pair;
import org.lilian.util.Series;

import weka.classifiers.functions.MultilayerPerceptron;
import weka.classifiers.trees.J48;
import weka.core.Instances;
import weka.core.SerializationHelper;

/**
 * Runs one of a set of predefined java-ml classifiers on a given dataset
 * 
 * @author Peter
 *
 */
public class Classification extends AbstractExperiment
{
	public static final int KNN_K = 7;
	public static final int ANN_HIDDEN_LAYERS = 1;
	public static final int ANN_HIDDEN_NODES = 20;
	public static final double ANN_LEARNING_RATE = 0.3;
	private static final double ANN_MOMENTUM = 0.2;
	private static final int ANN_ITERATIONS = 2000;
	private static double DT_CONFIDENCE = 0.25;
	private static boolean DT_PRUNE = true;
	
	private String classifierChoice;
	private Dataset trainingData, testData;
	private double withhold;
	
	public @State Classifier classifier;
	public @State double error;
	
	public Classification(
			@Parameter(name="classifier")
				String classifierChoice,
			@Parameter(name="data")
				String data,
			@Parameter(name="withhold")
				double withhold) throws IOException
	{
		this(
			classifierChoice, 
			Resources.csvClassification(new File(data)),
			withhold);
	}
	
	public Classification(
			@Parameter(name="classifier")
				String classifierChoice,
			@Parameter(name="data")
				Classified<Point> data,
			@Parameter(name="withhold")
				double withhold)
	{
		this.classifierChoice = classifierChoice;
		
		Pair<Classified<Point>, Classified<Point>> pair = org.lilian.data.real.classification.Classification.split(data, withhold);
		this.trainingData = convert(pair.first());
		this.testData = convert(pair.second());
		this.withhold = withhold;
	}

	@Override
	protected void body()
	{
		if(classifierChoice.equals("knn"))
			classifier = new KNearestNeighbors(KNN_K);
		else if(classifierChoice.equals("ann"))
			classifier = new WekaClassifier(mlp());
		else if(classifierChoice.equals("dt"))
			classifier = new WekaClassifier(j48());
		
		classifier.buildClassifier(trainingData);

		
		/* Counters for correct and wrong predictions. */
		int correct = 0, wrong = 0;
		/* Classify all instances and check with the correct class values */
		for (Instance inst : testData) 
		{
			Object predicted = classifier.classify(inst);
			Object real = inst.classValue();
			if (predicted.equals(real))
				correct++;
			else
				wrong++;
		}
		
		error = wrong / (double)(testData.size());
	}
	
	@Result(name="error")
	public double error()
	{
		return error;
	}

	public static Dataset convert(Classified<Point> in)
	{
		Dataset set = new DefaultDataset();
		for(int i : series(in.size()))
			set.add(new DenseInstance(in.get(i).getBackingData(), in.cls(i)));
		
		return set;
	}
	
	public static MultilayerPerceptron mlp()
	{
		MultilayerPerceptron model = new MultilayerPerceptron();
		
		model.setAutoBuild(true);
		
		String layers = "";
		for(int i = 0; i < ANN_HIDDEN_LAYERS; i++)
		{
			if(i != 0) layers += ", "; 
			layers += ANN_HIDDEN_NODES;
		}

		System.out.println(layers);
		model.setHiddenLayers(layers);
//			model.setHiddenLayers("" + Configuration.current.getHiddenLayerSize());
				
		model.setNominalToBinaryFilter(true);
		model.setDecay(true);
		model.setLearningRate(ANN_LEARNING_RATE);
		model.setMomentum(ANN_MOMENTUM);
		model.setNormalizeAttributes(true);
		model.setTrainingTime(ANN_ITERATIONS);
//			model.setGUI(true);
	
		return model;
	}
	
	public static J48 j48()
	{
		J48 model = new J48();
		
		model.setConfidenceFactor((float)DT_CONFIDENCE);
		model.setUnpruned(! DT_PRUNE);
				
		return model;
	}
}
