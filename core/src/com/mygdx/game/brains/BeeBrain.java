package com.mygdx.game.brains;

import io.jenetics.Chromosome;
import io.jenetics.DoubleGene;
import io.jenetics.Genotype;
import org.neuroph.core.Layer;
import org.neuroph.core.NeuralNetwork;
import org.neuroph.core.learning.LearningRule;
import org.neuroph.nnet.MultiLayerPerceptron;

import java.util.Iterator;

public class BeeBrain {

    MultiLayerPerceptron network;
    private double score;

    public BeeBrain(Genotype<DoubleGene> gt, int[] layers){
        int len = gt.length();
        int[] layerList = new int[len];
        int[] neuronList = new int[len+1];
        neuronList[0] = 512;
        neuronList[len] = 256;
        Iterator<Chromosome<DoubleGene>> gtIter = gt.iterator();
        int i = 0;
        while(gtIter.hasNext()){
            int genes = gtIter.next().length();

        }
        createNN(layers, getWeights(gt));
    }

    public void giveScore(double score) {
        this.score = score;
    }

    public double getScore() { return score; }

    public void createNN(int[] layers, double[] weights) {
        System.out.println(weights.length);
        network = new MultiLayerPerceptron();

        for (int i = 0; i < layers.length; i++) {
            Layer layer = new Layer(layers[i]);
            network.addLayer(layer);
        }

        network.setWeights(weights);
        network.setInputNeurons(network.getLayerAt(0).getNeurons());
        network.setOutputNeurons(network.getLayerAt(layers.length-1).getNeurons());
    }

    public MultiLayerPerceptron getNetwork() {
        return network;
    }

    double evaluate(){
        return 0;
    }

    private static double[] getWeights(Genotype<DoubleGene> genotype) {
        double[] weights = new double[genotype.geneCount()];

        int i = 0;
        Iterator<Chromosome<DoubleGene>> geneIt = genotype.iterator();

        while(geneIt.hasNext()) {
            Chromosome<DoubleGene> nextChrom = geneIt.next();
            Iterator<DoubleGene> chromIt = nextChrom.iterator();
            while(chromIt.hasNext()) {
                DoubleGene gene = chromIt.next();
                weights[i] = gene.doubleValue();
                i++;
            }
        }

        return weights;
    }
}
