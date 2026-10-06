import java.io.OutputStream;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.LinkedList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.cloudbus.cloudsim.Cloudlet;
import org.cloudbus.cloudsim.CloudletSchedulerSpaceShared;
import org.cloudbus.cloudsim.Datacenter;
import org.cloudbus.cloudsim.DatacenterBroker;
import org.cloudbus.cloudsim.DatacenterCharacteristics;
import org.cloudbus.cloudsim.Host;
import org.cloudbus.cloudsim.Pe;
import org.cloudbus.cloudsim.Storage;
import org.cloudbus.cloudsim.UtilizationModel;
import org.cloudbus.cloudsim.UtilizationModelFull;
import org.cloudbus.cloudsim.Vm;
import org.cloudbus.cloudsim.VmAllocationPolicySimple;
import org.cloudbus.cloudsim.VmSchedulerTimeShared;
import org.cloudbus.cloudsim.core.CloudSim;
import org.cloudbus.cloudsim.provisioners.BwProvisionerSimple;
import org.cloudbus.cloudsim.provisioners.PeProvisionerSimple;
import org.cloudbus.cloudsim.provisioners.RamProvisionerSimple;

/**
 * Question 8: CloudSim implementation of
 * "Joint optimization of resource utilization and makespan in cloud computing
 *  using genetic algorithm" (Dehury, Kumar, Kumar).
 *
 * Flow for every experiment:
 *   1. generate n tasks (lengths in MI) with a fixed seed
 *   2. each algorithm (RoundRobin, MCT, MinMin, MaxMin, GA) produces a mapping task -> VM
 *   3. the mapping is executed in CloudSim (broker.bindCloudletToVm)
 *   4. makespan / utilisation / imbalance are measured from CloudSim's results
 *   5. results are written to CSV files (graphs: run plot_results.py)
 *
 * Written against CloudSim 3.0.3 (package org.cloudbus.cloudsim).
 */
public class Q8Main {

    // ------------------------------------------------------------------
    // Experiment configuration  (document every value in your report)
    // ------------------------------------------------------------------

    // VM table from the question
    static final int[] VM_CORES   = {4, 2, 8, 6, 2};
    static final int[] VM_RAM_MB  = {8192, 4096, 16384, 8192, 8192};
    static final long[] VM_BW_MBPS = {1000, 500, 2000, 1000, 500};

    // ASSUMPTION: every core delivers 1000 MIPS. A VM is modelled as ONE processing
    // element whose speed is cores * 1000 MIPS, so ET = L / MIPS_vm exactly as in the
    // formulation, and tasks on a VM run one after another (space-shared queue).
    static final int MIPS_PER_CORE = 1000;
    static final double[] VM_MIPS = new double[VM_CORES.length];
    static {
        for (int j = 0; j < VM_CORES.length; j++) VM_MIPS[j] = VM_CORES[j] * MIPS_PER_CORE;
    }

    // ASSUMPTION: task length ~ Uniform(1000, 20000) MI, all tasks arrive at t = 0 (batch)
    static final double MIN_LEN = 1000, MAX_LEN = 20000;

    static final int[] TASK_COUNTS = {50, 100, 200, 500, 1000};
    static final int RUNS = 10;            // independent runs per task count (GA is stochastic)
    static final long BASE_SEED = 42;

    // GA parameters
    static final int    GA_POP = 100;
    static final int    GA_GENERATIONS = 300;
    static final double GA_CROSSOVER = 0.8;
    static final double GA_MUTATION = 0.02;
    static final int    GA_ELITE = 2;
    static final double W_MAKESPAN = 0.5;  // w1
    static final double W_UTIL = 0.5;      // w2  (w1 + w2 = 1)
    // false = plain random initial population (closest to the paper).
    // true  = seed 2 individuals with Min-Min / MCT (hybrid; report it as a difference).
    static final boolean SEED_HEURISTICS = false;

    // the run whose convergence curve and per-VM loads are saved for the report
    static final int DETAIL_TASKS = 100;

    static final String[] ALGOS = {"RoundRobin", "MCT", "MinMin", "MaxMin", "GA"};

    // ------------------------------------------------------------------

    static class SimResult {
        double makespan;
        double utilization;
        double imbalance;
        double[] busy;
    }

    public static void main(String[] args) throws Exception {
        // Silence CloudSim's own console output on any CloudSim version:
        //  1) call Log.disable() by reflection if that class exists in this version
        //  2) point System.out at a null stream; our own messages go through 'out'
        final PrintStream out = System.out;
        try {
            Class.forName("org.cloudbus.cloudsim.Log").getMethod("disable").invoke(null);
        } catch (Throwable ignored) {
            // this CloudSim version has no Log.disable(); the null stream below is enough
        }
        System.setOut(new PrintStream(new OutputStream() {
            @Override public void write(int b) { }
            @Override public void write(byte[] b, int off, int len) { }
        }));

        PrintWriter raw = new PrintWriter("raw_results.csv");
        raw.println("tasks,run,algorithm,pred_makespan,sim_makespan,pred_util,sim_util,sim_imbalance,algo_time_ms");
        PrintWriter conv = new PrintWriter("convergence.csv");
        conv.println("generation,best_fitness");
        PrintWriter vmLoad = new PrintWriter("vm_load.csv");
        vmLoad.println("algorithm,vm,load_seconds");

        // key "tasks|algo" -> {sumMakespan, sumUtil, sumImb, sumTime, count}
        Map<String, double[]> agg = new LinkedHashMap<String, double[]>();
        double maxRelErr = 0;

        for (int n : TASK_COUNTS) {
            for (int run = 0; run < RUNS; run++) {
                long seed = BASE_SEED + run * 1000L + n;
                double[] len = generateTasks(n, seed);

                for (String algo : ALGOS) {
                    long t0 = System.nanoTime();
                    int[] map;
                    if (algo.equals("RoundRobin")) {
                        map = Scheduling.roundRobin(n, VM_MIPS.length);
                    } else if (algo.equals("MCT")) {
                        map = Scheduling.mct(len, VM_MIPS);
                    } else if (algo.equals("MinMin")) {
                        map = Scheduling.minMin(len, VM_MIPS);
                    } else if (algo.equals("MaxMin")) {
                        map = Scheduling.maxMin(len, VM_MIPS);
                    } else {
                        GeneticScheduler ga = new GeneticScheduler(GA_POP, GA_GENERATIONS,
                                GA_CROSSOVER, GA_MUTATION, GA_ELITE, W_MAKESPAN, W_UTIL,
                                SEED_HEURISTICS, seed);
                        map = ga.run(len, VM_MIPS);
                        if (n == DETAIL_TASKS && run == 0) {
                            double[] c = ga.getConvergence();
                            for (int g = 0; g < c.length; g++) conv.println((g + 1) + "," + c[g]);
                        }
                    }
                    double algoMs = (System.nanoTime() - t0) / 1e6;

                    // predicted by the formulation
                    double[] predLoad = Scheduling.loads(map, len, VM_MIPS);
                    double predMs = Scheduling.makespan(predLoad);
                    double predUtil = Scheduling.utilization(predLoad);

                    // measured in CloudSim
                    SimResult sim = simulate(map, len);

                    double relErr = Math.abs(sim.makespan - predMs) / predMs;
                    maxRelErr = Math.max(maxRelErr, relErr);

                    raw.println(n + "," + run + "," + algo + "," + predMs + "," + sim.makespan + ","
                            + predUtil + "," + sim.utilization + "," + sim.imbalance + "," + algoMs);

                    if (n == DETAIL_TASKS && run == 0) {
                        for (int j = 0; j < sim.busy.length; j++) {
                            vmLoad.println(algo + ",VM" + (j + 1) + "," + sim.busy[j]);
                        }
                    }

                    String key = n + "|" + algo;
                    double[] a = agg.get(key);
                    if (a == null) {
                        a = new double[5];
                        agg.put(key, a);
                    }
                    a[0] += sim.makespan;
                    a[1] += sim.utilization;
                    a[2] += sim.imbalance;
                    a[3] += algoMs;
                    a[4] += 1;
                }
            }
            out.println("finished task count " + n);
        }
        raw.close();
        conv.close();
        vmLoad.close();

        // ---- console summary (means over RUNS runs) ----
        out.println();
        out.printf("%-6s %-11s %12s %10s %10s %12s%n",
                "tasks", "algorithm", "makespan(s)", "util", "imbalance", "algo_ms");
        for (Map.Entry<String, double[]> e : agg.entrySet()) {
            String[] k = e.getKey().split("\\|");
            double[] a = e.getValue();
            out.printf("%-6s %-11s %12.2f %10.4f %10.4f %12.2f%n",
                    k[0], k[1], a[0] / a[4], a[1] / a[4], a[2] / a[4], a[3] / a[4]);
        }
        out.printf("%nValidation: max relative difference between formula makespan and "
                + "CloudSim makespan = %.4f%%%n", maxRelErr * 100);
        out.println("Wrote raw_results.csv, convergence.csv, vm_load.csv");
    }

    // ------------------------------------------------------------------
    // Workload
    // ------------------------------------------------------------------

    static double[] generateTasks(int n, long seed) {
        Random r = new Random(seed);
        double[] len = new double[n];
        for (int i = 0; i < n; i++) {
            len[i] = Math.round(MIN_LEN + r.nextDouble() * (MAX_LEN - MIN_LEN));
        }
        return len;
    }

    // ------------------------------------------------------------------
    // CloudSim
    // ------------------------------------------------------------------

    static SimResult simulate(int[] map, double[] len) throws Exception {
        int m = VM_MIPS.length;

        CloudSim.init(1, Calendar.getInstance(), false);
        createDatacenter("Datacenter_0");
        DatacenterBroker broker = new DatacenterBroker("Broker_0");
        int brokerId = broker.getId();

        List<Vm> vmList = new ArrayList<Vm>();
        for (int j = 0; j < m; j++) {
            vmList.add(new Vm(j, brokerId, VM_MIPS[j], 1, VM_RAM_MB[j], VM_BW_MBPS[j],
                    10000, "Xen", new CloudletSchedulerSpaceShared()));
        }

        UtilizationModel full = new UtilizationModelFull();
        List<Cloudlet> cloudletList = new ArrayList<Cloudlet>();
        for (int i = 0; i < len.length; i++) {
            Cloudlet c = new Cloudlet(i, (long) len[i], 1, 300, 300, full, full, full);
            c.setUserId(brokerId);
            cloudletList.add(c);
        }

        broker.submitVmList(vmList);
        broker.submitCloudletList(cloudletList);
        for (int i = 0; i < len.length; i++) {
            broker.bindCloudletToVm(i, map[i]); // the mapping produced by the algorithm
        }

        CloudSim.startSimulation();
        List<Cloudlet> done = broker.getCloudletReceivedList();
        CloudSim.stopSimulation();

        if (done.size() != len.length) {
            throw new IllegalStateException("Only " + done.size() + " of " + len.length
                    + " cloudlets finished");
        }

        double[] busy = new double[m];
        double first = Double.MAX_VALUE, last = 0;
        for (Cloudlet c : done) {
            busy[c.getVmId()] += c.getActualCPUTime();
            first = Math.min(first, c.getExecStartTime());
            last = Math.max(last, c.getFinishTime());
        }
        double makespan = last - first;

        double sum = 0, min = Double.MAX_VALUE, max = 0;
        for (double b : busy) {
            sum += b;
            min = Math.min(min, b);
            max = Math.max(max, b);
        }
        double avg = sum / m;

        SimResult r = new SimResult();
        r.makespan = makespan;
        r.utilization = sum / (m * makespan);
        r.imbalance = (max - min) / avg;
        r.busy = busy;
        return r;
    }

    /**
     * ASSUMPTION: 2 identical hosts, each 32 cores (modelled as 4 PEs of 8000 MIPS,
     * the speed of the fastest VM), 64 GB RAM, 20 Gbps. Total capacity (64 cores, 128 GB)
     * exceeds what the 5 VMs need (22 cores, 44 GB), so all VMs are placed.
     */
    static Datacenter createDatacenter(String name) throws Exception {
        List<Host> hostList = new ArrayList<Host>();
        for (int h = 0; h < 2; h++) {
            List<Pe> peList = new ArrayList<Pe>();
            for (int p = 0; p < 4; p++) {
                peList.add(new Pe(p, new PeProvisionerSimple(8000)));
            }
            hostList.add(new Host(h,
                    new RamProvisionerSimple(65536),
                    new BwProvisionerSimple(20000),
                    1000000,
                    peList,
                    new VmSchedulerTimeShared(peList)));
        }
        DatacenterCharacteristics characteristics = new DatacenterCharacteristics(
                "x86", "Linux", "Xen", hostList, 10.0, 3.0, 0.05, 0.001, 0.0);
        return new Datacenter(name, characteristics,
                new VmAllocationPolicySimple(hostList), new LinkedList<Storage>(), 0);
    }
}