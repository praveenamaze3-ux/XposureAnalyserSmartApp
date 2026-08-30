"""Python mirror of the luminance/optical-density math in
app/src/main/java/.../imageprocessing/DoseCalculator.kt (robustLuminance + the OD formula).

Digitizing a manufacturer chart must produce OD values on the exact same scale the phone
computes them on at capture time, or the reference curve trains "accurate" against a feature
definition the app never actually sees. Keep this in sync with DoseCalculator.kt.
"""
from __future__ import annotations

import math

import numpy as np

# X-Rite ColorChecker Classic, 24 patches, published sRGB (D65) values, row-major
# (row 1 = patches 1-6, ... row 4 = patches 19-24). Used to fit a per-photo color-correction
# matrix from the chart's own ColorChecker card, the same "known ground truth swatches in
# frame" idea as the app's white/grey reference patches.
COLORCHECKER_SRGB = np.array(
    [
        [115, 82, 68], [194, 150, 130], [98, 122, 157], [87, 108, 67], [133, 128, 177], [103, 189, 170],
        [214, 126, 44], [80, 91, 166], [193, 90, 99], [94, 60, 108], [157, 188, 64], [224, 163, 46],
        [56, 61, 150], [70, 148, 73], [175, 54, 60], [231, 199, 31], [187, 86, 149], [8, 133, 161],
        [243, 243, 242], [200, 200, 200], [160, 160, 160], [122, 122, 121], [85, 85, 85], [52, 52, 52],
    ],
    dtype=np.float64,
)


def fit_color_correction_matrix(photographed_rgb: np.ndarray, reference_rgb: np.ndarray = COLORCHECKER_SRGB) -> np.ndarray:
    """Least-squares affine (3x4, RGB + bias) mapping photographed patch colors onto their
    known reference values. `photographed_rgb` must be Nx3 in the same patch order as
    `reference_rgb`."""
    ones = np.ones((photographed_rgb.shape[0], 1))
    design = np.hstack([photographed_rgb, ones])  # Nx4
    solution, *_ = np.linalg.lstsq(design, reference_rgb, rcond=None)  # 4x3
    return solution  # apply as [R,G,B,1] @ solution -> corrected [R,G,B]


def apply_color_correction(image_rgb: np.ndarray, matrix: np.ndarray) -> np.ndarray:
    """Applies a 4x3 affine matrix (from fit_color_correction_matrix) to an HxWx3 uint8 image."""
    h, w, _ = image_rgb.shape
    flat = image_rgb.reshape(-1, 3).astype(np.float64)
    ones = np.ones((flat.shape[0], 1))
    design = np.hstack([flat, ones])
    corrected = design @ matrix
    corrected = np.clip(corrected, 0, 255)
    return corrected.reshape(h, w, 3)


def _srgb_to_linear(channel_0_1: np.ndarray) -> np.ndarray:
    return np.where(
        channel_0_1 <= 0.04045,
        channel_0_1 / 12.92,
        ((channel_0_1 + 0.055) / 1.055) ** 2.4,
    )


def robust_luminance(patch_rgb: np.ndarray) -> float:
    """Trimmed-mean CIE1931 relative luminance over a patch, rejecting near-white glare
    pixels - mirrors DoseCalculator.robustLuminance() pixel-for-pixel."""
    pixels = patch_rgb.reshape(-1, 3).astype(np.float64) / 255.0

    glare_mask = (pixels[:, 0] > 0.96) & (pixels[:, 1] > 0.96) & (pixels[:, 2] > 0.96)
    kept = pixels[~glare_mask]
    if kept.shape[0] == 0:
        return 1.0

    linear = _srgb_to_linear(kept)
    luminances = np.sort(0.2126 * linear[:, 0] + 0.7152 * linear[:, 1] + 0.0722 * linear[:, 2])

    trim = int(luminances.shape[0] * 0.10)
    if luminances.shape[0] - 2 * trim > 0:
        luminances = luminances[trim : luminances.shape[0] - trim]

    return float(np.mean(luminances))


def optical_density(blank_patch_rgb: np.ndarray, sample_patch_rgb: np.ndarray) -> float:
    """Beer-Lambert-style OD between a blank/white-reference patch and the exposed sample
    patch - mirrors DoseCalculator.calculate()'s OD formula exactly."""
    y_blank = robust_luminance(blank_patch_rgb)
    y_sample = robust_luminance(sample_patch_rgb)

    clamped_sample = max(0.001, min(y_blank, y_sample))
    clamped_blank = max(0.01, y_blank)

    return math.log10(clamped_blank / clamped_sample)


def sample_patch(image_rgb: np.ndarray, x: int, y: int, radius_px: int = 8) -> np.ndarray:
    """Square ROI around (x, y), clamped to image bounds - mirrors PatchSampler.sample()."""
    h, w, _ = image_rgb.shape
    left = max(0, x - radius_px)
    top = max(0, y - radius_px)
    right = min(w, x + radius_px)
    bottom = min(h, y + radius_px)
    return image_rgb[top:bottom, left:right]
