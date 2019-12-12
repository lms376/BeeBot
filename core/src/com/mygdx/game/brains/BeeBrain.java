package com.mygdx.game.brains;

import io.jenetics.Chromosome;
import io.jenetics.DoubleGene;
import io.jenetics.Genotype;

import java.util.Iterator;

public class BeeBrain {

    public BeeBrain(Genotype<DoubleGene> gt){
    }

    double evaluate(){
        return 0;
    }

    public static double[] getWeightCoords(Genotype<DoubleGene> genotype) {
        double[] weightCoords = new double[genotype.geneCount()];

        int i = 0;
        Iterator<Chromosome<DoubleGene>> geneIt = genotype.iterator();

        while(geneIt.hasNext()) {
            Chromosome<DoubleGene> nextChrom = geneIt.next();
            Iterator<DoubleGene> chromIt = nextChrom.iterator();
            while(chromIt.hasNext()) {
                DoubleGene gene = chromIt.next();
                weightCoords[i] = gene.doubleValue();
                i++;
            }
        }

        return weightCoords;
    }
}
