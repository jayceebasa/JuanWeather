package com.juanweather.viewmodel

import android.content.Context
import android.content.SharedPreferences
import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.juanweather.data.firebase.FirebaseAuthManager
import com.juanweather.data.models.User
import com.juanweather.data.repository.UserRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class AuthViewModel(
    private val repository: UserRepository,
    context: Context? = null
) : ViewModel() {

    // ...existing code...

    private val sharedPref: SharedPreferences? = context?.applicationContext?.getSharedPreferences(
        "juanweather_auth",
        Context.MODE_PRIVATE
    )

    /**
     * Save login session to SharedPreferences for persistent login
     */
    private fun saveSessionLocally(email: String) {
        try {
            sharedPref?.edit()?.apply {
                putString("logged_in_email", email)
                putLong("session_time", System.currentTimeMillis())
                apply()
            }
            android.util.Log.d("AuthViewModel", "Session saved locally for: $email")
        } catch (e: Exception) {
            android.util.Log.e("AuthViewModel", "Failed to save session: ${e.message}")
        }
    }

    /**
     * Get saved session email from SharedPreferences
     */
    private fun getSavedSessionEmail(): String? {
        return try {
            sharedPref?.getString("logged_in_email", null)
        } catch (e: Exception) {
            android.util.Log.e("AuthViewModel", "Failed to get saved session: ${e.message}")
            null
        }
    }

    /**
     * Clear saved session from SharedPreferences
     */
    private fun clearSessionLocally() {
        try {
            sharedPref?.edit()?.apply {
                remove("logged_in_email")
                remove("session_time")
                apply()
            }
            android.util.Log.d("AuthViewModel", "Local session cleared")
        } catch (e: Exception) {
            android.util.Log.e("AuthViewModel", "Failed to clear session: ${e.message}")
        }
    }
    val allUsers: StateFlow<List<User>> = repository.allUsers.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    // All registered users as LiveData — satisfies "Integrate with LiveData" requirement
    val allUsersLiveData: LiveData<List<User>> = repository.allUsers.asLiveData()

    private val _authState = MutableStateFlow<AuthState>(AuthState.Idle)
    val authState: StateFlow<AuthState> = _authState.asStateFlow()

    private val _loggedInUser = MutableStateFlow<User?>(null)
    val loggedInUser: StateFlow<User?> = _loggedInUser.asStateFlow()

    // Firebase current UID
    val firebaseUid: StateFlow<String?> = FirebaseAuthManager.currentUid

    // RBAC — true only when the logged-in user has role == "admin"
    val isAdmin: StateFlow<Boolean> = _loggedInUser.map { it?.role == "admin" }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = false
    )

    init {
        // Restore session when ViewModel is created
        restoreSession()
    }

    /**
     * Restore user session from Firebase and Room
     * Called on ViewModel initialization to maintain login across app lifecycle events
     * Also checks SharedPreferences for persistent login after app close
     */
    private fun restoreSession() {
        viewModelScope.launch {
            try {
                android.util.Log.d("AuthViewModel", "=== Starting restoreSession ===")
                android.util.Log.d("AuthViewModel", "SharedPreferences available: ${sharedPref != null}")

                // First, check if there's a saved session in SharedPreferences
                val savedEmail = getSavedSessionEmail()
                android.util.Log.d("AuthViewModel", "Saved email from SharedPreferences: $savedEmail")

                if (savedEmail != null) {
                    android.util.Log.d("AuthViewModel", "Found saved session for: $savedEmail")
                }

                // Check if user is still authenticated in Firebase
                val isLoggedInFirebase = FirebaseAuthManager.isLoggedIn()
                android.util.Log.d("AuthViewModel", "Firebase isLoggedIn: $isLoggedInFirebase")

                if (isLoggedInFirebase) {
                    val firebaseUid = FirebaseAuthManager.getCurrentUid()
                    android.util.Log.d("AuthViewModel", "Firebase user still authenticated: $firebaseUid")

                    // Get current Firebase user to extract email
                    val auth = FirebaseAuth.getInstance()
                    val currentUser = auth.currentUser

                    if (currentUser != null && firebaseUid != null) {
                        val email = currentUser.email ?: firebaseUid

                        // Try to get user from local Room using getUserByEmail (not login, since we don't have password)
                        var user: User? = null
                        try {
                            user = repository.getUserByEmail(email)
                            android.util.Log.d("AuthViewModel", "Found user in Room by email: ${user?.id}")
                        } catch (e: Exception) {
                            android.util.Log.d("AuthViewModel", "Could not find user by email: ${e.message}")
                        }

                        if (user != null) {
                            _loggedInUser.value = user
                            _authState.value = AuthState.LoginSuccess(user)
                            saveSessionLocally(email)
                            android.util.Log.d("AuthViewModel", "Session restored for user: ${user.email}")
                        } else {
                            android.util.Log.d("AuthViewModel", "Firebase user not found in Room, creating entry")
                            // Create user in Room if missing
                            val registerResult = repository.registerUser(
                                name = email.substringBefore("@"),
                                email = email,
                                password = "" // Password not available after logout/restore
                            )

                            if (registerResult is UserRepository.RegisterResult.Success) {
                                // Now try to fetch the newly created user by email
                                try {
                                    val restoredUser = repository.getUserByEmail(email)
                                    if (restoredUser != null) {
                                        _loggedInUser.value = restoredUser
                                        _authState.value = AuthState.LoginSuccess(restoredUser)
                                        saveSessionLocally(email)
                                        android.util.Log.d("AuthViewModel", "User created and session restored: $email, userId=${restoredUser.id}")
                                    } else {
                                        android.util.Log.w("AuthViewModel", "User created but could not retrieve from Room")
                                    }
                                } catch (e: Exception) {
                                    android.util.Log.e("AuthViewModel", "Error fetching newly created user: ${e.message}")
                                }
                            } else {
                                android.util.Log.w("AuthViewModel", "Failed to create user in Room: $registerResult")
                            }
                        }
                    }
                } else if (savedEmail != null) {
                    // Firebase session lost but we have a saved email - try to restore from Room
                    android.util.Log.d("AuthViewModel", "Firebase session lost, restoring from saved session: $savedEmail")

                    var user: User? = null
                    try {
                        // Get user by email only (no password needed for restoration)
                        user = repository.getUserByEmail(savedEmail)
                        android.util.Log.d("AuthViewModel", "User found in Room: $user")
                    } catch (e: Exception) {
                        android.util.Log.w("AuthViewModel", "Could not find user by email in Room: ${e.message}")
                    }

                    if (user != null) {
                        _loggedInUser.value = user
                        _authState.value = AuthState.LoginSuccess(user)
                        android.util.Log.d("AuthViewModel", "Session restored from saved email: ${user.email}")
                    } else {
                        // User not found in Room, clear saved session
                        android.util.Log.w("AuthViewModel", "User not found in Room for saved email, clearing session")
                        clearSessionLocally()
                        _authState.value = AuthState.Idle
                    }
                } else {
                    android.util.Log.d("AuthViewModel", "No Firebase user and no saved session found")
                    _authState.value = AuthState.Idle
                }
            } catch (e: Exception) {
                android.util.Log.e("AuthViewModel", "Error restoring session: ${e.message}", e)
                _authState.value = AuthState.Idle
            }
        }
    }
    fun login(email: String, password: String) {
        if (email.isBlank()) { _authState.value = AuthState.Error("Email is required"); return }
        if (!isValidEmail(email)) { _authState.value = AuthState.Error("Please enter a valid email address"); return }
        if (password.isBlank()) { _authState.value = AuthState.Error("Password is required"); return }

        viewModelScope.launch {
            _authState.value = AuthState.Loading
            try {
                // Use Firebase authentication
                val result = FirebaseAuthManager.login(email, password)
                if (result.isSuccess) {
                    val uid = result.getOrNull()
                    // Try to get user from local Room cache
                    var user = repository.login(email, password)

                    // If user doesn't exist in Room (cross-device login), create them
                    if (user == null) {
                        android.util.Log.d("AuthViewModel", "User not in Room for email: $email, creating local copy")
                        val newUser = com.juanweather.data.models.User(
                            name = email.substringBefore("@"),
                            email = email,
                            password = password,
                            role = "user"
                        )
                        repository.registerUser(newUser.name, newUser.email, newUser.password)
                        // Now fetch the newly created user
                        user = repository.login(email, password)
                        android.util.Log.d("AuthViewModel", "Created user in Room: ${user?.id}")
                    }

                    if (user != null) {
                        _loggedInUser.value = user
                        _authState.value = AuthState.LoginSuccess(user)
                        saveSessionLocally(email)
                    } else {
                        _authState.value = AuthState.Error("Failed to sync user account")
                    }
                } else {
                    _authState.value = AuthState.Error("Invalid email or password")
                }
            } catch (e: Exception) {
                _authState.value = AuthState.Error("Login failed: ${e.message}")
            }
        }
    }

    // --- REGISTER ---
    fun register(name: String, email: String, password: String, confirmPassword: String) {
        when {
            name.isBlank() -> { _authState.value = AuthState.Error("Name is required"); return }
            email.isBlank() -> { _authState.value = AuthState.Error("Email is required"); return }
            !isValidEmail(email) -> { _authState.value = AuthState.Error("Please enter a valid email address"); return }
            password.isBlank() -> { _authState.value = AuthState.Error("Password is required"); return }
            password.length < 6 -> { _authState.value = AuthState.Error("Password must be at least 6 characters"); return }
            password != confirmPassword -> { _authState.value = AuthState.Error("Passwords do not match"); return }
        }

        viewModelScope.launch {
            _authState.value = AuthState.Loading
            try {
                // Register with Firebase (saves to Firebase, not local Room)
                val result = FirebaseAuthManager.register(name, email, password)
                if (result.isSuccess) {
                    val uid = result.getOrNull()
                    // Also save to local Room for offline cache (optional)
                    val localResult = repository.registerUser(name, email, password)
                    _authState.value = when (localResult) {
                        is UserRepository.RegisterResult.Success -> AuthState.RegisterSuccess
                        else -> AuthState.RegisterSuccess // Firebase registration succeeded
                    }
                } else {
                    val exception = result.exceptionOrNull()
                    _authState.value = AuthState.Error(
                        when {
                            exception?.message?.contains("already exists") == true -> "An account with this email already exists"
                            else -> exception?.message ?: "Registration failed"
                        }
                    )
                }
            } catch (e: Exception) {
                _authState.value = AuthState.Error("Registration failed: ${e.message}")
            }
        }
    }

    // --- UPDATE USER ---
    fun updateUser(user: User) {
        viewModelScope.launch {
            repository.updateUser(user)
        }
    }

    // --- DELETE USER ---
    fun deleteUser(user: User) {
        viewModelScope.launch {
            repository.deleteUser(user)
        }
    }

    fun logout() {
        // Logout from Firebase
        FirebaseAuthManager.logout()
        // Clear local state
        _loggedInUser.value = null
        _authState.value = AuthState.Idle
        // Clear saved session
        clearSessionLocally()
    }

    fun resetState() {
        _authState.value = AuthState.Idle
    }

    private fun isValidEmail(email: String): Boolean {
        if (email == "admin") return true   // admin username bypass
        val emailPattern = "^[A-Za-z0-9+_.-]+@(.+)$"
        return email.matches(emailPattern.toRegex()) && email.contains("@") && email.contains(".")
    }

    sealed class AuthState {
        object Idle : AuthState()
        object Loading : AuthState()
        data class LoginSuccess(val user: User) : AuthState()
        object RegisterSuccess : AuthState()
        data class Error(val message: String) : AuthState()
    }
}
