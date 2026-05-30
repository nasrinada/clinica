package com.example.healthconnect.ui.admin

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.healthconnect.data.admin.AdminViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserListScreen(
    role: String,
    onBack: () -> Unit,
    onEditDoctor: (String) -> Unit,
    adminViewModel: AdminViewModel = viewModel()
) {
    val users by adminViewModel.users.collectAsState()

    LaunchedEffect(role) {
        adminViewModel.fetchUsersByRole(role)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Manage ${role.replaceFirstChar { it.uppercase() }}s", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(it)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(users) { user ->
                UserCard(
                    user = user,
                    role = role,
                    onEditDoctor = onEditDoctor,
                    onBlock = { userId ->
                        adminViewModel.toggleBlockUser(userId) { success ->
                            // Refresh handled in ViewModel
                        }
                    }
                )
            }
        }
    }
}

@Composable
private fun UserCard(
    user: com.example.healthconnect.data.admin.UserData,
    role: String,
    onEditDoctor: (String) -> Unit,
    onBlock: (String) -> Unit,
) {
    var showBlockDialog by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(2.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(user.name, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                    if (user.isBlocked) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "[Blocked]",
                            color = Color.Red,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                Text(user.email, color = Color.Gray, fontSize = 14.sp)
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (role == "doctor") {
                    IconButton(
                        onClick = {
                            onEditDoctor(user.id)
                        }
                    ) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit", tint = Color(0xFF3B82F6))
                    }
                }

                IconButton(
                    onClick = { showBlockDialog = true }
                ) {
                    Icon(
                        Icons.Default.Block,
                        contentDescription = if (user.isBlocked) "Unblock" else "Block",
                        tint = if (user.isBlocked) Color(0xFF16A34A) else Color.Red
                    )
                }
            }
        }
    }

    if (showBlockDialog) {
        AlertDialog(
            onDismissRequest = { showBlockDialog = false },
            title = { Text(if (user.isBlocked) "Unblock User" else "Block User") },
            text = {
                Text(
                    if (user.isBlocked)
                        "Are you sure you want to unblock ${user.name}?"
                    else
                        "Are you sure you want to block ${user.name}? They will not be able to access the app."
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        onBlock(user.id)
                        showBlockDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (user.isBlocked) Color(0xFF16A34A) else Color.Red
                    )
                ) {
                    Text(if (user.isBlocked) "Unblock" else "Block")
                }
            },
            dismissButton = {
                TextButton(onClick = { showBlockDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}