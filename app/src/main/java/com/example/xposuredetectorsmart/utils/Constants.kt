package com.example.xposuredetectorsmart.utils

object Constants {
    const val DATABASE_NAME = "h2s_dose_reader.db"

    // QR payload format: "h2s-dose:WRK_{ID}|{DATE}|{LOCATION}|{SHIFT_TYPE}"
    const val QR_PREFIX = "h2s-dose:"
    const val QR_WORKER_PREFIX = "WRK_"

    // OSHA exposure thresholds (ppm), used for badges + alerts + PDF report reference lines.
    const val OSHA_PEL_8HR = 10.0
    const val OSHA_STEL_15MIN = 15.0
    const val IDLH_PPM = 100.0
    const val ALERT_THRESHOLD_PPM = 100.0

    // Confidence scoring weights
    const val WEIGHT_SATURATION = 0.4
    const val WEIGHT_CONTRAST = 0.3
    const val WEIGHT_SHARPNESS = 0.3
    const val MIN_CONFIDENCE_WARNING = 0.5

    // Color correction sanity bounds
    const val MIN_CORRECTION_SCALE = 0.5
    const val MAX_CORRECTION_SCALE = 3.0
    const val MAX_DEVIATION_FACTOR = 1.5
    const val GAMMA = 0.45 // inverse gamma used for correction exponent (1/gamma = 2.22)
    const val COLOR_PROFILE_HISTORY_SIZE = 10

    // Sync
    const val SYNC_WORK_NAME = "h2s_dose_sync_worker"
    const val SYNC_INTERVAL_MINUTES = 15L // WorkManager PeriodicWork minimum is 15 min
    const val FAST_SYNC_INTERVAL_SECONDS = 30L // used for foreground/manual re-trigger loop

    // Firestore collections
    const val COLLECTION_DOSE_LOGS = "dose_logs"
    const val COLLECTION_SHIFT_REPORTS = "shift_reports"
    const val COLLECTION_WORKER_PROFILES = "worker_profiles"

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
