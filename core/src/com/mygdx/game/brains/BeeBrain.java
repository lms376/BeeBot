package com.mygdx.game.brains;

import io.jenetics.Chromosome;
import io.jenetics.DoubleGene;
import io.jenetics.Genotype;
import org.neuroph.core.Layer;
import org.neuroph.core.NeuralNetwork;
import org.neuroph.core.learning.LearningRule;
import org.neuroph.nnet.MultiLayerPerceptron;
import org.neuroph.util.TransferFunctionType;

import java.util.Arrays;
import java.util.Iterator;

public class BeeBrain {

    MultiLayerPerceptron network;
    private double score;

    public BeeBrain(Genotype<DoubleGene> gt){
        int[] layerList = getLayerStruct(gt);
        createNN(layerList, getWeights(gt));
    }

    public int[] getLayerStruct(Genotype<DoubleGene> gt){
        int[] numNeurs = new int[gt.length()+1];

        int i = 1;
        int prevN = 28;//inNum;
        numNeurs[0] = prevN;
        int lastNum = 6;//outNum;
        Iterator<Chromosome<DoubleGene>> geneIt = gt.iterator();

        while(geneIt.hasNext()) {
            if (i>1) prevN++;

            Chromosome<DoubleGene> nextChrom = geneIt.next();
            int numWeights = nextChrom.length();
            int nextN = numWeights/(prevN);
            numNeurs[i++] = nextN;
            prevN = nextN;

        }

        if(numNeurs[numNeurs.length-1]!=lastNum){ System.out.println("u fucked up");}

        return numNeurs;

    }

    public void giveScore(double score) {
        this.score = score;
    }

    public double getScore() { return score; }

    public void createNN(int[] layers, double[] weights) {
        network = new MultiLayerPerceptron(layers);//in#, layer#, out#
        network = new MultiLayerPerceptron(TransferFunctionType.SIGMOID, layers);

        //todo: fix weights



        network.setWeights(weights);
//        network.setInputNeurons(network.getLayerAt(0).getNeurons());
//        network.setOutputNeurons(network.getLayerAt(layers.length-1).getNeurons());
    }

    public MultiLayerPerceptron getNetwork() {
        return network;
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
