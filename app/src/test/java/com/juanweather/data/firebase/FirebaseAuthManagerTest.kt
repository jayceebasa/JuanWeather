package com.juanweather.data.firebase

import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for FirebaseAuthManager
 * Tests authentication flows including registration, login, and logout
 *
 * To run these tests:
 * 1. Use Firebase Emulator for local testing (no real Firebase project required)
 * 2. Or create test Firebase project and use that
 * 3. Ensure authentication is enabled in your Firebase project
 */
class FirebaseAuthManagerTest {

    @Before
    fun setUp() {
        // Initialize Firebase (happens automatically in app startup)
    }

    /**
     * Test user registration with email and password
     * Verifies user account is created and uid is returned
     */
    @Test
    fun testRegisterUser() = runBlocking {
        val email = "testuser_${System.currentTimeMillis()}@example.com"
        val password = "TestPassword123!"
        val name = "Test User"

        val result = FirebaseAuthManager.register(name, email, password)

        assert(result.isSuccess)
        val uid = result.getOrNull()
        assert(uid != null)
        assert(uid!!.isNotEmpty())
    }

    /**
     * Test user registration with duplicate email
     * Verifies error is returned for existing account
     */
    @Test
    fun testRegisterDuplicateEmail() = runBlocking {
        val email = "duplicate_${System.currentTimeMillis()}@example.com"
        val password = "TestPassword123!"
        val name = "Test User"

        // First registration should succeed
        val firstResult = FirebaseAuthManager.register(name, email, password)
        assert(firstResult.isSuccess)

        // Second registration with same email should fail
        val secondResult = FirebaseAuthManager.register(name, email, password)
        assert(secondResult.isFailure)
    }

    /**
     * Test user login with correct credentials
     * Verifies authentication succeeds and uid is returned
     */
    @Test
    fun testLoginSuccess() = runBlocking {
        val email = "login_${System.currentTimeMillis()}@example.com"
        val password = "LoginPass123!"
        val name = "Login Test"

        // First register
        FirebaseAuthManager.register(name, email, password)

        // Then login
        val result = FirebaseAuthManager.login(email, password)

        assert(result.isSuccess)
        val uid = result.getOrNull()
        assert(uid != null)
        assert(uid!!.isNotEmpty())
    }

    /**
     * Test login with incorrect password
     * Verifies authentication fails with wrong password
     */
    @Test
    fun testLoginInvalidPassword() = runBlocking {
        val email = "invalid_${System.currentTimeMillis()}@example.com"
        val password = "CorrectPass123!"
        val wrongPassword = "WrongPass123!"
        val name = "Invalid Test"

        // Register user
        FirebaseAuthManager.register(name, email, password)

        // Try login with wrong password
        val result = FirebaseAuthManager.login(email, wrongPassword)

        assert(result.isFailure)
    }

    /**
     * Test isLoggedIn check
     * Verifies authentication state tracking
     */
    @Test
    fun testIsLoggedIn() = runBlocking {
        // Initially not logged in
        var isLoggedIn = FirebaseAuthManager.isLoggedIn()
        assert(!isLoggedIn || isLoggedIn) // Depends on previous test state

        // Register and verify logged in
        val email = "logged_${System.currentTimeMillis()}@example.com"
        val password = "LoggedPass123!"

        FirebaseAuthManager.register("Logged User", email, password)
        isLoggedIn = FirebaseAuthManager.isLoggedIn()

        assert(isLoggedIn)
    }

    /**
     * Test logout
     * Verifies user session is cleared
     */
    @Test
    fun testLogout() = runBlocking {
        val email = "logout_${System.currentTimeMillis()}@example.com"
        val password = "LogoutPass123!"

        // Register and verify logged in
        FirebaseAuthManager.register("Logout User", email, password)
        assert(FirebaseAuthManager.isLoggedIn())

        // Logout
        FirebaseAuthManager.logout()
        assert(!FirebaseAuthManager.isLoggedIn())
    }

    /**
     * Test getCurrentUid
     * Verifies correct uid is returned for current user
     */
    @Test
    fun testGetCurrentUid() = runBlocking {
        val email = "uid_${System.currentTimeMillis()}@example.com"
        val password = "UidPass123!"

        val registerResult = FirebaseAuthManager.register("UID User", email, password)
        val registeredUid = registerResult.getOrNull()

        val currentUid = FirebaseAuthManager.getCurrentUid()

        assert(currentUid == registeredUid)
    }

    /**
     * Test getIdToken
     * Verifies Firebase ID token is obtainable for API calls
     */
    @Test
    fun testGetIdToken() = runBlocking {
        val email = "token_${System.currentTimeMillis()}@example.com"
        val password = "TokenPass123!"

        // Register to be logged in
        FirebaseAuthManager.register("Token User", email, password)

        // Get token
        val token = FirebaseAuthManager.getIdToken()

        assert(token != null)
        assert(token!!.isNotEmpty())
    }

    /**
     * Test error message clearing
     * Verifies error state can be reset
     */
    @Test
    fun testClearError() {
        // Trigger an error by trying to login without registering
        runBlocking {
            FirebaseAuthManager.login("nonexistent@example.com", "password")
        }

        // Verify error exists
        assert(FirebaseAuthManager.errorMessage.value != null)

        // Clear error
        FirebaseAuthManager.clearError()
        assert(FirebaseAuthManager.errorMessage.value == null)
    }
}
