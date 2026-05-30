package com.example.healthconnect.ui.profile

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.healthconnect.data.ImageUploadService
import com.example.healthconnect.data.profile.ProfileViewModel
import kotlinx.coroutines.launch

@Composable
fun ProfileScreen(
    onBack: () -> Unit,
    onLogout: () -> Unit,
    profileViewModel: ProfileViewModel = viewModel()
) {
    val userProfile = profileViewModel.userProfile
    var name by remember(userProfile) { mutableStateOf(userProfile.name) }
    var phone by remember(userProfile) { mutableStateOf(userProfile.phone) }
    var address by remember(userProfile) { mutableStateOf(userProfile.address) }
    var showSuccessDialog by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isUploadingImage by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    val imagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
        onResult = { uri: Uri? ->
            if (uri != null && userProfile.role == "doctor") {
                val imageUploadService = ImageUploadService(context)
                scope.launch {
                    isUploadingImage = true
                    val uploadedUrl = imageUploadService.uploadImage(uri)
                    if (uploadedUrl != null) {
                        profileViewModel.updateProfileImage(uploadedUrl) { ok ->
                            if (!ok) {
                                errorMessage = "Failed to update profile image"
                            }
                        }
                    } else {
                        errorMessage = "Failed to upload image"
                    }
                    isUploadingImage = false
                }
            }
        }
    )

    val headerGradient = Brush.horizontalGradient(
        colors = listOf(Color(0xFF3B82F6), Color(0xFF14B8A6))
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF9FAFB))
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
            Column(
                modifier = Modifier.fillMaxWidth().padding(top = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Top Bar
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onBack,
                        colors = IconButtonDefaults.iconButtonColors(containerColor = Color.White.copy(alpha = 0.2f))
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                    Text("Profile", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
                    IconButton(
                        onClick = {
                            profileViewModel.updateProfile(name, phone, address) { ok ->
                                if (ok) {
                                    errorMessage = null
                                    showSuccessDialog = true
                                } else {
                                    errorMessage = "Failed to update profile"
                                }
                            }
                        },
                        colors = IconButtonDefaults.iconButtonColors(containerColor = Color.White.copy(alpha = 0.2f))
                    ) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit", tint = Color.White)
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))

                // Avatar
                Box(
                    modifier = Modifier
                        .size(96.dp)
                        .background(Color.White.copy(alpha = 0.2f), RoundedCornerShape(24.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    if (userProfile.role == "doctor" && userProfile.imageUrl.isNotBlank()) {
                        AsyncImage(
                            model = userProfile.imageUrl,
                            contentDescription = "Profile Image",
                            modifier = Modifier.size(96.dp),
                            contentScale = androidx.compose.ui.layout.ContentScale.Crop
                        )
                    } else {
                        Icon(Icons.Default.Person, contentDescription = "User Avatar", tint = Color.White, modifier = Modifier.size(48.dp))
                    }

                    if (userProfile.role == "doctor") {
                        IconButton(
                            onClick = {
                                if (!isUploadingImage) {
                                    imagePicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                                }
                            },
                            modifier = Modifier.align(Alignment.BottomEnd),
                            colors = IconButtonDefaults.iconButtonColors(containerColor = Color.White.copy(alpha = 0.25f))
                        ) {
                            Icon(Icons.Default.Image, contentDescription = "Change Image", tint = Color.White)
                        }
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(userProfile.name, color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            }
        }

        // Content
        Column(
            modifier = Modifier
                .padding(horizontal = 24.dp)
                .padding(top = 16.dp) // Overlap adjustment
        ) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                color = Color.White,
                shadowElevation = 8.dp
            ) {
                Column(modifier = Modifier.padding(24.dp)) {
                    Text("Personal Information", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Color(0xFF1F2937))
                    Spacer(modifier = Modifier.height(24.dp))

                    errorMessage?.let {
                        Text(it, color = MaterialTheme.colorScheme.error)
                        Spacer(modifier = Modifier.height(12.dp))
                    }

                    InfoTextField(icon = Icons.Default.Person, value = name, onValueChange = { name = it }, iconColor = Color(0xFF3B82F6))
                    Spacer(modifier = Modifier.height(16.dp))
                    InfoTextField(icon = Icons.Default.Email, value = userProfile.email, onValueChange = {}, iconColor = Color(0xFF14B8A6), readOnly = true)
                    Spacer(modifier = Modifier.height(16.dp))
                    InfoTextField(icon = Icons.Default.Phone, value = phone, onValueChange = { phone = it }, iconColor = Color(0xFF22C55E))

                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Logout Button
            Button(
                onClick = onLogout,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .shadow(8.dp, RoundedCornerShape(16.dp)),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                contentPadding = PaddingValues()
            ) {
                Box(modifier = Modifier.fillMaxSize().background(headerGradient), contentAlignment = Alignment.Center) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = "Logout")
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Logout", fontWeight = FontWeight.Bold)
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                "Version 1.0.0",
                color = Color.Gray,
                fontSize = 12.sp,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            )
        }

        if (showSuccessDialog) {
            AlertDialog(
                onDismissRequest = { showSuccessDialog = false },
                title = { Text("Success") },
                text = { Text("Profile updated successfully.") },
                confirmButton = {
                    TextButton(onClick = { showSuccessDialog = false }) {
                        Text("OK")
                    }
                }
            )
        }
    }
}

@Composable
private fun InfoTextField(icon: ImageVector, value: String, onValueChange: (String) -> Unit, iconColor: Color, readOnly: Boolean = false) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, contentDescription = null, tint = iconColor, modifier = Modifier.size(20.dp))
        Spacer(modifier = Modifier.width(12.dp))
        TextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = TextFieldDefaults.colors(
                focusedContainerColor = Color(0xFFF3F4F6),
                unfocusedContainerColor = Color(0xFFF3F4F6),
                disabledContainerColor = Color(0xFFF3F4F6),
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
                cursorColor = Color(0xFF3B82F6)
            ),
            readOnly = readOnly
        )
    }
}