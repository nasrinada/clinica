package com.example.healthconnect.data.auth

import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

data class LoginState(
    val isLoading: Boolean = false,
    val isSuccess: Boolean = false,
    val error: String? = null,
    val isPasswordResetEmailSent: Boolean = false,
    val userRole: String? = null
)

class LoginViewModel : ViewModel() {

    var loginState by mutableStateOf(LoginState())
        private set

    fun login(email: String, password: String) {
        viewModelScope.launch {
            loginState = loginState.copy(isLoading = true, error = null)
            try {
                val authResult = Firebase.auth.signInWithEmailAndPassword(email, password).await()
                val user = authResult.user
                if (user != null) {
                    val userDoc = Firebase.firestore.collection("users").document(user.uid).get().await()
                    val isBlocked = userDoc.getBoolean("isBlocked") ?: false
                    if (isBlocked) {
                        Firebase.auth.signOut()
                        loginState = loginState.copy(
                            isLoading = false,
                            isSuccess = false,
                            userRole = null,
                            error = "Your account has been blocked. Please contact support."
                        )
                        return@launch
                    }
                    val role = userDoc.getString("role")
                    loginState = loginState.copy(isLoading = false, isSuccess = true, userRole = role)
                } else {
                    loginState = loginState.copy(isLoading = false, error = "User not found")
                }
            } catch (e: Exception) {
                loginState = loginState.copy(isLoading = false, error = e.message)
            }
        }
    }

    fun sendPasswordResetEmail(email: String) {
        val trimmedEmail = email.trim()
        // Validate email format
        if (trimmedEmail.isBlank()) {
            loginState = loginState.copy(error = "Please enter your email address")
            return
        }
        
        val emailPattern = android.util.Patterns.EMAIL_ADDRESS
        if (!emailPattern.matcher(trimmedEmail).matches()) {
            loginState = loginState.copy(error = "Please enter a valid email address")
            return
        }
        
        viewModelScope.launch {
            loginState = loginState.copy(isLoading = true, error = null, isPasswordResetEmailSent = false)
            try {
                Firebase.auth.sendPasswordResetEmail(trimmedEmail).await()
                Log.i("LoginViewModel", "Password reset email requested successfully for $trimmedEmail")
                loginState = loginState.copy(isLoading = false, isPasswordResetEmailSent = true)
            } catch (e: com.google.firebase.auth.FirebaseAuthInvalidUserException) {
                Log.w("LoginViewModel", "Password reset failed: invalid user", e)
                loginState = loginState.copy(isLoading = false, error = "No account found with this email address")
            } catch (e: FirebaseNetworkException) {
                Log.w("LoginViewModel", "Password reset failed: network", e)
                loginState = loginState.copy(isLoading = false, error = "Network error. Please check your connection and try again")
            } catch (e: FirebaseAuthException) {
                Log.w("LoginViewModel", "Password reset failed: auth errorCode=${e.errorCode}", e)

                val friendlyMessage = when {
                    e.errorCode.equals("ERROR_TOO_MANY_REQUESTS", ignoreCase = true) -> {
                        "Too many attempts. Please try again later"
                    }
                    e.errorCode.contains("RECAPTCHA", ignoreCase = true) -> {
                        "reCAPTCHA/Play Services verification failed. Update Google Play services (or try a real device) and try again"
                    }
                    else -> "Failed to send reset email (${e.errorCode}): ${e.message}"
                }

                loginState = loginState.copy(isLoading = false, error = friendlyMessage)
            } catch (e: Exception) {
                Log.e("LoginViewModel", "Password reset failed: unexpected", e)
                loginState = loginState.copy(isLoading = false, error = "Failed to send reset email: ${e.message}")
            }
        }
    }

    fun clearPasswordResetState() {
        loginState = loginState.copy(isPasswordResetEmailSent = false, error = null)
    }

    fun clearLoginState() {
        loginState = LoginState()
    }
}