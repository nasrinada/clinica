package com.example.healthconnect.ui.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
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
import com.example.healthconnect.data.chat.ChatViewModel
import com.example.healthconnect.data.chat.Message
import com.google.firebase.auth.FirebaseAuth

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun MessageScreen(doctorId: String, doctorName: String, onBack: () -> Unit, chatViewModel: ChatViewModel = viewModel()) {
    val messages by chatViewModel.messages.collectAsState()
    val blockedBy by chatViewModel.blockedBy.collectAsState()
    val listState = rememberLazyListState()
    var text by remember { mutableStateOf("") }
    var showMoreMenu by remember { mutableStateOf(false) }
    var showBlockConfirm by remember { mutableStateOf(false) }
    var showUnblockConfirm by remember { mutableStateOf(false) }

    val snackbarHostState = remember { SnackbarHostState() }
    val currentUserId = FirebaseAuth.getInstance().currentUser?.uid
    val isCurrentUserBlocked = blockedBy != null && blockedBy == currentUserId
    val isOtherUserBlocked = blockedBy != null && blockedBy == doctorId

    LaunchedEffect(doctorId) {
        chatViewModel.loadMessages(doctorId)
    }

    LaunchedEffect(messages) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    LaunchedEffect(Unit) {
        chatViewModel.events.collect { msg ->
            snackbarHostState.showSnackbar(msg)
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text(doctorName, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {


                    IconButton(onClick = { showMoreMenu = true }) {
                        Icon(Icons.Default.MoreVert, contentDescription = "More")
                    }

                    DropdownMenu(
                        expanded = showMoreMenu,
                        onDismissRequest = { showMoreMenu = false }
                    ) {
                        if (isOtherUserBlocked) {
                            DropdownMenuItem(
                                text = { Text("Unblock user") },
                                onClick = {
                                    showMoreMenu = false
                                    showUnblockConfirm = true
                                }
                            )
                        } else {
                            DropdownMenuItem(
                                text = { Text("Block user") },
                                onClick = {
                                    showMoreMenu = false
                                    showBlockConfirm = true
                                }
                            )
                        }
                    }
                }
            )
        },
        bottomBar = {
            Surface(shadowElevation = 4.dp) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = text,
                        onValueChange = { text = it },
                        enabled = !isCurrentUserBlocked,
                        modifier = Modifier.weight(1f),
                        placeholder = { Text("Type a message...") },
                        shape = RoundedCornerShape(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    IconButton(
                        onClick = {
                            chatViewModel.sendMessage(text, doctorId)
                            text = ""
                        },
                        enabled = !isCurrentUserBlocked && text.isNotBlank(),
                        modifier = Modifier.clip(CircleShape).background(MaterialTheme.colorScheme.primary)
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send", tint = Color.White)
                    }
                }
            }
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(it)
        ) {
            if (isCurrentUserBlocked) {
                Surface(color = Color(0xFFFEE2E2)) {
                    Text(
                        text = "You are blocked in this conversation",
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        color = Color(0xFF991B1B),
                        fontWeight = FontWeight.SemiBold
                    )
                }
            } else if (isOtherUserBlocked) {
                Surface(color = Color(0xFFFEF3C7)) {
                    Text(
                        text = "User is blocked",
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        color = Color(0xFF92400E),
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(vertical = 16.dp)
            ) {
                items(messages) {
                    MessageBubble(
                        message = it,
                        onDelete = { chatViewModel.deleteMessage(it.id) },
                        onEdit = { newText -> chatViewModel.editMessage(it.id, newText) }
                    )
                }
            }
        }
    }

    if (showBlockConfirm) {
        AlertDialog(
            onDismissRequest = { showBlockConfirm = false },
            title = { Text("Block") },
            text = { Text("Block $doctorName? They will no longer be able to read this conversation.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showBlockConfirm = false
                        chatViewModel.blockOtherUser()
                    }
                ) { Text("Block") }
            },
            dismissButton = {
                TextButton(onClick = { showBlockConfirm = false }) { Text("Cancel") }
            }
        )
    }

    if (showUnblockConfirm) {
        AlertDialog(
            onDismissRequest = { showUnblockConfirm = false },
            title = { Text("Unblock") },
            text = { Text("Unblock $doctorName?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showUnblockConfirm = false
                        chatViewModel.unblockOtherUser()
                    }
                ) { Text("Unblock") }
            },
            dismissButton = {
                TextButton(onClick = { showUnblockConfirm = false }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun MessageBubble(
    message: Message,
    onDelete: () -> Unit,
    onEdit: (String) -> Unit
) {
    val currentUserId = FirebaseAuth.getInstance().currentUser?.uid
    val isMe = message.senderId == currentUserId
    val alignment = if (isMe) Alignment.CenterEnd else Alignment.CenterStart
    val shape = if (isMe) RoundedCornerShape(20.dp, 4.dp, 20.dp, 20.dp) else RoundedCornerShape(4.dp, 20.dp, 20.dp, 20.dp)
    val backgroundColor = if (isMe) MaterialTheme.colorScheme.primary else Color.LightGray
    val textColor = if (isMe) Color.White else Color.Black

    var showMenu by remember { mutableStateOf(false) }
    var showEditDialog by remember { mutableStateOf(false) }
    var editText by remember(message.text) { mutableStateOf(message.text) }

    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = alignment) {
        Surface(
            shape = shape,
            color = backgroundColor,
            modifier = Modifier
                .padding(
                    start = if (isMe) 48.dp else 0.dp,
                    end = if (isMe) 0.dp else 48.dp
                )
                .combinedClickable(
                    onClick = { },
                    onLongClick = {
                        if (isMe) {
                            showMenu = true
                        }
                    }
                )
        ) {
            Text(
                text = message.text,
                modifier = Modifier.padding(vertical = 8.dp, horizontal = 12.dp),
                color = textColor,
                fontSize = 16.sp
            )
        }

        DropdownMenu(
            expanded = showMenu,
            onDismissRequest = { showMenu = false }
        ) {
            DropdownMenuItem(
                text = { Text("Edit") },
                onClick = {
                    showMenu = false
                    showEditDialog = true
                }
            )
            DropdownMenuItem(
                text = { Text("Delete") },
                onClick = {
                    showMenu = false
                    onDelete()
                }
            )
        }
    }

    if (showEditDialog) {
        AlertDialog(
            onDismissRequest = { showEditDialog = false },
            title = { Text("Edit message") },
            text = {
                OutlinedTextField(
                    value = editText,
                    onValueChange = { editText = it },
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showEditDialog = false
                        onEdit(editText)
                    }
                ) { Text("Save") }
            },
            dismissButton = {
                TextButton(onClick = { showEditDialog = false }) { Text("Cancel") }
            }
        )
    }
}