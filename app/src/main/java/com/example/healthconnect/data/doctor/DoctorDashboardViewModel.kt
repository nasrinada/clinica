package com.example.healthconnect.data.doctor

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

// Represents an appointment to be displayed on the doctor's dashboard
data class Appointment(
    val id: String,
    val patientName: String,
    val date: String,
    val time: String,
    val reason: String
)

class DoctorDashboardViewModel : ViewModel() {

    private val _appointments = MutableStateFlow<List<Appointment>>(emptyList())
    val appointments = _appointments.asStateFlow()

    private val firestore: FirebaseFirestore by lazy { FirebaseFirestore.getInstance() }
    private val auth: FirebaseAuth by lazy { FirebaseAuth.getInstance() }

    init {
        fetchUpcomingAppointments()
    }

    fun refresh() {
        fetchUpcomingAppointments()
    }

    private fun fetchUpcomingAppointments() {
        viewModelScope.launch {
            val userId = auth.currentUser?.uid
            if (userId != null) {
                try {
                    val snapshot = firestore.collection("appointments")
                        .whereEqualTo("doctorId", userId)
                        .whereEqualTo("status", "upcoming") // Filter for upcoming appointments
                        .orderBy("date", Query.Direction.ASCENDING)
                        .get()
                        .await()

                    val appointmentList = snapshot.documents.mapNotNull { doc ->
                        Appointment(
                            id = doc.id,
                            patientName = doc.getString("patientName") ?: "N/A",
                            date = doc.getString("date") ?: "N/A",
                            time = doc.getString("time") ?: "N/A",
                            reason = doc.getString("reason") ?: "No reason provided"
                        )
                    }
                    _appointments.value = appointmentList
                } catch (e: Exception) {
                    // Handle error, e.g., log it or update a UI state
                }
            }
        }
    }
    
    fun cancelAppointment(appointmentId: String, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            try {
                firestore.collection("appointments").document(appointmentId)
                    .update("status", "cancelled")
                    .await()
                onResult(true)
                fetchUpcomingAppointments() // Refresh the list
            } catch (e: Exception) {
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
                fetchUpcomingAppointments() // Refresh the list
            } catch (e: Exception) {
                onResult(false)
            }
        }
    }
}