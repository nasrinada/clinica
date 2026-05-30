package com.example.healthconnect.ui.doctor

import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.ui.platform.LocalContext
import coil.compose.AsyncImage
import com.example.healthconnect.R
import com.example.healthconnect.data.doctor.DoctorDetailsState
import com.example.healthconnect.data.doctor.DoctorDetailsViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Locale

@Composable
fun DoctorDetailsScreen(
    doctorId: String?,
    onBack: () -> Unit,
    onMessage: () -> Unit,
    onScheduleAppointment: (String) -> Unit,
    doctorDetailsViewModel: DoctorDetailsViewModel = viewModel()
) {
    val doctorProfile by doctorDetailsViewModel.doctorProfile.collectAsState()
    val state by doctorDetailsViewModel.state.collectAsState()

    LaunchedEffect(doctorId) {
        if (doctorId != null) {
            doctorDetailsViewModel.fetchDoctorDetails(doctorId)
        }
    }

    when (state) {
        is DoctorDetailsState.Loading -> {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    CircularProgressIndicator()
                    Text("Loading doctor details...", color = Color.Gray)
                }
            }
            return
        }
        is DoctorDetailsState.Error -> {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.padding(24.dp)
                ) {
                    Text(
                        text = "Error Loading Doctor Details",
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp,
                        color = Color(0xFF1F2937)
                    )
                    Text(
                        text = (state as DoctorDetailsState.Error).message,
                        fontSize = 14.sp,
                        color = Color(0xFF6B7280),
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = { doctorId?.let { doctorDetailsViewModel.fetchDoctorDetails(it) } },
                        modifier = Modifier.padding(top = 8.dp)
                    ) {
                        Text("Retry")
                    }
                    Button(
                        onClick = onBack,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE5E7EB), contentColor = Color(0xFF374151))
                    ) {
                        Text("Go Back")
                    }
                }
            }
            return
        }
        is DoctorDetailsState.Success -> {
            // Continue to show the doctor details
        }
    }

    if (doctorProfile.name.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
                CircularProgressIndicator()
                Text("Loading doctor details...", color = Color.Gray)
            }
        }
        return
    }

    val scrollState = rememberScrollState()

    val context = LocalContext.current

    var resolvedAddress by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(doctorProfile.locationGeoPoint) {
        resolvedAddress = null
        val geoPoint = doctorProfile.locationGeoPoint ?: return@LaunchedEffect
        try {
            val address = withContext(Dispatchers.IO) {
                val geocoder = android.location.Geocoder(context, Locale.getDefault())
                val results = geocoder.getFromLocation(geoPoint.latitude, geoPoint.longitude, 1)
                results?.firstOrNull()?.getAddressLine(0)
            }
            if (!address.isNullOrBlank()) {
                resolvedAddress = address
            }
        } catch (_: Exception) {
            resolvedAddress = null
        }
    }
    
    Box(modifier = Modifier.fillMaxSize().background(Color(0xFFF9FAFB))) {
        Column(modifier = Modifier.verticalScroll(scrollState)) {
            // Header Image
            Box(modifier = Modifier.height(288.dp)) {
                if (doctorProfile.imageUrl.isNotBlank()) {
                    AsyncImage(
                        model = doctorProfile.imageUrl,
                        contentDescription = doctorProfile.name,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Image(
                        painter = painterResource(id = R.drawable.doctor_photo),
                        contentDescription = doctorProfile.name,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
            
            // Content
            Column(modifier = Modifier.padding(24.dp)) {
                MainInfoCard(doctor = doctorProfile)
                Spacer(modifier = Modifier.height(16.dp))
                ContactInfoCard(
                    doctor = doctorProfile,
                    resolvedAddress = resolvedAddress
                )
            }

            Spacer(modifier = Modifier.height(100.dp)) // Space for the floating action buttons
        }

        // Back Button
        IconButton(
            onClick = onBack, 
            modifier = Modifier.padding(start = 24.dp, top = 48.dp).size(48.dp),
            colors = IconButtonDefaults.iconButtonColors(containerColor = Color.White.copy(alpha = 0.9f))
        ) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
        }

        // Action Buttons at the bottom
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
                .align(Alignment.BottomCenter),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Button(
                onClick = onMessage,
                modifier = Modifier.weight(1f).height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = MaterialTheme.colorScheme.primary),
                elevation = ButtonDefaults.buttonElevation(4.dp)
            ) {
                Icon(Icons.AutoMirrored.Filled.Chat, contentDescription = "Message")
                Spacer(modifier = Modifier.width(8.dp))
                Text("Messenger", fontWeight = FontWeight.Bold)
            }
            Button(
                onClick = { doctorId?.let { onScheduleAppointment(it) } },
                modifier = Modifier.weight(1f).height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent, contentColor = Color.White),
                contentPadding = PaddingValues(),
                elevation = ButtonDefaults.buttonElevation(4.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.horizontalGradient(
                                colors = listOf(Color(0xFF3B82F6), Color(0xFF14B8A6))
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Schedule, contentDescription = "Schedule Appointment")
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Schedule", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun MainInfoCard(doctor: com.example.healthconnect.data.doctor.DoctorProfile) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = Color.White,
        shadowElevation = 8.dp
    ) {
        Column(modifier = Modifier.padding(24.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(doctor.name, fontWeight = FontWeight.Bold, fontSize = 22.sp, color = Color(0xFF1F2937))
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(doctor.specialty, color = MaterialTheme.colorScheme.primary, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {


                    }
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.background(Color(0xFFEFF6FF), RoundedCornerShape(16.dp)).padding(8.dp)) {
                    Icon(Icons.Default.VerifiedUser, contentDescription = "Verified", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Verified", color = MaterialTheme.colorScheme.primary, fontSize = 12.sp)
                }
            }
            Divider(modifier = Modifier.padding(vertical = 16.dp), color = Color(0xFFF3F4F6))
            Text("About", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Color(0xFF1F2937))
            Spacer(modifier = Modifier.height(8.dp))
            Text(doctor.bio, color = Color(0xFF4B5563), fontSize = 14.sp, lineHeight = 22.sp)
        }
    }
}

@Composable
private fun ContactInfoCard(
    doctor: com.example.healthconnect.data.doctor.DoctorProfile,
    resolvedAddress: String?
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = Color.White,
        shadowElevation = 8.dp
    ) {
        Column(modifier = Modifier.padding(24.dp)) {
            Text("Contact Information", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Color(0xFF1F2937))
            Spacer(modifier = Modifier.height(16.dp))
            ContactRow(icon = Icons.Default.Phone, label = "Phone", value = doctor.phone, iconBgColor = Color(0xFFEFF6FF), iconColor = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.height(16.dp))
            ContactRow(
                icon = Icons.Default.LocationOn,
                label = "Address",
                value = resolvedAddress ?: doctor.location,
                iconBgColor = Color(0xFFF0FDF4),
                iconColor = Color(0xFF22C55E)
            )

        }
    }
}

@Composable
private fun ContactRow(icon: ImageVector, label: String, value: String, iconBgColor: Color, iconColor: Color) {
    Row(verticalAlignment = Alignment.Top) {
        Box(modifier = Modifier.size(48.dp).background(iconBgColor, RoundedCornerShape(12.dp)), contentAlignment = Alignment.Center) {
            Icon(icon, contentDescription = label, tint = iconColor, modifier = Modifier.size(24.dp))
        }
        Spacer(modifier = Modifier.width(16.dp))
        Column {
            Text(label, color = Color(0xFF6B7280), fontSize = 14.sp)
            Text(value, color = Color(0xFF1F2937), fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
        }
    }
}
