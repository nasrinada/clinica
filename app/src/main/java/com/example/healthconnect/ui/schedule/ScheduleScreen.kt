package com.example.healthconnect.ui.schedule

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.healthconnect.data.appointment.AppointmentState
import com.example.healthconnect.data.appointment.AppointmentViewModel
import com.example.healthconnect.data.appointment.CreateAppointmentResult
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAdjusters
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ScheduleScreen(
    doctorId: String,
    onBack: () -> Unit,
    onConfirm: (String, String) -> Unit,
    appointmentViewModel: AppointmentViewModel = viewModel()
) {
    val availability by appointmentViewModel.doctorAvailability.collectAsState()
    var selectedDayName by remember { mutableStateOf<String?>(null) }
    var selectedDate by remember { mutableStateOf<LocalDate?>(null) }
    var selectedTime by remember { mutableStateOf<String?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    val upcomingDates = remember(availability) {
        buildUpcomingDateOptions(availability.workingDays, countPerDay = 3)
    }

    LaunchedEffect(doctorId) {
        appointmentViewModel.fetchDoctorAvailability(doctorId)
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Schedule Appointment", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        bottomBar = {
            Box(
                modifier = Modifier
                    .padding(horizontal = 24.dp, vertical = 16.dp)
                    .fillMaxWidth()
            ) {
                Button(
                    onClick = {
                        val appointment = AppointmentState(
                            doctorId = doctorId,
                            patientId = FirebaseAuth.getInstance().currentUser?.uid ?: "",
                            date = selectedDate?.toString() ?: "",
                            time = selectedTime!!,
                            reason = "General Checkup" // Placeholder
                        )
                        appointmentViewModel.createAppointment(appointment) { result ->
                            when (result) {
                                CreateAppointmentResult.SUCCESS -> onConfirm(selectedDate?.toString() ?: "", selectedTime!!)
                                CreateAppointmentResult.ALREADY_EXISTS -> {
                                    scope.launch {
                                        snackbarHostState.showSnackbar("You already have an upcoming appointment with this doctor")
                                    }
                                }
                                CreateAppointmentResult.FAILURE -> {
                                    scope.launch {
                                        snackbarHostState.showSnackbar("Failed to create appointment")
                                    }
                                }
                            }
                        }
                    },
                    enabled = selectedDayName != null && selectedDate != null && selectedTime != null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF3B82F6)
                    )
                ) {
                    Text("Confirm Appointment", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(it)
                .padding(24.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Date Selection
            Column {
                Text("Select a Date", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                Spacer(modifier = Modifier.height(16.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    upcomingDates.forEach { option ->
                        DateChip(
                            label = option.label,
                            isSelected = selectedDate == option.date,
                            onToggle = {
                                selectedDayName = option.dayName
                                selectedDate = option.date
                                selectedTime = null
                            }
                        )
                    }
                }
            }

            if (selectedDate != null) {
                val selectedDateLabel = remember(selectedDate) {
                    selectedDate!!.format(DateTimeFormatter.ofPattern("EEE dd/MM/yyyy", Locale.getDefault()))
                }
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Selected", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color(0xFF111827))
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = if (selectedTime != null) "$selectedDateLabel • $selectedTime" else selectedDateLabel,
                            fontSize = 14.sp,
                            color = Color(0xFF374151)
                        )
                    }
                }
            }

            // Time Selection
            if (selectedDayName != null) {
                val daySchedule = availability.getScheduleForDay(selectedDayName!!)
                if (daySchedule != null) {
                    Column {
                        Text("Select a Time", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        Text(
                            "${daySchedule.startTime} - ${daySchedule.endTime}",
                            fontSize = 14.sp,
                            color = Color(0xFF6B7280)
                        )
                        Spacer(modifier = Modifier.height(16.dp))

                        // Generate time slots based on the day's schedule
                        val timeSlots = generateTimeSlots(daySchedule.startTime, daySchedule.endTime)
                        if (timeSlots.isNotEmpty()) {
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                timeSlots.forEach { time ->
                                    TimeChip(
                                        time = time,
                                        isSelected = selectedTime == time,
                                        onToggle = { selectedTime = time }
                                    )
                                }
                            }
                        } else {
                            Text(
                                "No available time slots for this day",
                                color = Color(0xFF9CA3AF),
                                fontSize = 14.sp
                            )
                        }
                    }
                } else {
                    Text(
                        "No schedule set for ${selectedDayName}",
                        color = Color(0xFF9CA3AF),
                        fontSize = 14.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun DateChip(label: String, isSelected: Boolean, onToggle: () -> Unit) {
    val backgroundColor = if (isSelected) Color(0xFF3B82F6) else Color(0xFFF3F4F6)
    val textColor = if (isSelected) Color.White else Color(0xFF6B7280)

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(backgroundColor)
            .clickable(onClick = onToggle)
            .padding(horizontal = 20.dp, vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            if (isSelected) {
                Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.height(16.dp))
            }
            Text(label, color = textColor, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium, fontSize = 14.sp)
        }
    }
}

private data class DateOption(
    val dayName: String,
    val date: LocalDate,
    val label: String
)

private fun buildUpcomingDateOptions(workingDays: List<String>, countPerDay: Int): List<DateOption> {
    val formatter = DateTimeFormatter.ofPattern("EEE dd/MM/yyyy", Locale.getDefault())
    val today = LocalDate.now()
    val options = mutableListOf<DateOption>()

    workingDays.forEach { dayName ->
        val dayOfWeek = parseDayOfWeek(dayName) ?: return@forEach
        val first = today.with(TemporalAdjusters.nextOrSame(dayOfWeek))
        repeat(countPerDay) { idx ->
            val date = first.plusWeeks(idx.toLong())
            options.add(
                DateOption(
                    dayName = dayName,
                    date = date,
                    label = date.format(formatter)
                )
            )
        }
    }

    return options.sortedBy { it.date }
}

private fun parseDayOfWeek(dayName: String): DayOfWeek? {
    val normalized = dayName.trim().lowercase(Locale.ROOT)
    return when (normalized) {
        "monday", "mon", "lundi" -> DayOfWeek.MONDAY
        "tuesday", "tue", "mardi" -> DayOfWeek.TUESDAY
        "wednesday", "wed", "mercredi" -> DayOfWeek.WEDNESDAY
        "thursday", "thu", "jeudi" -> DayOfWeek.THURSDAY
        "friday", "fri", "vendredi" -> DayOfWeek.FRIDAY
        "saturday", "sat", "samedi" -> DayOfWeek.SATURDAY
        "sunday", "sun", "dimanche" -> DayOfWeek.SUNDAY
        else -> null
    }
}

@Composable
private fun TimeChip(time: String, isSelected: Boolean, onToggle: () -> Unit) {
    val borderColor = if (isSelected) Color(0xFF3B82F6) else Color(0xFFE5E7EB)
    val backgroundColor = if (isSelected) Color(0xFFEFF6FF) else Color.Transparent

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(backgroundColor)
            .border(1.dp, borderColor, RoundedCornerShape(12.dp))
            .clickable(onClick = onToggle)
            .padding(horizontal = 20.dp, vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(time, color = if (isSelected) Color(0xFF3B82F6) else Color(0xFF6B7280), fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
    }
}

private fun generateTimeSlots(startTime: String, endTime: String, intervalMinutes: Int = 30): List<String> {
    val slots = mutableListOf<String>()
    try {
        val start = parseTime(startTime)
        val end = parseTime(endTime)
        var current = start
        
        while (current < end) {
            slots.add(formatTime(current))
            current += intervalMinutes
        }
    } catch (e: Exception) {
        // If parsing fails, return empty list or default slots
        return emptyList()
    }
    return slots
}

private fun parseTime(timeStr: String): Int {
    val parts = timeStr.split(":")
    if (parts.size == 2) {
        val hours = parts[0].toIntOrNull() ?: 0
        val minutes = parts[1].toIntOrNull() ?: 0
        return hours * 60 + minutes
    }
    return 0
}

private fun formatTime(minutes: Int): String {
    val hours = minutes / 60
    val mins = minutes % 60
    return String.format("%02d:%02d", hours, mins)
}