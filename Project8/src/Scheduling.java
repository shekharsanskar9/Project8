import java.util.Arrays;

/**
 * Shared evaluation functions and baseline schedulers for Question 8.
 *
 * Model (matches the paper-style formulation):
 *   ET(i,j)    = L_i / MIPS_j
 *   Load_j     = sum of ET(i,j) over tasks assigned to VM j
 *   Makespan   = max_j Load_j
 *   Utilization= sum_j Load_j / (m * Makespan)
 *   Imbalance  = (max Load - min Load) / avg Load      (degree of imbalance)
 *
 * A "mapping" is an int[] where map[i] = index of the VM that runs task i.
 */
public class Scheduling {

    public static double[] loads(int[] map, double[] len, double[] mips) {
        double[] load = new double[mips.length];
        for (int i = 0; i < map.length; i++) {
            load[map[i]] += len[i] / mips[map[i]];
        }
        return load;
    }

    public static double makespan(double[] load) {
        double max = 0;
        for (double l : load) max = Math.max(max, l);
        return max;
    }

    public static double utilization(double[] load) {
        double sum = 0;
        for (double l : load) sum += l;
        double ms = makespan(load);
        return ms == 0 ? 0 : sum / (load.length * ms);
    }

    public static double imbalance(double[] load) {
        double min = Double.MAX_VALUE, max = 0, sum = 0;
        for (double l : load) {
            min = Math.min(min, l);
            max = Math.max(max, l);
            sum += l;
        }
        double avg = sum / load.length;
        return avg == 0 ? 0 : (max - min) / avg;
    }

    // ------------------------------------------------------------------
    // Baselines
    // ------------------------------------------------------------------

    /** Round Robin: task i goes to VM (i mod m). */
    public static int[] roundRobin(int n, int m) {
        int[] map = new int[n];
        for (int i = 0; i < n; i++) map[i] = i % m;
        return map;
    }

    /**
     * FCFS / MCT: tasks are taken in arrival order; each goes to the VM
     * that would finish it earliest (minimum completion time).
     */
    public static int[] mct(double[] len, double[] mips) {
        int n = len.length, m = mips.length;
        int[] map = new int[n];
        double[] ready = new double[m];
        for (int i = 0; i < n; i++) {
            int best = 0;
            double bestCt = Double.MAX_VALUE;
            for (int j = 0; j < m; j++) {
                double ct = ready[j] + len[i] / mips[j];
                if (ct < bestCt) {
                    bestCt = ct;
                    best = j;
                }
            }
            map[i] = best;
            ready[best] = bestCt;
        }
        return map;
    }

    public static int[] minMin(double[] len, double[] mips) {
        return minOrMaxMin(len, mips, false);
    }

    public static int[] maxMin(double[] len, double[] mips) {
        return minOrMaxMin(len, mips, true);
    }

    /**
     * Min-Min: repeatedly pick the unscheduled task whose best completion time
     * is smallest and assign it to that VM.
     * Max-Min: same, but pick the task whose best completion time is largest.
     */
    private static int[] minOrMaxMin(double[] len, double[] mips, boolean max) {
        int n = len.length, m = mips.length;
        int[] map = new int[n];
        boolean[] done = new boolean[n];
        double[] ready = new double[m];
        for (int k = 0; k < n; k++) {
            int selTask = -1, selVm = -1;
            double selCt = max ? -1 : Double.MAX_VALUE;
            for (int i = 0; i < n; i++) {
                if (done[i]) continue;
                int bv = 0;
                double bc = Double.MAX_VALUE;
                for (int j = 0; j < m; j++) {
                    double ct = ready[j] + len[i] / mips[j];
                    if (ct < bc) {
                        bc = ct;
                        bv = j;
                    }
                }
                boolean better = max ? bc > selCt : bc < selCt;
                if (better) {
                    selCt = bc;
                    selTask = i;
                    selVm = bv;
                }
            }
            map[selTask] = selVm;
            done[selTask] = true;
            ready[selVm] = selCt;
        }
        return map;
    }

    public static String describe(int[] map) {
        return Arrays.toString(map);
    }
}
