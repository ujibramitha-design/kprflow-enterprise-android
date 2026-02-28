package com.kprflow.enterprise.viewmodel

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.kprflow.enterprise.data.model.UserProfile
import com.kprflow.enterprise.data.model.UserRole
import com.kprflow.enterprise.domain.repository.IAuthRepository
import com.kprflow.enterprise.ui.viewmodel.AuthViewModel
import com.kprflow.enterprise.util.Resource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.mockito.MockitoAnnotations
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

/**
 * Example unit test demonstrating how interface injection enables easy testing
 * This test would be impossible with direct repository injection
 */
@ExperimentalCoroutinesApi
class AuthViewModelTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    @Mock
    private lateinit var mockAuthRepository: IAuthRepository

    private lateinit var authViewModel: AuthViewModel

    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)
        Dispatchers.setMain(testDispatcher)
        
        // ViewModel with mocked interface - easy to test!
        authViewModel = AuthViewModel(mockAuthRepository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `signIn success should update login state`() = runTest {
        // Given
        val testUser = UserProfile(
            id = "test-id",
            name = "Test User",
            email = "test@example.com",
            nik = "1234567890123456",
            phoneNumber = "08123456789",
            maritalStatus = "Single",
            role = UserRole.CUSTOMER,
            createdAt = "2024-01-01T00:00:00Z",
            updatedAt = "2024-01-01T00:00:00Z",
            isActive = true
        )
        
        `when`(mockAuthRepository.signIn("test@example.com", "password"))
            .thenReturn(Result.success(testUser))
        
        // When
        authViewModel.signIn("test@example.com", "password")
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        verify(mockAuthRepository).signIn("test@example.com", "password")
        
        val loginState = authViewModel.loginState.value
        assert(loginState is Resource.Success)
        assertEquals(testUser, (loginState as Resource.Success).data)
    }

    @Test
    fun `signIn failure should update login state with error`() = runTest {
        // Given
        val errorMessage = "Invalid credentials"
        `when`(mockAuthRepository.signIn("test@example.com", "wrong"))
            .thenReturn(Result.failure(Exception(errorMessage)))
        
        // When
        authViewModel.signIn("test@example.com", "wrong")
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        verify(mockAuthRepository).signIn("test@example.com", "wrong")
        
        val loginState = authViewModel.loginState.value
        assert(loginState is Resource.Error)
        assertEquals(errorMessage, (loginState as Resource.Error).message)
    }

    @Test
    fun `signUp success should update register state`() = runTest {
        // Given
        val testUser = UserProfile(
            id = "new-user-id",
            name = "New User",
            email = "new@example.com",
            nik = "1234567890123456",
            phoneNumber = "08123456789",
            maritalStatus = "Single",
            role = UserRole.CUSTOMER,
            createdAt = "2024-01-01T00:00:00Z",
            updatedAt = "2024-01-01T00:00:00Z",
            isActive = true
        )
        
        `when`(mockAuthRepository.signUp(
            "new@example.com", 
            "password", 
            "New User", 
            "1234567890123456", 
            "08123456789", 
            "Single"
        )).thenReturn(Result.success(testUser))
        
        // When
        authViewModel.signUp(
            "new@example.com", 
            "password", 
            "New User", 
            "1234567890123456", 
            "08123456789", 
            "Single"
        )
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        verify(mockAuthRepository).signUp(
            "new@example.com", 
            "password", 
            "New User", 
            "1234567890123456", 
            "08123456789", 
            "Single"
        )
        
        val registerState = authViewModel.registerState.value
        assert(registerState is Resource.Success)
        assertEquals(testUser, (registerState as Resource.Success).data)
    }

    @Test
    fun `signOut should clear user state`() = runTest {
        // Given
        `when`(mockAuthRepository.signOut()).thenReturn(Result.success(Unit))
        
        // When
        authViewModel.signOut()
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        verify(mockAuthRepository).signOut()
        
        assertNull(authViewModel.currentUser.value)
        assertEquals(false, authViewModel.isLoggedIn.value)
    }

    @Test
    fun `checkCurrentUser when user is logged in`() = runTest {
        // Given
        val testUser = UserProfile(
            id = "test-id",
            name = "Test User",
            email = "test@example.com",
            nik = "1234567890123456",
            phoneNumber = "08123456789",
            maritalStatus = "Single",
            role = UserRole.CUSTOMER,
            createdAt = "2024-01-01T00:00:00Z",
            updatedAt = "2024-01-01T00:00:00Z",
            isActive = true
        )
        
        `when`(mockAuthRepository.isUserLoggedIn()).thenReturn(true)
        `when`(mockAuthRepository.getCurrentUser()).thenReturn(testUser)
        
        // When
        val newViewModel = AuthViewModel(mockAuthRepository)
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        assertEquals(testUser, newViewModel.currentUser.value)
        assertEquals(true, newViewModel.isLoggedIn.value)
    }

    @Test
    fun `checkCurrentUser when user is not logged in`() = runTest {
        // Given
        `when`(mockAuthRepository.isUserLoggedIn()).thenReturn(false)
        `when`(mockAuthRepository.getCurrentUser()).thenReturn(null)
        
        // When
        val newViewModel = AuthViewModel(mockAuthRepository)
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        assertNull(newViewModel.currentUser.value)
        assertEquals(false, newViewModel.isLoggedIn.value)
    }

    @Test
    fun `resetPassword success`() = runTest {
        // Given
        `when`(mockAuthRepository.resetPassword("test@example.com"))
            .thenReturn(Result.success(Unit))
        
        // When
        authViewModel.resetPassword("test@example.com")
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        verify(mockAuthRepository).resetPassword("test@example.com")
        
        val resetState = authViewModel.resetPasswordState.value
        assert(resetState is Resource.Success)
    }

    @Test
    fun `resetPassword failure`() = runTest {
        // Given
        val errorMessage = "Email not found"
        `when`(mockAuthRepository.resetPassword("nonexistent@example.com"))
            .thenReturn(Result.failure(Exception(errorMessage)))
        
        // When
        authViewModel.resetPassword("nonexistent@example.com")
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        verify(mockAuthRepository).resetPassword("nonexistent@example.com")
        
        val resetState = authViewModel.resetPasswordState.value
        assert(resetState is Resource.Error)
        assertEquals(errorMessage, (resetState as Resource.Error).message)
    }
}
