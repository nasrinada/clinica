package com.example.healthconnect.data.auth

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.firestore
import com.google.firebase.Firebase
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.UUID

data class SignupState(
    val isLoading: Boolean = false,
    val isSuccess: Boolean = false,
    val error: String? = null
)

class SignupViewModel : ViewModel() {

    var signupState by mutableStateOf(SignupState())
        private set

    fun clearSignupState() {
        signupState = SignupState()
    }

    fun signup(email: String, password: String, role: String, userDetails: Map<String, Any>) {
        viewModelScope.launch {
            signupState = SignupState(isLoading = true)
            try {
                val authResult = Firebase.auth.createUserWithEmailAndPassword(email, password).await()
                val user = authResult.user
                if (user != null) {
                    // Create the main user document in the "users" collection
                    val userDocument = userDetails.toMutableMap()
                    userDocument["email"] = email
                    userDocument["role"] = role
                    Firebase.firestore.collection("users").document(user.uid).set(userDocument).await()
                }
                signupState = signupState.copy(isLoading = false, isSuccess = true)
            } catch (e: Exception) {
                signupState = signupState.copy(isLoading = false, isSuccess = false, error = e.message)
            }
        }
    }

    fun createDoctorAndSendPasswordEmail(context: Context, email: String, doctorDetails: Map<String, Any>) {
        viewModelScope.launch {
            signupState = SignupState(isLoading = true)

            val trimmedEmail = email.trim()
            if (trimmedEmail.isEmpty()) {
                signupState = signupState.copy(isLoading = false, isSuccess = false, error = "Email is required")
                return@launch
            }

            val defaultApp = FirebaseApp.getInstance()
            val secondaryApp = try {
                FirebaseApp.getInstance("Secondary")
            } catch (_: IllegalStateException) {
                FirebaseApp.initializeApp(context, defaultApp.options, "Secondary")
            }

            val secondaryAuth = FirebaseAuth.getInstance(secondaryApp)
            val tempPassword = (UUID.randomUUID().toString().replace("-", "").take(12) + "aA1!").take(16)

            var createdUser: com.google.firebase.auth.FirebaseUser? = null
            try {
                val authResult = secondaryAuth.createUserWithEmailAndPassword(trimmedEmail, tempPassword).await()
                val user = authResult.user
                createdUser = user

                if (user == null) {
                    signupState = signupState.copy(isLoading = false, isSuccess = false, error = "Failed to create doctor account")
                    return@launch
                }

                val userDocument = doctorDetails.toMutableMap()
                userDocument["email"] = trimmedEmail
                userDocument["role"] = "doctor"

                Firebase.firestore.collection("users").document(user.uid).set(userDocument).await()

                Firebase.auth.sendPasswordResetEmail(trimmedEmail).await()

                signupState = signupState.copy(isLoading = false, isSuccess = true, error = null)
            } catch (e: Exception) {
                try {
                    createdUser?.delete()?.await()
                } catch (_: Exception) {
                }
                signupState = signupState.copy(isLoading = false, isSuccess = false, error = e.message)
            } finally {
                try {
                    secondaryAuth.signOut()
                } catch (_: Exception) {
                }
            }
        }
    }
}