package com.example.xposuredetectorsmart.security

import java.security.MessageDigest
import javax.inject.Inject

/**
 * Hashes a supervisor PIN salted with the industry id, so the registration-screen PIN gate
 * never has to compare or transmit a raw PIN. This is a UX gate, not a cryptographic access
 * control boundary - see the Firestore rules comment on `worker_profiles` for the accepted
 * limitation that any authenticated client can still call the underlying write directly.
 */
class PinHasher @Inject constructor() {
    fun hash(pin: String, industryId: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val bytes = digest.digest("$industryId:$pin".toByteArray(Charsets.UTF_8))
        return bytes.joinToString("") { "%02x".format(it) }
    }
}
