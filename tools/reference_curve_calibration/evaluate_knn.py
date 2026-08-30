"""Leave-one-out cross-validation of the k-NN od->ppm regression over
data/reference_curve_master.csv, to pick a good k before exporting to the app.

Usage:
    python evaluate_knn.py [--master-csv data/reference_curve_master.csv] [--k-min 3] [--k-max 7]

Writes reports/knn_cv_report.txt and reports/knn_curve.png.
"""
from __future__ import annotations

import argparse
import csv
from pathlib import Path

import matplotlib

matplotlib.use("Agg")  # headless: this runs from the CLI, not a notebook
import matplotlib.pyplot as plt
import numpy as np

from od_knn import KnnPrediction, ReferencePoint, predict


def load_master_csv(path: Path) -> list[ReferencePoint]:
    with open(path, newline="") as f:
        rows = [ReferencePoint(od=float(r["od"]), ppm=float(r["ppm"]), source=r["source"]) for r in csv.DictReader(f)]
    if not rows:
        raise ValueError(f"{path} has no data rows")
    return rows


def leave_one_out_errors(table: list[ReferencePoint], k: int) -> np.ndarray:
    errors = []
    for i, held_out in enumerate(table):
        remaining = table[:i] + table[i + 1 :]
        prediction: KnnPrediction = predict(remaining, held_out.od, k=k)
        errors.append(prediction.ppm - held_out.ppm)
    return np.array(errors)


def check_monotonicity(table: list[ReferencePoint]) -> list[str]:
    """OD should increase monotonically with ppm (darker stain = more exposure). A violation
    usually means a mislabeled swatch or a bad chart photo, not real signal - flag it."""
    warnings = []
    ordered = sorted(table, key=lambda p: p.od)
    for a, b in zip(ordered, ordered[1:]):
        if b.ppm < a.ppm - 1e-6:
            warnings.append(
                f"non-monotonic: od={a.od:.4f} (ppm={a.ppm}, {a.source}) precedes "
                f"od={b.od:.4f} (ppm={b.ppm}, {b.source}) but ppm decreases",
            )
    return warnings


def main() -> None:
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    here = Path(__file__).parent
    parser.add_argument("--master-csv", type=Path, default=here / "data" / "reference_curve_master.csv")
    parser.add_argument("--k-min", type=int, default=3)
    parser.add_argument("--k-max", type=int, default=7)
    parser.add_argument("--reports-dir", type=Path, default=here / "reports")
    args = parser.parse_args()

    table = load_master_csv(args.master_csv)
    args.reports_dir.mkdir(parents=True, exist_ok=True)

    lines = [f"Loaded {len(table)} reference points from {args.master_csv}", ""]

    monotonicity_warnings = check_monotonicity(table)
    if monotonicity_warnings:
        lines.append(f"WARNING: {len(monotonicity_warnings)} monotonicity violation(s):")
        lines.extend(f"  - {w}" for w in monotonicity_warnings)
    else:
        lines.append("Monotonicity check: OK (od increases with ppm throughout)")
    lines.append("")

    lines.append("Leave-one-out cross-validation (predict each point's ppm from all others):")
    lines.append(f"{'k':>3}  {'MAE':>10}  {'RMSE':>10}  {'max abs err':>12}")
    best_k, best_mae = None, float("inf")
    for k in range(args.k_min, args.k_max + 1):
        errors = leave_one_out_errors(table, k)
        mae = np.mean(np.abs(errors))
        rmse = np.sqrt(np.mean(errors ** 2))
        max_abs = np.max(np.abs(errors))
        lines.append(f"{k:>3}  {mae:>10.3f}  {rmse:>10.3f}  {max_abs:>12.3f}")
        if mae < best_mae:
            best_k, best_mae = k, mae

    lines.append("")
    lines.append(f"Recommended k: {best_k} (lowest MAE = {best_mae:.3f}). Set Constants.KNN_K to this value.")

    report_path = args.reports_dir / "knn_cv_report.txt"
    report_path.write_text("\n".join(lines) + "\n")
    print("\n".join(lines))
    print(f"\nWrote {report_path}")

    ods = np.array([p.od for p in table])
    ppms = np.array([p.ppm for p in table])
    order = np.argsort(ods)

    fig, (curve_ax, residual_ax) = plt.subplots(2, 1, figsize=(8, 8), sharex=True)
    curve_ax.scatter(ods[order], ppms[order], s=20, label="reference anchors")
    curve_ax.set_ylabel("ppm-hours")
    curve_ax.set_title(f"od -> ppm reference curve ({len(table)} points)")
    curve_ax.legend()

    best_errors = leave_one_out_errors(table, best_k)
    residual_ax.scatter(ods, best_errors, s=20, color="tab:red")
    residual_ax.axhline(0.0, color="black", linewidth=0.8)
    residual_ax.set_xlabel("optical density")
    residual_ax.set_ylabel(f"LOO residual (k={best_k})")

    fig.tight_layout()
    plot_path = args.reports_dir / "knn_curve.png"
    fig.savefig(plot_path, dpi=150)
    print(f"Wrote {plot_path}")


if __name__ == "__main__":
    main()
