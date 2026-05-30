package com.example.healthconnect.ui.map

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.healthconnect.data.map.DoctorLocation
import com.example.healthconnect.data.map.MapViewModel
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker

@Composable
fun MapScreen(onBack: () -> Unit, onSelectDoctor: (String, String) -> Unit, mapViewModel: MapViewModel = viewModel()) {
    var selectedDoctor by remember { mutableStateOf<DoctorLocation?>(null) }
    val doctors by mapViewModel.doctors.collectAsState()
    val isLoading by mapViewModel.isLoading.collectAsState()
    val searchQuery by mapViewModel.searchQuery.collectAsState()
    Scaffold(
        topBar = { MapHeader(onBack=onBack , searchQuery = searchQuery, onSearchChange = mapViewModel::onSearchQueryChage) }
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(it)
        ) {
            MapView(
                doctors = doctors,
                isLoading = isLoading,
                onSelectDoctor = onSelectDoctor,
                onSelectDoctorMarker = { selectedDoctor = it }
            )
            
            // Selected Doctor Info Card
            AnimatedVisibility(
                visible = selectedDoctor != null,
                modifier = Modifier.align(Alignment.BottomCenter),
                enter = slideInVertically(initialOffsetY = { it / 2 }),
                exit = slideOutVertically(targetOffsetY = { it / 2 })
            ) {
                selectedDoctor?.let { doctor ->
                    DoctorInfoCard(
                        doctor = doctor,
                        onSelectDoctor = { onSelectDoctor(doctor.id, doctor.name) }
                    ) { selectedDoctor = null }
                }
            }
        }
    }
}

@Composable
private fun MapHeader(onBack: () -> Unit
, searchQuery:String,
  onSearchChange : (String)->Unit ) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                Brush.horizontalGradient(
                    colors = listOf(Color(0xFF3B82F6), Color(0xFF14B8A6))
                )
            )
            .padding(start = 16.dp, end = 16.dp, top = 32.dp, bottom = 16.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack, colors = IconButtonDefaults.iconButtonColors(containerColor = Color.White.copy(0.2f))) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = Color.White)
            }
            Text("Nearby Doctors", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            IconButton(onClick = {}, colors = IconButtonDefaults.iconButtonColors(containerColor = Color.White.copy(0.2f))) {

            }
        }
        Spacer(Modifier.height(16.dp))
        TextField(
            value = searchQuery,
            onValueChange = {onSearchChange(it)},
            placeholder = { Text("Search by speciality...") },
            leadingIcon = { Icon(Icons.Default.Search, null) },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = TextFieldDefaults.colors(focusedIndicatorColor = Color.Transparent, unfocusedIndicatorColor = Color.Transparent)
        )
    }
}

@Composable
fun MapView(
    doctors: List<DoctorLocation>,
    isLoading: Boolean,
    onSelectDoctor: (String, String) -> Unit,
    onSelectDoctorMarker: (DoctorLocation) -> Unit
) {
    val context = LocalContext.current
    val mapView = remember { MapView(context) }

    DisposableEffect(Unit) {
        onDispose {
            mapView.onPause()
        }
    }
    
    LaunchedEffect(doctors) {
        // Clear existing markers
        mapView.overlays.clear()
        
        // Add markers for each doctor
        doctors.forEach { doctor ->
            val marker = Marker(mapView)
            marker.position = doctor.position
            marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
            marker.title = doctor.name
            marker.snippet = doctor.specialty
            marker.setOnMarkerClickListener { _, _ ->
                onSelectDoctorMarker(doctor)
                true
            }
            mapView.overlays.add(marker)
        }
        
        // Center map on doctors if available
        if (doctors.isNotEmpty()) {
            val centerLat = doctors.map { it.position.latitude }.average()
            val centerLng = doctors.map { it.position.longitude }.average()
            mapView.controller.setCenter(GeoPoint(centerLat, centerLng))
            mapView.controller.setZoom(14.0)
        } else {
            // Default to Sidi Bouzid, Tunisia
            mapView.controller.setCenter(GeoPoint(35.038, 9.485))
            mapView.controller.setZoom(14.0)
        }
    }

    AndroidView(
        factory = { 
            mapView.apply {
                setTileSource(TileSourceFactory.MAPNIK)
                controller.setZoom(14.0)
                controller.setCenter(GeoPoint(35.038, 9.485)) // Sidi Bouzid, Tunisia
            }
            mapView 
        },
        modifier = Modifier.fillMaxSize(),
        update = {
            it.onResume()
        }
    )
}


@Composable
private fun DoctorInfoCard(doctor: DoctorLocation, onSelectDoctor: () -> Unit, onClose: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(16.dp)
    ) {
        Column(modifier = Modifier.padding(24.dp)) {
            Box(modifier = Modifier.fillMaxWidth()) {
                Text(doctor.name, fontWeight = FontWeight.Bold, fontSize = 18.sp, modifier = Modifier.align(Alignment.CenterStart))
                IconButton(onClick = onClose, modifier = Modifier.align(Alignment.CenterEnd)) {
                    Text("✕", fontSize = 18.sp)
                }
            }
            Text(doctor.specialty, color = Color(0xFF3B82F6), fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(16.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.LocationOn, contentDescription = "Location", tint = Color.Gray, modifier = Modifier.size(16.dp))
                Text("${String.format("%.4f", doctor.position.latitude)}, ${String.format("%.4f", doctor.position.longitude)}", color = Color.Gray, modifier = Modifier.padding(start = 4.dp), fontSize = 12.sp)
            }
            Spacer(Modifier.height(24.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = { onSelectDoctor(); onClose() },
                    modifier = Modifier.weight(1f).height(48.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3B82F6))
                ) {
                    Text("View Profile")
                }
                if (doctor.phone != null) {
                    IconButton(
                        onClick = { },
                        modifier = Modifier.size(48.dp),
                        colors = IconButtonDefaults.iconButtonColors(containerColor = Color(0xFFEFF6FF), contentColor = Color(0xFF3B82F6))
                    ) {

                    }
                }
            }
        }
    }
}
