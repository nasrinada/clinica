package com.example.healthconnect.ui.results

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.firestore.GeoPoint as FirestoreGeoPoint
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

data class DoctorResult(
    val id: String,
    val name: String,
    val specialty: String,

    val distance: String,
    val imageUrl: String
)

data class ResultsState(
    val doctors: List<DoctorResult> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)

class ResultsViewModel : ViewModel() {

    private val _state = MutableStateFlow(ResultsState())
    val state = _state.asStateFlow()

    fun fetchDoctors(specialtyId: String?) {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, error = null)
            try {
                val currentUserId = Firebase.auth.currentUser?.uid
                val patientLatLng: Pair<Double, Double>? = if (currentUserId != null) {
                    try {
                        val patientDoc = Firebase.firestore.collection("users").document(currentUserId).get().await()
                        val lat = patientDoc.getDouble("locationLat")
                        val lng = patientDoc.getDouble("locationLng")
                        if (lat != null && lng != null) {
                            lat to lng
                        } else {
                            null
                        }
                    } catch (_: Exception) {
                        null
                    }
                } else {
                    null
                }

                var query = Firebase.firestore.collection("users")
                    .whereEqualTo("role", "doctor")
                    .limit(20)

                if (!specialtyId.isNullOrEmpty()) {
                    query = query.whereEqualTo("specialty", specialtyId)
                }

                val snapshot = query.get().await()
                val doctorWithDistance = snapshot.documents.mapNotNull { doc ->
                    val name = doc.getString("name") ?: ""
                    val doctorLatLng = extractLatLng(doc)
                    val distanceKm = if (patientLatLng != null && doctorLatLng != null) {
                        distanceKm(
                            lat1 = patientLatLng.first,
                            lon1 = patientLatLng.second,
                            lat2 = doctorLatLng.first,
                            lon2 = doctorLatLng.second
                        )
                    } else {
                        null
                    }

                    val result = DoctorResult(
                        id = doc.id,
                        name = name,
                        specialty = doc.getString("specialty") ?: "",
                        distance = distanceKm?.let { formatDistanceKm(it) } ?: "—",
                        imageUrl = doc.getString("imageUrl") ?: ""
                    )

                    result to distanceKm
                }

                val sorted = doctorWithDistance
                    .sortedWith(compareBy<Pair<DoctorResult, Double?>> { it.second == null }.thenBy { it.second ?: Double.MAX_VALUE })
                    .map { it.first }

                _state.value = _state.value.copy(isLoading = false, doctors = sorted)
            } catch (e: Exception) {
                Log.e("ResultsViewModel", "Error fetching doctors: ", e)
                _state.value = _state.value.copy(isLoading = false, error = "Failed to fetch doctors. Please check your network connection and Firestore Rules.")
            }
        }
    }

    private fun extractLatLng(doc: com.google.firebase.firestore.DocumentSnapshot): Pair<Double, Double>? {
        val locationValue = doc.get("location")

        if (locationValue is FirestoreGeoPoint) {
            return locationValue.latitude to locationValue.longitude
        }

        val latNum = doc.getDouble("latitude") ?: (doc.get("latitude") as? Number)?.toDouble()
        val lngNum = doc.getDouble("longitude") ?: (doc.get("longitude") as? Number)?.toDouble()
        if (latNum != null && lngNum != null) {
            return latNum to lngNum
        }

        val locationStr = doc.getString("location") ?: doc.getString("address")
        if (!locationStr.isNullOrBlank() && locationStr.contains(",")) {
            val parts = locationStr.split(",")
            if (parts.size == 2) {
                val lat = parts[0].trim().toDoubleOrNull()
                val lng = parts[1].trim().toDoubleOrNull()
                if (lat != null && lng != null) {
                    return lat to lng
                }
            }
        }

        return null
    }

fun distanceKm(
    lat1: Double, lon1: Double,
    lat2: Double, lon2: Double
): Double {
    val results = FloatArray(1)
    android.location.Location.distanceBetween(
        lat1, lon1, lat2, lon2, results
    )
    return results[0] / 1000.0 // meters → km
}


    private fun formatDistanceKm(km: Double): String {
        return when {
            km < 1.0 -> "${(km * 1000).toInt()} m"
            else -> String.format("%.1f km", km)
        }
    }
}