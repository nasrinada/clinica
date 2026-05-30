package com.example.healthconnect.data.profile

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.android.Android
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.serialization.kotlinx.json.json
import com.google.firebase.auth.auth
import com.google.firebase.firestore.firestore
import com.google.firebase.Firebase
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

data class UserProfile(
    val name: String = "",
    val email: String = "",
    val role: String = "",
    val phone: String = "",
    val address: String = "",
    val imageUrl: String = "",
    val locationLat: Double? = null,
    val locationLng: Double? = null,
    val locationSource: String? = null
)

class ProfileViewModel : ViewModel() {

    @Serializable
    private data class NominatimResult(
        val lat: String,
        val lon: String
    )

    private val httpClient = HttpClient(Android) {
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
                coerceInputValues = true
            })
        }
    }

    var userProfile by mutableStateOf(UserProfile())
        private set

    init {
        fetchUserProfile()
    }

    private fun fetchUserProfile() {
        viewModelScope.launch {
            val userId = Firebase.auth.currentUser?.uid
            if (userId != null) {
                try {
                    val document = Firebase.firestore.collection("users").document(userId).get().await()
                    val profile = document.toObject(UserProfile::class.java)
                    if (profile != null) {
                        userProfile = profile
                    }
                } catch (e: Exception) {
                    // Handle error
                }
            }
        }
    }

    fun updateProfile(name: String, phone: String, address: String, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            val userId = Firebase.auth.currentUser?.uid
            if (userId != null) {
                val updates = mapOf(
                    "name" to name,
                    "phone" to phone,
                    "address" to address
                )
                try {
                    Firebase.firestore.collection("users").document(userId).update(updates).await()
                    fetchUserProfile() // Refresh data
                    onResult(true)
                } catch (e: Exception) {
                    onResult(false)
                }
            }
        }
    }

    fun updateProfileImage(imageUrl: String, onResult: (Boolean) -> Unit) {
        val trimmed = imageUrl.trim()
        viewModelScope.launch {
            val userId = Firebase.auth.currentUser?.uid
            if (userId != null) {
                try {
                    Firebase.firestore.collection("users").document(userId).update("imageUrl", trimmed).await()
                    fetchUserProfile()
                    onResult(true)
                } catch (_: Exception) {
                    onResult(false)
                }
            } else {
                onResult(false)
            }
        }
    }

    fun updateLocation(lat: Double, lng: Double, source: String, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            val userId = Firebase.auth.currentUser?.uid
            if (userId != null) {
                val updates = mapOf(
                    "locationLat" to lat,
                    "locationLng" to lng,
                    "locationSource" to source
                )
                try {
                    Firebase.firestore.collection("users").document(userId).update(updates).await()
                    fetchUserProfile()
                    onResult(true)
                } catch (e: Exception) {
                    onResult(false)
                }
            } else {
                onResult(false)
            }
        }
    }

    fun geocodeCityAndSave(query: String, onResult: (Boolean) -> Unit) {
        val trimmed = query.trim()
        if (trimmed.isEmpty()) {
            onResult(false)
            return
        }
        viewModelScope.launch {
            try {
                val results: List<NominatimResult> = httpClient.get("https://nominatim.openstreetmap.org/search") {
                    header("User-Agent", "HealthConnect/1.0")
                    parameter("q", trimmed)
                    parameter("format", "json")
                    parameter("limit", "1")
                }.body()

                val first = results.firstOrNull()
                val lat = first?.lat?.toDoubleOrNull()
                val lng = first?.lon?.toDoubleOrNull()

                if (lat != null && lng != null) {
                    updateLocation(lat, lng, "manual") { ok -> onResult(ok) }
                } else {
                    onResult(false)
                }
            } catch (_: Exception) {
                onResult(false)
            }
        }
    }
}