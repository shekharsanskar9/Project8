import java.io.OutputStream;
import java.io.PrintStream;
import java.io.PrintWriter;

/**
 * Additional experiments for the Project8 report (run after Q8Main).
 *
 *   java Q8Experiments weights      -> weights.csv      : fitness-weight sweep w1 in {0,...,1}
 *   java Q8Experiments sensitivity  -> sensitivity.csv  : one-factor-at-a-time GA parameter study
 *   java Q8Experiments variants     -> variants.csv     : the paper's Algorithm 1 (PaperGA) with its own
 *                                                         settings and with mr = 1/n, and the Max-Min-seeded
 *                                                         GA, all sizes
 *                                      conv_variants.csv: their convergence (run 0 of each size)
 *   java Q8Experiments multipe      -> multipe.csv      : the same mappings executed with multi-PE VMs
 *   java Q8Experiments all          -> everything above
 *
 * Every task set is generated with the same seed formula as Q8Main (42 + 1000*run + n), so all
 * results can be paired run-by-run with raw_results.csv. Every mapping is executed in CloudSim.
 */
public class Q8Experiments {

    static PrintStream out;

    public static void main(String[] args) throws Exception {
        out = System.out;
        try {
            Class.forName("org.cloudbus.cloudsim.Log").getMethod("disable").invoke(null);
        } catch (Throwable ignored) { }
        System.setOut(new PrintStream(new OutputStream() {
            @Override public void write(int b) { }
            @Override public void write(byte[] b, int off, int len) { }
        }));

        String mode = args.length > 0 ? args[0] : "all";
        Q8Main.warmUp();
        if (mode.equals("weights") || mode.equals("all")) weights();
        if (mode.equals("sensitivity") || mode.equals("all")) sensitivity();
        if (mode.equals("variants") || mode.equals("all")) variants();
        if (mode.equals("multipe") || mode.equals("all")) multiPe();
    }

    static long seedOf(int n, int run) {
        return Q8Main.BASE_SEED + run * 1000L + n;
    }

    static double total(double[] a) {
        double s = 0;
        for (double v : a) s += v;
        return s;
    }

    static GeneticScheduler defaultGa(long seed) {
        return new GeneticScheduler(Q8Main.GA_POP, Q8Main.GA_GENERATIONS, Q8Main.GA_CROSSOVER,
                Q8Main.GA_MUTATION, Q8Main.GA_ELITE, Q8Main.W_MAKESPAN, Q8Main.W_UTIL,
                GeneticScheduler.Selection.TOURNAMENT, GeneticScheduler.Crossover.TWO_POINT,
                GeneticScheduler.Seeding.NONE, seed);
    }

    // ------------------------------------------------------------------
    // 1) Weight sweep: does changing w1/w2 trade makespan against utilisation?
    // ------------------------------------------------------------------
    static void weights() throws Exception {
        int[] sizes = {100, 500};
        double[] w1s = {0.0, 0.25, 0.5, 0.75, 1.0};
        PrintWriter pw = new PrintWriter("weights.csv");
        pw.println("tasks,run,w1,w2,lower_bound,sim_makespan,sim_util,sim_imbalance,total_busy");
        for (int n : sizes) {
            for (int run = 0; run < Q8Main.RUNS; run++) {
                long seed = seedOf(n, run);
                double[] len = Q8Main.generateTasks(n, seed);
                double lb = Scheduling.lowerBound(len, Q8Main.VM_MIPS);
                for (double w1 : w1s) {
                    double w2 = 1.0 - w1;
                    GeneticScheduler ga = new GeneticScheduler(Q8Main.GA_POP, Q8Main.GA_GENERATIONS,
                            Q8Main.GA_CROSSOVER, Q8Main.GA_MUTATION, Q8Main.GA_ELITE, w1, w2,
                            GeneticScheduler.Selection.TOURNAMENT, GeneticScheduler.Crossover.TWO_POINT,
                            GeneticScheduler.Seeding.NONE, seed);
                    int[] map = ga.run(len, Q8Main.VM_MIPS);
                    Q8Main.SimResult r = Q8Main.simulate(map, len);
                    pw.println(n + "," + run + "," + w1 + "," + w2 + "," + lb + "," + r.makespan + ","
                            + r.utilization + "," + r.imbalance + "," + total(r.busy));
                }
            }
            out.println("weights: finished " + n);
        }
        pw.close();
    }

    // ------------------------------------------------------------------
    // 2) GA parameter sensitivity (one factor at a time around the default)
    // ------------------------------------------------------------------
    static void sensitivity() throws Exception {
        int[] sizes = {100, 1000};
        PrintWriter pw = new PrintWriter("sensitivity.csv");
        pw.println("tasks,run,param,value,pop,gens,pc,pm,lower_bound,sim_makespan,sim_util,best_fitness,algo_time_ms");
        for (int n : sizes) {
            // {param name, label, pop, gens, pc, pm}
            Object[][] cfgs = {
                {"default", "default", 100, 300, 0.8, 0.02},
                {"population", "50", 50, 300, 0.8, 0.02},
                {"population", "200", 200, 300, 0.8, 0.02},
                {"generations", "100", 100, 100, 0.8, 0.02},
                {"generations", "1000", 100, 1000, 0.8, 0.02},
                {"crossover", "0.6", 100, 300, 0.6, 0.02},
                {"crossover", "1.0", 100, 300, 1.0, 0.02},
                {"mutation", "1/n", 100, 300, 0.8, 1.0 / n},
                {"mutation", "0.005", 100, 300, 0.8, 0.005},
                {"mutation", "0.05", 100, 300, 0.8, 0.05},
            };
            for (int run = 0; run < Q8Main.RUNS; run++) {
                long seed = seedOf(n, run);
                double[] len = Q8Main.generateTasks(n, seed);
                double lb = Scheduling.lowerBound(len, Q8Main.VM_MIPS);
                for (Object[] c : cfgs) {
                    int pop = (Integer) c[2], gens = (Integer) c[3];
                    double pc = (Double) c[4], pm = (Double) c[5];
                    long t0 = System.nanoTime();
                    GeneticScheduler ga = new GeneticScheduler(pop, gens, pc, pm, Q8Main.GA_ELITE,
                            Q8Main.W_MAKESPAN, Q8Main.W_UTIL,
                            GeneticScheduler.Selection.TOURNAMENT, GeneticScheduler.Crossover.TWO_POINT,
                            GeneticScheduler.Seeding.NONE, seed);
                    int[] map = ga.run(len, Q8Main.VM_MIPS);
                    double ms = (System.nanoTime() - t0) / 1e6;
                    double[] conv = ga.getConvergence();
                    Q8Main.SimResult r = Q8Main.simulate(map, len);
                    pw.println(n + "," + run + "," + c[0] + "," + c[1] + "," + pop + "," + gens + "," + pc + ","
                            + pm + "," + lb + "," + r.makespan + "," + r.utilization + ","
                            + conv[conv.length - 1] + "," + ms);
                }
            }
            out.println("sensitivity: finished " + n);
        }
        pw.close();
    }

    // ------------------------------------------------------------------
    // 3) GA variants: operators of the paper, and a Max-Min-seeded hybrid
    // ------------------------------------------------------------------
    static void variants() throws Exception {
        PrintWriter pw = new PrintWriter("variants.csv");
        pw.println("tasks,run,algorithm,lower_bound,sim_makespan,sim_util,sim_imbalance,best_fitness,algo_time_ms");
        PrintWriter cv = new PrintWriter("conv_variants.csv");
        cv.println("tasks,algorithm,generation,best_fitness");
        // GA-paper     : Algorithm 1 of the paper with its settings (P = 100, 100 iterations, mr = 0.1)
        // GA-paper-1/n : the same algorithm with mr = 1/n (isolates the effect of the mutation rate)
        // GA-MaxMin-seeded : our GA with one initial chromosome set to the Max-Min schedule
        String[] names = {"GA-paper", "GA-paper-1/n", "GA-MaxMin-seeded"};
        for (int n : Q8Main.TASK_COUNTS) {
            for (int run = 0; run < Q8Main.RUNS; run++) {
                long seed = seedOf(n, run);
                double[] len = Q8Main.generateTasks(n, seed);
                double lb = Scheduling.lowerBound(len, Q8Main.VM_MIPS);
                for (String name : names) {
                    long t0 = System.nanoTime();
                    int[] map;
                    double[] conv;
                    if (name.startsWith("GA-paper")) {
                        double mr = name.equals("GA-paper") ? 0.1 : 1.0 / n;
                        PaperGA ga = new PaperGA(100, 100, mr, Q8Main.W_MAKESPAN, Q8Main.W_UTIL, seed);
                        map = ga.run(len, Q8Main.VM_MIPS);
                        conv = ga.getConvergence();
                    } else {
                        GeneticScheduler ga = new GeneticScheduler(Q8Main.GA_POP, Q8Main.GA_GENERATIONS,
                                Q8Main.GA_CROSSOVER, Q8Main.GA_MUTATION, Q8Main.GA_ELITE,
                                Q8Main.W_MAKESPAN, Q8Main.W_UTIL, GeneticScheduler.Selection.TOURNAMENT,
                                GeneticScheduler.Crossover.TWO_POINT, GeneticScheduler.Seeding.MAXMIN, seed);
                        map = ga.run(len, Q8Main.VM_MIPS);
                        conv = ga.getConvergence();
                    }
                    double ms = (System.nanoTime() - t0) / 1e6;
                    Q8Main.SimResult r = Q8Main.simulate(map, len);
                    pw.println(n + "," + run + "," + name + "," + lb + "," + r.makespan + "," + r.utilization
                            + "," + r.imbalance + "," + conv[conv.length - 1] + "," + ms);
                    if (run == 0) {
                        for (int g = 0; g < conv.length; g++) {
                            cv.println(n + "," + name + "," + (g + 1) + "," + conv[g]);
                        }
                    }
                }
            }
            out.println("variants: finished " + n);
        }
        pw.close();
        cv.close();
    }

    // ------------------------------------------------------------------
    // 4) Single-PE assumption: execute the same mappings with c_j PEs of 1000 MIPS per VM
    // ------------------------------------------------------------------
    static void multiPe() throws Exception {
        PrintWriter pw = new PrintWriter("multipe.csv");
        pw.println("tasks,run,algorithm,sim_makespan_single_pe,sim_makespan_multi_pe,sim_util_multi_pe");
        for (int n : Q8Main.TASK_COUNTS) {
            for (int run = 0; run < Q8Main.RUNS; run++) {
                long seed = seedOf(n, run);
                double[] len = Q8Main.generateTasks(n, seed);
                for (String algo : Q8Main.ALGOS) {
                    int[] map = algo.equals("GA") ? defaultGa(seed).run(len, Q8Main.VM_MIPS)
                                                  : Q8Main.heuristic(algo, len);
                    Q8Main.SimResult one = Q8Main.simulate(map, len, false);
                    Q8Main.SimResult many = Q8Main.simulate(map, len, true);
                    pw.println(n + "," + run + "," + algo + "," + one.makespan + "," + many.makespan + ","
                            + many.utilization);
                }
            }
            out.println("multipe: finished " + n);
        }
        pw.close();
    }
}
