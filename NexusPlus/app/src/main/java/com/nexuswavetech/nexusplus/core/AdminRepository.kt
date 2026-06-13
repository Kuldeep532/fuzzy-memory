package com.nexuswavetech.nexusplus.core

import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

/**
 * Checks whether a given UID has admin privileges.
 *
 * Firestore structure:
 *   /nexus_admins/{uid}  — document exists → admin; does not exist → regular user
 *
 * Gracefully returns false if Firestore is unavailable.
 */
class AdminRepository {

    private val db get() = try { FirebaseFirestore.getInstance() } catch (_: Exception) { null }

    /** Returns true if [uid] exists in the `nexus_admins` collection. */
    suspend fun isAdmin(uid: String): Boolean {
        val firestore = db ?: return false
        return runCatching {
            firestore.collection("nexus_admins").document(uid).get().await().exists()
        }.getOrDefault(false)
    }
}
