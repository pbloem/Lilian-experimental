package org.lilian.experiment.dimension;

import static org.lilian.util.Series.series;

import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import nl.peterbloem.powerlaws.Functions;

import org.lilian.Global;
import org.lilian.data.dimension.CorrelationIntegral;
import org.lilian.data.dimension.Takens;
import org.lilian.data.real.Datasets;
import org.lilian.data.real.Draw;
import org.lilian.data.real.Point;
import org.lilian.experiment.AbstractExperiment;
import org.lilian.experiment.Parameter;
import org.lilian.experiment.Result;
import org.lilian.experiment.Result.Plot;
import org.lilian.experiment.State;
import org.lilian.util.Series;
import org.lilian.util.distance.Distance;
import org.lilian.util.distance.EuclideanDistance;

public class Dimension extends AbstractExperiment
{
	public static final int PLOT_SAMPLES = 5000;
	
	private List<Point> data;
	private int numCandidates, samplesPerCandidate, dataSamples;
	private double epsilon;
	private double stepSize;
	private boolean calculateSignificance;
	
	public @State Takens.Fit fit;
	public @State Takens takens;
	
	public @State int instances;
	public @State int embeddingDimension;
	public @State List<Double> distances, sampled, generated;
	public @State double significance;
	public @State BufferedImage plot;
	
	public @State CorrelationIntegral cint;

	public Dimension(
			@Parameter(name="data") 
				List<Point> data,
			@Parameter(name="data samples")
				int dataSamples,
			@Parameter(name="candidates", description="The number of candidates to generate for the maxDistance parameter (-1 to use all data)")
				int numCandidates,
			@Parameter(name="samples per candidate", description="The number of times to sample for maxDistance values")
				int samplesPerCandidate,
			@Parameter(name="epsilon", description="The accuracy required in the significance calculation.")
				double epsilon,
			@Parameter(name="ci step size", description="Stepsize for the correlation integral.")
				double stepSize,
			@Parameter(name="significance")
				boolean calculateSignificance)
	
	{
		this.data = data;
		this.dataSamples = dataSamples;
		this.numCandidates = numCandidates;
		this.samplesPerCandidate = samplesPerCandidate;
		this.epsilon = epsilon;
		this.stepSize = stepSize;
		this.calculateSignificance = calculateSignificance;
	}

	@Override
	protected void setup()
	{
		if(dataSamples != -1)
			data = Datasets.sampleWithoutReplacement(data, dataSamples);
		plot = Draw.draw(data, 500, true);
	}

	@Override
	protected void body()
	{
		instances = data.size();
		embeddingDimension = data.get(0).dimensionality();
		Distance<Point> metric = new EuclideanDistance();

		distances = Takens.distances(data, metric);
		double max = distances.get(distances.size() - 1);
		
		logger.info(instances + " instances of " + embeddingDimension + " features.");
		
		fit = Takens.fit(distances, true);
		takens = 
				numCandidates == -1 ? 
						fit.fit() :
						fit.fit(numCandidates, samplesPerCandidate);
						
		Global.log().info("Finished fitting model.");				
					
		sampled = Datasets.sample(distances, PLOT_SAMPLES);
		Collections.sort(sampled);
		
		generated = takens.generate(distances, PLOT_SAMPLES);
		Collections.sort(generated);
		
		if(calculateSignificance)
		{
			Global.log().info("Calculating significance.");				
			significance = takens.significance(distances, epsilon, numCandidates, samplesPerCandidate);
		}
		
		Global.log().info("Correlation integral");
		cint = CorrelationIntegral.fromDataset(data, stepSize, max, metric);
	}
	
	@Result(name="data plot")
	public BufferedImage plot()
	{
		return plot;
	}

	@Result(name="dimension")
	public double dimension()
	{
		return takens.dimension();
	}
	
	@Result(name="max distance")
	public double maxDistance()
	{
		return takens.maxDistance();
	}
	
	@Result(name="distances", description="Some distances sampled from the data.")
	public List<Double> distances()
	{
		return sampled;
	}
	
	@Result(name="significance")
	public double significance()
	{
		return significance;
	}
	
	@Result(name="generated distances", description="Distances generated from the induced distribution.")
	public List<Double> generated()
	{
		return generated;
	}
	
	@Result(name="instances")
	public int instances()
	{
		return instances;
	}
	
	@Result(name="embedding dimension")
	public int embeddingDimension()
	{
		return embeddingDimension;
	}
	
	@Result(name="Correlation Integral", plot = Plot.SCATTER)
	public List<List<Double>> ci()
	{
		List<List<Double>> table = new ArrayList<List<Double>>();
		for(int i : series(cint.counts().size()))
			table.add(Arrays.asList(cint.distances().get(i), cint.counts().get(i)));
	
		return table;
	}
		
//	@Result(name="KS values")
//	public List<List<Double>> distances()
//	{
//		List<List<Double>> table = new ArrayList<List<Double>>();
//		for(Double distance : fit.unique(true))
//		{
//			Takens<Point> takens = fit.fit(distance);
//			table.add(Arrays.asList(distance, takens.dimension(), takens.ksTest(data) ));
//		}
//	
//		return table;
//	}
}
