package com.example.healthconnect.data.map

import android.R
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.GeoPoint as FirestoreGeoPoint
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import org.osmdroid.util.GeoPoint

data class DoctorLocation(
    val id: String,
    val name: String,
    val specialty: String,
    val position: GeoPoint,
    val rating: Double,
    val phone: String? = null
)

class MapViewModel : ViewModel() {
    
    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()
    private  val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()
    private  val _allDoctors = MutableStateFlow<List<DoctorLocation>>(emptyList())
    private  val _doctors = MutableStateFlow<List<DoctorLocation>>(emptyList())
    val doctors = _doctors.asStateFlow()


    private val firestore: FirebaseFirestore by lazy { FirebaseFirestore.getInstance() }
    
    init {
        fetchDoctors()
    }
    
    fun fetchDoctors() {
        _isLoading.value = true
        viewModelScope.launch {
            try {
                val snapshot = firestore.collection("users")
                    .whereEqualTo("role", "doctor")
                    .get()
                    .await()
                
                val doctorList = mutableListOf<DoctorLocation>()
                
                for (doc in snapshot.documents) {
                    val name = doc.getString("name") ?: "Unknown Doctor"
                    val specialty = doc.getString("specialty") ?: "General"
                    val rating = (doc.getDouble("rating") ?: doc.get("rating") as? Number)?.toDouble() ?: 0.0
                    val phone = doc.getString("phone")
                    
                    // Try to get location - could be in different formats
                    var lat = 35.038 // Default to Sidi Bouzid, Tunisia
                    var lng = 9.485
                    
                    val locationValue = doc.get("location")
                    
                    // Handle Firestore GeoPoint (most common case)
                    if (locationValue is FirestoreGeoPoint) {
                        lat = locationValue.latitude
                        lng = locationValue.longitude
                    } else {
                        // Try string format "lat,lng"
                        val location = doc.getString("location") ?: doc.getString("address") ?: ""
                        if (location.isNotEmpty()) {
                            try {
                                if (location.contains(",")) {
                                    val parts = location.split(",")
                                    if (parts.size == 2) {
                                        lat = parts[0].trim().toDoubleOrNull() ?: lat
                                        lng = parts[1].trim().toDoubleOrNull() ?: lng
                                    }
                                }
                            } catch (e: Exception) {
                                // If parsing fails, use default location
                            }
                        }
                        
                        // Try separate latitude/longitude fields
                        val latitude = doc.getDouble("latitude") ?: doc.get("latitude") as? Number
                        val longitude = doc.getDouble("longitude") ?: doc.get("longitude") as? Number
                        
                        if (latitude != null && longitude != null) {
                            lat = latitude.toDouble()
                            lng = longitude.toDouble()
                        }
                    }
                    
                    doctorList.add(
                        DoctorLocation(
                            id = doc.id,
                            name = name,
                            specialty = specialty,
                            position = GeoPoint(lat, lng),
                            rating = rating,
                            phone = phone
                        )
                    )
                }
                _allDoctors.value = doctorList
                _doctors.value = doctorList
                _isLoading.value = false
            } catch (e: Exception) {
                _isLoading.value = false
                e.printStackTrace()
            }
        }
    }
    fun onSearchQueryChage(query : String){
        _searchQuery.value = query
        val trimmed = query.trim().lowercase()
        _doctors.value =
            if(trimmed.isEmpty()){
                _allDoctors.value
            }else{
                _allDoctors.value.filter{
                    doctor->doctor.specialty.lowercase().contains(trimmed)
                }
            }
    }
    
    fun refresh() {
        fetchDoctors()
    }
}

