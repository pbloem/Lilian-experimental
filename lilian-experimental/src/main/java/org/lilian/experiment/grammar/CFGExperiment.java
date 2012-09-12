package org.lilian.experiment.grammar;

import static java.util.Collections.reverseOrder;
import static org.lilian.util.Series.series;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.lilian.Global;
import org.lilian.data.real.Point;
import org.lilian.experiment.AbstractExperiment;
import org.lilian.experiment.Parameter;
import org.lilian.experiment.Result;
import org.lilian.experiment.State;
import org.lilian.grammars.Grammar;
import org.lilian.grammars.Grammars;
import org.lilian.grammars.PCFGrammar;
import org.lilian.search.evo.Crossover;
import org.lilian.search.evo.GA;
import org.lilian.search.evo.Mutator;
import org.lilian.search.evo.Target;
import org.lilian.search.evo.grammar.Rules;
import org.lilian.util.Series;

public class CFGExperiment extends AbstractExperiment
{
	protected int populationSize;
	protected int generations;
	protected int binomN;
	protected double binomP;
	protected List<List<String>> testData;
	protected List<List<String>> trainData;
	protected double mutationProbability;
	protected double mutationWeight;
	protected double mutationWeightProb;
	protected int numRules;
	protected double terminalRules;
	protected int fitnessSampleSize;
	protected int fitnessRepeats;
	protected Grammar<String> target;

	public @State GA<Rules> ga;
	public @State double precision;
	public @State double recall;
	public @State PCFGrammar<String> best;

	public CFGExperiment(
			@Parameter(name="population size", description="") int populationSize, 
			@Parameter(name="generations", description="") int generations,
			@Parameter(name="binom n", description="") int binomN,
			@Parameter(name="binom p", description="") double binomP,
			@Parameter(name="data", description="") List<List<String>> data,
			@Parameter(name="data split") double testRatio,
			@Parameter(name="mutation probability", description="") double mutationProbability,
			@Parameter(name="weight mutation size", description="") double mutationWeight,
			@Parameter(name="weight mutation probability", description="") double mutationWeightProb,
			@Parameter(name="number of rules", description="") int numRules,
			@Parameter(name="terminal rule probability", description="") double terminalRules, 
			@Parameter(name="fitness sample size", description="") int fitnessSampleSize,
			@Parameter(name="fitness repeats") int fitnessRepeats,
			@Parameter(name="target grammar", description="") Grammar<String> target)
	{
		
		List<List<String>> dataCopy = new ArrayList<List<String>>(data);
		
		this.testData = new ArrayList<List<String>>(data.size());
		
		for(int i : series((int)(data.size() * testRatio)))
		{
			int draw = Global.random.nextInt(dataCopy.size());
			testData.add(dataCopy.remove(draw));
		}
		
		this.trainData = dataCopy;
		
		Global.log().info("Training data size " + trainData.size());		
		Global.log().info("Test data size " + testData.size());

		this.populationSize = populationSize;
		this.generations = generations;
		this.binomN = binomN;
		this.binomP = binomP;
		this.mutationProbability = mutationProbability;
		this.mutationWeight = mutationWeight;
		this.mutationWeightProb = mutationWeightProb;
		this.numRules = numRules;
		this.terminalRules = terminalRules;
		this.fitnessSampleSize = fitnessSampleSize;
		this.fitnessRepeats = fitnessRepeats;
		this.target = target;
	}

	@Override
	protected void setup()
	{
		Target<Rules> target   = Rules.target(testData, fitnessSampleSize, fitnessRepeats);
		Crossover<Rules> cross = Rules.crossover(); 
		Mutator<Rules> mutator = Rules.mutator(mutationWeight, mutationWeightProb);
		
		List<String> terminals = Rules.terminals(trainData);
		List<Rules> initial = new ArrayList<Rules>(populationSize);
		for(int i : series(populationSize))
			initial.add(Rules.random(numRules, terminalRules, terminals, binomP, binomN));
		
		ga = new GA<Rules>(initial, cross, mutator, target, mutationProbability);
	}

	@Override
	protected void body()
	{
		for(int i : series(generations))
		{
			ga.breed();
			
			best = ga.best().genes().grammar();
			List<Double> fitnesses = new ArrayList<Double>();
			for(GA<Rules>.Agent agent : ga.population())
				fitnesses.add(agent.fitness());
			
			Collections.sort(fitnesses, reverseOrder());
			logger.info("-- fitnesses: " + fitnesses);
			
			logger.info("Generation " + i + " sample sentence:" + best.generateSentence(Rules.TOP, 0, Rules.MAX_DEPTH));
		}
		
		// * calculate precision
		precision = Grammars.precision(best, testData);
		
		// * calculate recall
		if(target != null)
		{
			List<List<String>> recData = new ArrayList<List<String>>(testData.size());
			for(int i : series(testData.size()))
				recData.add(best.generateSentence(Rules.TOP, 0, Rules.MAX_DEPTH));
			
			recall = Grammars.precision(target, recData);
		} else 
			recall = -1.0;
			
		
	}

	@Result(name="precision")
	public double precision()
	{
		return precision;
	}
	
	@Result(name="recall")
	public double recall()
	{
		return recall;
	}
	
	@Result(name="f-score")
	public double fScore()
	{
		if(recall == -1.0)
			return -1.0;
		return 2.0 * (precision * recall) / (precision + recall);
	}
	
	@Result(name="Sample")
	public List<String> sample()
	{
		int size = 25;
		List<String> sample = new ArrayList<String>(size);
		
		for(int i : series(size))
		{
			String sentence = "";
			List<String> gen = best.generateSentence(Rules.TOP, 0, 25);
			for(String word : gen)
				sentence += word + " ";
			sample.add(sentence);
		}
		
		return sample;
	}
	
}
