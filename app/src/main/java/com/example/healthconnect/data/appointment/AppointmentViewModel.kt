package com.example.healthconnect.data.appointment

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.example.healthconnect.data.doctor.AvailabilityData
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

data class AppointmentState(
    val doctorId: String = "",
    val patientId: String = "",
    val patientName: String = "",
    val doctorName: String = "",
    val date: String = "",
    val time: String = "",
    val reason: String = "",
    val status: String = "upcoming"
)

enum class CreateAppointmentResult {
    SUCCESS,
    ALREADY_EXISTS,
    FAILURE
}

class AppointmentViewModel : ViewModel() {

    private val _doctorAvailability = MutableStateFlow(AvailabilityData())
    val doctorAvailability = _doctorAvailability.asStateFlow()

    private val firestore: FirebaseFirestore by lazy { FirebaseFirestore.getInstance() }
    private val auth: FirebaseAuth by lazy { FirebaseAuth.getInstance() }

    fun fetchDoctorAvailability(doctorId: String) {
        viewModelScope.launch {
            try {
                // Fetch from "users" collection (where doctors are stored with role="doctor")
                val snapshot = firestore.collection("users").document(doctorId).get().await()
                if (snapshot.exists()) {
                    // Try to get availability data from the document
                    val data = snapshot.data
                    if (data != null) {
                        val schedule = when {
                            data.containsKey("schedule") -> {
                                // New format: schedule is a map
                                val scheduleMap = data["schedule"] as? Map<*, *> ?: emptyMap<Any, Any>()
                                scheduleMap.mapNotNull { (day, scheduleData) ->
                                    if (day is String && scheduleData is Map<*, *>) {
                                        val startTime = scheduleData["startTime"] as? String ?: "09:00"
                                        val endTime = scheduleData["endTime"] as? String ?: "17:00"
                                        day to com.example.healthconnect.data.doctor.DaySchedule(startTime, endTime)
                                    } else null
                                }.toMap()
                            }
                            data.containsKey("workingDays") -> {
                                // Old format: convert to new format
                                val workingDays = (data["workingDays"] as? List<*>)?.mapNotNull { it as? String } ?: emptyList()
                                val startTime = data["startTime"] as? String ?: "09:00"
                                val endTime = data["endTime"] as? String ?: "17:00"
                                workingDays.associateWith { 
                                    com.example.healthconnect.data.doctor.DaySchedule(startTime, endTime) 
                                }
                            }
                            else -> emptyMap()
                        }
                        _doctorAvailability.value = AvailabilityData(schedule)
                    }
                }
            } catch (e: Exception) {
                // Handle error - availability will remain empty/default
                e.printStackTrace()
            }
        }
    }

    fun createAppointment(appointment: AppointmentState, onResult: (CreateAppointmentResult) -> Unit) {
        viewModelScope.launch {
            try {
                val patientId = appointment.patientId.ifBlank { auth.currentUser?.uid ?: "" }
                if (patientId.isBlank() || appointment.doctorId.isBlank()) {
                    onResult(CreateAppointmentResult.FAILURE)
                    return@launch
                }

                val existing = firestore.collection("appointments")
                    .whereEqualTo("patientId", patientId)
                    .whereEqualTo("doctorId", appointment.doctorId)
                    .whereEqualTo("status", "upcoming")
                    .limit(1)
                    .get()
                    .await()

                if (!existing.isEmpty) {
                    onResult(CreateAppointmentResult.ALREADY_EXISTS)
                    return@launch
                }

                firestore.collection("appointments").add(appointment.copy(patientId = patientId)).await()
                onResult(CreateAppointmentResult.SUCCESS)
            } catch (e: Exception) {
                onResult(CreateAppointmentResult.FAILURE)
            }
        }
    }
}