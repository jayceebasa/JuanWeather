package com.juanweather.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
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

class AuthViewModel(private val repository: UserRepository) : ViewModel() {

    // All registered users as StateFlow (Compose-friendly) - from local Room cache
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

    // --- LOGIN ---
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
                    // Also try to get user from local Room cache for display purposes
                    val user = repository.login(email, password)
                    if (user != null) {
                        _loggedInUser.value = user
                        _authState.value = AuthState.LoginSuccess(user)
                    } else {
                        // User exists in Firebase but not in local Room, create local copy
                        _authState.value = AuthState.LoginSuccess(User(name = email, email = email, password = "", role = "user"))
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
