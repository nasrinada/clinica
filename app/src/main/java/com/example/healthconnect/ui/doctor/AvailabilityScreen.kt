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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.healthconnect.data.doctor.AvailabilityViewModel
import com.example.healthconnect.data.doctor.DaySchedule

enum class Weekdays { Monday, Tuesday, Wednesday, Thursday, Friday, Saturday, Sunday }

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AvailabilityScreen(onBack: () -> Unit, availabilityViewModel: AvailabilityViewModel = viewModel()) {
    val availability by availabilityViewModel.availability.collectAsState()
    
    var scheduleMap by remember { mutableStateOf<Map<String, DaySchedule>>(emptyMap()) }
    var showAddDayDialog by remember { mutableStateOf(false) }
    var showSuccessDialog by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(availability) {
        scheduleMap = availability.schedule
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Manage Availability", fontWeight = FontWeight.Bold) },
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
                        availabilityViewModel.updateAvailability(scheduleMap) { success ->
                            if (success) {
                                errorMessage = null
                                showSuccessDialog = true
                            } else {
                                errorMessage = "Failed to save availability"
                            }
                        }
                    },
                    enabled = scheduleMap.isNotEmpty(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF3B82F6)
                    )
                ) {
                    Text("Register Schedule", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFFF9FAFB))
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            errorMessage?.let {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
                ) {
                    Text(
                        it,
                        modifier = Modifier.padding(16.dp),
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            }

            // Info Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFEFF6FF))
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.AccessTime,
                        contentDescription = null,
                        tint = Color(0xFF3B82F6),
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "Set Your Working Schedule",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = Color(0xFF1F2937)
                        )
                        Text(
                            "Select days and set individual hours for each",
                            fontSize = 12.sp,
                            color = Color(0xFF6B7280)
                        )
                    }
                }
            }

            // Add Day Button
            Button(
                onClick = { showAddDayDialog = true },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.White,
                    contentColor = Color(0xFF3B82F6)
                ),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF3B82F6))
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Day", modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Add Working Day", fontWeight = FontWeight.SemiBold)
            }

            // Selected Days List
            if (scheduleMap.isNotEmpty()) {
                scheduleMap.forEach { (day, daySchedule) ->
                    DayScheduleCard(
                        day = day,
                        schedule = daySchedule,
                        onUpdate = { newSchedule ->
                            scheduleMap = scheduleMap + (day to newSchedule)
                        },
                        onRemove = {
                            scheduleMap = scheduleMap - day
                        }
                    )
                }
            } else {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "No working days added yet.\nTap 'Add Working Day' to get started.",
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                            color = Color(0xFF9CA3AF),
                            fontSize = 14.sp
                        )
                    }
                }
            }
        }

        // Add Day Dialog
        if (showAddDayDialog) {
            AddDayDialog(
                existingDays = scheduleMap.keys.toSet(),
                onDaySelected = { day ->
                    scheduleMap = scheduleMap + (day to DaySchedule("09:00", "17:00"))
                    showAddDayDialog = false
                },
                onDismiss = { showAddDayDialog = false }
            )
        }

        if (showSuccessDialog) {
            AlertDialog(
                onDismissRequest = { showSuccessDialog = false },
                title = { Text("Success") },
                text = { Text("Availability saved successfully.") },
                confirmButton = {
                    TextButton(
                        onClick = {
                            showSuccessDialog = false
                            onBack()
                        }
                    ) {
                        Text("OK")
                    }
                }
            )
        }
    }
}

@Composable
private fun DayScheduleCard(
    day: String,
    schedule: DaySchedule,
    onUpdate: (DaySchedule) -> Unit,
    onRemove: () -> Unit
) {
    var startTime by remember { mutableStateOf(schedule.startTime) }
    var endTime by remember { mutableStateOf(schedule.endTime) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    day,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = Color(0xFF1F2937)
                )
                IconButton(
                    onClick = onRemove,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = "Remove",
                        tint = Color(0xFFEF4444),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "Start Time",
                        fontSize = 12.sp,
                        color = Color(0xFF6B7280),
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    OutlinedTextField(
                        value = startTime,
                        onValueChange = {
                            startTime = it
                            onUpdate(DaySchedule(startTime, endTime))
                        },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("09:00") },
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "End Time",
                        fontSize = 12.sp,
                        color = Color(0xFF6B7280),
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    OutlinedTextField(
                        value = endTime,
                        onValueChange = {
                            endTime = it
                            onUpdate(DaySchedule(startTime, endTime))
                        },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("17:00") },
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true
                    )
                }
            }
        }
    }
}

@Composable
private fun AddDayDialog(
    existingDays: Set<String>,
    onDaySelected: (String) -> Unit,
    onDismiss: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.5f))
            .clickable(onClick = onDismiss),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                Text(
                    "Select a Day",
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
                    color = Color(0xFF1F2937)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "Choose a day to add to your schedule",
                    fontSize = 14.sp,
                    color = Color(0xFF6B7280)
                )
                Spacer(modifier = Modifier.height(24.dp))
                
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Weekdays.values().forEach { day ->
                        val dayName = day.name
                        val isAlreadyAdded = existingDays.contains(dayName)
                        DayChip(
                            day = dayName,
                            isSelected = false,
                            isDisabled = isAlreadyAdded,
                            onToggle = {
                                if (!isAlreadyAdded) {
                                    onDaySelected(dayName)
                                }
                            }
                        )
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))
                Button(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFE5E7EB),
                        contentColor = Color(0xFF374151)
                    )
                ) {
                    Text("Cancel")
                }
            }
        }
    }
}

@Composable
private fun DayChip(day: String, isSelected: Boolean, isDisabled: Boolean, onToggle: () -> Unit) {
    val backgroundColor = when {
        isDisabled -> Color(0xFFF3F4F6)
        isSelected -> Color(0xFF3B82F6)
        else -> Color(0xFFF3F4F6)
    }
    val textColor = when {
        isDisabled -> Color(0xFF9CA3AF)
        isSelected -> Color.White
        else -> Color(0xFF6B7280)
    }

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(backgroundColor)
            .clickable(enabled = !isDisabled, onClick = onToggle)
            .padding(horizontal = 20.dp, vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            if (isSelected) {
                Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
            }
            Text(
                if (day.length > 3) day.take(3) else day,
                color = textColor,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                fontSize = 14.sp
            )
        }
    }
}
