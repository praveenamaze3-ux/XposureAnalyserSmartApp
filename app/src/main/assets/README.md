# reference_curve.json

Optical-density -> ppm-hours anchor table consumed by `ReferenceCurveLoader` /
`OdKnnRegressor` (see `imageprocessing/OdReferenceCurve.kt`). Loaded as-is into k-NN
regression at capture time - there is no training step, the "model" is this table plus the
fixed inverse-distance weighting rule in `OdKnnRegressor`.

**Current contents are a placeholder**, not real calibration data: the 71 points are sampled
densely off the app's old 2-constant quadratic regression formula
(`18.45*od + 6.12*od^2`), tagged `"source": "placeholder-synthetic-v1"`. They exist so the
k-NN path behaves identically to the old formula out of the box and so the pipeline below has
something to load and validate against immediately.

## Replacing this with real data

Do not hand-edit this file. Regenerate it from `tools/reference_curve_calibration/` at the
repo root:

1. Digitize manufacturer reference charts (Dräger/Gastec/Sensidyne/Kitagawa tube or strip
   color-vs-ppm charts) per `tools/reference_curve_calibration/README.md`.
2. Append the resulting (od, ppm, source) rows to
   `tools/reference_curve_calibration/data/reference_curve_master.csv`.
3. Run `python evaluate_knn.py` to sanity-check k and see residuals.
4. Run `python export_reference_curve.py` - it writes this file directly.

Each point's `source` should stay traceable (e.g. `"drager-8h-h2s-lot4021-swatch7"`) so a bad
anchor can be tracked back to the chart/lot it came from during a safety audit.
