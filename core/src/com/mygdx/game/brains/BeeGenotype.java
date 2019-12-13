package com.mygdx.game.brains;

import com.mygdx.game.bees.BeeController;
import io.jenetics.*;
import io.jenetics.engine.Engine;
import io.jenetics.engine.EvolutionResult;
import io.jenetics.internal.math.random;
import io.jenetics.internal.util.IntRef;
import io.jenetics.util.*;

import static java.lang.Math.min;
import static java.lang.Math.pow;
import static io.jenetics.internal.math.random.indexes;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.stream.IntStream;

import io.jenetics.AbstractAlterer;
import io.jenetics.Chromosome;
import io.jenetics.DoubleChromosome;
import io.jenetics.DoubleGene;
import io.jenetics.Gene;
import io.jenetics.Genotype;
import io.jenetics.Phenotype;
import io.jenetics.util.Factory;
import io.jenetics.util.ISeq;
import io.jenetics.util.RandomRegistry;

public class BeeGenotype {

    private static int minLength, maxLength;
    private static double minVal, maxVal;
    private static int minGenes, maxGenes;
    private static int[] layers;
    private static int inNum, outNum;
    private static BeeController beeController;
    //(min, maxLength): Range of Genotype length/number of layers
    //(min, maxGenes): Range of DoubleChromosome length
    //(min, maxVal): Range of DoubleGene values
    public BeeGenotype(int _minLength, int _maxLength, int _minGenes, int _maxGenes, double _minVal, double _maxVal, int _in, int _out){
        minLength = _minLength;
        maxLength = _maxLength;
        minGenes = _minGenes;
        maxGenes = _maxGenes;
        minVal = _minVal;
        maxVal = _maxVal;
        inNum = _in;
        outNum = _out;
        final Random random = RandomRegistry.getRandom();
    }

    public static void set(BeeController controller, int _minLength, int _maxLength, int _minGenes, int _maxGenes, double _minVal, double _maxVal, int _in, int _out) {
        minLength = _minLength;
        maxLength = _maxLength;
        minGenes = _minGenes;
        maxGenes = _maxGenes;
        minVal = _minVal;
        maxVal = _maxVal;
        inNum = _in;
        outNum = _out;
        beeController = controller;
        final Random random = RandomRegistry.getRandom();
    }


    public static DoubleChromosome getChromosome(int length, int i){
        //length = #hidden layers + 1, total layers -1.
        final Random random = RandomRegistry.getRandom();
        if(i==0){
            layers = new int[length+1];
            layers[0] = inNum;
            layers[length] = outNum;
        }
        int prevNum = layers[i]; //neurons in prev layer
        int nextNum = 0;
        if(i<length-2){
            nextNum = random.nextInt(maxGenes/prevNum)+minGenes/prevNum; //rand num to produce weights btwn min and max genes
            layers[i+1] = nextNum; //neurons in next layer
        }else if(i==length-2){//2nd to last weight layer
            nextNum = random.nextInt(Math.min(maxGenes/prevNum,maxGenes/outNum))+Math.max(minGenes/prevNum, minGenes/outNum);
            layers[i+1] = nextNum;
        }else{//last weight layer
            nextNum = layers[length];
        }

        //current 'weight layer' between prev and next layer => #weights = prevNum*nextNum

        return DoubleChromosome.of(minVal, maxVal, prevNum*nextNum);

    }

    public static Genotype<DoubleGene> getGenotype(int length){
        //length = #chromosomes -> number of weight layers, or #hidden layers+1
        Genotype<DoubleGene> gt = Genotype.of(
                IntStream.range(0, length)
                        .mapToObj(i -> getChromosome(length, i))
                        .collect(ISeq.toISeq())
        );

        return gt;
    }

    private static final Factory<Genotype<DoubleGene>> ENCODING = () -> {
        final Random random = RandomRegistry.getRandom();
        return getGenotype(random.nextInt(maxLength)+minLength);
    };

    private static final Factory<Genotype<DoubleGene>> ENCODING_OLD = () -> {
        final Random random = RandomRegistry.getRandom();
        int length = random.nextInt(maxLength)+minLength;
        return Genotype.of(
                // Vary the chromosome count between 10 and 20.
                IntStream.range(0, length)
                        // Vary the chromosome length between 10 and 20.
                        .mapToObj(i -> getChromosome(length, i))
                        .collect(ISeq.toISeq())

        );

    };

    private static double fitness(final Genotype<DoubleGene> gt) {
        // Change this
        double score = beeController.evaluate(gt, layers);
        System.out.println("Score: " + score);
        return score;
    }

    // The special mutator also variates the chromosome/genotype length.
    private static final class DynamicMutator<G extends Gene<?, G>, C extends Comparable<? super C>> extends AbstractAlterer<G, C>
    {
        public DynamicMutator(double probability) {
            super(probability);
        }

        @Override
        public AltererResult<G, C> alter(Seq<Phenotype<G, C>> seq, long generation) {
            final double p = pow(_probability, 1.0/3.0);
            final IntRef alterations = new IntRef(0);
            List<Phenotype<G, C>> mptList = seq.asList();

            indexes(RandomRegistry.getRandom(), seq.size(), p).forEach(i -> {
                final Phenotype<G, C> pt = seq.get(i);

                final Genotype<G> gt = pt.getGenotype();
                final Genotype<G> mgt = mutate(gt, p, alterations);

                final Phenotype<G, C> mpt = pt.of(mgt, generation);
                mptList.set(i, mpt);
            });
            ISeq<Phenotype<G, C>> result = ISeq.of(mptList);
            return AltererResult.of(result, alterations.value);
        }


        private Genotype<G> mutate(final Genotype<G> genotype, final double p, final IntRef alterations) {
            final List<Chromosome<G>> chromosomes =
                    new ArrayList<>(genotype.toSeq().asList());

            // Add/remove Chromosome to Genotype.
            final Random random = RandomRegistry.getRandom();
            final double rd = random.nextDouble();
            if (rd < 1/3.0) {
                chromosomes.remove(0);
            } else if (rd < 2/3.0) {
                chromosomes.add(chromosomes.get(0).newInstance());
            }

            alterations.value +=
                    indexes(RandomRegistry.getRandom(), chromosomes.size(), p)
                            .map(i -> mutate(chromosomes, i, p))
                            .sum();

            return Genotype.of(chromosomes);
        }

        private int mutate(final List<Chromosome<G>> c, final int i, final double p) {
            final Chromosome<G> chromosome = c.get(i);
            final List<G> genes = new ArrayList<>(chromosome.toSeq().asList());

            final int mutations = mutate(genes, p);
            if (mutations > 0) {
                c.set(i, chromosome.newInstance(ISeq.of(genes)));
            }
            return mutations;
        }

        private int mutate(final List<G> genes, final double p) {
            final Random random = RandomRegistry.getRandom();

            // Add/remove Gene from chromosome.
            final double rd = random.nextDouble();
            if (rd < 1/3.0) {
                genes.remove(0);
            } else if (rd < 2/3.0) {
                genes.add(genes.get(0).newInstance());
            }

            return (int)indexes(random, genes.size(), p)
                    .peek(i -> genes.set(i, genes.get(i).newInstance()))
                    .count();
        }


    }

    public static void evolve(BeeController bc) {
        final Engine<DoubleGene, Double> engine = Engine
                .builder(BeeGenotype::fitness, ENCODING)
                .alterers(new DynamicMutator<>(0.25))
                .build();

        final EvolutionResult<DoubleGene, Double> result = engine.stream()
                .limit(20)
                .collect(EvolutionResult.toBestEvolutionResult());

        System.out.println(result.getBestFitness());
    }

}
