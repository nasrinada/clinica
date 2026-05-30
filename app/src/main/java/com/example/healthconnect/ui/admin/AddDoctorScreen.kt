package com.example.healthconnect.ui.admin

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import coil.compose.AsyncImage
import com.example.healthconnect.data.ImageUploadService
import com.example.healthconnect.data.auth.SignupViewModel
import com.example.healthconnect.data.specialties
import kotlinx.coroutines.launch
import com.google.firebase.firestore.GeoPoint as FirestoreGeoPoint
import org.osmdroid.events.MapEventsReceiver
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.MapEventsOverlay
import org.osmdroid.views.overlay.Marker



private fun isValidPhone(phone: String): Boolean {
    return phone.matches(Regex("^\\d{8}$"))
}



@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddDoctorScreen(
    onDoctorAdded: () -> Unit,
    onBack: () -> Unit,
    signupViewModel: SignupViewModel
) {
    var name by remember { mutableStateOf("") }
    var selectedSpecialty by remember { mutableStateOf(specialties.first()) }
    var email by remember { mutableStateOf("") }
    var bio by remember { mutableStateOf("") }
    var location by remember { mutableStateOf<GeoPoint?>(null) }
    var phone by remember { mutableStateOf("") }
    var phoneError by remember { mutableStateOf<String?>(null) }
    var imageUri by remember { mutableStateOf<Uri?>(null) }
    var isExpanded by remember { mutableStateOf(false) }
    var showSuccessDialog by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val signupState = signupViewModel.signupState

    LaunchedEffect(Unit) {
        signupViewModel.clearSignupState()
    }

    LaunchedEffect(signupState.isSuccess) {
        if (signupState.isSuccess) showSuccessDialog = true
    }

    val imagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
        onResult = { imageUri = it }
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Add New Doctor", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                    }
                }
            )
        }
    ) { padding ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
        ) {


            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                if (imageUri != null) {
                    AsyncImage(
                        model = imageUri,
                        contentDescription = null,
                        modifier = Modifier
                            .size(120.dp)
                            .clip(CircleShape),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .size(120.dp)
                            .background(Color.LightGray, CircleShape)
                            .clickable {
                                imagePicker.launch(
                                    PickVisualMediaRequest(
                                        ActivityResultContracts.PickVisualMedia.ImageOnly
                                    )
                                )
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Image, contentDescription = null, modifier = Modifier.size(40.dp))
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Name") },
                leadingIcon = { Icon(Icons.Default.Person, null) },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            ExposedDropdownMenuBox(
                expanded = isExpanded,
                onExpandedChange = { isExpanded = it }
            ) {
                OutlinedTextField(
                    value = selectedSpecialty.name,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Specialty") },
                    leadingIcon = { Icon(Icons.Default.Work, null) },
                    trailingIcon = {
                        ExposedDropdownMenuDefaults.TrailingIcon(expanded = isExpanded)
                    },
                    modifier = Modifier.menuAnchor().fillMaxWidth()
                )
                ExposedDropdownMenu(
                    expanded = isExpanded,
                    onDismissRequest = { isExpanded = false }
                ) {
                    specialties.forEach {
                        DropdownMenuItem(
                            text = { Text(it.name) },
                            onClick = {
                                selectedSpecialty = it
                                isExpanded = false
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text("Email") },
                leadingIcon = { Icon(Icons.Default.Email, null) },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = bio,
                onValueChange = { bio = it },
                label = { Text("Bio") },
                leadingIcon = { Icon(Icons.Default.Info, null) },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text("Select Location on Map", fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .clip(RoundedCornerShape(16.dp))
            ) {
                LocationPickerMap { location = it }
            }

            Spacer(modifier = Modifier.height(16.dp))


            OutlinedTextField(
                value = phone,
                onValueChange = {
                    if (it.all { ch -> ch.isDigit() } && it.length <= 8) {
                        phone = it
                        phoneError = null
                    }
                },
                label = { Text("Phone") },
                leadingIcon = { Icon(Icons.Default.Phone, null) },
                modifier = Modifier.fillMaxWidth(),
                isError = phoneError != null
            )

            phoneError?.let {
                Text(it, color = MaterialTheme.colorScheme.error)
            }

            Spacer(modifier = Modifier.height(32.dp))

            signupState.error?.let {
                Text(it, color = MaterialTheme.colorScheme.error)
                Spacer(modifier = Modifier.height(12.dp))
            }

            Button(
                onClick = {
                    if (!isValidPhone(phone)) {
                        phoneError = "Phone must be exactly 8 digits"
                        return@Button
                    }

                    val imageUploadService = ImageUploadService(context)

                    scope.launch {
                        val imageUrl = imageUri?.let { imageUploadService.uploadImage(it) }

                        val doctorDetails = hashMapOf<String, Any>(
                            "name" to name,
                            "specialty" to selectedSpecialty.id,
                            "bio" to bio,
                            "location" to FirestoreGeoPoint(
                                location!!.latitude,
                                location!!.longitude
                            ),
                            "phone" to phone,
                            "imageUrl" to (imageUrl ?: "")
                        )

                        signupViewModel.createDoctorAndSendPasswordEmail(
                            context = context,
                            email = email,
                            doctorDetails = doctorDetails
                        )
                    }
                },
                enabled = location != null && imageUri != null,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text("Add Doctor", fontWeight = FontWeight.Bold)
            }
        }

        if (showSuccessDialog) {
            AlertDialog(
                onDismissRequest = {
                    showSuccessDialog = false
                    signupViewModel.clearSignupState()
                },
                title = { Text("Success") },
                text = {
                    Text("Doctor account created. A password setup link was sent to their email.")
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            showSuccessDialog = false
                            signupViewModel.clearSignupState()
                            onDoctorAdded()
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
fun LocationPickerMap(onLocationSelected: (GeoPoint) -> Unit) {
    val context = LocalContext.current

    val mapView = remember {
        MapView(context).apply {
            setTileSource(TileSourceFactory.MAPNIK)
            controller.setZoom(12.0)
            controller.setCenter(GeoPoint(35.038, 9.485)) // Sidi Bouzid
        }
    }

    DisposableEffect(mapView) {
        val receiver = object : MapEventsReceiver {
            var marker: Marker? = null

            override fun singleTapConfirmedHelper(p: GeoPoint?): Boolean {
                p?.let {
                    onLocationSelected(it)
                    marker?.let { m -> mapView.overlays.remove(m) }

                    val newMarker = Marker(mapView).apply {
                        position = it
                        setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                    }

                    mapView.overlays.add(newMarker)
                    marker = newMarker
                    mapView.invalidate()
                }
                return true
            }

            override fun longPressHelper(p: GeoPoint?) = false
        }

        mapView.overlays.add(MapEventsOverlay(receiver))

        onDispose {
            mapView.onPause()
        }
    }

    AndroidView(factory = { mapView }) {
        it.onResume()
    }
}
