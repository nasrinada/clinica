package com.example.healthconnect.ui.home

 import android.Manifest
 import android.content.pm.PackageManager
 import android.location.LocationManager
 import androidx.activity.compose.rememberLauncherForActivityResult
 import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Message
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.SportsGymnastics
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.healthconnect.data.appointment.AppointmentViewModel
import com.example.healthconnect.data.appointment.PatientAppointmentsViewModel
import com.example.healthconnect.data.profile.ProfileViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAdjusters
import java.util.Locale

@Composable
fun DashboardScreen(
    onNavigateToProfile: () -> Unit,
    onNavigateToHome: () -> Unit,
    onNavigateToSchedule: () -> Unit,
    onNavigateToMap: () -> Unit,
    onNavigateToConversations: () -> Unit,
    profileViewModel: ProfileViewModel = viewModel(),
    appointmentsViewModel: PatientAppointmentsViewModel = viewModel()
) {
    var activeTab by remember { mutableStateOf("overview") }
    val scrollState = rememberScrollState()
    val userProfile = profileViewModel.userProfile
    val appointments by appointmentsViewModel.appointments.collectAsState()
    val isLoadingAppointments by appointmentsViewModel.isLoading.collectAsState()

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

    val context = LocalContext.current
    val locationManager = remember {
        context.getSystemService(android.content.Context.LOCATION_SERVICE) as LocationManager
    }

    var manualCity by remember { mutableStateOf("Sidi Bouzid, Tunisia") }
    var locationStatus by remember { mutableStateOf<String?>(null) }

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { granted ->
            if (!granted) {
                locationStatus = "Location permission denied"
                return@rememberLauncherForActivityResult
            }

            val hasFine = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED

            val hasCoarse = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED

            if (!hasFine && !hasCoarse) {
                locationStatus = "Location permission denied"
                return@rememberLauncherForActivityResult
            }

            val location = try {
                locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
                    ?: locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
                    ?: locationManager.getLastKnownLocation(LocationManager.PASSIVE_PROVIDER)
            } catch (_: SecurityException) {
                null
            }

            if (location == null) {
                locationStatus = "Could not read your location. Try manual city."
            } else {
                profileViewModel.updateLocation(location.latitude, location.longitude, "gps") { ok ->
                    locationStatus = if (ok) "Location saved" else "Failed to save location"
                }
            }
        }
    )

    // Refresh appointments when switching to appointments tab
    LaunchedEffect(activeTab) {
        if (activeTab == "appointments") {
            appointmentsViewModel.refresh()
        }
    }

    val headerGradient = Brush.horizontalGradient(
        colors = listOf(Color(0xFF3B82F6), Color(0xFF14B8A6))
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF9FAFB))
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
                        Text(userProfile.name, color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        IconButton(
                            onClick = onNavigateToMap,
                            colors = IconButtonDefaults.iconButtonColors(containerColor = Color.White.copy(alpha = 0.2f))
                        ) {
                            Icon(Icons.Default.Map, contentDescription = "Map", tint = Color.White)
                        }
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
                            onClick = onNavigateToProfile,
                            colors = IconButtonDefaults.iconButtonColors(containerColor = Color.White.copy(alpha = 0.2f))
                        ) {
                            Icon(Icons.Default.Person, contentDescription = "Profile", tint = Color.White)
                        }
                    }
                }
                Spacer(modifier = Modifier.height(32.dp))


                Spacer(modifier = Modifier.height(32.dp))

                // Tabs
                TabRow(activeTab = activeTab, onTabChange = { activeTab = it })
            }
        }

        // Content based on tab
        Column(modifier = Modifier.padding(24.dp)) {
            if (activeTab == "overview") {
                if (userProfile.locationLat == null || userProfile.locationLng == null) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .shadow(6.dp, RoundedCornerShape(20.dp)),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White)
                    ) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Text(
                                text = "Set your location",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = Color(0xFF1F2937)
                            )
                            Text(
                                text = "We use it to calculate the distance to nearby doctors.",
                                fontSize = 13.sp,
                                color = Color(0xFF6B7280)
                            )

                            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                                Button(
                                    onClick = {
                                        locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
                                    },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(14.dp)
                                ) {
                                    Text("Enable GPS")
                                }
                                Button(
                                    onClick = {
                                        profileViewModel.geocodeCityAndSave(manualCity) { ok ->
                                            locationStatus = if (ok) "Location saved" else "Could not find that city"
                                        }
                                    },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(14.dp)
                                ) {
                                    Text("Use City")
                                }
                            }

                            androidx.compose.material3.OutlinedTextField(
                                value = manualCity,
                                onValueChange = { manualCity = it },
                                label = { Text("City (e.g. Sidi Bouzid, Tunisia)") },
                                modifier = Modifier.fillMaxWidth()
                            )

                            locationStatus?.let {
                                Text(it, color = Color(0xFF6B7280), fontSize = 12.sp)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                }

                OverviewTab(
                    appointments = appointments,
                    isLoading = isLoadingAppointments,
                    onNavigateToHome = onNavigateToHome,
                    onNavigateToSchedule = onNavigateToSchedule
                )
            } else {
                AppointmentsTab(
                    appointments = appointments,
                    isLoading = isLoadingAppointments,
                    appointmentsViewModel = appointmentsViewModel
                )
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
private fun TabRow(activeTab: String, onTabChange: (String) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(8.dp, RoundedCornerShape(16.dp))
            .background(Color.White, RoundedCornerShape(16.dp))
            .padding(4.dp)
    ) {
        TabButton(text = "Overview", isSelected = activeTab == "overview", onClick = { onTabChange("overview") }, modifier = Modifier.weight(1f))
        TabButton(text = "Appointments", isSelected = activeTab == "appointments", onClick = { onTabChange("appointments") }, modifier = Modifier.weight(1f))
    }
}

@Composable
private fun TabButton(text: String, isSelected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val background = if (isSelected) {
        Brush.horizontalGradient(colors = listOf(Color(0xFF3B82F6), Color(0xFF14B8A6)))
    } else {
        Brush.horizontalGradient(colors = listOf(Color.Transparent, Color.Transparent))
    }
    Button(
        onClick = onClick,
        modifier = modifier.height(48.dp),
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent, contentColor = if(isSelected) Color.White else Color(0xFF4B5563)),
        contentPadding = PaddingValues()
    ) {
        Box(modifier = Modifier.fillMaxSize().background(background, RoundedCornerShape(12.dp)), contentAlignment = Alignment.Center) {
            Text(text, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun OverviewTab(
    appointments: List<com.example.healthconnect.data.appointment.PatientAppointment>,
    isLoading: Boolean,
    onNavigateToHome: () -> Unit,
    onNavigateToSchedule: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        // Next Appointment
        val nextAppointment = appointments.firstOrNull()
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
                if (isLoading) {
                    Text("Loading...", color = Color.White.copy(alpha = 0.8f), fontSize = 14.sp)
                } else if (nextAppointment != null) {
                    Text("Dr. ${nextAppointment.doctorName}", color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                    Text(nextAppointment.doctorSpecialty, color = Color.White.copy(alpha = 0.8f), fontSize = 14.sp)
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
                } else {
                    Text("No upcoming appointments", color = Color.White.copy(alpha = 0.8f), fontSize = 14.sp)
                    Spacer(Modifier.height(8.dp))
                    Text("Book your first appointment", color = Color.White.copy(alpha = 0.6f), fontSize = 12.sp)
                }
            }
        }
        
        // Quick Actions
        Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
             Column(Modifier.padding(24.dp)) {
                Text("Quick Actions", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Color(0xFF1F2937))
                Spacer(Modifier.height(16.dp))
                QuickAction(text = "Book Appointment", icon = Icons.Default.CalendarMonth, color = Color(0xFF14B8A6), onClick = onNavigateToSchedule)

            }
        }
    }
}

@Composable
private fun QuickAction(text: String, icon: ImageVector, color: Color, onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).clickable(onClick = onClick).background(color.copy(alpha = 0.1f)).padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(modifier = Modifier.size(40.dp).background(color, RoundedCornerShape(12.dp)), contentAlignment = Alignment.Center) {
            Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
        }
        Spacer(Modifier.width(16.dp))
        Text(text, color = Color(0xFF1F2937), fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.weight(1f))
        Icon(Icons.Default.ChevronRight, contentDescription = null, tint = Color(0xFF9CA3AF))
    }
}

@Composable
private fun AppointmentsTab(
    appointments: List<com.example.healthconnect.data.appointment.PatientAppointment>,
    isLoading: Boolean,
    appointmentsViewModel: com.example.healthconnect.data.appointment.PatientAppointmentsViewModel
) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("Upcoming Appointments", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Color(0xFF1F2937))
            Text("${appointments.size} total", color = Color(0xFF6B7280), fontSize = 14.sp)
        }

        if (isLoading) {
            Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                Text("Loading appointments...", color = Color(0xFF6B7280), fontSize = 14.sp)
            }
        } else if (appointments.isEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Box(
                    modifier = Modifier.fillMaxWidth().padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.CalendarMonth, contentDescription = null, tint = Color(0xFF9CA3AF), modifier = Modifier.size(48.dp))
                        Spacer(Modifier.height(16.dp))
                        Text("No upcoming appointments", color = Color(0xFF6B7280), fontSize = 16.sp, fontWeight = FontWeight.Medium)
                        Text("Book an appointment to get started", color = Color(0xFF9CA3AF), fontSize = 14.sp)
                    }
                }
            }
        } else {
            appointments.forEach { appointment ->
                AppointmentCard(
                    appointment = appointment,
                    onCancel = { appointmentId ->
                        appointmentsViewModel.cancelAppointment(appointmentId) { success ->
                            if (success) appointmentsViewModel.refresh()
                        }
                    },
                    onEdit = { appointmentId, date, time ->
                        appointmentsViewModel.updateAppointment(appointmentId, date, time) { success ->
                            if (success) appointmentsViewModel.refresh()
                        }
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AppointmentCard(
    appointment: com.example.healthconnect.data.appointment.PatientAppointment,
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
            appointmentViewModel.fetchDoctorAvailability(appointment.doctorId)
        }
    }
    
    Surface(
        modifier = Modifier.fillMaxWidth().shadow(8.dp, RoundedCornerShape(24.dp)),
        shape = RoundedCornerShape(24.dp),
        color = Color.White,
        onClick = {}
    ) {
        Column(Modifier.padding(20.dp)) {
            Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                 Column {
                     Text("Dr. ${appointment.doctorName}", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color(0xFF1F2937))
                     Text(appointment.doctorSpecialty, color = Color(0xFF3B82F6), fontSize = 14.sp)
                 }
                Box(modifier = Modifier.background(Color(0xFFDCFCE7), CircleShape).padding(horizontal = 12.dp, vertical = 4.dp)) {
                    Text("Upcoming", color = Color(0xFF16A34A), fontSize = 12.sp)
                }
            }
            Spacer(Modifier.height(16.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.CalendarMonth, contentDescription = null, tint = Color(0xFF6B7280), modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
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
        androidx.compose.material3.AlertDialog(
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
                    }
                    ,
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
        androidx.compose.material3.AlertDialog(
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
