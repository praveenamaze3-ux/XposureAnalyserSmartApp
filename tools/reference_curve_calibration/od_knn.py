"""Python mirror of app/src/main/java/.../imageprocessing/OdReferenceCurve.kt.

Keep this in exact sync with OdKnnRegressor.kt (same weighting rule, same epsilon) so
offline cross-validation here predicts what the on-device k-NN regression will actually do.
If you change the weighting scheme in one place, change it in the other.
"""
from __future__ import annotations

from dataclasses import dataclass

KNN_DISTANCE_EPSILON = 1e-6
DEFAULT_K = 5


@dataclass(frozen=True)
class ReferencePoint:
    od: float
    ppm: float
    source: str = "unknown"


@dataclass(frozen=True)
class KnnPrediction:
    ppm: float
    neighbors_used: int
    extrapolated: bool


def predict(table: list[ReferencePoint], optical_density: float, k: int = DEFAULT_K) -> KnnPrediction:
    if not table:
        raise ValueError("Reference curve table must not be empty.")

    min_od = min(p.od for p in table)
    max_od = max(p.od for p in table)
    extrapolated = optical_density < min_od or optical_density > max_od

    by_distance = sorted(table, key=lambda p: abs(p.od - optical_density))
    nearest = by_distance[0]
    nearest_distance = abs(nearest.od - optical_density)

    if nearest_distance < KNN_DISTANCE_EPSILON:
        return KnnPrediction(ppm=nearest.ppm, neighbors_used=1, extrapolated=extrapolated)

    neighbors = by_distance[: max(1, min(k, len(table)))]

    weighted_sum = 0.0
    weight_total = 0.0
    for point in neighbors:
        distance = max(abs(point.od - optical_density), KNN_DISTANCE_EPSILON)
        weight = 1.0 / distance
        weighted_sum += weight * point.ppm
        weight_total += weight

    return KnnPrediction(
        ppm=weighted_sum / weight_total,
        neighbors_used=len(neighbors),
        extrapolated=extrapolated,
    )
