# Reference curve calibration tooling

Offline pipeline for building and validating the od->ppm reference table that the app's
k-NN regression (`imageprocessing/OdKnnRegressor.kt`) runs against on-device. There is no
"training" step in the neural-network sense - the model *is* this reference table plus a
fixed inverse-distance-weighting rule, so this tooling is about curating good anchor points
and validating them, not fitting model weights.

```
tools/reference_curve_calibration/
  od_knn.py                    Python mirror of OdKnnRegressor.kt - keep them in sync
  color_science.py             Python mirror of DoseCalculator's luminance/OD math
  digitize_chart.py            Photo of a manufacturer chart -> (od, ppm) rows
  evaluate_knn.py               Leave-one-out CV + monotonicity check + diagnostic plot
  export_reference_curve.py     Validates data/*.csv -> writes the app's assets/reference_curve.json
  data/reference_curve_master.csv         Master dataset (currently seeded with the placeholder curve)
  data/colorchecker_points_template.csv   Manifest template: 24 ColorChecker patch pixel coords
  data/chart_swatch_points_template.csv   Manifest template: chart swatch pixel coords + printed ppm
  reports/                                evaluate_knn.py output (gitignored-friendly scratch dir)
```

## Setup

```
python -m venv .venv
.venv/Scripts/activate        # Windows; use .venv/bin/activate on macOS/Linux
pip install -r requirements.txt
```

## Tier 2 workflow: digitizing manufacturer reference charts

This is the approach recommended in the project brief: printed colorimetric strip/tube vendors
(Dräger, Gastec, Sensidyne, Kitagawa) publish official color-vs-ppm charts for their H2S
detection media. Photographing their chart gives manufacturer-validated anchor points without
running a single gas exposure.

**Capture protocol** (do this for every chart/lot you digitize):

1. Photograph the printed chart under diffuse, even lighting (no direct glare on the glossy
   print) - the same phone/lighting-independence assumption the app relies on for its own
   blank/sample ratio, but a controlled shot still reduces noise going into the table.
2. Include an X-Rite ColorChecker Classic card flat in the same frame, same lighting, not
   tilted relative to the chart.
3. Shoot straight-on (minimal perspective distortion) and keep the chart in focus edge to edge.
4. Note which printed swatch is the "blank"/0 ppm reference - every chart has one; if not,
   use the lightest/unexposed swatch.

**Digitizing**:

1. Open the photo in any image viewer that shows pixel coordinates (e.g. GIMP, Photoshop, or
   `matplotlib`'s interactive viewer) and record:
   - The pixel center of each of the 24 ColorChecker patches, in the same row-major order as
     `color_science.COLORCHECKER_SRGB` (row 1 left-to-right, then row 2, ...) ->
     `colorchecker_points.csv` (copy `data/colorchecker_points_template.csv`).
   - The pixel center of each chart swatch, its printed ppm label, and which one is the blank
     -> `swatch_points.csv` (copy `data/chart_swatch_points_template.csv`).
2. Run:
   ```
   python digitize_chart.py \
       --image path/to/photo.jpg \
       --colorchecker-points path/to/colorchecker_points.csv \
       --swatch-points path/to/swatch_points.csv \
       --source-tag drager-h2s-lot4021
   ```
   This fits a color-correction matrix from the photographed ColorChecker patches against
   their known reference values, applies it to the whole image, samples each swatch, computes
   optical density against the blank using the exact same formula as `DoseCalculator.kt`, and
   appends the resulting rows to `data/reference_curve_master.csv`.
3. Repeat for every chart/lot you have. More charts (and more swatches per chart) = a denser,
   more reliable reference table - that density is the entire value of the k-NN approach over
   the old 6-point table.

You can also hand-append rows to `data/reference_curve_master.csv` directly (columns:
`od,ppm,source`) if you have lab-derived (od, ppm) pairs from real strip exposures instead of
a printed chart.

## Validating before you ship it

```
python evaluate_knn.py
```

Reports leave-one-out cross-validation error per k (3-7), flags any non-monotonic anchors
(OD should only increase as ppm increases - a violation usually means a mislabeled swatch or a
bad photo, not real signal), and writes a curve + residual plot to `reports/knn_curve.png`.
Update `Constants.KNN_K` in the app if the recommended k changes.

## Exporting to the app

```
python export_reference_curve.py
```

Validates `data/reference_curve_master.csv` (no NaN/inf, no duplicate od, no negative ppm) and
overwrites `app/src/main/assets/reference_curve.json` directly. Commit both the updated master
CSV and the regenerated JSON asset together.
