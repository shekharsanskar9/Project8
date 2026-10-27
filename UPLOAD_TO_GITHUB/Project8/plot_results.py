"""
Project8: analysis of the CloudSim results.

Reads the CSV files written by Q8Main and Q8Experiments (in the current directory or the one
given as the first argument) and writes
    figures/*.pdf, figures/*.png   all graphs used in the report
    tables/*.tex                   LaTeX tables included by the report (regenerated, never hand-edited)
    summary_table.csv              mean results per task count and scheduler
    numbers.json                   every number quoted in the report text, for checking

Usage:  pip install pandas matplotlib scipy
        python plot_results.py [results_dir]
"""
import json
import os
import sys

import matplotlib
matplotlib.use("Agg")
import matplotlib.pyplot as plt
import numpy as np
import pandas as pd
from scipy import stats

D = sys.argv[1] if len(sys.argv) > 1 else "."
FIG = os.path.join(D, "figures")
TAB = os.path.join(D, "tables")
os.makedirs(FIG, exist_ok=True)
os.makedirs(TAB, exist_ok=True)

ALGOS = ["RoundRobin", "MCT", "MinMin", "MaxMin", "GA"]
LABEL = {"RoundRobin": "Round Robin", "MCT": "MCT", "MinMin": "Min-Min", "MaxMin": "Max-Min", "GA": "GA",
         "GA-paper": "Paper's GA, its settings", "GA-paper-1/n": "Paper's GA, $m_r = 1/n$",
         "GA-MaxMin-seeded": "GA, Max-Min seeded"}
# categorical slots in a fixed order (validated palette); every series also gets its own marker
COLOR = {"GA": "#2a78d6", "MaxMin": "#eb6834", "MCT": "#1baf7a", "MinMin": "#eda100", "RoundRobin": "#e87ba4",
         "GA-paper": "#eb6834", "GA-paper-1/n": "#1baf7a", "GA-MaxMin-seeded": "#eda100"}
LSTYLE = {"GA": "-", "GA-paper": "--", "GA-paper-1/n": "-.", "GA-MaxMin-seeded": ":"}
MARK = {"GA": "o", "MaxMin": "s", "MCT": "^", "MinMin": "D", "RoundRobin": "v"}
INK, INK2, GRID = "#0b0b0b", "#52514e", "#e4e3df"

plt.rcParams.update({
    "font.size": 10, "axes.titlesize": 11, "axes.labelsize": 10, "legend.fontsize": 9,
    "axes.edgecolor": INK2, "axes.labelcolor": INK, "xtick.color": INK2, "ytick.color": INK2,
    "axes.grid": True, "grid.color": GRID, "grid.linewidth": 0.8, "axes.axisbelow": True,
    "axes.spines.top": False, "axes.spines.right": False, "lines.linewidth": 2, "lines.markersize": 6,
    "legend.frameon": False, "savefig.bbox": "tight", "figure.dpi": 150,
})


def save(fig, name):
    fig.savefig(os.path.join(FIG, name + ".pdf"))
    fig.savefig(os.path.join(FIG, name + ".png"), dpi=200)
    plt.close(fig)


def write(name, text):
    with open(os.path.join(TAB, name + ".tex"), "w") as f:
        f.write(text)


def ci95(x):
    x = np.asarray(x, float)
    return stats.t.ppf(0.975, len(x) - 1) * x.std(ddof=1) / np.sqrt(len(x)) if len(x) > 1 else 0.0


def wilcoxon_p(d):
    d = np.asarray(d, float)
    return float(stats.wilcoxon(d).pvalue) if np.any(np.abs(d) > 1e-12) else 1.0


def sig3(ms):
    """Scheduling time in ms with three significant figures, without scientific notation."""
    if ms >= 100:
        return f"{ms:.0f}"
    s = f"{ms:.3g}"
    if "e" in s:
        s = f"{ms:.6f}".rstrip("0")
    return s


N = {}  # every number quoted in the report text

# =============================================================================================
# Main experiment (Q8Main)
# =============================================================================================
raw = pd.read_csv(os.path.join(D, "raw_results.csv"))
raw["norm_makespan"] = raw.sim_makespan / raw.lower_bound
sizes = sorted(raw.tasks.unique())

g = raw.groupby(["tasks", "algorithm"])
summary = pd.DataFrame({
    "makespan_mean": g.sim_makespan.mean(), "makespan_std": g.sim_makespan.std(),
    "norm_makespan_mean": g.norm_makespan.mean(),
    "util_mean": g.sim_util.mean(), "util_std": g.sim_util.std(),
    "imbalance_mean": g.sim_imbalance.mean(), "imbalance_std": g.sim_imbalance.std(),
    "algo_time_ms_median": g.algo_time_ms.median(),
}).reset_index()
summary["order"] = summary.algorithm.map({a: i for i, a in enumerate(ALGOS)})
summary = summary.sort_values(["tasks", "order"]).drop(columns="order")
summary.to_csv(os.path.join(D, "summary_table.csv"), index=False)
S = summary.set_index(["tasks", "algorithm"])

rows = []
for n in sizes:
    for k, a in enumerate(ALGOS):
        r = S.loc[(n, a)]
        rows.append(f"{n if k == 0 else ''} & {LABEL[a]} & {r.makespan_mean:.2f} $\\pm$ {r.makespan_std:.2f} & "
                    f"{r.norm_makespan_mean:.3f} & {100 * r.util_mean:.1f} & {r.imbalance_mean:.3f} & "
                    f"{sig3(r.algo_time_ms_median)} \\\\")
    if n != sizes[-1]:
        rows.append("\\midrule")
write("main_results", "\n".join(rows) + "\n")

for n in sizes:
    N[f"ga_vs_rr_pct_{n}"] = 100 * (1 - S.loc[(n, "GA")].makespan_mean / S.loc[(n, "RoundRobin")].makespan_mean)
    for a in ALGOS:
        N[f"time_{a}_{n}"] = S.loc[(n, a)].algo_time_ms_median
        N[f"norm_{a}_{n}"] = S.loc[(n, a)].norm_makespan_mean
        N[f"ms_{a}_{n}"] = S.loc[(n, a)].makespan_mean
        N[f"util_{a}_{n}"] = 100 * S.loc[(n, a)].util_mean
        N[f"di_{a}_{n}"] = S.loc[(n, a)].imbalance_mean
        N[f"std_{a}_{n}"] = S.loc[(n, a)].makespan_std
    N[f"ratio_ga_maxmin_time_{n}"] = S.loc[(n, "GA")].algo_time_ms_median / S.loc[(n, "MaxMin")].algo_time_ms_median
    N[f"ga_overhead_pct_{n}"] = 100 * (S.loc[(n, "GA")].algo_time_ms_median / 1000) / S.loc[(n, "GA")].makespan_mean
    N[f"ms_per_task_GA_{n}"] = S.loc[(n, "GA")].makespan_mean / n
    N[f"ms_per_task_RR_{n}"] = S.loc[(n, "RoundRobin")].makespan_mean / n
N["rr_util_range"] = [100 * min(S.loc[(n, "RoundRobin")].util_mean for n in sizes),
                      100 * max(S.loc[(n, "RoundRobin")].util_mean for n in sizes)]
N["ga_util_range"] = [100 * min(S.loc[(n, "GA")].util_mean for n in sizes),
                      100 * max(S.loc[(n, "GA")].util_mean for n in sizes)]
N["ga_di_range"] = [min(S.loc[(n, "GA")].imbalance_mean for n in sizes),
                    max(S.loc[(n, "GA")].imbalance_mean for n in sizes)]
N["rr_di_range"] = [min(S.loc[(n, "RoundRobin")].imbalance_mean for n in sizes),
                    max(S.loc[(n, "RoundRobin")].imbalance_mean for n in sizes)]
N["std_range_nonrr"] = [min(S.loc[(n, a)].makespan_std for n in sizes for a in ALGOS[1:]),
                        max(S.loc[(n, a)].makespan_std for n in sizes for a in ALGOS[1:])]

# verification of the CloudSim execution against the closed-form model
raw["relerr"] = (raw.sim_makespan - raw.pred_makespan).abs() / raw.pred_makespan
N["verif_mean_pct"] = 100 * raw.relerr.mean()
N["verif_max_pct"] = 100 * raw.relerr.max()
N["verif_sim_higher_pct"] = 100 * (raw.sim_makespan > raw.pred_makespan).mean()
N["verif_util_maxabs"] = (raw.sim_util - raw.pred_util).abs().max()

# =============================================================================================
# Paired comparisons: every scheduler sees the same task set in a run
# =============================================================================================
piv = raw.pivot_table(index=["tasks", "run"], columns="algorithm", values="sim_makespan")
rows = []
for base in ["MCT", "MinMin", "MaxMin"]:
    for k, n in enumerate(sizes):
        x = piv.loc[n]
        d = x["GA"] - x[base]
        rel = 100 * d / x[base]
        w, l = int((d < -1e-9).sum()), int((d > 1e-9).sum())
        p = wilcoxon_p(d)
        rows.append(f"{('GA $-$ ' + LABEL[base]) if k == 0 else ''} & {n} & {d.mean():+.2f} $\\pm$ {ci95(d):.2f} & "
                    f"{rel.mean():+.2f} & {w}/{len(d) - w - l}/{l} & {p:.3f} \\\\")
        for key, val in [("mean_s", d.mean()), ("ci", ci95(d)), ("rel_pct", rel.mean()), ("wins", w),
                         ("losses", l), ("p", p)]:
            N[f"pair_{base}_{n}_{key}"] = val
    if base != "MaxMin":
        rows.append("\\midrule")
write("paired", "\n".join(rows) + "\n")


# =============================================================================================
# Figures: main experiment
# =============================================================================================
def lineplot(ax, col, algos=ALGOS, err=True):
    for a in algos:
        sub = raw[raw.algorithm == a].groupby("tasks")[col]
        m, s = sub.mean(), sub.std()
        ax.errorbar(m.index, m.values, yerr=s.values if err else None, color=COLOR[a], marker=MARK[a],
                    label=LABEL[a], capsize=3, elinewidth=1)


def log_x(ax):
    ax.set_xscale("log")
    ax.set_xticks(sizes)
    ax.set_xticklabels([str(s) for s in sizes])
    ax.minorticks_off()


fig, ax = plt.subplots(figsize=(6.4, 3.6))
lineplot(ax, "sim_makespan")
ax.set(xlabel="Number of tasks", ylabel="Makespan (s)", title="Makespan vs number of tasks (mean ± std, 10 runs)")
ax.legend(loc="upper left")
save(fig, "makespan_vs_tasks")

fig, ax = plt.subplots(figsize=(6.4, 3.6))
lineplot(ax, "norm_makespan", algos=["MCT", "MinMin", "MaxMin", "GA"], err=False)
ax.axhline(1.0, color=INK2, lw=1, ls="--")
ax.annotate("perfect balance ($M = M_{LB}$)", (sizes[0], 1.0), xytext=(2, 4), textcoords="offset points",
            ha="left", va="bottom", color=INK2, fontsize=8.5)
log_x(ax)
ax.set(xlabel="Number of tasks (log scale)", ylabel="Makespan / $M_{LB}$",
       title="Makespan relative to the load-balance lower bound (mean, 10 runs)")
ax.legend(loc="upper right")
save(fig, "norm_makespan_vs_tasks")

fig, ax = plt.subplots(figsize=(6.4, 3.6))
lineplot(ax, "sim_util")
ax.set(xlabel="Number of tasks", ylabel="Average utilization $\\bar U$",
       title="Resource utilization vs number of tasks (mean ± std)")
ax.legend(loc="center right")
save(fig, "utilization_vs_tasks")

fig, ax = plt.subplots(figsize=(6.4, 3.6))
lineplot(ax, "sim_imbalance")
ax.set(xlabel="Number of tasks", ylabel="Degree of imbalance DI",
       title="Load imbalance vs number of tasks (mean ± std, lower is better)")
ax.legend(loc="center right")
save(fig, "imbalance_vs_tasks")

fig, ax = plt.subplots(figsize=(6.4, 3.6))
for a in ALGOS:
    m = raw[raw.algorithm == a].groupby("tasks").algo_time_ms.median()
    ax.plot(m.index, m.values, color=COLOR[a], marker=MARK[a], label=LABEL[a])
ax.set_yscale("log")
ax.set(xlabel="Number of tasks", ylabel="Scheduling time (ms, log scale)",
       title="Scheduler execution time (median of 10 runs, after JIT warm-up)")
ax.legend(loc="upper left", bbox_to_anchor=(1.01, 1.0))
save(fig, "algo_time_vs_tasks")

conv = pd.read_csv(os.path.join(D, "convergence.csv"))
fig, ax = plt.subplots(figsize=(6.4, 3.6))
blues = ["#9ec5f0", "#6aa6e6", "#3987e5", "#2a78d6", "#1a4f8f"]   # one hue, light -> dark = size
for i, n in enumerate(sizes):
    c = conv[conv.tasks == n]
    ax.plot(c.generation, c.best_fitness - 0.5, color=blues[i], lw=2, label=f"{n} tasks")
ax.set_yscale("log")
ax.set(xlabel="Generation", ylabel="Best fitness $-$ 0.5 (log scale)",
       title="GA convergence, run 1 of each size (gap to the bound $F \\geq 0.5$)")
ax.legend(loc="upper right", ncol=2)
save(fig, "ga_convergence")
for n in sizes:
    c = conv[conv.tasks == n].set_index("generation").best_fitness
    final = c.iloc[-1]
    better = c[c > final + 1e-12]
    N[f"conv_{n}_g1"] = c.iloc[0]
    N[f"conv_{n}_final"] = final
    N[f"conv_{n}_last_improve_gen"] = int(better.index.max() + 1) if len(better) else 1
    N[f"conv_{n}_g100"] = c.loc[100]
    N[f"conv_{n}_g200"] = c.loc[200]
c100 = conv[conv.tasks == 100].set_index("generation").best_fitness
N["conv_100_first_below_0p52"] = int(c100[c100 < 0.52].index.min())

vm = pd.read_csv(os.path.join(D, "vm_load.csv"))
fig, ax = plt.subplots(figsize=(6.4, 3.6))
vms = sorted(vm.vm.unique())
w = 0.15
for k, a in enumerate(ALGOS):
    s = vm[vm.algorithm == a].set_index("vm").load_seconds.reindex(vms)
    ax.bar(np.arange(len(vms)) + (k - 2) * w, s.values, width=w - 0.02, color=COLOR[a], label=LABEL[a])
ax.set_xticks(np.arange(len(vms)))
ax.set_xticklabels(vms)
ax.set(xlabel="Virtual machine", ylabel="Busy time (s)", title="Busy time per VM (100 tasks, run 1)")
ax.legend(loc="upper left", bbox_to_anchor=(1.01, 1.0))
save(fig, "vm_load")
rr = vm[vm.algorithm == "RoundRobin"].load_seconds
oth = vm[vm.algorithm != "RoundRobin"].load_seconds
N["vm_rr_util"], N["vm_rr_di"] = rr.mean() / rr.max(), (rr.max() - rr.min()) / rr.mean()
N["vm_rr_range"], N["vm_other_range"] = [rr.min(), rr.max()], [oth.min(), oth.max()]

# =============================================================================================
# Additional experiments (Q8Experiments), used when their CSV files exist
# =============================================================================================
p = os.path.join(D, "weights.csv")
if os.path.exists(p):
    wt = pd.read_csv(p)
    wt["norm"] = wt.sim_makespan / wt.lower_bound
    wt["work_ratio"] = wt.total_busy / (wt.lower_bound * 5)   # VM-seconds consumed / minimum possible
    rows = []
    wn = sorted(wt.tasks.unique())
    for n in wn:
        for k, w1 in enumerate(sorted(wt.w1.unique())):
            s = wt[(wt.tasks == n) & (wt.w1 == w1)]
            rows.append(f"{n if k == 0 else ''} & {w1:.2f} & {1 - w1:.2f} & {s.sim_makespan.mean():.2f} & "
                        f"{s.norm.mean():.4f} & {100 * s.sim_util.mean():.2f} & {s.sim_imbalance.mean():.3f} & "
                        f"{s.work_ratio.mean():.4f} \\\\")
            for key, val in [("norm", s.norm.mean()), ("util", 100 * s.sim_util.mean()),
                             ("work", s.work_ratio.mean()), ("ms", s.sim_makespan.mean()),
                             ("di", s.sim_imbalance.mean())]:
                N[f"w_{n}_{w1}_{key}"] = val
        if n != wn[-1]:
            rows.append("\\midrule")
    write("weights", "\n".join(rows) + "\n")

    fig, axs = plt.subplots(1, 2, figsize=(7.0, 3.0))
    for n, col, mk in zip(wn, [COLOR["GA"], COLOR["MaxMin"]], ["o", "s"]):
        m = wt[wt.tasks == n].groupby("w1")
        axs[0].plot(m.norm.mean().index, m.norm.mean().values, marker=mk, color=col, label=f"{n} tasks")
        axs[1].plot(m.sim_util.mean().index, 100 * m.sim_util.mean().values, marker=mk, color=col,
                    label=f"{n} tasks")
    axs[0].set(xlabel="Makespan weight $w_1$  ($w_2 = 1 - w_1$)", ylabel="Makespan / $M_{LB}$", title="Makespan")
    axs[1].set(xlabel="Makespan weight $w_1$  ($w_2 = 1 - w_1$)", ylabel="Utilization (%)", title="Utilization")
    axs[0].set_ylim(1.0, 1.02)       # fixed, honest ranges: the variation is tiny
    axs[1].set_ylim(98.0, 100.0)
    axs[0].legend(loc="upper left")
    fig.tight_layout()
    save(fig, "weights")

p = os.path.join(D, "sensitivity.csv")
if os.path.exists(p):
    se = pd.read_csv(p)
    se["norm"] = se.sim_makespan / se.lower_bound
    mm = raw[raw.algorithm == "MaxMin"].set_index(["tasks", "run"]).sim_makespan
    order = [("default", "default"), ("population", "50"), ("population", "200"), ("generations", "100"),
             ("generations", "1000"), ("crossover", "0.6"), ("crossover", "1.0"),
             ("mutation", "0.005"), ("mutation", "1/n"), ("mutation", "0.05")]
    desc = {("default", "default"): "Default",
            ("population", "50"): "$P = 50$", ("population", "200"): "$P = 200$",
            ("generations", "100"): "$G = 100$", ("generations", "1000"): "$G = 1000$",
            ("crossover", "0.6"): "$p_c = 0.6$", ("crossover", "1.0"): "$p_c = 1.0$",
            ("mutation", "0.005"): "$p_m = 0.005$", ("mutation", "1/n"): "$p_m = 1/n$",
            ("mutation", "0.05"): "$p_m = 0.05$"}
    ns = sorted(se.tasks.unique())
    rows = []
    for key in order:
        cells = [desc[key]]
        for n in ns:
            s = se[(se.tasks == n) & (se.param == key[0]) & (se.value.astype(str) == key[1])].set_index("run")
            ref = mm.loc[n].reindex(s.index)
            gap = 100 * (s.sim_makespan - ref) / ref
            beats = int((s.sim_makespan < ref - 1e-9).sum())
            cells += [f"{s.norm.mean():.4f}", f"{gap.mean():+.2f}", f"{beats}/{len(s)}",
                      f"{s.algo_time_ms.median():.0f}"]
            tag = f"sens_{n}_{key[0]}_{key[1]}"
            N[tag + "_norm"], N[tag + "_gap_pct"] = s.norm.mean(), gap.mean()
            N[tag + "_beats"], N[tag + "_ms"] = beats, s.algo_time_ms.median()
        rows.append(" & ".join(cells) + " \\\\")
        if key[0] == "default":
            rows.append("\\midrule")
    write("sensitivity", "\n".join(rows) + "\n")

p = os.path.join(D, "variants.csv")
if os.path.exists(p):
    va = pd.read_csv(p)
    va["norm"] = va.sim_makespan / va.lower_bound
    rows = []
    for n in sizes:
        mmx = raw[(raw.algorithm == "MaxMin") & (raw.tasks == n)].set_index("run").sim_makespan
        for k, a in enumerate(["GA", "GA-paper", "GA-paper-1/n", "GA-MaxMin-seeded"]):
            if a == "GA":
                s = raw[(raw.algorithm == "GA") & (raw.tasks == n)].set_index("run").rename(
                    columns={"norm_makespan": "norm"})
            else:
                s = va[(va.algorithm == a) & (va.tasks == n)].set_index("run")
            ref = mmx.reindex(s.index)
            d = s.sim_makespan - ref
            w, l = int((d < -1e-9).sum()), int((d > 1e-9).sum())
            rows.append(f"{n if k == 0 else ''} & {LABEL[a]} & {s.sim_makespan.mean():.2f} & {s.norm.mean():.4f} & "
                        f"{100 * s.sim_util.mean():.2f} & {100 * (d / ref).mean():+.3f} & "
                        f"{w}/{len(d) - w - l}/{l} & {s.algo_time_ms.median():.0f} \\\\")
            for key, val in [("norm", s.norm.mean()), ("vs_mm_pct", 100 * (d / ref).mean()), ("wins", w),
                             ("losses", l), ("util", 100 * s.sim_util.mean()), ("ms", s.algo_time_ms.median())]:
                N[f"var_{a}_{n}_{key}"] = val
        if n != sizes[-1]:
            rows.append("\\midrule")
    write("variants", "\n".join(rows) + "\n")

    cv = pd.read_csv(os.path.join(D, "conv_variants.csv"))
    fig, axs = plt.subplots(1, 2, figsize=(7.2, 3.1), sharey=True)
    for ax, n in zip(axs, [100, 1000]):
        c0 = conv[conv.tasks == n]
        ax.plot(c0.generation, c0.best_fitness - 0.5, color=COLOR["GA"], ls=LSTYLE["GA"], label="GA (ours)")
        for a in ["GA-paper", "GA-paper-1/n", "GA-MaxMin-seeded"]:
            c = cv[(cv.tasks == n) & (cv.algorithm == a)]
            ax.plot(c.generation, c.best_fitness - 0.5, color=COLOR[a], ls=LSTYLE[a], label=LABEL[a])
        ax.set_yscale("log")
        ax.set(xlabel="Generation / iteration", title=f"{n} tasks, run 1")
    axs[0].set_ylabel("Best fitness $-$ 0.5 (log scale)")
    axs[1].legend(loc="upper left", bbox_to_anchor=(1.02, 1.0))
    fig.tight_layout()
    save(fig, "ga_variants_convergence")
    for a in ["GA-paper", "GA-paper-1/n", "GA-MaxMin-seeded"]:
        for n in [100, 1000]:
            c = cv[(cv.tasks == n) & (cv.algorithm == a)].set_index("generation").best_fitness
            N[f"conv_var_{a}_{n}_g1"], N[f"conv_var_{a}_{n}_final"] = c.iloc[0], c.iloc[-1]
            N[f"conv_var_{a}_{n}_g90"] = c.loc[90] if 90 in c.index else None

p = os.path.join(D, "multipe.csv")
if os.path.exists(p):
    mp = pd.read_csv(p)
    mp["rel"] = 100 * (mp.sim_makespan_multi_pe - mp.sim_makespan_single_pe) / mp.sim_makespan_single_pe
    rows = []
    for n in sizes:
        cells = [str(n)]
        for a in ALGOS:
            s = mp[(mp.tasks == n) & (mp.algorithm == a)]
            cells.append(f"{s.rel.mean():+.2f}")
            N[f"mpe_{a}_{n}"] = s.rel.mean()
        rows.append(" & ".join(cells) + " \\\\")
    write("multipe", "\n".join(rows) + "\n")
    rk1 = mp.groupby(["tasks", "algorithm"]).sim_makespan_single_pe.mean().unstack()
    rk2 = mp.groupby(["tasks", "algorithm"]).sim_makespan_multi_pe.mean().unstack()
    N["mpe_rank_single"] = {str(n): list(rk1.loc[n].sort_values().index) for n in sizes}
    N["mpe_rank_multi"] = {str(n): list(rk2.loc[n].sort_values().index) for n in sizes}
    N["mpe_abs_max"] = mp.rel.abs().max()

with open(os.path.join(D, "numbers.json"), "w") as f:
    json.dump(N, f, indent=1, default=lambda o: o.item() if hasattr(o, "item") else str(o))
print("wrote figures/, tables/, summary_table.csv, numbers.json")
