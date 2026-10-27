import java.util.Arrays;
import java.util.Comparator;
import java.util.Random;

/**
 * Genetic Algorithm for joint optimisation of makespan and resource utilisation.
 *
 * Chromosome : int[n], gene i = index of the VM assigned to task i
 * Fitness    : F = w1 * (Makespan / LowerBound) + w2 * (1 - Utilization)   (minimised)
 *              LowerBound = sum(L_i) / sum(MIPS_j)  = ideal makespan with perfect balance
 *              (1 - Utilization) is the fraction of VM time left idle, i.e. resource wastage
 * Selection  : tournament (k = 3)                       [paper: roulette wheel  -> Selection.ROULETTE]
 * Crossover  : two-point                                [paper: one-point       -> Crossover.ONE_POINT]
 * Mutation   : each gene is reassigned to a random VM with probability mutationRate
 * Elitism    : best 'elite' chromosomes are copied unchanged into the next generation
 * Generation : elite individuals + (popSize - elite) offspring, each offspring made from
 *              two selected parents by crossover (prob. pc, else a copy of parent 1) and mutation
 *
 * The default configuration (tournament, two-point, no seeding) is exactly the GA used for the
 * main results. The other options exist only for the ablation experiments in Q8Experiments.
 */
public class GeneticScheduler {

    public enum Selection { TOURNAMENT, ROULETTE }
    public enum Crossover { TWO_POINT, ONE_POINT }
    /** NONE = plain random population; MINMIN_MCT = original optional seeding;
     *  MAXMIN = one individual seeded with the Max-Min schedule (hybrid GA). */
    public enum Seeding { NONE, MINMIN_MCT, MAXMIN }

    private final int popSize, generations, elite;
    private final int tournamentK = 3;
    private final double crossoverRate, mutationRate, w1, w2;
    private final Selection selection;
    private final Crossover crossoverType;
    private final Seeding seeding;
    private final Random rnd;

    private double[] len, mips;
    private double lowerBound;
    private double[] convergence = new double[0];

    /** Original constructor, kept for compatibility (tournament + two-point). */
    public GeneticScheduler(int popSize, int generations, double crossoverRate,
                            double mutationRate, int elite, double w1, double w2,
                            boolean seedHeuristics, long seed) {
        this(popSize, generations, crossoverRate, mutationRate, elite, w1, w2,
                Selection.TOURNAMENT, Crossover.TWO_POINT,
                seedHeuristics ? Seeding.MINMIN_MCT : Seeding.NONE, seed);
    }

    public GeneticScheduler(int popSize, int generations, double crossoverRate,
                            double mutationRate, int elite, double w1, double w2,
                            Selection selection, Crossover crossoverType, Seeding seeding,
                            long seed) {
        this.popSize = popSize;
        this.generations = generations;
        this.crossoverRate = crossoverRate;
        this.mutationRate = mutationRate;
        this.elite = elite;
        this.w1 = w1;
        this.w2 = w2;
        this.selection = selection;
        this.crossoverType = crossoverType;
        this.seeding = seeding;
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

        lowerBound = Scheduling.lowerBound(len, mips);

        // ---- initial population (random, as in a plain GA) ----
        int[][] pop = new int[popSize][n];
        for (int i = 0; i < popSize; i++) {
            for (int g = 0; g < n; g++) pop[i][g] = rnd.nextInt(m);
        }
        // Optional seeding with heuristic solutions (NOT part of a plain GA; reported as a hybrid).
        if (seeding == Seeding.MINMIN_MCT && popSize >= 2) {
            pop[0] = Scheduling.minMin(len, mips);
            pop[1] = Scheduling.mct(len, mips);
        } else if (seeding == Seeding.MAXMIN && popSize >= 1) {
            pop[0] = Scheduling.maxMin(len, mips);
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

            // roulette weights for a minimised fitness: probability proportional to 1/F
            double[] cum = null;
            if (selection == Selection.ROULETTE) {
                cum = new double[popSize];
                double s = 0;
                for (int i = 0; i < popSize; i++) {
                    s += 1.0 / fit[i];
                    cum[i] = s;
                }
            }

            int[][] next = new int[popSize][];
            for (int e = 0; e < elite && e < popSize; e++) {
                next[e] = pop[idx[e]].clone();
            }
            for (int k = elite; k < popSize; k++) {
                int[] p1 = select(pop, fit, cum);
                int[] p2 = select(pop, fit, cum);
                int[] child = (rnd.nextDouble() < crossoverRate) ? crossover(p1, p2) : p1.clone();
                mutate(child, m);
                next[k] = child;
            }
            pop = next;
        }
        return bestChr;
    }

    private int[] select(int[][] pop, double[] fit, double[] cum) {
        return selection == Selection.ROULETTE ? roulette(pop, cum) : tournament(pop, fit);
    }

    private int[] tournament(int[][] pop, double[] fit) {
        int best = rnd.nextInt(popSize);
        for (int t = 1; t < tournamentK; t++) {
            int c = rnd.nextInt(popSize);
            if (fit[c] < fit[best]) best = c;
        }
        return pop[best];
    }

    private int[] roulette(int[][] pop, double[] cum) {
        double r = rnd.nextDouble() * cum[cum.length - 1];
        int lo = 0, hi = cum.length - 1;
        while (lo < hi) {               // first index with cum[i] >= r
            int mid = (lo + hi) >>> 1;
            if (cum[mid] < r) lo = mid + 1; else hi = mid;
        }
        return pop[lo];
    }

    private int[] crossover(int[] p1, int[] p2) {
        int n = p1.length;
        if (crossoverType == Crossover.ONE_POINT) {
            int c = 1 + rnd.nextInt(Math.max(1, n - 1));   // cut between genes c-1 and c
            int[] child = p1.clone();
            for (int i = c; i < n; i++) child[i] = p2[i];
            return child;
        }
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
