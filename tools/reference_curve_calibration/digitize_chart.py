"""Digitizes one photographed manufacturer reference chart (Dräger/Gastec/Sensidyne/Kitagawa
color-vs-ppm chart, shot with an X-Rite ColorChecker Classic in frame) into (od, ppm, source)
rows, and appends them to data/reference_curve_master.csv.

See README.md for the capture protocol and the two manifest CSVs this script expects.

Usage:
    python digitize_chart.py \\
        --image charts/drager_h2s_lot4021.jpg \\
        --colorchecker-points charts/drager_h2s_lot4021_colorchecker.csv \\
        --swatch-points charts/drager_h2s_lot4021_swatches.csv \\
        --source-tag drager-h2s-lot4021
"""
from __future__ import annotations

import argparse
import csv
import sys
from pathlib import Path

import numpy as np

from color_science import (
    apply_color_correction,
    fit_color_correction_matrix,
    optical_density,
    sample_patch,
)

MASTER_CSV_COLUMNS = ["od", "ppm", "source"]


def load_image_rgb(path: Path) -> np.ndarray:
    import cv2  # local import: only needed by this script, not by export/evaluate

    bgr = cv2.imread(str(path))
    if bgr is None:
        raise FileNotFoundError(f"Could not read image: {path}")
    return cv2.cvtColor(bgr, cv2.COLOR_BGR2RGB)


def read_colorchecker_points(path: Path) -> list[tuple[int, int, int]]:
    """CSV columns: patch_index,x,y (patch_index 0-23, matches COLORCHECKER_SRGB row order)."""
    rows = []
    with open(path, newline="") as f:
        for row in csv.DictReader(f):
            rows.append((int(row["patch_index"]), int(row["x"]), int(row["y"])))
    rows.sort(key=lambda r: r[0])
    if [r[0] for r in rows] != list(range(24)):
        raise ValueError(f"{path} must define all 24 ColorChecker patch_index values (0-23)")
    return rows


def read_swatch_points(path: Path) -> list[dict]:
    """CSV columns: swatch_id,printed_ppm,x,y,is_blank (is_blank: 1 for the 0-ppm/unexposed
    reference swatch, 0 otherwise)."""
    rows = []
    with open(path, newline="") as f:
        for row in csv.DictReader(f):
            rows.append(
                {
                    "swatch_id": row["swatch_id"],
                    "printed_ppm": float(row["printed_ppm"]),
                    "x": int(row["x"]),
                    "y": int(row["y"]),
                    "is_blank": row["is_blank"].strip() in ("1", "true", "True"),
                },
            )
    blanks = [r for r in rows if r["is_blank"]]
    if len(blanks) != 1:
        raise ValueError(f"{path} must mark exactly one swatch with is_blank=1 (found {len(blanks)})")
    return rows


def digitize(image_path: Path, colorchecker_csv: Path, swatch_csv: Path, source_tag: str, patch_radius: int) -> list[dict]:
    image = load_image_rgb(image_path)

    checker_points = read_colorchecker_points(colorchecker_csv)
    photographed_patches = np.array(
        [sample_patch(image, x, y, radius_px=patch_radius).reshape(-1, 3).mean(axis=0) for _, x, y in checker_points],
    )
    matrix = fit_color_correction_matrix(photographed_patches)
    corrected = apply_color_correction(image, matrix)

    swatches = read_swatch_points(swatch_csv)
    blank = next(s for s in swatches if s["is_blank"])
    blank_patch = sample_patch(corrected, blank["x"], blank["y"], radius_px=patch_radius)

    rows = []
    for swatch in swatches:
        if swatch["is_blank"]:
            continue
        sample = sample_patch(corrected, swatch["x"], swatch["y"], radius_px=patch_radius)
        od = optical_density(blank_patch, sample)
        rows.append(
            {
                "od": round(od, 4),
                "ppm": swatch["printed_ppm"],
                "source": f"{source_tag}-{swatch['swatch_id']}",
            },
        )
    return rows


def append_to_master_csv(rows: list[dict], master_csv: Path) -> None:
    master_csv.parent.mkdir(parents=True, exist_ok=True)
    file_exists = master_csv.exists()
    with open(master_csv, "a", newline="") as f:
        writer = csv.DictWriter(f, fieldnames=MASTER_CSV_COLUMNS)
        if not file_exists:
            writer.writeheader()
        writer.writerows(rows)


def main() -> None:
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument("--image", required=True, type=Path)
    parser.add_argument("--colorchecker-points", required=True, type=Path)
    parser.add_argument("--swatch-points", required=True, type=Path)
    parser.add_argument("--source-tag", required=True, help="e.g. drager-h2s-lot4021")
    parser.add_argument("--patch-radius", type=int, default=8, help="ROI half-width in pixels, mirrors PatchSampler")
    parser.add_argument(
        "--master-csv",
        type=Path,
        default=Path(__file__).parent / "data" / "reference_curve_master.csv",
    )
    args = parser.parse_args()

    rows = digitize(args.image, args.colorchecker_points, args.swatch_points, args.source_tag, args.patch_radius)
    append_to_master_csv(rows, args.master_csv)

    print(f"Appended {len(rows)} rows to {args.master_csv}:")
    for row in rows:
        print(f"  od={row['od']:.4f}  ppm={row['ppm']}  source={row['source']}")


if __name__ == "__main__":
    try:
        main()
    except Exception as exc:  # surface a clean message instead of a traceback for CLI use
        print(f"Error: {exc}", file=sys.stderr)
        sys.exit(1)
