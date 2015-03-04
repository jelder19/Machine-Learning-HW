package nnet_experiments;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.stream.IntStream;

import data.UciDataReader;
import func.nn.NeuralNetwork;
import func.nn.backprop.BackPropagationNetworkFactory;
import opt.OptimizationAlgorithm;
import opt.RandomizedHillClimbing;
import opt.example.NeuralNetworkOptimizationProblem;
import shared.DataSet;
import shared.DataSetDescription;
import shared.SumOfSquaresError;

public class Program {
	
	public static void main(String[] args) throws FileNotFoundException {
		String trainingPath = "higgs/training1000.csv";
		String testingPath = "higgs/testing1000.csv";
		int numHiddenNodes = 15;
		int numIterations = 3000;
		int numRestarts = 10;

		runRandomizedHillclimbing(trainingPath, testingPath, numHiddenNodes, numIterations, numRestarts);
	}
	
	private static void runRandomizedHillclimbing(String trainingPath, String testingPath, int numHiddenNodes,
			int numIterations, int numRestarts) throws FileNotFoundException {
		DataSet trainingData = new UciDataReader(new File(trainingPath)).read();
		DataSet testingData = new UciDataReader(new File(testingPath)).read();
		BackPropagationNetworkFactory networkFactory = new BackPropagationNetworkFactory();
		
		long start = System.currentTimeMillis();
		NetworkPerformance best = IntStream.range(0, numRestarts).parallel()
				.mapToObj(i -> runRandomizedHillclimbing(numHiddenNodes, numIterations, trainingData, testingData, networkFactory))
				.max(NetworkPerformance::compareByTestingPerf).get();
		long end = System.currentTimeMillis();
		
		System.out.println("Best Training: " + best.numTrainingCorrect);
		System.out.println("Best Testing: " + best.numTestingCorrect);
		
		System.out.println("Time: " + (end - start) / 1000 + " seconds");
	}

	private static NetworkPerformance runRandomizedHillclimbing(int numHiddenNodes,
			int numIterations, DataSet trainingData, DataSet testingData,
			BackPropagationNetworkFactory networkFactory) {
		NeuralNetwork network = createNetwork(networkFactory, trainingData, numHiddenNodes);
		NeuralNetworkOptimizationProblem problem = new NeuralNetworkOptimizationProblem(trainingData, network, new SumOfSquaresError());
		RandomizedHillClimbing algorithm = new RandomizedHillClimbing(problem);
		
		runAlgorithm(algorithm, numIterations);
		int numTrainingCorrect = countNumCorrect(network, trainingData);
		int numTestingCorrect = countNumCorrect(network, testingData);
		
		System.out.println("Training Correct: " + numTrainingCorrect);
		System.out.println("Testing Correct: " + numTestingCorrect);
		System.out.println();
		
		return new NetworkPerformance(numTrainingCorrect, numTestingCorrect);
	}
	
	private static int countNumCorrect(NeuralNetwork network, DataSet data) {
		int numCorrect = 0;
		for(int i = 0; i < data.size(); i++) {
            network.setInputValues(data.get(i).getData());
            network.run();

            boolean actualCategory = data.get(i).getLabel().getBoolean();
            boolean outputCategory = network.getOutputValues().get(0) > 0.5;
            
            if (actualCategory == outputCategory) {
            	numCorrect++;
            }
        }

		return numCorrect;
	}
	
	
	private static void runAlgorithm(OptimizationAlgorithm algo, int numIterations) {
		for(int i = 0; i < numIterations; i++) {
            algo.train();
		}
	}

	private static NeuralNetwork createNetwork(BackPropagationNetworkFactory networkFactory, DataSet trainingData,
			int numHiddenNodes) {
		int numAttributes = new DataSetDescription(trainingData).getAttributeCount();
		int[] layerNodeCounts = { numAttributes, numHiddenNodes, 1 };
		NeuralNetwork network = networkFactory.createClassificationNetwork(layerNodeCounts);

		return network;
	}
	
	
}
