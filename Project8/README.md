# Cloud Task Scheduling Optimization Using a Genetic Algorithm

A Java-based cloud task scheduling simulation that compares a Genetic Algorithm (GA) with traditional scheduling strategies across virtual machines (VMs). The project evaluates scheduling performance using experimental results and visualizations.

## Project Highlights

* Compares five scheduling strategies: **Genetic Algorithm (GA), Round Robin, Minimum Completion Time (MCT), Min-Min, and Max-Min**.
* Evaluates workloads of **50, 100, 200, 500, and 1,000 tasks**.
* Measures makespan, resource utilization, load imbalance, and algorithm execution time.
* Visualizes GA convergence and task-load distribution across virtual machines.
* Includes Java source code, experiment data, and result plots.

## Results and Visualizations

### 1. Makespan vs. Number of Tasks

![Makespan vs. Number of Tasks](https://raw.githubusercontent.com/shekharsanskar9/Project8/main/Project8/makespan_vs_tasks.png)

### 2. Resource Utilization vs. Number of Tasks

![Resource Utilization vs. Number of Tasks](https://raw.githubusercontent.com/shekharsanskar9/Project8/main/Project8/utilization_vs_tasks.png)

### 3. Load Imbalance vs. Number of Tasks

![Load Imbalance vs. Number of Tasks](https://raw.githubusercontent.com/shekharsanskar9/Project8/main/Project8/imbalance_vs_tasks.png)

### 4. Scheduling Algorithm Execution Time

![Scheduling Algorithm Execution Time](https://raw.githubusercontent.com/shekharsanskar9/Project8/main/Project8/algo_time_vs_tasks.png)

### 5. Genetic Algorithm Convergence

![Genetic Algorithm Convergence](https://raw.githubusercontent.com/shekharsanskar9/Project8/main/Project8/ga_convergence.png)

### 6. Load Distribution Across Virtual Machines

![Load Distribution Across Virtual Machines](https://raw.githubusercontent.com/shekharsanskar9/Project8/main/Project8/vm_load.png)

## Technology Stack

* **Java** — scheduling algorithms and simulation logic
* **CloudSim** — cloud computing simulation framework
* **Python** — experiment-result processing
* **pandas** — data analysis
* **Matplotlib** — visualization

## Repository Contents

* [Project Repository](https://github.com/shekharsanskar9/Project8)
* [Project Source Folder](https://github.com/shekharsanskar9/Project8/tree/main/Project8/src)
* [Plotting Script](https://github.com/shekharsanskar9/Project8/blob/main/Project8/scripts/plot_results.py)
* [Summary Results CSV](https://github.com/shekharsanskar9/Project8/blob/main/Project8/summary_table.csv)
* [Raw Experiment Results CSV](https://github.com/shekharsanskar9/Project8/blob/main/Project8/raw_results.csv)
* [GA Convergence Data CSV](https://github.com/shekharsanskar9/Project8/blob/main/Project8/convergence.csv)
* [VM Load Data CSV](https://github.com/shekharsanskar9/Project8/blob/main/Project8/vm_load.csv)

## Evaluation Metrics

* **Makespan:** Total time required to complete all scheduled tasks. Lower is generally better.
* **Resource Utilization:** Measures how effectively available VM resources are used.
* **Load Imbalance:** Measures how unevenly tasks are distributed across VMs. Lower generally indicates better balance.
* **Algorithm Execution Time:** Measures the time required by a scheduling algorithm to generate a schedule.
* **GA Convergence:** Tracks how the best fitness value changes over successive generations.
* **VM Load Distribution:** Shows the amount of work assigned to each virtual machine.

## Running the Project

### Java Simulation

1. Clone or download the repository.
2. Open the Java project in your IDE.
3. Configure the required CloudSim libraries.
4. Run the main Java class in the source folder.

### Generate the Result Plots

Install the required Python libraries:

```bash
pip install pandas matplotlib
```

Run the plotting script from the appropriate project directory:

```bash
python scripts/plot_results.py
```

Check the script for the expected CSV filenames and working directory.

## Notes

* Image links use the `main` branch and the `Project8/` subdirectory.
* The PNG files must be committed to the repository at those exact paths for the images to render.
* Filenames and capitalization must match exactly.
* Experimental results may vary depending on the workload, VM configuration, and algorithm parameters.
