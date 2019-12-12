package com.mygdx.game.brains;

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
    //(min, maxLength): Range of Genotype length/number of layers
    //(min, maxGenes): Range of DoubleChromosome length
    //(min, maxVal): Range of DoubleGene values
    public BeeGenotype(int minLength, int maxLength, int minGenes, int maxGenes, double minVal, double maxVal, int in, int out){
        this.minLength = minLength;
        this.maxLength = maxLength;
        this.minGenes = minGenes;
        this.maxGenes = maxGenes;
        this.minVal = minVal;
        this.maxVal = maxVal;
        this.inNum = in;
        this.outNum = out;
        final Random random = RandomRegistry.getRandom();
        randomLayers(random.nextInt(maxLength) + minLength);
    }

    public static void randomLayers(int layerNum){
        final Random random = RandomRegistry.getRandom();
        layers = new int[layerNum];
        layers[0] = inNum;
        layers[layerNum-1] = outNum;
        int prevNum = inNum;
        for(int i=1; i<layerNum-2;i++){
            prevNum = random.nextInt(maxGenes/prevNum)+minGenes/prevNum;
            layers[i] = prevNum;
        }

        int r1 = random.nextInt(Math.min(maxGenes/prevNum,maxGenes/outNum))+Math.max(minGenes/prevNum, minGenes/outNum);
        layers[layerNum-2] = r1;
    }

    private static final Factory<Genotype<DoubleGene>> ENCODING = () -> Genotype.of(
            // Vary the chromosome count between minLength and maxLength.
            IntStream.range(0, layers.length-2)
                    // Vary the chromosome length between minGenes and maxGenes number of DoubleGenes with values between minVal and maxVal.
                    //.mapToObj(i -> DoubleChromosome.of(minVal, maxVal, random.nextInt(maxGenes) + minGenes))
                    .mapToObj(i -> DoubleChromosome.of(minVal, maxVal, layers[i]*layers[i+1]))
                    .collect(ISeq.toISeq())
    );

    private static double fitness(final Genotype<DoubleGene> gt) {
        // Change this
        double score = new BeeBrain(gt).evaluate();
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

    public static void evolve() {
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
