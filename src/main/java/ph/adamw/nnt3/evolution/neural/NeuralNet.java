/*
 * MIT License
 *
 * Copyright (c) 2018 awphi
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package ph.adamw.nnt3.evolution.neural;

import lombok.Getter;
import lombok.Setter;
import ph.adamw.nnt3.evolution.neural.neuron.Neuron;
import ph.adamw.nnt3.evolution.neural.neuron.NeuronConnection;
import ph.adamw.nnt3.evolution.neural.neuron.NeuronLayer;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public abstract class NeuralNet implements Runnable {
	// --- Evaluation properties ---

	private final NeuralNetSettings settings;
	@Getter
	private final List<NeuronLayer> allLayers = new ArrayList<>();

	// --- Threading ---
	@Getter
	protected double fitness = 0;
	@Getter
	private boolean isDone = false;

	// -- Evolutionary stuff ---
	@Getter
	@Setter
	private String threadName;

	// --- Layers ---
	protected Thread thread;
	@Getter
	private NeuronLayer inputLayer;
	@Getter
	private List<NeuronLayer> hiddenLayers = new ArrayList<>();
	@Getter
	private NeuronLayer outputLayer;

	// ------

	public NeuralNet(NeuralNetSettings settings) {
		this.settings = settings;
	}

	public void init(NeuralNet parent, String threadName) {
		setThreadName(threadName);

		inputLayer = new NeuronLayer(settings.getInputs(), settings.getActivationFunction());
		allLayers.add(inputLayer);

		for (int i = 0; i < settings.getHiddenLayersAmount(); i++) {
			hiddenLayers.add(new NeuronLayer(settings.getHiddenLayersSize(), settings.getActivationFunction()));
			allLayers.add(hiddenLayers.get(hiddenLayers.size() - 1));
		}

		outputLayer = new NeuronLayer(settings.getOutputs(), settings.getActivationFunction());
		allLayers.add(outputLayer);

		// Inputs to first hidden layer
		hiddenLayers.get(0).connectToLayer(inputLayer);

		// Hidden layers to each other
		for (int i = 0; i <= hiddenLayers.size() - 2; i++) {
			hiddenLayers.get(i + 1).connectToLayer(hiddenLayers.get(i));
		}

		// Last hidden layer to output layer
		outputLayer.connectToLayer(hiddenLayers.get(hiddenLayers.size() - 1));

		randomizeWeights(parent);
	}

	private void randomizeWeights(NeuralNet parent) {
		if (parent == null) {
			final Random random = new Random();
			for (NeuronLayer layer : allLayers) {
				for (Neuron neuron : layer)
					for (NeuronConnection connection : neuron.getConnections()) {
						connection.setWeight(random.nextFloat() * 2 - 1);
					}
			}
			return;
		}

		// Mutate the weights from the parent if it is not null
		for (int i = 0; i < getAllLayers().size(); i++) {
			for (int j = 0; j < getAllLayers().get(i).size(); j++) {
				Neuron thisNeuron = getAllLayers().get(i).get(j);
				Neuron parentNeuron = parent.getAllLayers().get(i).get(j);

				for (int k = 0; k < thisNeuron.getConnections().size(); k++) {
					double w = parentNeuron.getConnections().get(k).getWeight();
					double mutatedWeight = NeuralNet.Utils.mutateWeight(w, settings.getMutationRate());

					thisNeuron.getConnections().get(k).setWeight(mutatedWeight);
				}
			}
		}
	}

	protected List<Double> evaluate(List<Double> inputs) {
		if (inputs.size() != inputLayer.size()) {
			throw new RuntimeException("Input sample size needs to match the size of the input layer!");
		}

		inputLayer.setValues(new ArrayList<>(inputs));

		for (NeuronLayer hiddenLayer : hiddenLayers) {
			hiddenLayer.feedForward();
		}
		outputLayer.feedForward();

		return outputLayer.getValues();
	}

	public void start(boolean threaded) {
		if (threaded) {
			thread = new Thread(this, threadName);
			thread.start();
		} else {
			run();
		}
	}

	@Override
	public void run() {
		calculateFitness();
		synchronized (this) {
			isDone = true;
			notifyAll();
		}
	}

	protected abstract void calculateFitness();

	private static class Utils {
		private static final Random random = new Random();

		static double mutateWeight(double parentWeight, double mutateRate) {
			float percent = random.nextFloat() * 2 - 1;
			parentWeight += parentWeight * (mutateRate * percent);
			return parentWeight;
		}
	}
}
