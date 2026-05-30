package com.example.healthconnect.data.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

// Represents a single chat message
data class Message(
    val id: String = "",
    val senderId: String = "",
    val text: String = "",
    val timestamp: Long = System.currentTimeMillis(),
)

class ChatViewModel : ViewModel() {

    private val _messages = MutableStateFlow<List<Message>>(emptyList())
    val messages = _messages.asStateFlow()

    private val _blockedBy = MutableStateFlow<String?>(null)
    val blockedBy = _blockedBy.asStateFlow()

    private val _events = MutableSharedFlow<String>()
    val events = _events.asSharedFlow()

    private val firestore: FirebaseFirestore by lazy { FirebaseFirestore.getInstance() }
    private val auth: FirebaseAuth by lazy { FirebaseAuth.getInstance() }

    private var conversationId: String? = null
    private var otherUserId: String? = null

    private var messagesRegistration: ListenerRegistration? = null
    private var conversationRegistration: ListenerRegistration? = null

    private fun buildConversationId(userA: String, userB: String): String {
        return if (userA > userB) {
            "$userA-$userB"
        } else {
            "$userB-$userA"
        }
    }

    private fun buildParticipants(userA: String, userB: String): List<String> {
        return if (userA > userB) {
            listOf(userA, userB)
        } else {
            listOf(userB, userA)
        }
    }

    private suspend fun ensureConversationExists(conversationId: String, participants: List<String>) {
        val conversationUpdates = mapOf(
            "participants" to participants
        )

        firestore.collection("conversations").document(conversationId)
            .set(conversationUpdates, SetOptions.merge())
            .await()
    }

    fun loadMessages(doctorId: String) {
        val patientId = auth.currentUser?.uid ?: return

        messagesRegistration?.remove()
        messagesRegistration = null
        conversationRegistration?.remove()
        conversationRegistration = null

        conversationId = buildConversationId(patientId, doctorId)
        otherUserId = doctorId
        val participants = buildParticipants(patientId, doctorId)

        viewModelScope.launch {
            try {
                ensureConversationExists(conversationId!!, participants)
            } catch (_: Exception) {
            }

            try {
                firestore.collection("conversations").document(conversationId!!)
                    .set(mapOf("unreadBy" to FieldValue.arrayRemove(patientId)), SetOptions.merge())
                    .await()
            } catch (_: Exception) {
            }

            conversationRegistration = firestore.collection("conversations").document(conversationId!!)
                .addSnapshotListener { snapshot, _ ->
                    _blockedBy.value = snapshot?.getString("blockedBy")
                }

            messagesRegistration = firestore.collection("conversations").document(conversationId!!)
                .collection("messages")
                .orderBy("timestamp", Query.Direction.ASCENDING)
                .addSnapshotListener { snapshot, e ->
                    if (e != null) { return@addSnapshotListener }

                    val messageList = snapshot?.documents?.mapNotNull { doc ->
                        val senderId = doc.getString("senderId") ?: return@mapNotNull null
                        val text = doc.getString("text") ?: ""
                        val timestamp = doc.getLong("timestamp") ?: 0L
                        Message(
                            id = doc.id,
                            senderId = senderId,
                            text = text,
                            timestamp = timestamp
                        )
                    } ?: emptyList()
                    _messages.value = messageList
                }
        }
    }

    fun sendMessage(text: String, doctorId: String) {
        val patientId = auth.currentUser?.uid ?: return
        if (conversationId == null) {
            conversationId = buildConversationId(patientId, doctorId)
            otherUserId = doctorId
        }

        val convId = conversationId ?: return
        val participants = buildParticipants(patientId, doctorId)

        val messageData = mapOf(
            "senderId" to patientId,
            "text" to text,
            "timestamp" to System.currentTimeMillis()
        )

        viewModelScope.launch {
            // Ensure conversation exists (best-effort). If it fails due to participants order rules,
            // we still want to send/update as long as the conversation already exists.
            try {
                ensureConversationExists(convId, participants)
            } catch (_: Exception) {
            }

            try {
                // Mark current user as having read the conversation.
                firestore.collection("conversations").document(convId)
                    .set(mapOf("unreadBy" to FieldValue.arrayRemove(patientId)), SetOptions.merge())
                    .await()

                // Add the message.
                firestore.collection("conversations").document(convId)
                    .collection("messages").add(messageData).await()
            } catch (_: Exception) {
                _events.emit("Failed to send message")
                return@launch
            }

            try {
                // Update conversation metadata + mark other user as unread.
                val conversationUpdates = mapOf(
                    "lastMessage" to text,
                    "lastMessageTimestamp" to System.currentTimeMillis(),
                    "unreadBy" to FieldValue.arrayUnion(doctorId)
                )
                firestore.collection("conversations").document(convId)
                    .set(conversationUpdates, SetOptions.merge())
                    .await()
            } catch (_: Exception) {
                _events.emit("Message sent, but unread status was not updated")
            }
        }
    }

    fun deleteMessage(messageId: String) {
        val convId = conversationId ?: return
        viewModelScope.launch {
            try {
                firestore.collection("conversations").document(convId)
                    .collection("messages").document(messageId)
                    .delete().await()
            } catch (_: Exception) {
            }
        }
    }

    fun editMessage(messageId: String, newText: String) {
        val convId = conversationId ?: return
        val trimmed = newText.trim()
        if (trimmed.isEmpty()) return
        viewModelScope.launch {
            try {
                firestore.collection("conversations").document(convId)
                    .collection("messages").document(messageId)
                    .update("text", trimmed)
                    .await()
            } catch (_: Exception) {
            }
        }
    }

    fun blockOtherUser() {
        val convId = conversationId ?: return
        val otherId = otherUserId ?: return
        viewModelScope.launch {
            try {
                firestore.collection("conversations").document(convId)
                    .set(mapOf("blockedBy" to otherId), SetOptions.merge())
                    .await()
                _events.emit("User blocked successfully")
            } catch (_: Exception) {
                _events.emit("Failed to block user")
            }
        }
    }

    fun unblockOtherUser() {
        val convId = conversationId ?: return
        viewModelScope.launch {
            try {
                firestore.collection("conversations").document(convId)
                    .update("blockedBy", null)
                    .await()
                _events.emit("User unblocked successfully")
            } catch (_: Exception) {
                _events.emit("Failed to unblock user")
            }
        }
    }

    override fun onCleared() {
        messagesRegistration?.remove()
        conversationRegistration?.remove()
        super.onCleared()
    }
}