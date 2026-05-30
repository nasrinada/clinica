package com.example.healthconnect.data.admin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.firestore.firestore
import com.google.firebase.Firebase
import com.google.firebase.firestore.GeoPoint
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

data class UserData(val id: String, val name: String, val email: String, val role: String = "", val isBlocked: Boolean = false)

data class DoctorDetails(
    val name: String,
    val specialty: String,
    val phone: String,
    val bio: String,
    val imageUrl: String,
    val location: GeoPoint?
)

class AdminViewModel : ViewModel() {

    private val _users = MutableStateFlow<List<UserData>>(emptyList())
    val users = _users.asStateFlow()

    fun fetchUsersByRole(role: String) {
        viewModelScope.launch {
            try {
                val snapshot = Firebase.firestore.collection("users")
                    .whereEqualTo("role", role)
                    .get()
                    .await()
                
                val userList = snapshot.documents.mapNotNull { doc ->
                    UserData(
                        id = doc.id,
                        name = doc.getString("name") ?: "No Name",
                        email = doc.getString("email") ?: "No Email",
                        role = doc.getString("role") ?: "",
                        isBlocked = doc.getBoolean("isBlocked") ?: false
                    )
                }
                _users.value = userList
            } catch (e: Exception) {
                // Handle error
            }
        }
    }
    
    fun updateDoctor(
        doctorId: String,
        name: String,
        specialty: String,
        phone: String,
        bio: String,
        location: GeoPoint?,
        imageUrl: String?,
        onResult: (Boolean) -> Unit
    ) {
        viewModelScope.launch {
            try {
                val updates = mutableMapOf<String, Any>(
                    "name" to name,
                    "specialty" to specialty,
                    "phone" to phone,
                    "bio" to bio
                )

                if (location != null) {
                    updates["location"] = location
                }
                if (imageUrl != null) {
                    updates["imageUrl"] = imageUrl
                }

                Firebase.firestore.collection("users").document(doctorId).update(updates).await()
                onResult(true)
                // Refresh the user list
                val currentRole = users.value.firstOrNull { it.id == doctorId }?.role ?: "doctor"
                fetchUsersByRole(currentRole)
            } catch (e: Exception) {
                onResult(false)
            }
        }
    }
    
    fun toggleBlockUser(userId: String, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            try {
                val user = users.value.firstOrNull { it.id == userId }
                val newBlockedStatus = !(user?.isBlocked ?: false)
                
                Firebase.firestore.collection("users").document(userId)
                    .update("isBlocked", newBlockedStatus)
                    .await()
                
                onResult(true)
                // Refresh the user list
                val currentRole = user?.role ?: ""
                if (currentRole.isNotEmpty()) {
                    fetchUsersByRole(currentRole)
                }
            } catch (e: Exception) {
                onResult(false)
            }
        }
    }
    
    fun getDoctorDetails(doctorId: String, onResult: (DoctorDetails?) -> Unit) {
        viewModelScope.launch {
            try {
                val doc = Firebase.firestore.collection("users").document(doctorId).get().await()
                if (doc.exists()) {
                    val details = DoctorDetails(
                        name = doc.getString("name") ?: "",
                        specialty = doc.getString("specialty") ?: "",
                        phone = doc.getString("phone") ?: "",
                        bio = doc.getString("bio") ?: "",
                        imageUrl = doc.getString("imageUrl") ?: "",
                        location = doc.getGeoPoint("location")
                    )
                    onResult(details)
                } else {
                    onResult(null)
                }
            } catch (e: Exception) {
                onResult(null)
            }
        }
    }
}