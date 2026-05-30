package com.example.healthconnect.data.appointment

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

data class PatientAppointment(
    val id: String,
    val doctorId: String,
    val doctorName: String,
    val doctorSpecialty: String,
    val date: String,
    val time: String,
    val reason: String,
    val status: String
)

class PatientAppointmentsViewModel : ViewModel() {

    private val _appointments = MutableStateFlow<List<PatientAppointment>>(emptyList())
    val appointments = _appointments.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()

    private val firestore: FirebaseFirestore by lazy { FirebaseFirestore.getInstance() }
    private val auth: FirebaseAuth by lazy { FirebaseAuth.getInstance() }
    private val TAG = "PatientAppointmentsViewModel"

    init {
        fetchAppointments()
    }

    fun fetchAppointments() {
        viewModelScope.launch {
            val patientId = auth.currentUser?.uid
            if (patientId == null) {
                Log.e(TAG, "No authenticated user")
                return@launch
            }

            _isLoading.value = true
            try {
                // Fetch appointments for this patient

                val appointmentsSnapshot = try {
                    firestore.collection("appointments")
                        .whereEqualTo("patientId", patientId)
                        .whereEqualTo("status", "upcoming")
                        .orderBy("date", Query.Direction.ASCENDING)
                        .orderBy("time", Query.Direction.ASCENDING)
                        .get()
                        .await()
                } catch (e: Exception) {
                    // Fallback: try without time ordering if index doesn't exist
                    Log.w(TAG, "Composite index not found, using date only ordering: ${e.message}")
                    firestore.collection("appointments")
                        .whereEqualTo("patientId", patientId)
                        .whereEqualTo("status", "upcoming")
                        .orderBy("date", Query.Direction.ASCENDING)
                        .get()
                        .await()
                }

                val appointmentList = mutableListOf<PatientAppointment>()

                // Fetch doctor details for each appointment
                for (doc in appointmentsSnapshot.documents) {
                    val doctorId = doc.getString("doctorId") ?: continue
                    val date = doc.getString("date") ?: ""
                    val time = doc.getString("time") ?: ""
                    val reason = doc.getString("reason") ?: "General Checkup"
                    val status = doc.getString("status") ?: "upcoming"

                    // Fetch doctor details
                    try {
                        val doctorDoc = firestore.collection("users").document(doctorId).get().await()
                        if (doctorDoc.exists()) {
                            val doctorData = doctorDoc.data
                            val doctorName = doctorData?.get("name") as? String ?: "Unknown Doctor"
                            val doctorSpecialty = doctorData?.get("specialty") as? String ?: "General"

                            appointmentList.add(
                                PatientAppointment(
                                    id = doc.id,
                                    doctorId = doctorId,
                                    doctorName = doctorName,
                                    doctorSpecialty = doctorSpecialty,
                                    date = date,
                                    time = time,
                                    reason = reason,
                                    status = status
                                )
                            )
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Error fetching doctor details for $doctorId: ${e.message}")
                        // Add appointment with default values if doctor fetch fails
                        appointmentList.add(
                            PatientAppointment(
                                id = doc.id,
                                doctorId = doctorId,
                                doctorName = "Unknown Doctor",
                                doctorSpecialty = "General",
                                date = date,
                                time = time,
                                reason = reason,
                                status = status
                            )
                        )
                    }
                }

                _appointments.value = appointmentList
                Log.d(TAG, "Fetched ${appointmentList.size} appointments")
            } catch (e: Exception) {
                Log.e(TAG, "Error fetching appointments: ${e.message}", e)
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun refresh() {
        fetchAppointments()
    }
    
    fun cancelAppointment(appointmentId: String, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            try {
                firestore.collection("appointments").document(appointmentId)
                    .update("status", "cancelled")
                    .await()
                onResult(true)
                fetchAppointments() // Refresh the list
            } catch (e: Exception) {
                Log.e(TAG, "Error cancelling appointment: ${e.message}", e)
                onResult(false)
            }
        }
    }
    
    fun updateAppointment(appointmentId: String, date: String, time: String, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            try {
                firestore.collection("appointments").document(appointmentId)
                    .update(mapOf("date" to date, "time" to time))
                    .await()
                onResult(true)
                fetchAppointments() // Refresh the list
            } catch (e: Exception) {
                Log.e(TAG, "Error updating appointment: ${e.message}", e)
                onResult(false)
            }
        }
    }
}

