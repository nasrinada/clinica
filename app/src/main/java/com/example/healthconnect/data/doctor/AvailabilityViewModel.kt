package com.example.healthconnect.data.doctor

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

data class DaySchedule(
    val startTime: String,
    val endTime: String
)

data class AvailabilityData(
    val schedule: Map<String, DaySchedule> = emptyMap()
) {
    // Helper properties for backward compatibility
    val workingDays: List<String> get() = schedule.keys.toList()
    val startTime: String get() = schedule.values.firstOrNull()?.startTime ?: "09:00"
    val endTime: String get() = schedule.values.firstOrNull()?.endTime ?: "17:00"
    
    // Get schedule for a specific day
    fun getScheduleForDay(day: String): DaySchedule? = schedule[day]
}

class AvailabilityViewModel : ViewModel() {

    private val _availability = MutableStateFlow(AvailabilityData())
    val availability = _availability.asStateFlow()

    private val firestore: FirebaseFirestore by lazy { FirebaseFirestore.getInstance() }
    private val auth: FirebaseAuth by lazy { FirebaseAuth.getInstance() }

    init {
        fetchAvailability()
    }

    private fun fetchAvailability() {
        viewModelScope.launch {
            val userId = auth.currentUser?.uid
            if (userId != null) {
                try {
                    val document = firestore.collection("users").document(userId).get().await()
                    val data = document.data
                    if (data != null) {
                        // Handle both old format (workingDays + startTime/endTime) and new format (schedule)
                        val schedule = when {
                            data.containsKey("schedule") -> {
                                // New format: schedule is a map
                                val scheduleMap = data["schedule"] as? Map<*, *> ?: emptyMap<Any, Any>()
                                scheduleMap.mapNotNull { (day, scheduleData) ->
                                    if (day is String && scheduleData is Map<*, *>) {
                                        val startTime = scheduleData["startTime"] as? String ?: "09:00"
                                        val endTime = scheduleData["endTime"] as? String ?: "17:00"
                                        day to DaySchedule(startTime, endTime)
                                    } else null
                                }.toMap()
                            }
                            data.containsKey("workingDays") -> {
                                // Old format: convert to new format
                                val workingDays = (data["workingDays"] as? List<*>)?.mapNotNull { it as? String } ?: emptyList()
                                val startTime = data["startTime"] as? String ?: "09:00"
                                val endTime = data["endTime"] as? String ?: "17:00"
                                workingDays.associateWith { DaySchedule(startTime, endTime) }
                            }
                            else -> emptyMap()
                        }
                        _availability.value = AvailabilityData(schedule)
                    }
                } catch (e: Exception) {
                    // Handle error
                    e.printStackTrace()
                }
            }
        }
    }

    fun updateAvailability(schedule: Map<String, DaySchedule>, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            val userId = auth.currentUser?.uid
            if (userId != null) {
                try {
                    // Convert schedule map to Firestore-compatible format
                    val scheduleMap = schedule.mapValues { (_, daySchedule) ->
                        mapOf(
                            "startTime" to daySchedule.startTime,
                            "endTime" to daySchedule.endTime
                        )
                    }
                    
                    val updates = mapOf(
                        "schedule" to scheduleMap,
                        // Keep old fields for backward compatibility (can be removed later)
                        "workingDays" to schedule.keys.toList(),
                        "startTime" to (schedule.values.firstOrNull()?.startTime ?: "09:00"),
                        "endTime" to (schedule.values.firstOrNull()?.endTime ?: "17:00")
                    )
                    
                    firestore.collection("users").document(userId).set(updates, SetOptions.merge()).await()
                    fetchAvailability() // Refresh data
                    onResult(true)
                } catch (e: Exception) {
                    e.printStackTrace()
                    onResult(false)
                }
            }
        }
    }
}