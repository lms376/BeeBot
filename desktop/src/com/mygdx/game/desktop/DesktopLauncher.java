package com.mygdx.game.desktop;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.backends.lwjgl.LwjglApplication;
import com.badlogic.gdx.backends.lwjgl.LwjglApplicationConfiguration;
import com.mygdx.game.GDXRoot;
import com.mygdx.game.brains.BeeBrain;
import io.jenetics.*;
import io.jenetics.internal.util.IntRef;
import io.jenetics.util.*;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.IntStream;

import static io.jenetics.internal.math.random.indexes;
import static java.lang.Math.min;
import static java.lang.Math.pow;
import static java.lang.String.format;

public class DesktopLauncher {
    public static void main(String[] arg) throws IOException {
        Evolver evolver = new Evolver();
        try {
            evolver.evolve(10, 20);
        } catch (Exception e) {
            evolver.writePop(-1);
            System.out.println("ERROR: " + e.getMessage());
        }
    }
}

class Evolver {
    //    static void run(BeeBrain[] bb) {
    //        LwjglApplicationConfiguration config = new LwjglApplicationConfiguration();
    //        LwjglApplicationConfiguration.disableAudio = true;
    //        config.title = "BeeGame#";
    //        config.width  = 1600;
    //        config.height = 900;
    //
    //        new LwjglApplication(new GDXRoot(bb), config);
    //    }
    private static final int minLength = 4,
            maxLength = 8;
    private static final double minVal = 0,
            maxVal = 1;
    private static final int minChroms = 4,
            maxChroms = 16;
    private static final int inNum = 28,
            outNum = 6;
    private static int[] layers;

    private double[] fitnesses;

    private static ISeq<Phenotype<DoubleGene, Double>> pop;

    private static DoubleChromosome getChromosome(int length, int[] l, int i){
        //length = #hidden layers + 1, total layers -1.
        final Random random = RandomRegistry.getRandom();
        if(i==0){//this class is static so using the same layers arr for every gt may cause errors - fixed
            l[0] = inNum;
            l[length] = outNum;
        }
        int prevNum = l[i]; //neurons in prev layer
        int nextNum = 0;
        if(i < length - 1) {
            nextNum = random.nextInt(maxChroms) + minChroms;
            l[i+1] = nextNum;
        } else {
            nextNum = l[length];
        }

        //current 'weight layer' between prev and next layer => #weights = prevNum*nextNum
        int chromLen = prevNum*nextNum;
        if (i > 0) chromLen += nextNum;
        return DoubleChromosome.of(minVal, maxVal, chromLen);

    }

    private static Genotype<DoubleGene> getGenotype(int length){
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

    void evolve(int size, int generations)
            throws ExecutionException, InterruptedException, IOException {
//        final Engine<DoubleGene, Double> engine = Engine
//                .builder(DesktopLauncher::fitness, ENCODING)
//                .alterers(new DynamicMutator<>(0.25))
//                .populationSize(20)
//                //.executor(new BeeExecutor())
//                .build();
//
//        final EvolutionResult<DoubleGene, Double> result = engine.stream()
//                .limit(20)
//                .collect(EvolutionResult.toBestEvolutionResult());
//
//        System.out.println("Fitness: " + result.getBestFitness());

        LwjglApplicationConfiguration.disableAudio = true;

        LwjglApplicationConfiguration config = new LwjglApplicationConfiguration();
        config.forceExit = false;
        config.width  = 1600;
        config.height = 900;

        GDXRoot root = new GDXRoot();
        LwjglApplication app = new LwjglApplication(root, config);

        ISeq<Phenotype<DoubleGene, Double>> population = readPop(new File("pop_obj_20"));

        //ISeq<Phenotype<DoubleGene, Double>> population = getPopulation(size, 0);
        for (int gen = 0; gen < generations; gen++) {
            evolutionStep(population, gen, app);
            population = selectAndMutate(population, gen);
            pop = population;
        }

        writePop(population, generations);

        app.exit();
    }

    /**
     * @param population population to mutate to 25% of
     * @param gen generation number
     * @return a mutated population
     */
    private ISeq<Phenotype<DoubleGene, Double>> selectAndMutate(ISeq<Phenotype<DoubleGene, Double>> population, int gen) {
        // :n/2 twice mutated top 25%
        // :n randomly generated

        MSeq<Phenotype<DoubleGene, Double>> popWithFit = MSeq.of(population);
        for(int i = 0; i < population.length(); i++) {
            Phenotype<DoubleGene, Double> pt = population.get(i);
            popWithFit.set(i, pt.withFitness(fitnesses[i]));
        }

        int count = (int)(popWithFit.length()*0.25);

        TruncationSelector<DoubleGene, Double> ts = new TruncationSelector<>();
        ISeq<Phenotype<DoubleGene, Double>> top25p = ts.select(popWithFit, count, Optimize.MAXIMUM);

        ISeq<Phenotype<DoubleGene, Double>> mutatedSeq = ISeq.of(top25p.get(0));

        DynamicMutator<DoubleGene, Double> dm = new DynamicMutator<>(DynamicMutator.DEFAULT_ALTER_PROBABILITY);
        AltererResult<DoubleGene, Double> alteredTop25p = dm.alter(top25p, gen);

        mutatedSeq = mutatedSeq.append(alteredTop25p.getPopulation());
        alteredTop25p = dm.alter(top25p, gen);
        mutatedSeq = mutatedSeq.append(alteredTop25p.getPopulation());

        count = popWithFit.length() - mutatedSeq.length();
        mutatedSeq = mutatedSeq.append(getPopulation(count, gen));

        return mutatedSeq;
    }

    /**
     * @param population population
     * @param gen generation number
     * @return scores array
     */
    private double[] evolutionStep(ISeq<Phenotype<DoubleGene, Double>> population, int gen, LwjglApplication app)
            throws ExecutionException, InterruptedException {
        //Run game w/ beebrains
        //collect results
        //selectAndMutate with results
        //gen++
        BeeBrain[] brains = new BeeBrain[population.length()];
        int i = 0;

        for(Phenotype<DoubleGene, Double> pt: population){
            BeeBrain bb = new BeeBrain(pt.getGenotype());
            brains[i++] = bb;
        }

        Future<double[]> future = new Stepper().step(app, brains, gen);
        while(!future.isDone()) {
            Thread.sleep(1000);
        }
        System.out.println();

        double[] scores = future.get();
        fitnesses = scores;

        return scores;
    }

    public static void writePop(ISeq<Phenotype<DoubleGene, Double>> population, int gen) throws IOException {
        final File file = new File("pop_obj_"+gen);
        IO.object.write(population, file);

    }

    public static void writePop(int gen) throws IOException {
        final File file = new File("pop_obj_"+gen);
        IO.object.write(pop, file);

    }

    public static ISeq<Phenotype<DoubleGene, Double>> readPop(File file) throws IOException {
        ISeq<Phenotype<DoubleGene, Double>> population = (ISeq<Phenotype<DoubleGene, Double>>)IO.object.read(file);
        return population;
    }




    class Stepper {
        private ExecutorService executor = Executors.newSingleThreadExecutor();

        Future<double[]> step(LwjglApplication app, BeeBrain[] brains, int gen) {
            return executor.submit(()  -> {
                GDXRoot root = (GDXRoot)app.getApplicationListener();
                while(root == null) {
                    System.out.print("no root...");
                    Thread.sleep(500);
                    root = (GDXRoot)app.getApplicationListener();
                }

                if(gen == 0) {
                    System.out.print("loading");
                    root.set(brains);
                    while (root.isLoading()) {
                        System.out.print("...");
                        Thread.sleep(500);
                    }
                    System.out.println("loaded");

                    System.out.println("---------------GEN " + gen + "---------------");
                } else {
                    System.out.println("---------------GEN " + gen + "---------------");

                    System.out.print("resetting...");
                    root.reset(brains);
                    while (root.resetting()) {
                        System.out.print("...");
                        Thread.sleep(500);
                    }
                    System.out.println("reset");
                }

                System.out.print("running");
                while(root.isRunning()) {
                    System.out.print("...");
                    Thread.sleep(500);
                }
                System.out.println();

                double[] scores = root.getScores();
                System.out.println(format("%.2f", root.secondsElapsed()) + "s");
                System.out.println("\n" + scores.length + " scores found");

              //  System.out.print("resetting...");
                //root.reset();while (root.resetting()) {
//                    System.out.print("...");
//                    Thread.sleep(1000);
//                }
             //   System.out.println("reset");

                return scores;
            });
        }
    }

    /**
     * @param size number of phenotypes in population
     * @param gen generation number
     * @return ISeq of [size] random phenotypes
     */
    private ISeq<Phenotype<DoubleGene, Double>> getPopulation(int size, int gen) {
        //generates [size] random phenotypes

        //todo: try to find better data structure
        ArrayList<Phenotype<DoubleGene, Double>> popList = new ArrayList<>();
        for(int i = 0; i < size; i++) {
            Genotype<DoubleGene> gt = ENCODING.newInstance();
            Phenotype<DoubleGene, Double> pt = Phenotype.of(
                    gt,
                    gen
            );

            popList.add(pt);
        }

        return ISeq.of(popList);
    }
}
