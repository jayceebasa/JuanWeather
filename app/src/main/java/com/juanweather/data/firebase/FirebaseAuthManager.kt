package com.juanweather.data.firebase

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.tasks.await

/**
 * Firebase Authentication Manager
 * Handles user authentication and manages current user session
 * Replaces local Room-based authentication with Firebase Auth
 */
object FirebaseAuthManager {

    private val auth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()

    private val _currentUser = MutableStateFlow<FirebaseUser?>(null)
    val currentUser: StateFlow<FirebaseUser?> = _currentUser.asStateFlow()

    private val _currentUid = MutableStateFlow<String?>(null)
    val currentUid: StateFlow<String?> = _currentUid.asStateFlow()

    private val _isAuthenticated = MutableStateFlow(false)
    val isAuthenticated: StateFlow<Boolean> = _isAuthenticated.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    init {
        // Restore current auth state
        _currentUser.value = auth.currentUser
        _currentUid.value = auth.currentUser?.uid
        _isAuthenticated.value = auth.currentUser != null

        // Listen to auth state changes
        auth.addAuthStateListener { firebaseAuth ->
            _currentUser.value = firebaseAuth.currentUser
            _currentUid.value = firebaseAuth.currentUser?.uid
            _isAuthenticated.value = firebaseAuth.currentUser != null
        }
    }

    /**
     * Register a new user with email and password
     * Creates user document in Firestore with empty subcollections for user-scoped data
     */
    suspend fun register(
        name: String,
        email: String,
        password: String
    ): Result<String> {
        return try {
            // Create user in Firebase Auth
            val authResult = auth.createUserWithEmailAndPassword(email, password).await()
            val user = authResult.user
            val uid = user?.uid

            if (uid != null) {
                // Initialize user document in Firestore with profile data
                val userProfile = mapOf(
                    "uid" to uid,
                    "name" to name,
                    "email" to email,
                    "createdAt" to System.currentTimeMillis(),
                    "role" to "user"
                )

                firestore.collection("users").document(uid)
                    .set(userProfile)
                    .await()

                // Initialize empty subcollections for user-scoped data
                initializeUserSubcollections(uid)
                _errorMessage.value = null
                Result.success(uid)
            } else {
                val error = Exception("Failed to create user")
                _errorMessage.value = error.message
                Result.failure(error)
            }
        } catch (e: Exception) {
            _errorMessage.value = e.message ?: "Registration failed"
            Result.failure(e)
        }
    }

    /**
     * Login user with email and password
     */
    suspend fun login(email: String, password: String): Result<String> {
        return try {
            val authResult = auth.signInWithEmailAndPassword(email, password).await()
            val uid = authResult.user?.uid

            if (uid != null) {
                _errorMessage.value = null
                Result.success(uid)
            } else {
                val error = Exception("Login failed")
                _errorMessage.value = error.message
                Result.failure(error)
            }
        } catch (e: Exception) {
            _errorMessage.value = e.message ?: "Login failed"
            Result.failure(e)
        }
    }

    /**
     * Get Firebase ID token for Retrofit requests
     */
    suspend fun getIdToken(): String? {
        return try {
            auth.currentUser?.getIdToken(false)?.await()?.token
        } catch (e: Exception) {
            _errorMessage.value = e.message
            null
        }
    }

    /**
     * Logout current user
     */
    fun logout() {
        try {
            auth.signOut()
            _currentUser.value = null
            _currentUid.value = null
            _isAuthenticated.value = false
            _errorMessage.value = null
        } catch (e: Exception) {
            _errorMessage.value = e.message
        }
    }

    /**
     * Check if user is authenticated
     */
    fun isLoggedIn(): Boolean = auth.currentUser != null

    /**
     * Get current user's UID
     */
    fun getCurrentUid(): String? = auth.currentUser?.uid

    /**
     * Initialize empty subcollections for new users to prevent data leaking
     * This ensures fresh accounts have no pre-existing data
     */
    private fun initializeUserSubcollections(uid: String) {
        // Create empty documents in subcollections to initialize them
        // Emergency contacts subcollection
        firestore.collection("users").document(uid)
            .collection("emergencyContacts")
            .document("_init")
            .set(mapOf("initialized" to true))
            .addOnFailureListener { e ->
                _errorMessage.value = "Failed to initialize emergency contacts: ${e.message}"
            }

        // Settings subcollection
        firestore.collection("users").document(uid)
            .collection("settings")
            .document("_init")
            .set(mapOf("initialized" to true))
            .addOnFailureListener { e ->
                _errorMessage.value = "Failed to initialize settings: ${e.message}"
            }

        // Locations subcollection
        firestore.collection("users").document(uid)
            .collection("locations")
            .document("_init")
            .set(mapOf("initialized" to true))
            .addOnFailureListener { e ->
                _errorMessage.value = "Failed to initialize locations: ${e.message}"
            }
    }

    /**
     * Clear error message
     */
    fun clearError() {
        _errorMessage.value = null
    }
}
