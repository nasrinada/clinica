package com.example.healthconnect.ui.schedule

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.healthconnect.data.doctor.DoctorDetailsState
import com.example.healthconnect.data.doctor.DoctorDetailsViewModel
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun ConfirmationScreen(
    doctorId: String,
    date: String,
    time: String,
    onBackToDashboard: () -> Unit,
    onAddToCalendar: () -> Unit,
    doctorDetailsViewModel: DoctorDetailsViewModel = viewModel()
) {
    val doctorProfile by doctorDetailsViewModel.doctorProfile.collectAsState()
    val state by doctorDetailsViewModel.state.collectAsState()

    val displayDate = remember(date) {
        try {
            val parsed = LocalDate.parse(date)
            parsed.format(DateTimeFormatter.ofPattern("EEE dd/MM/yyyy", Locale.getDefault()))
        } catch (_: Exception) {
            date
        }
    }

    LaunchedEffect(doctorId) {
        if (doctorId.isNotEmpty()) {
            doctorDetailsViewModel.fetchDoctorDetails(doctorId)
        }
    }

    val backgroundGradient = Brush.verticalGradient(
        colors = listOf(Color(0xFFF0FDF4), Color.White)
    )
    val buttonGradient = Brush.horizontalGradient(
        colors = listOf(Color(0xFF3B82F6), Color(0xFF14B8A6))
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundGradient)
            .padding(24.dp)
            .navigationBarsPadding()
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top
    ) {
        Spacer(modifier = Modifier.height(24.dp))

        // Success Icon
        Box(
            modifier = Modifier
                .size(128.dp)
                .shadow(16.dp, CircleShape)
                .background(
                    brush = Brush.verticalGradient(colors = listOf(Color(0xFF34D399), Color(0xFF14B8A6))),
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.CheckCircle,
                contentDescription = "Confirmed",
                tint = Color.White,
                modifier = Modifier.size(64.dp)
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            "Appointment Confirmed!",
            fontWeight = FontWeight.Bold,
            fontSize = 24.sp,
            color = Color(0xFF1F2937)
        )
        Text(
            "Your appointment has been successfully scheduled",
            color = Color(0xFF4B5563),
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 24.dp)
        )

        Spacer(modifier = Modifier.height(32.dp))

        // Details Card
        if (state is DoctorDetailsState.Loading) {
            Box(
                modifier = Modifier.fillMaxWidth().height(200.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else if (state is DoctorDetailsState.Error) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                color = Color.White,
                shadowElevation = 8.dp
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        "Error loading doctor details",
                        color = Color(0xFFEF4444),
                        fontSize = 14.sp
                    )
                }
            }
        } else {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                color = Color.White,
                shadowElevation = 8.dp
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier.size(64.dp).background(
                            Color(0xFFEFF6FF),
                            RoundedCornerShape(16.dp)
                        ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.CalendarMonth,
                            contentDescription = null,
                            tint = Color(0xFF3B82F6),
                            modifier = Modifier.size(32.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        doctorProfile.name.ifEmpty { "Doctor" },
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                    Text(
                        doctorProfile.specialty.ifEmpty { "General" },
                        color = Color(0xFF3B82F6),
                        fontSize = 14.sp
                    )
                    HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))
                    DetailRow(icon = Icons.Default.CalendarMonth, label = "Date", value = displayDate)
                    Spacer(modifier = Modifier.height(16.dp))
                    DetailRow(icon = Icons.Default.Schedule, label = "Time", value = time)
                    Spacer(modifier = Modifier.height(16.dp))

                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Action Buttons
        Button(
            onClick = onAddToCalendar,
            modifier = Modifier.fillMaxWidth().height(56.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color.White,
                contentColor = Color(0xFF3B82F6)
            ),
            border = ButtonDefaults.outlinedButtonBorder.copy(
                width = 1.dp,
                brush = SolidColor(Color(0xFF3B82F6))
            )
        ) {
            Icon(Icons.Default.CalendarMonth, contentDescription = "Add to Calendar")
            Spacer(modifier = Modifier.width(8.dp))
            Text("Add to Calendar", fontWeight = FontWeight.Bold)
        }
        Spacer(modifier = Modifier.height(12.dp))
        Button(
            onClick = onBackToDashboard,
            modifier = Modifier.fillMaxWidth().height(56.dp).shadow(8.dp, RoundedCornerShape(16.dp)),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
            contentPadding = PaddingValues()
        ) {
            Box(
                modifier = Modifier.fillMaxSize().background(buttonGradient),
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Home, contentDescription = "Back to Dashboard", tint = Color.White)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Back to Dashboard", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
        }

       
        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
private fun DetailRow(icon: ImageVector, label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(20.dp))
        Spacer(modifier = Modifier.width(16.dp))
        Column {
            Text(label, color = Color.Gray, fontSize = 12.sp)
            Text(value, fontWeight = FontWeight.SemiBold, color = Color(0xFF374151))
        }
    }
}
