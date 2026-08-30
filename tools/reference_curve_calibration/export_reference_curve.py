"""Validates data/reference_curve_master.csv and writes it out as
app/src/main/assets/reference_curve.json, in the schema ReferenceCurveLoader.kt expects.

Usage:
    python export_reference_curve.py [--master-csv data/reference_curve_master.csv]

Run evaluate_knn.py first to review CV error and monotonicity warnings - this script only
checks for hard errors (NaN/inf, duplicate od, empty file), not curve quality.
"""
from __future__ import annotations

import argparse
import csv
import json
import math
from pathlib import Path

DEFAULT_ASSET_PATH = Path(__file__).parents[2] / "app" / "src" / "main" / "assets" / "reference_curve.json"


def load_and_validate(master_csv: Path) -> list[dict]:
    with open(master_csv, newline="") as f:
        rows = list(csv.DictReader(f))

    if not rows:
        raise ValueError(f"{master_csv} has no data rows")

    parsed = []
    seen_od = set()
    for i, row in enumerate(rows):
        od = float(row["od"])
        ppm = float(row["ppm"])
        source = row["source"]

        if math.isnan(od) or math.isinf(od) or math.isnan(ppm) or math.isinf(ppm):
            raise ValueError(f"row {i} ({source}) has a NaN/inf value: od={od}, ppm={ppm}")
        if ppm < 0:
            raise ValueError(f"row {i} ({source}) has a negative ppm: {ppm}")

        rounded_od = round(od, 4)
        if rounded_od in seen_od:
            raise ValueError(f"duplicate od={rounded_od} (row {i}, {source}); merge or drop one")
        seen_od.add(rounded_od)

        parsed.append({"od": rounded_od, "ppm": round(ppm, 3), "source": source})

    return sorted(parsed, key=lambda r: r["od"])


def main() -> None:
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    here = Path(__file__).parent
    parser.add_argument("--master-csv", type=Path, default=here / "data" / "reference_curve_master.csv")
    parser.add_argument("--asset-path", type=Path, default=DEFAULT_ASSET_PATH)
    args = parser.parse_args()

    rows = load_and_validate(args.master_csv)

    args.asset_path.parent.mkdir(parents=True, exist_ok=True)
    with open(args.asset_path, "w") as f:
        json.dump(rows, f, indent=2)
        f.write("\n")

    print(f"Wrote {len(rows)} points to {args.asset_path}")
    print(f"od range: {rows[0]['od']:.4f} .. {rows[-1]['od']:.4f}")


if __name__ == "__main__":
    main()
