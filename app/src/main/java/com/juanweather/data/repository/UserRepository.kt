package com.juanweather.data.repository

import com.juanweather.data.local.UserDao
import com.juanweather.data.models.User
import kotlinx.coroutines.flow.Flow

class UserRepository(private val userDao: UserDao) {

    // READ - All users as reactive Flow (satisfies LiveData/Flow requirement)
    val allUsers: Flow<List<User>> = userDao.getAllUsers()

    // CREATE - Register a new user
    // Returns true if successful, false if email already taken
    suspend fun registerUser(name: String, email: String, password: String): RegisterResult {
        val existing = userDao.getUserByEmail(email)
        if (existing != null) {
            return RegisterResult.EmailAlreadyExists
        }
        return try {
            // Role is always "user" for self-registration — admin is seeded by the DB only
            userDao.insertUser(User(name = name, email = email, password = password, role = "user"))
            RegisterResult.Success
        } catch (e: Exception) {
            RegisterResult.Error(e.message ?: "Registration failed")
        }
    }

    // READ - Login validation
    suspend fun login(email: String, password: String): User? {
        return userDao.getUserByCredentials(email, password)
    }

    // READ - Get user by email only (for session restoration)
    suspend fun getUserByEmail(email: String): User? {
        return userDao.getUserByEmail(email)
    }

    // UPDATE - Update user profile
    suspend fun updateUser(user: User) {
        userDao.updateUser(user)
    }

    // DELETE - Remove a user
    suspend fun deleteUser(user: User) {
        userDao.deleteUser(user)
    }

    suspend fun deleteUserById(id: Int) {
        userDao.deleteUserById(id)
    }

    sealed class RegisterResult {
        object Success : RegisterResult()
        object EmailAlreadyExists : RegisterResult()
        data class Error(val message: String) : RegisterResult()
    }
}
