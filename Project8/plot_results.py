"""
Builds the summary table and graphs for the Question 8 report.

Run after Q8Main has produced raw_results.csv, convergence.csv and vm_load.csv:
    pip install pandas matplotlib
    python plot_results.py
"""
import os

import matplotlib.pyplot as plt
import pandas as pd

ORDER = ["RoundRobin", "MCT", "MinMin", "MaxMin", "GA"]

raw = pd.read_csv("raw_results.csv")

summary = (
    raw.groupby(["tasks", "algorithm"])
    .agg(
        makespan_mean=("sim_makespan", "mean"),
        makespan_std=("sim_makespan", "std"),
        util_mean=("sim_util", "mean"),
        util_std=("sim_util", "std"),
        imbalance_mean=("sim_imbalance", "mean"),
        imbalance_std=("sim_imbalance", "std"),
        algo_time_ms_mean=("algo_time_ms", "mean"),
    )
    .reset_index()
)
summary.to_csv("summary_table.csv", index=False)
print(summary.round(3).to_string(index=False))

# validation: formula vs CloudSim
rel_err = ((raw["sim_makespan"] - raw["pred_makespan"]).abs() / raw["pred_makespan"]) * 100
print(f"\nMakespan formula vs CloudSim: mean diff {rel_err.mean():.4f}%, max {rel_err.max():.4f}%")


def line_plot(mean_col, std_col, ylabel, title, fname, logy=False):
    plt.figure(figsize=(7, 4.5))
    for algo in ORDER:
        d = summary[summary["algorithm"] == algo].sort_values("tasks")
        plt.errorbar(d["tasks"], d[mean_col], yerr=d[std_col] if std_col else None,
                     marker="o", capsize=3, label=algo)
    plt.xlabel("Number of tasks")
    plt.ylabel(ylabel)
    plt.title(title)
    if logy:
        plt.yscale("log")
    plt.grid(alpha=0.3)
    plt.legend()
    plt.tight_layout()
    plt.savefig(fname, dpi=200)
    plt.close()


line_plot("makespan_mean", "makespan_std", "Makespan (s)",
          "Makespan vs number of tasks (mean ± std, 10 runs)", "makespan_vs_tasks.png")
line_plot("util_mean", "util_std", "Resource utilization",
          "Resource utilization vs number of tasks", "utilization_vs_tasks.png")
line_plot("imbalance_mean", "imbalance_std", "Degree of imbalance",
          "Load imbalance vs number of tasks", "imbalance_vs_tasks.png")
line_plot("algo_time_ms_mean", None, "Scheduling time (ms, log scale)",
          "Algorithm execution time vs number of tasks", "algo_time_vs_tasks.png", logy=True)

# GA convergence
if os.path.exists("convergence.csv"):
    c = pd.read_csv("convergence.csv")
    plt.figure(figsize=(7, 4.5))
    plt.plot(c["generation"], c["best_fitness"])
    plt.xlabel("Generation")
    plt.ylabel("Best fitness (lower is better)")
    plt.title("GA convergence (100 tasks, run 1)")
    plt.grid(alpha=0.3)
    plt.tight_layout()
    plt.savefig("ga_convergence.png", dpi=200)
    plt.close()

# per-VM load for the detailed run
if os.path.exists("vm_load.csv"):
    v = pd.read_csv("vm_load.csv")
    pivot = v.pivot(index="vm", columns="algorithm", values="load_seconds")[ORDER]
    pivot.plot(kind="bar", figsize=(8, 4.5))
    plt.xlabel("Virtual machine")
    plt.ylabel("Busy time (s)")
    plt.title("Load per VM (100 tasks, run 1)")
    plt.xticks(rotation=0)
    plt.grid(axis="y", alpha=0.3)
    plt.tight_layout()
    plt.savefig("vm_load.png", dpi=200)
    plt.close()

print("\nSaved summary_table.csv and PNG graphs.")
