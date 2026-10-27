# Cloud Task Scheduling Optimization Using a Genetic Algorithm

A CloudSim 5.0 implementation of **joint optimization of resource utilization and makespan** using a Genetic Algorithm (GA), reproducing Dehury, Kumar and Kumar, *"Joint optimization of resource utilization and makespan in cloud computing using genetic algorithm"*, Discover Computing 29:315 (2026), [doi:10.1007/s10791-026-10167-0](https://doi.org/10.1007/s10791-026-10167-0).

Cloud Computing Lab (IT348) project, B.Tech Computer Science and Engineering, Birla Institute of Technology, Mesra.
Sanskar Shekhar  and Aman Nautiyal .



## Project Highlights

* Compares five scheduling strategies: **Genetic Algorithm (GA), Round Robin, Minimum Completion Time (MCT), Min-Min, and Max-Min**.
* Evaluates workloads of **50, 100, 200, 500, and 1,000 tasks**.
* Measures makespan, resource utilization, load imbalance, and algorithm execution time.
* Visualizes GA convergence and task-load distribution across virtual machines.
* Paired statistics (Wilcoxon signed-rank tests) on the same task sets for every scheduler.
* Additional studies: fitness-weight sweep, GA parameter sensitivity, the paper's own GA (Algorithm 1) reproduced exactly, a Max-Min-seeded GA, and a multi-PE VM check.
* Every mapping is executed in CloudSim and checked against the closed-form model (0.46% average difference).
* Includes Java source code, experiment data, result plots and the full LaTeX report.

## Results and Visualizations

### 1. Makespan vs. Number of Tasks

![Makespan vs. Number of Tasks](https://raw.githubusercontent.com/shekharsanskar9/Project8/main/Project8/results/figures/makespan_vs_tasks.png)

### 2. Resource Utilization vs. Number of Tasks

![Resource Utilization vs. Number of Tasks](https://raw.githubusercontent.com/shekharsanskar9/Project8/main/Project8/results/figures/utilization_vs_tasks.png)

### 3. Load Imbalance vs. Number of Tasks

![Load Imbalance vs. Number of Tasks](https://raw.githubusercontent.com/shekharsanskar9/Project8/main/Project8/results/figures/imbalance_vs_tasks.png)

### 4. Scheduling Algorithm Execution Time

![Scheduling Algorithm Execution Time](https://raw.githubusercontent.com/shekharsanskar9/Project8/main/Project8/results/figures/algo_time_vs_tasks.png)

### 5. Genetic Algorithm Convergence

![Genetic Algorithm Convergence](https://raw.githubusercontent.com/shekharsanskar9/Project8/main/Project8/results/figures/ga_convergence.png)

### 6. Load Distribution Across Virtual Machines

![Load Distribution Across Virtual Machines](https://raw.githubusercontent.com/shekharsanskar9/Project8/main/Project8/results/figures/vm_load.png)

## Technology Stack

* **Java** — scheduling algorithms and simulation logic
* **CloudSim** — cloud computing simulation framework
* **Python** — experiment-result processing
* **pandas** — data analysis
* **Matplotlib** — visualization

## Repository Contents

* [Project Repository](https://github.com/shekharsanskar9/Project8)
* [Project Source Folder](https://github.com/shekharsanskar9/Project8/tree/main/Project8/src)
* [Plotting Script](https://github.com/shekharsanskar9/Project8/blob/main/Project8/plot_results.py)
* [Report (PDF)](https://github.com/shekharsanskar9/Project8/blob/main/Project8/report.pdf) and [LaTeX source](https://github.com/shekharsanskar9/Project8/blob/main/Project8/report.tex)
* [Results folder (CSV files, figures, tables)](https://github.com/shekharsanskar9/Project8/tree/main/Project8/results)

## Evaluation Metrics

* **Makespan:** Total time required to complete all scheduled tasks. Lower is generally better.
* **Resource Utilization:** Measures how effectively available VM resources are used.
* **Load Imbalance:** Measures how unevenly tasks are distributed across VMs. Lower generally indicates better balance.
* **Algorithm Execution Time:** Measures the time required by a scheduling algorithm to generate a schedule.
* **GA Convergence:** Tracks how the best fitness value changes over successive generations.
* **VM Load Distribution:** Shows the amount of work assigned to each virtual machine.

## Running the Project

### Java simulation (CloudSim 5.0, Java 8 or later)

1. Add CloudSim 5.0 (and its dependency commons-math3 3.4.1) to the project, as in the lab setup.
2. Run `Q8Main` (main experiment). It writes `raw_results.csv`, `convergence.csv`, `vm_load.csv` and `environment.txt`.
3. Run `Q8Experiments all` (additional studies). It writes `weights.csv`, `sensitivity.csv`, `variants.csv`, `conv_variants.csv` and `multipe.csv`. The variants study includes `PaperGA`, the GA of Dehury et al. implemented exactly as their Algorithm 1.
4. Compile the report with `pdflatex report.tex` (twice); it reads its figures and tables from `results/`. The cover page is `cover.tex`.

All task sets use the seed `42 + 1000*run + n`, so every result is reproducible and every scheduler sees the same tasks within a run.

### Tables, figures and statistics

```bash
pip install pandas matplotlib scipy
python plot_results.py results
```

This reads the CSV files in `results/` and writes `results/figures/`, `results/tables/` (LaTeX rows used by the report), `results/summary_table.csv` and `results/numbers.json`.

## Notes

* Image links use the `main` branch and the `Project8/` subdirectory.
* The PNG files must be committed to the repository at those exact paths for the images to render.
* Filenames and capitalization must match exactly.
* Experimental results may vary depending on the workload, VM configuration, and algorithm parameters.
