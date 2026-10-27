import java.util.Arrays;
import java.util.Comparator;
import java.util.Random;

/**
 * The GA exactly as given in Algorithm 1 of Dehury, Kumar and Kumar,
 * "Joint optimization of resource utilization and makespan in cloud computing using
 * genetic algorithm", Discover Computing 29:315 (2026). Used only for the comparison in
 * Section IX-C of the report; the main results use GeneticScheduler.
 *
 *   Encoding    : integer vector, gene i = VM index of task i                       (Sec. 6.1)
 *   Fitness     : f = w1 * Makespan' + w2 * ResW, ResW = 1 - U                        (Eq. 8)
 *                 Makespan' is the makespan divided by M_LB (see the report for why)
 *   Selection   : roulette wheel, P_i = (F_max - F_i) / sum_p (F_max - F_p)           (Eq. 12)
 *                 -> a mating pool of P parents
 *   Crossover   : for r = 1..P/2, two random parents from the pool give two children
 *                 by one-point crossover (always applied)                            (Alg. 1, 25-27)
 *   Mutation    : each gene, with probability mr, is reassigned to another VM        (Sec. 6.4)
 *   Replacement : (mu + lambda) elitism: the best P of parents and children survive  (Sec. 6.5)
 *   Stopping    : I_max iterations
 */
public class PaperGA {

    private final int popSize, iterations;
    private final double mutationRate, w1, w2;
    private final Random rnd;

    private double[] len, mips;
    private double lowerBound;
    private double[] convergence = new double[0];

    public PaperGA(int popSize, int iterations, double mutationRate, double w1, double w2, long seed) {
        this.popSize = popSize;
        this.iterations = iterations;
        this.mutationRate = mutationRate;
        this.w1 = w1;
        this.w2 = w2;
        this.rnd = new Random(seed);
    }

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

        // lines 2-16: random initial population and its fitness
        int[][] pop = new int[popSize][n];
        double[] fit = new double[popSize];
        for (int i = 0; i < popSize; i++) {
            for (int g = 0; g < n; g++) pop[i][g] = rnd.nextInt(m);
            fit[i] = fitness(pop[i]);
        }
        int best = argmin(fit);
        int[] bestChr = pop[best].clone();
        double bestFit = fit[best];

        convergence = new double[iterations];
        for (int t = 0; t < iterations; t++) {
            // lines 19-23: roulette selection with P_i proportional to (F_max - F_i)
            double fMax = Double.NEGATIVE_INFINITY;
            for (double f : fit) fMax = Math.max(fMax, f);
            double[] cum = new double[popSize];
            double s = 0;
            for (int i = 0; i < popSize; i++) {
                s += fMax - fit[i];
                cum[i] = s;
            }
            int[][] pool = new int[popSize][];
            for (int k = 0; k < popSize; k++) {
                pool[k] = pop[s > 0 ? spin(cum, s) : rnd.nextInt(popSize)];
            }

            // lines 24-30: P/2 pairs -> two children each by one-point crossover, then mutation
            int nChildren = 2 * (popSize / 2);
            int[][] children = new int[nChildren][];
            double[] childFit = new double[nChildren];
            for (int r = 0; r < popSize / 2; r++) {
                int[] p1 = pool[rnd.nextInt(popSize)];
                int[] p2 = pool[rnd.nextInt(popSize)];
                int c = 1 + rnd.nextInt(Math.max(1, n - 1));
                int[] c1 = new int[n], c2 = new int[n];
                for (int g = 0; g < n; g++) {
                    c1[g] = g < c ? p1[g] : p2[g];
                    c2[g] = g < c ? p2[g] : p1[g];
                }
                mutate(c1, m);
                mutate(c2, m);
                children[2 * r] = c1;
                children[2 * r + 1] = c2;
            }
            for (int k = 0; k < nChildren; k++) childFit[k] = fitness(children[k]);   // line 31

            // lines 32-35: (mu + lambda) replacement, keep the best P
            int total = popSize + nChildren;
            final double[] allFit = new double[total];
            final int[][] all = new int[total][];
            for (int i = 0; i < popSize; i++) { all[i] = pop[i]; allFit[i] = fit[i]; }
            for (int k = 0; k < nChildren; k++) { all[popSize + k] = children[k]; allFit[popSize + k] = childFit[k]; }
            Integer[] idx = new Integer[total];
            for (int i = 0; i < total; i++) idx[i] = i;
            Arrays.sort(idx, Comparator.comparingDouble(i -> allFit[i]));
            int[][] next = new int[popSize][];
            double[] nextFit = new double[popSize];
            for (int i = 0; i < popSize; i++) { next[i] = all[idx[i]]; nextFit[i] = allFit[idx[i]]; }
            pop = next;
            fit = nextFit;
            if (fit[0] < bestFit) {
                bestFit = fit[0];
                bestChr = pop[0].clone();
            }
            convergence[t] = bestFit;
        }
        return bestChr;
    }

    private int spin(double[] cum, double total) {
        double r = rnd.nextDouble() * total;
        int lo = 0, hi = cum.length - 1;
        while (lo < hi) {
            int mid = (lo + hi) >>> 1;
            if (cum[mid] < r) lo = mid + 1; else hi = mid;
        }
        return lo;
    }

    /** Per-gene mutation: with probability mr the task moves to a different, randomly chosen VM. */
    private void mutate(int[] chr, int m) {
        for (int g = 0; g < chr.length; g++) {
            if (rnd.nextDouble() < mutationRate && m > 1) {
                int v = rnd.nextInt(m - 1);
                chr[g] = v >= chr[g] ? v + 1 : v;
            }
        }
    }

    private static int argmin(double[] a) {
        int b = 0;
        for (int i = 1; i < a.length; i++) if (a[i] < a[b]) b = i;
        return b;
    }
}
