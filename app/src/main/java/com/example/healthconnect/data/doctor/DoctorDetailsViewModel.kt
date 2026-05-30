package com.example.healthconnect.data.doctor

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.GeoPoint
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

// Represents a complete doctor profile from Firestore
data class DoctorProfile(
    val name: String = "",
    val specialty: String = "",
    val bio: String = "",
    val location: String = "",
    val locationGeoPoint: GeoPoint? = null,
    val phone: String = "",
    val hours: String = "",
    val imageUrl: String = "",

)

sealed class DoctorDetailsState {
    object Loading : DoctorDetailsState()
    data class Success(val profile: DoctorProfile) : DoctorDetailsState()
    data class Error(val message: String) : DoctorDetailsState()
}

class DoctorDetailsViewModel : ViewModel() {

    private val _doctorProfile = MutableStateFlow(DoctorProfile())
    val doctorProfile = _doctorProfile.asStateFlow()

    private val _state = MutableStateFlow<DoctorDetailsState>(DoctorDetailsState.Loading)
    val state = _state.asStateFlow()

    private val firestore: FirebaseFirestore by lazy { FirebaseFirestore.getInstance() }
    private val TAG = "DoctorDetailsViewModel"

    fun fetchDoctorDetails(doctorId: String) {
        if (doctorId.isEmpty()) {
            _state.value = DoctorDetailsState.Error("Doctor ID is empty")
            Log.e(TAG, "Doctor ID is empty")
            return
        }
        
        _state.value = DoctorDetailsState.Loading
        viewModelScope.launch {
            try {
                Log.d(TAG, "Fetching doctor details for ID: $doctorId")
                
                // Fetch from "users" collection (where doctors are stored with role="doctor")
                val document = firestore.collection("users").document(doctorId).get().await()
                
                if (document.exists()) {
                    Log.d(TAG, "Found doctor in users collection")
                    val data = document.data
                    if (data != null) {
                        // Check if this user is actually a doctor
                        val role = data["role"] as? String ?: ""
                        if (role != "doctor") {
                            val errorMsg = "User with ID $doctorId is not a doctor (role: $role)"
                            _state.value = DoctorDetailsState.Error(errorMsg)
                            Log.e(TAG, errorMsg)
                            return@launch
                        }
                        
                        // Handle location - it might be a GeoPoint or a String
                        val locationValue = data["location"]
                        val locationGeoPoint = locationValue as? GeoPoint
                        val locationString = when {
                            locationGeoPoint != null -> {
                                data["address"] as? String ?: "Location available"
                            }
                            locationValue is String -> locationValue
                            else -> data["address"] as? String ?: "Not specified"
                        }
                        
                        val profile = DoctorProfile(
                            name = data["name"] as? String ?: "Unknown Doctor",
                            specialty = data["specialty"] as? String ?: "General",
                            bio = data["bio"] as? String ?: data["biography"] as? String ?: "No biography available",
                            location = locationString,
                            locationGeoPoint = locationGeoPoint,
                            phone = data["phone"] as? String ?: "Not specified",
                            hours = data["hours"] as? String ?: data["workingHours"] as? String ?: data["availability"] as? String ?: "Not specified",
                            imageUrl = data["imageUrl"] as? String ?: "",

                        )
                        _doctorProfile.value = profile
                        _state.value = DoctorDetailsState.Success(profile)
                        Log.d(TAG, "Successfully loaded doctor profile: ${profile.name}")
                    } else {
                        val errorMsg = "Doctor document exists but contains no data"
                        _state.value = DoctorDetailsState.Error(errorMsg)
                        Log.e(TAG, errorMsg)
                    }
                } else {
                    val errorMsg = "Doctor not found with ID: $doctorId. Make sure the doctor exists in the 'users' collection with role='doctor'"
                    _state.value = DoctorDetailsState.Error(errorMsg)
                    Log.e(TAG, errorMsg)
                }
            } catch (e: com.google.firebase.firestore.FirebaseFirestoreException) {
                val errorMsg = "Firestore error: ${e.message} (Code: ${e.code})"
                _state.value = DoctorDetailsState.Error(errorMsg)
                Log.e(TAG, errorMsg, e)
                e.printStackTrace()
            } catch (e: Exception) {
                val errorMsg = "Error fetching doctor details: ${e.message}"
                _state.value = DoctorDetailsState.Error(errorMsg)
                Log.e(TAG, errorMsg, e)
                e.printStackTrace()
            }
        }
    }
}