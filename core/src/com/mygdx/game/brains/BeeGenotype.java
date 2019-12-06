package com.mygdx.game.brains;

import io.jenetics.*;
import io.jenetics.internal.math.random;
import io.jenetics.internal.util.IntRef;
import io.jenetics.util.*;
import static java.lang.Math.pow;
import static io.jenetics.internal.math.random.indexes;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.stream.IntStream;

import io.jenetics.internal.util.IntRef;

import io.jenetics.AbstractAlterer;
import io.jenetics.Chromosome;
import io.jenetics.DoubleChromosome;
import io.jenetics.DoubleGene;
import io.jenetics.Gene;
import io.jenetics.Genotype;
import io.jenetics.Phenotype;
import io.jenetics.Population;
import io.jenetics.engine.Engine;
import io.jenetics.engine.EvolutionResult;
import io.jenetics.util.Factory;
import io.jenetics.util.ISeq;
import io.jenetics.util.RandomRegistry;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.stream.IntStream;

import static io.jenetics.internal.math.random.indexes;
import static java.lang.Math.pow;

public class BeeGenotype {

    private static int minLength, maxLength;
    private static double minVal, maxVal;
    private static int minGenes, maxGenes;

    //(min, maxLength): Range of Genotype length
    //(min, maxGenes): Range of DoubleChromosome length
    //(min, maxVal): Range of DoubleGene values
    public BeeGenotype(int minLength, int maxLength, int minGenes, int maxGenes, double minVal, double maxVal){
        this.minLength = minLength;
        this.maxLength = maxLength;
        this.minGenes = minGenes;
        this.maxGenes = maxGenes;
        this.minVal = minVal;
        this.maxVal = maxVal;
    }

    private static final Factory<Genotype<DoubleGene>> ENCODING = () -> {
        final Random random = RandomRegistry.getRandom();
        return Genotype.of(
                // Vary the chromosome count between minLength and maxLength.
                IntStream.range(0, random.nextInt(maxLength) + minLength)
                        // Vary the chromosome length between minGenes and maxGenes number of DoubleGenes with values between minVal and maxVal.
                        .mapToObj(i -> DoubleChromosome.of(minVal, maxVal, random.nextInt(maxGenes) + minGenes))
                        .collect(ISeq.toISeq())
        );
    };

    private static double fitness(final Genotype<DoubleGene> gt) {
        // Calculate fitness from "dynamic" Genotype.
        System.out.println("Gene count: " + gt.geneCount());
        return 0;
    }

    // The special mutator also variates the chromosome/genotype length.
    private static final class DynamicMutator<G extends Gene<?, G>, C extends Comparable<? super C>> extends AbstractAlterer<G, C>
    {
        public DynamicMutator(double probability) {
            super(probability);
        }

        @Override
        public int alter(
                final Population<G, C> population,
                final long generation
        ) {
            final double p = pow(_probability, 1.0/3.0);
            final IntRef alterations = new IntRef(0);

            indexes(RandomRegistry.getRandom(), population.size(), p).forEach(i -> {
                final Phenotype<G, C> pt = population.get(i);

                final Genotype<G> gt = pt.getGenotype();
                final Genotype<G> mgt = mutate(gt, p, alterations);

                final Phenotype<G, C> mpt = pt.newInstance(mgt, generation);
                population.set(i, mpt);
            });

            return alterations.value;
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

}
