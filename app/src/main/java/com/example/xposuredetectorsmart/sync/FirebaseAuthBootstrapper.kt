package com.example.xposuredetectorsmart.sync

import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.tasks.await
import timber.log.Timber
import javax.inject.Inject

/**
 * Firestore security rules require an authenticated request. This app has no login screen (the
 * QR scan identifies the worker instead), so we anonymously sign in once per install - enough to
 * satisfy `request.auth != null` rules while keeping the field workflow frictionless.
 */
class FirebaseAuthBootstrapper @Inject constructor(
    private val auth: FirebaseAuth,
) {
    suspend fun ensureSignedIn(): Boolean {
        if (auth.currentUser != null) return true
        return runCatching {
            auth.signInAnonymously().await()
            true
        }.onFailure { Timber.w(it, "Anonymous sign-in failed") }.getOrDefault(false)
    }
}
