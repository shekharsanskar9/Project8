import java.util.Arrays;
import java.util.Comparator;
import java.util.Random;

/**
 * Genetic Algorithm for joint optimisation of makespan and resource utilisation.
 *
 * Chromosome : int[n], gene i = index of the VM assigned to task i
 * Fitness    : F = w1 * (Makespan / LowerBound) + w2 * (1 - Utilization)   (minimised)
 *              LowerBound = sum(L_i) / sum(MIPS_j)  = ideal makespan with perfect balance
 * Selection  : tournament (k = 3)
 * Crossover  : two-point
 * Mutation   : each gene is reassigned to a random VM with probability mutationRate
 * Elitism    : best 'elite' chromosomes are copied unchanged into the next generation
 */
public class GeneticScheduler {

    private final int popSize, generations, elite;
    private final int tournamentK = 3;
    private final double crossoverRate, mutationRate, w1, w2;
    private final boolean seedHeuristics;
    private final Random rnd;

    private double[] len, mips;
    private double lowerBound;
    private double[] convergence = new double[0];

    public GeneticScheduler(int popSize, int generations, double crossoverRate,
                            double mutationRate, int elite, double w1, double w2,
                            boolean seedHeuristics, long seed) {
        this.popSize = popSize;
        this.generations = generations;
        this.crossoverRate = crossoverRate;
        this.mutationRate = mutationRate;
        this.elite = elite;
        this.w1 = w1;
        this.w2 = w2;
        this.seedHeuristics = seedHeuristics;
        this.rnd = new Random(seed);
    }

    /** Best fitness found up to each generation (for the convergence plot). */
    public double[] getConvergence() {
        return convergence;
    }

    public double fitness(int[] chr) {
        double[] load = Scheduling.loads(chr, len, mips);
        double ms = Scheduling.makespan(load);
        double util = Scheduling.utilization(load);
        return w1 * (ms / lowerBound) + w2 * (1.0 - util);
    }

    public int[] run(double[] taskLen, double[] vmMips) {
        this.len = taskLen;
        this.mips = vmMips;
        int n = len.length, m = mips.length;

        double sumLen = 0, sumMips = 0;
        for (double l : len) sumLen += l;
        for (double p : mips) sumMips += p;
        lowerBound = sumLen / sumMips;

        // ---- initial population (random, as in a plain GA) ----
        int[][] pop = new int[popSize][n];
        for (int i = 0; i < popSize; i++) {
            for (int g = 0; g < n; g++) pop[i][g] = rnd.nextInt(m);
        }
        // Optional: seed with heuristic solutions. This is NOT in a plain GA,
        // so if you switch it on, list it as a difference from the paper.
        if (seedHeuristics && popSize >= 2) {
            pop[0] = Scheduling.minMin(len, mips);
            pop[1] = Scheduling.mct(len, mips);
        }

        final double[] fit = new double[popSize];
        convergence = new double[generations];
        int[] bestChr = null;
        double bestFit = Double.MAX_VALUE;

        for (int gen = 0; gen < generations; gen++) {
            for (int i = 0; i < popSize; i++) fit[i] = fitness(pop[i]);

            Integer[] idx = new Integer[popSize];
            for (int i = 0; i < popSize; i++) idx[i] = i;
            Arrays.sort(idx, Comparator.comparingDouble(i -> fit[i]));

            if (fit[idx[0]] < bestFit) {
                bestFit = fit[idx[0]];
                bestChr = pop[idx[0]].clone();
            }
            convergence[gen] = bestFit;

            int[][] next = new int[popSize][];
            for (int e = 0; e < elite && e < popSize; e++) {
                next[e] = pop[idx[e]].clone();
            }
            for (int k = elite; k < popSize; k++) {
                int[] p1 = tournament(pop, fit);
                int[] p2 = tournament(pop, fit);
                int[] child = (rnd.nextDouble() < crossoverRate) ? crossover(p1, p2) : p1.clone();
                mutate(child, m);
                next[k] = child;
            }
            pop = next;
        }
        return bestChr;
    }

    private int[] tournament(int[][] pop, double[] fit) {
        int best = rnd.nextInt(popSize);
        for (int t = 1; t < tournamentK; t++) {
            int c = rnd.nextInt(popSize);
            if (fit[c] < fit[best]) best = c;
        }
        return pop[best];
    }

    private int[] crossover(int[] p1, int[] p2) {
        int n = p1.length;
        int a = rnd.nextInt(n), b = rnd.nextInt(n);
        if (a > b) {
            int t = a;
            a = b;
            b = t;
        }
        int[] child = p1.clone();
        for (int i = a; i <= b; i++) child[i] = p2[i];
        return child;
    }

    private void mutate(int[] chr, int m) {
        for (int i = 0; i < chr.length; i++) {
            if (rnd.nextDouble() < mutationRate) chr[i] = rnd.nextInt(m);
        }
    }
}