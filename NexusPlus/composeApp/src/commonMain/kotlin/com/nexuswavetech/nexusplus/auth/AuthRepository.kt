package com.nexuswavetech.nexusplus.auth

/**
 * Contract for Firebase Authentication.
 * The stub implementation works without any Firebase dependency.
 * When Firebase is added, swap [StubFirebaseAuthRepository] for a real implementation
 * — no UI or navigation code changes required.
 */
interface AuthRepository {
    suspend fun signInWithGoogle(idToken: String): AuthResult
    suspend fun signOut()
    suspend fun isSignedIn(): Boolean
    suspend fun getCurrentUserId(): String?
    suspend fun getCurrentUserEmail(): String?
    suspend fun getCurrentUserDisplayName(): String?
    suspend fun getCurrentUserPhotoUrl(): String?
}

sealed class AuthResult {
    data class Success(
        val uid: String,
        val displayName: String,
        val email: String,
        val photoUrl: String?,
        val isAdmin: Boolean = false,
    ) : AuthResult()

    data class Failure(val message: String) : AuthResult()
}

/**
 * Stub implementation — simulates a successful sign-in with placeholder data.
 * Replace this class body with real FirebaseAuth calls when the Firebase SDK is added.
 */
class StubFirebaseAuthRepository : AuthRepository {

    override suspend fun signInWithGoogle(idToken: String): AuthResult {
        return AuthResult.Success(
            uid = "stub_uid_12345",
            displayName = "Nexus User",
            email = "user@nexuswavetech.com",
            photoUrl = null
        )
    }

    override suspend fun signOut() {}
    override suspend fun isSignedIn(): Boolean = false
    override suspend fun getCurrentUserId(): String? = null
    override suspend fun getCurrentUserEmail(): String? = null
    override suspend fun getCurrentUserDisplayName(): String? = null
    override suspend fun getCurrentUserPhotoUrl(): String? = null
}
