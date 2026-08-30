package com.example.xposuredetectorsmart.utils

object Constants {
    const val DATABASE_NAME = "h2s_dose_reader.db"

    // Permanent wristband QR payload format: "h2s-worker:{industryId}:{workerId}"
    // Encodes only an opaque worker reference - all mutable info (name, status, shift schedule)
    // is resolved from the worker_profiles/industries records at scan time, so this QR is printed
    // once at registration and reused for the worker's entire tenure at that industry.
    const val QR_WORKER_PREFIX_V2 = "h2s-worker:"

    // Risk classification thresholds (shift-average ppm concentration), matching H2SRiskLevel:
    // SAFE < 1.0, MODERATE 1.0-5.0, HIGH 5.0-10.0, DANGEROUS > 10.0.
    const val RISK_MODERATE_MIN_PPM = 1.0
    const val RISK_HIGH_MIN_PPM = 5.0
    const val RISK_DANGEROUS_MIN_PPM = 10.0
    const val MAX_EXPECTED_CONCENTRATION_PPM = 15.0 // ceiling for gauge/chart scaling

    // Confidence scoring weights
    const val WEIGHT_SATURATION = 0.4
    const val WEIGHT_CONTRAST = 0.3
    const val WEIGHT_SHARPNESS = 0.3

    // Optical-density -> ppm reference curve (k-NN regression), see ReferenceCurveLoader /
    // OdKnnRegressor. The asset is a much larger, denser stand-in for the old 6-point hardcoded
    // table; regenerate it from digitized manufacturer reference charts via
    // tools/reference_curve_calibration/export_reference_curve.py.
    const val REFERENCE_CURVE_ASSET_PATH = "reference_curve.json"
    const val KNN_K = 5
    const val KNN_DISTANCE_EPSILON = 1e-6

    // Below this optical density the stain is considered indistinguishable from the blank
    // reference (sensor/print noise floor), independent of the learned od->ppm curve.
    const val MIN_DETECTABLE_OPTICAL_DENSITY = 0.015

    // Sync
    const val SYNC_WORK_NAME = "h2s_dose_sync_worker"
    const val SYNC_INTERVAL_MINUTES = 15L // WorkManager PeriodicWork minimum is 15 min
    const val FAST_SYNC_INTERVAL_SECONDS = 30L // used for foreground/manual re-trigger loop

    // Firestore collections
    const val COLLECTION_DOSE_LOGS = "dose_logs"
    const val COLLECTION_SHIFT_REPORTS = "shift_reports"
    const val COLLECTION_WORKER_PROFILES = "worker_profiles"
    const val COLLECTION_INDUSTRIES = "industries"

    // Shift session
    const val DEFAULT_SHIFT_DURATION_HOURS = 8L

    // Notifications
    const val CHANNEL_ALERTS = "h2s_alerts"
    const val CHANNEL_SYNC = "h2s_sync"
    const val NOTIFICATION_ID_ALERT = 1001
    const val NOTIFICATION_ID_SYNC = 1002

    // Biometric lock session
    const val BIOMETRIC_SESSION_TIMEOUT_MS = 15 * 60 * 1000L // 15 minutes

    // Camera
    const val STRIP_FRAME_SIZE_DP = 300
}
