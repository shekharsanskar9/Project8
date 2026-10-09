# Cloud Task Scheduling Optimization Using a Genetic Algorithm

A Java-based cloud task scheduling simulation that compares a Genetic Algorithm (GA) with traditional scheduling approaches across virtual machines (VMs). The project evaluates scheduling performance using experiment results and visualizations.

## Project Highlights

- Compares five scheduling strategies: **Genetic Algorithm (GA), Round Robin, Minimum Completion Time (MCT), Min-Min, and Max-Min**.
- Evaluates workloads with different numbers of tasks, including **50, 100, 200, 500, and 1,000 tasks**.
- Measures makespan, resource utilization, load imbalance, and algorithm execution time.
- Visualizes GA convergence and task-load distribution across virtual machines.
- Includes Java source code, experiment data, result plots, and a project report.

## Results at a Glance

> The plot images below are stored in the same directory as this README, so the paths intentionally do **not** include `results/`.

### Makespan vs. Number of Tasks
![Makespan vs. number of tasks](makespan_vs_tasks.png)

### Resource Utilization
![Resource utilization](utilization_vs_tasks.png)

### Load Imbalance
![Load imbalance](imbalance_vs_tasks.png)

### Scheduling Algorithm Execution Time
![Scheduling algorithm execution time](algo_time_vs_tasks.png)

### Genetic Algorithm Convergence
![Genetic Algorithm convergence](ga_convergence.png)

### Load Distribution Across Virtual Machines
![Load distribution across virtual machines](vm_load.png)

## Technology Stack

- **Java** — scheduling algorithms and simulation logic
- **CloudSim** — cloud computing simulation framework (check the project configuration for the exact version)
- **Python, pandas, Matplotlib** — experiment-result processing and plotting

## Repository Structure

The current GitHub folder layout places the PNG plots and CSV result files directly beside this README:

```text
Project8/
├── README.md
├── src/
│   ├── GeneticScheduler.java
│   ├── Q8Main.java
│   └── Scheduling.java
├── scripts/
│   └── plot_results.py
├── makespan_vs_tasks.png
├── utilization_vs_tasks.png
├── imbalance_vs_tasks.png
├── algo_time_vs_tasks.png
├── ga_convergence.png
├── vm_load.png
├── summary_table.csv
├── raw_results.csv
├── convergence.csv
└── vm_load.csv
```

## Evaluation Metrics

- **Makespan:** total time required to complete all scheduled tasks. Lower is generally better.
- **Resource utilization:** how effectively available VM resources are used. Higher is generally better, provided the workload is completed correctly.
- **Load imbalance:** how unevenly work is distributed among VMs. Lower generally indicates a more balanced allocation.
- **Scheduling execution time:** time spent by the scheduling algorithm to generate a schedule. Lower is faster.
- **GA convergence:** how the best fitness value changes over generations.
- **VM load distribution:** the amount of work assigned to each virtual machine.

## Running the Project

Open the project in your Java IDE and run the appropriate main class (see `src/Q8Main.java` and the project configuration). Ensure the required CloudSim libraries are configured in the build path.

To regenerate plots, install the Python dependencies:

```bash
pip install pandas matplotlib
```

Then run the plotting script from the directory expected by the script:

```bash
python scripts/plot_results.py
```

Check `scripts/plot_results.py` for the expected CSV filenames and working directory before running it, because the script must be able to find its input data files.

## Project Report

See [`report.tex`](report.tex) for the detailed project report and experiment discussion.

---

*Note: Results depend on the workload, VM configuration, and simulation parameters. Review the experiment setup before drawing performance conclusions.*
