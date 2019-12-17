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
@Deprecated
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


    public static DoubleChromosome getChromosome(int length, int[] l, int i){
        //length = #hidden layers + 1, total layers -1.
        final Random random = RandomRegistry.getRandom();
        int maxNeurons = 256, minNeurons = 32;
        if(i==0){//this class is static so using the same layers arr for every gt may cause errors - fixed
            //layers = new int[length+1];
            l[0] = inNum;
            l[length] = outNum;
        }
        int prevNum = l[i]; //neurons in prev layer
        int nextNum = 0;
        if(i < length - 1) {
            nextNum = random.nextInt(maxNeurons) + minNeurons;
            l[i+1] = nextNum;
        } else {
            nextNum = l[length];
        }

        //current 'weight layer' between prev and next layer => #weights = prevNum*nextNum
        return DoubleChromosome.of(minVal, maxVal, prevNum*nextNum);

    }

    public static Genotype<DoubleGene> getGenotype(int length){
        int[] layerList = new int[length+1];
        //length = #chromosomes -> number of weight layers, or #hidden layers+1
        Genotype<DoubleGene> gt = Genotype.of(
                IntStream.range(0, length)
                        .mapToObj(i -> getChromosome(length, layerList, i))
                        .collect(ISeq.toISeq())
        );

        layers = layerList;
        return gt;
    }


    private static final Factory<Genotype<DoubleGene>> ENCODING = () -> {
        final Random random = RandomRegistry.getRandom();
        return getGenotype(random.nextInt(maxLength)+minLength);
    };

    private static double fitness(final Genotype<DoubleGene> gt) {
        // Change this
        //double score = beeController.evaluate(gt, layers); //cannot pass in layers - static list same for all gt's
        //System.out.println("Score: " + score);
        return 0;
    }

    // The special mutator also variates the chromosome/genotype length.
    private static final class DynamicMutator<
            G extends Gene<?, G>,
            C extends Comparable<? super C>
            >
            extends AbstractAlterer<G, C>
    {
        DynamicMutator(double probability) {
            super(probability);
        }

        @Override
        public AltererResult<G, C> alter(
                final Seq<Phenotype<G, C>> population,
                final long generation
        ) {
            final double p = pow(_probability, 1.0/3.0);
            final IntRef alterations = new IntRef(0);
            final MSeq<Phenotype<G, C>> pop = MSeq.of(population);

            indexes(RandomRegistry.getRandom(), pop.size(), p).forEach(i -> {
                final Phenotype<G, C> pt = pop.get(i);

                final Genotype<G> gt = pt.getGenotype();
                final Genotype<G> mgt = mutate(gt, p, alterations);

                final Phenotype<G, C> mpt = Phenotype.of(mgt, generation);
                pop.set(i, mpt);
            });

            return AltererResult.of(pop.toISeq(), alterations.value);
        }

        private Genotype<G> mutate(
                final Genotype<G> genotype,
                final double p,
                final IntRef alterations
        ) {
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
