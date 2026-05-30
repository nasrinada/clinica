package com.example.healthconnect.ui.doctor

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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Message
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.healthconnect.data.appointment.AppointmentViewModel
import com.example.healthconnect.data.doctor.Appointment
import com.example.healthconnect.data.doctor.DoctorDashboardViewModel
import com.example.healthconnect.data.profile.ProfileViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAdjusters
import java.util.Locale

@Composable
fun DoctorDashboardScreen(
    onManageProfile: () -> Unit,
    onManageAvailability: () -> Unit,
    onNavigateToConversations: () -> Unit,
    profileViewModel: ProfileViewModel = viewModel(),
    doctorDashboardViewModel: DoctorDashboardViewModel = viewModel()
) {
    val appointments by doctorDashboardViewModel.appointments.collectAsState()
    val userProfile = profileViewModel.userProfile
    val scrollState = rememberScrollState()

    var hasUnreadMessages by remember { mutableStateOf(false) }

    var authUid by remember { mutableStateOf(FirebaseAuth.getInstance().currentUser?.uid) }

    DisposableEffect(Unit) {
        val auth = FirebaseAuth.getInstance()
        val listener = FirebaseAuth.AuthStateListener { firebaseAuth ->
            authUid = firebaseAuth.currentUser?.uid
        }
        auth.addAuthStateListener(listener)
        onDispose { auth.removeAuthStateListener(listener) }
    }

    DisposableEffect(authUid) {
        val uid = authUid
        if (uid == null) {
            hasUnreadMessages = false
            onDispose { }
        } else {
            val reg = FirebaseFirestore.getInstance()
                .collection("conversations")
                .whereArrayContains("participants", uid)
                .addSnapshotListener { snapshot, _ ->
                    hasUnreadMessages = snapshot?.documents?.any { doc ->
                        val blockedBy = doc.getString("blockedBy")
                        if (blockedBy == uid) return@any false
                        val unreadBy = doc.get("unreadBy") as? List<*>
                        unreadBy?.any { it?.toString() == uid } == true
                    } == true
                }
            onDispose { reg.remove() }
        }
    }

    // Refresh appointments on screen load
    LaunchedEffect(Unit) {
        doctorDashboardViewModel.refresh()
    }

    val headerGradient = Brush.horizontalGradient(
        colors = listOf(Color(0xFF3B82F6), Color(0xFF14B8A6))
    )

    Scaffold {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFFF9FAFB))
                .padding(it)
                .verticalScroll(scrollState)
        ) {
            // Header
            Box {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(240.dp)
                        .background(
                            brush = headerGradient,
                            shape = RoundedCornerShape(bottomStart = 48.dp, bottomEnd = 48.dp)
                        )
                )
                Column(modifier = Modifier.padding(start = 24.dp, end = 24.dp, top = 32.dp)) {
                    // Top Bar
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Welcome back,", color = Color.White.copy(alpha = 0.8f), fontSize = 14.sp)
                            Text("Doctor" + userProfile.name, color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            IconButton(
                                onClick = onNavigateToConversations,
                                colors = IconButtonDefaults.iconButtonColors(containerColor = Color.White.copy(alpha = 0.2f))
                            ) {
                                Box {
                                    Icon(Icons.Default.Message, contentDescription = "Messages", tint = Color.White)
                                    if (hasUnreadMessages) {
                                        Box(
                                            modifier = Modifier
                                                .align(Alignment.TopEnd)
                                                .size(10.dp)
                                                .clip(CircleShape)
                                                .background(Color(0xFFEF4444))
                                                .border(1.dp, Color.White, CircleShape)
                                        )
                                    }
                                }
                            }
                            IconButton(
                                onClick = onManageProfile,
                                colors = IconButtonDefaults.iconButtonColors(containerColor = Color.White.copy(alpha = 0.2f))
                            ) {
                                Icon(Icons.Default.Person, contentDescription = "Profile", tint = Color.White)
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(32.dp))


                }
            }

            // Content
            Column(modifier = Modifier.padding(24.dp)) {
                // Next Appointment Card
                val nextAppointment = appointments.firstOrNull()
                if (nextAppointment != null) {
                    Card(
                        modifier = Modifier.fillMaxWidth().shadow(8.dp, RoundedCornerShape(24.dp)),
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF3B82F6))
                    ) {
                        Column(Modifier.padding(24.dp)) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Text("Next Appointment", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                                Icon(Icons.Default.CalendarMonth, contentDescription = null, tint = Color.White)
                            }
                            Spacer(Modifier.height(16.dp))
                            Text(nextAppointment.patientName, color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                            Text(nextAppointment.reason, color = Color.White.copy(alpha = 0.8f), fontSize = 14.sp)
                            Spacer(Modifier.height(16.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.CalendarMonth, contentDescription = null, tint = Color.White.copy(alpha = 0.8f), modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text(formatDisplayDate(nextAppointment.date), color = Color.White, fontSize = 14.sp)
                                }
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Schedule, contentDescription = null, tint = Color.White.copy(alpha = 0.8f), modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text(nextAppointment.time, color = Color.White, fontSize = 14.sp)
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }
                
                // Quick Actions Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White)
                ) {
                    Column(Modifier.padding(24.dp)) {
                        Text("Quick Actions", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Color(0xFF1F2937))
                        Spacer(Modifier.height(16.dp))
                        QuickAction(text = "Set Availability", icon = Icons.Default.Schedule, color = Color(0xFF14B8A6), onClick = onManageAvailability)
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Upcoming Appointments
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Upcoming Appointments", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Color(0xFF1F2937))
                    Text("${appointments.size} total", color = Color(0xFF6B7280), fontSize = 14.sp)
                }

                Spacer(modifier = Modifier.height(16.dp))

                if (appointments.isEmpty()) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White)
                    ) {
                        Box(
                            modifier = Modifier.fillMaxWidth().padding(48.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("No upcoming appointments.", color = Color(0xFF9CA3AF), fontSize = 16.sp)
                        }
                    }
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        appointments.forEach { appointment ->
                            AppointmentCard(
                                appointment = appointment,
                                onCancel = { appointmentId ->
                                    doctorDashboardViewModel.cancelAppointment(appointmentId) { success ->
                                        if (success) doctorDashboardViewModel.refresh()
                                    }
                                },
                                onEdit = { appointmentId, day, time ->
                                    doctorDashboardViewModel.updateAppointment(appointmentId, day, time) { success ->
                                        if (success) doctorDashboardViewModel.refresh()
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun QuickStat(label: String, value: String, icon: ImageVector, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .shadow(4.dp, RoundedCornerShape(16.dp))
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White.copy(alpha = 0.2f))
            .padding(16.dp)
    ) {
        Column {
            Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(24.dp).padding(bottom = 8.dp))
            Text(label, color = Color.White.copy(alpha = 0.8f), fontSize = 12.sp)
            Text(value, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
        }
    }
}

@Composable
private fun QuickAction(text: String, icon: ImageVector, color: Color, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .background(color.copy(alpha = 0.1f))
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .background(color, RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
        }
        Spacer(Modifier.width(16.dp))
        Text(text, color = Color(0xFF1F2937), fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.weight(1f))
        Icon(Icons.Default.ChevronRight, contentDescription = null, tint = Color(0xFF9CA3AF))
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AppointmentCard(
    appointment: Appointment,
    onCancel: (String) -> Unit,
    onEdit: (String, String, String) -> Unit
) {
    val appointmentViewModel: AppointmentViewModel = viewModel()
    val availability by appointmentViewModel.doctorAvailability.collectAsState()

    var showEditDialog by remember { mutableStateOf(false) }
    var showCancelDialog by remember { mutableStateOf(false) }

    var selectedDayName by remember { mutableStateOf<String?>(null) }
    var selectedDate by remember { mutableStateOf<LocalDate?>(null) }
    var selectedTime by remember { mutableStateOf<String?>(null) }

    val upcomingDates = remember(availability) {
        buildUpcomingDateOptions(availability.workingDays, countPerDay = 3)
    }

    LaunchedEffect(showEditDialog) {
        if (showEditDialog) {
            selectedDayName = null
            selectedDate = null
            selectedTime = null
            appointmentViewModel.fetchDoctorAvailability(com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid ?: "")
        }
    }
    
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(8.dp, RoundedCornerShape(24.dp)),
        shape = RoundedCornerShape(24.dp),
        color = Color.White,
        onClick = {}
    ) {
        Column(Modifier.padding(20.dp)) {
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column {
                    Text(appointment.patientName, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color(0xFF1F2937))
                    Text(appointment.reason, color = Color(0xFF3B82F6), fontSize = 14.sp)
                }
                Box(
                    modifier = Modifier
                        .background(Color(0xFFDCFCE7), CircleShape)
                        .padding(horizontal = 12.dp, vertical = 4.dp)
                ) {
                    Text("Upcoming", color = Color(0xFF16A34A), fontSize = 12.sp)
                }
            }
            Spacer(Modifier.height(16.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.CalendarMonth, contentDescription = null, tint = Color(0xFF6B7280), modifier = Modifier.size(16.dp))
                    Spacer(Modifier.height(4.dp))
                    Text(formatDisplayDate(appointment.date), color = Color(0xFF6B7280), fontSize = 14.sp)
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Schedule, contentDescription = null, tint = Color(0xFF6B7280), modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(appointment.time, color = Color(0xFF6B7280), fontSize = 14.sp)
                }
            }
            Spacer(Modifier.height(16.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = { showEditDialog = true },
                    modifier = Modifier.weight(1f).height(40.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEFF6FF), contentColor = Color(0xFF3B82F6))
                ) {
                    Text("Reschedule", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                }
                Button(
                    onClick = { showCancelDialog = true },
                    modifier = Modifier.weight(1f).height(40.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFEE2E2), contentColor = Color(0xFFDC2626))
                ) {
                    Text("Cancel", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
    
    if (showEditDialog) {
        AlertDialog(
            onDismissRequest = { showEditDialog = false },
            title = { Text("Reschedule Appointment") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text("Select a Date", fontWeight = FontWeight.Bold)
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

                    if (selectedDayName != null) {
                        val daySchedule = availability.getScheduleForDay(selectedDayName!!)
                        if (daySchedule != null) {
                            Text("Select a Time", fontWeight = FontWeight.Bold)
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                generateTimeSlots(daySchedule.startTime, daySchedule.endTime).forEach { time ->
                                    TimeChip(
                                        time = time,
                                        isSelected = selectedTime == time,
                                        onToggle = { selectedTime = time }
                                    )
                                }
                            }
                        } else {
                            Text("No schedule set for $selectedDayName", color = Color(0xFF9CA3AF))
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onEdit(appointment.id, selectedDate!!.toString(), selectedTime!!)
                        showEditDialog = false
                    },
                    enabled = selectedDate != null && selectedTime != null
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
    
    if (showCancelDialog) {
        AlertDialog(
            onDismissRequest = { showCancelDialog = false },
            title = { Text("Cancel Appointment") },
            text = { Text("Are you sure you want to cancel this appointment?") },
            confirmButton = {
                Button(
                    onClick = {
                        onCancel(appointment.id)
                        showCancelDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626))
                ) {
                    Text("Yes, Cancel")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCancelDialog = false }) {
                    Text("No")
                }
            }
        )
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
        Text(label, color = textColor, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium, fontSize = 14.sp)
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
            options.add(DateOption(dayName = dayName, date = date, label = date.format(formatter)))
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

private fun formatDisplayDate(date: String): String {
    return try {
        LocalDate.parse(date).format(DateTimeFormatter.ofPattern("EEE dd/MM/yyyy", Locale.getDefault()))
    } catch (_: Exception) {
        date
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
    } catch (_: Exception) {
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
        Text(
            time,
            color = if (isSelected) Color(0xFF3B82F6) else Color(0xFF6B7280),
            fontWeight = FontWeight.SemiBold,
            fontSize = 14.sp
        )
    }
}