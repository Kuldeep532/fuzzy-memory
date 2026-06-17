package com.nexuswavetech.nexusplus.auth

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.nexuswavetech.nexusplus.core.AdminRepository
import kotlinx.coroutines.tasks.await

/**
 * Real Firebase Auth implementation.
 * Replaces [StubFirebaseAuthRepository] when google-services.json has valid credentials.
 * Gracefully falls back to Failure if Firebase is not configured.
 */
class FirebaseAuthRepository(
    private val adminRepository: AdminRepository,
) : AuthRepository {

    private val auth get() = try { FirebaseAuth.getInstance() } catch (_: Exception) { null }

    override suspend fun signInWithGoogle(idToken: String): AuthResult {
        val firebaseAuth = auth
            ?: return AuthResult.Failure("Firebase not configured. Replace google-services.json with real credentials.")

        return runCatching {
            val credential = GoogleAuthProvider.getCredential(idToken, null)
            val result     = firebaseAuth.signInWithCredential(credential).await()
            val user       = result.user
                ?: return AuthResult.Failure("Sign-in returned no user")

            val isAdmin = runCatching { adminRepository.isAdmin(user.uid) }.getOrDefault(false)

            AuthResult.Success(
                uid         = user.uid,
                displayName = user.displayName ?: "Nexus User",
                email       = user.email ?: "",
                photoUrl    = user.photoUrl?.toString(),
                isAdmin     = isAdmin,
            )
        }.getOrElse { AuthResult.Failure(it.localizedMessage ?: "Google Sign-In failed") }
    }

    override suspend fun signOut() {
        runCatching { auth?.signOut() }
    }

    override suspend fun isSignedIn(): Boolean =
        runCatching { auth?.currentUser != null }.getOrDefault(false)

    override suspend fun getCurrentUserId(): String? =
        runCatching { auth?.currentUser?.uid }.getOrNull()

    override suspend fun getCurrentUserEmail(): String? =
        runCatching { auth?.currentUser?.email }.getOrNull()

    override suspend fun getCurrentUserDisplayName(): String? =
        runCatching { auth?.currentUser?.displayName }.getOrNull()

    override suspend fun getCurrentUserPhotoUrl(): String? =
        runCatching { auth?.currentUser?.photoUrl?.toString() }.getOrNull()
}
