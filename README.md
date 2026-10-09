# Cloud Task Scheduling Optimization Using a Genetic Algorithm

A Java/CloudSim project comparing task-scheduling strategies across heterogeneous virtual machines. It evaluates a Genetic Algorithm (GA) against Round Robin, Minimum Completion Time (MCT), Min-Min, and Max-Min scheduling.

## Project highlights

- Simulates workloads containing **50, 100, 200, 500, and 1,000 tasks**.
- Compares **five scheduling algorithms**.
- Measures makespan, resource utilization, load imbalance, and scheduling execution time.
- Tracks GA fitness convergence and visualizes per-VM load.
- Includes experiment results and publication-style plots in `results/`.

## Results at a glance

### Makespan vs. number of tasks
![Makespan comparison](results/makespan_vs_tasks.png)

### Resource utilization
![Resource utilization comparison](results/utilization_vs_tasks.png)

### Load imbalance
![Load imbalance comparison](results/imbalance_vs_tasks.png)

### Scheduling algorithm execution time
![Scheduling execution time](results/algo_time_vs_tasks.png)

### Genetic Algorithm convergence
![GA convergence](results/ga_convergence.png)

### Load distribution across virtual machines
![VM load distribution](results/vm_load.png)

## Technology stack

- **Java**
- **CloudSim 3.0.3** (simulation framework, as noted in the source)
- **Python, pandas, Matplotlib** for result analysis and plotting

## Repository structure

```text
.
├── src/
│   ├── GeneticScheduler.java
│   ├── Q8Main.java
│   └── Scheduling.java
├── scripts/
│   └── plot_results.py
├── results/
│   ├── summary_table.csv
│   └── *.png
└── report.tex
```

## Metrics

- **Makespan:** total time until the last task finishes; lower is better.
- **Resource utilization:** average utilization across the simulated VMs; higher is better.
- **Load imbalance:** relative difference between the maximum and minimum VM loads; lower generally indicates more balanced load.
- **Scheduling time:** time taken by the scheduling algorithm; lower is faster.
- **GA convergence:** best fitness found across generations; the fitness function is minimized.

## How to use the result plots

The PNG images are committed in `results/`, so they display directly on GitHub when this README is opened. To regenerate them, run the Java experiment to produce its expected CSV inputs, then run the plotting script from the directory containing those CSV files:

```bash
pip install pandas matplotlib
python scripts/plot_results.py
```

Check the script's expected input CSV filenames and working directory before running it; the supplied archive includes the plotting script and result images, but may not include every raw CSV input required to regenerate all plots.

## Reproducibility note

The experiment configuration and assumptions are documented in `src/Q8Main.java` and `report.tex`. Review these before interpreting results or comparing them with published benchmarks.
