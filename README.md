# Genetic Algorithm Scheduler for Cloud Computing (CloudSim)

A CloudSim implementation of **joint optimization of resource utilization and makespan**
in cloud computing using a Genetic Algorithm, based on Dehury, Kumar & Kumar. A GA-based
task scheduler is compared against four baseline schedulers, and every result is validated
by executing the mapping inside CloudSim.

## Problem

Given *n* independent tasks (lengths in Million Instructions) and *m* heterogeneous VMs,
assign each task to a VM so as to minimize **makespan** while maximizing **resource
utilization**. A schedule is an `int[]` mapping where `map[i]` is the VM index for task *i*.

Model (see [`src/Scheduling.java`](Project8/src/Scheduling.java)):

```
ET(i,j)     = L_i / MIPS_j
Load_j      = sum of ET(i,j) over tasks assigned to VM j
Makespan    = max_j Load_j
Utilization = sum_j Load_j / (m * Makespan)
Imbalance   = (max Load - min Load) / avg Load
```

## Algorithms

| Algorithm    | Description |
|--------------|-------------|
| `RoundRobin` | Task *i* → VM `i mod m` |
| `MCT`        | Minimum Completion Time, tasks taken in arrival order |
| `MinMin`     | Repeatedly schedule the task with the smallest best completion time |
| `MaxMin`     | Same as MinMin but picks the largest best completion time |
| `GA`         | Genetic algorithm (the proposed method) |

### Genetic algorithm ([`src/GeneticScheduler.java`](Project8/src/GeneticScheduler.java))

- **Chromosome:** `int[n]`, gene *i* = VM assigned to task *i*
- **Fitness (minimized):** `F = w1 * (Makespan / LowerBound) + w2 * (1 - Utilization)`,
  where `LowerBound = sum(L_i) / sum(MIPS_j)` is the ideal perfectly-balanced makespan
- **Selection:** tournament (k = 3)
- **Crossover:** two-point
- **Mutation:** per-gene reassignment with probability `mutationRate`
- **Elitism:** best `elite` chromosomes copied unchanged each generation

Default GA parameters (in [`src/Q8Main.java`](Project8/src/Q8Main.java)): population 100,
300 generations, crossover 0.8, mutation 0.02, elite 2, `w1 = w2 = 0.5`.

## Experiment setup

- **VMs:** 5 heterogeneous VMs (cores `{4,2,8,6,2}`, 1000 MIPS/core, space-shared queue)
- **Workload:** task lengths ~ Uniform(1000, 20000) MI, all arriving at *t = 0* (batch)
- **Task counts:** 50, 100, 200, 500, 1000
- **Runs:** 10 independent seeded runs per task count (GA is stochastic)
- **Validation:** for each run the formula makespan is compared against the makespan
  measured by CloudSim; the maximum relative difference is reported

## Project layout

```
Project8/
├── src/
│   ├── Q8Main.java          # experiment driver + CloudSim simulation
│   ├── GeneticScheduler.java# the genetic algorithm
│   └── Scheduling.java      # metrics + baseline schedulers
├── plot_results.py          # builds summary table + graphs from the CSVs
├── raw_results.csv          # per-run results (generated)
├── convergence.csv          # GA best-fitness per generation (generated)
└── vm_load.csv              # per-VM busy time for the detailed run (generated)
```

> The CSV files are sample output from a previous run; rerunning `Q8Main` overwrites them.

## Requirements

- **Java 8** (JavaSE-1.8)
- **CloudSim 3.0.3** (`org.cloudbus.cloudsim`) on the classpath
- **commons-math3 3.4.1** (a CloudSim dependency)
- **Python 3** with `pandas` and `matplotlib` for the plots

## Build & run

The project is set up as an Eclipse project (`.classpath`, `.project`). Add CloudSim 3.0.3
and `commons-math3-3.4.1.jar` to the classpath, then:

```bash
# from the Project8/ source folder, with CloudSim + commons-math3 on the classpath:
javac -cp "cloudsim-3.0.3.jar:commons-math3-3.4.1.jar" -d bin src/*.java
java  -cp "bin:cloudsim-3.0.3.jar:commons-math3-3.4.1.jar" Q8Main
```

Running `Q8Main` prints a summary table and writes `raw_results.csv`,
`convergence.csv` and `vm_load.csv`.

## Plots

```bash
pip install pandas matplotlib
python plot_results.py
```

This writes `summary_table.csv` and these graphs: makespan, utilization, imbalance and
scheduling time vs. number of tasks, GA convergence, and per-VM load.
