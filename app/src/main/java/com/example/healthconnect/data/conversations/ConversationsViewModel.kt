package com.example.healthconnect.data.conversations

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

data class Conversation(
    val conversationId: String,
    val otherUserId: String,
    val otherUserName: String,
    val lastMessage: String,
    val lastMessageTimestamp: Long,
    val hasNewMessages: Boolean = false,
    val unreadCount: Int = 0
)

class ConversationsViewModel : ViewModel() {
    
    private val _conversations = MutableStateFlow<List<Conversation>>(emptyList())
    val conversations = _conversations.asStateFlow()
    
    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()
    
    private val firestore: FirebaseFirestore by lazy { FirebaseFirestore.getInstance() }
    private val auth: FirebaseAuth by lazy { FirebaseAuth.getInstance() }
    
    fun fetchConversations() {
        _isLoading.value = true
        viewModelScope.launch {
            val currentUserId = auth.currentUser?.uid ?: return@launch
            
            try {

                val snapshot = try {
                    firestore.collection("conversations")
                        .whereArrayContains("participants", currentUserId)
                        .orderBy("lastMessageTimestamp", Query.Direction.DESCENDING)
                        .get()
                        .await()
                } catch (e: Exception) {
                    // Fallback: try without ordering if index doesn't exist
                    android.util.Log.w("ConversationsViewModel", "Composite index not found, fetching without ordering: ${e.message}")
                    firestore.collection("conversations")
                        .whereArrayContains("participants", currentUserId)
                        .get()
                        .await()
                }
                
                val conversationList = mutableListOf<Conversation>()
                
                for (doc in snapshot.documents) {
                    val participants = doc.get("participants") as? List<*> ?: continue
                    val otherUserId = participants.firstOrNull { it.toString() != currentUserId }?.toString() ?: continue
                    
                    // Check if current user is blocked (filter out blocked conversations)
                    val blockedBy = doc.getString("blockedBy")
                    if (blockedBy == currentUserId) {
                        continue // Skip conversations where current user is blocked
                    }
                    
                    // Fetch other user's details
                    val otherUserDoc = firestore.collection("users").document(otherUserId).get().await()
                    val otherUserName = otherUserDoc.getString("name") ?: "Unknown User"
                    
                    val lastMessage = doc.getString("lastMessage") ?: ""
                    val lastMessageTimestamp = doc.getLong("lastMessageTimestamp") ?: 0L

                    val unreadCount = 0 // TODO: Implement proper unread count
                    
                    conversationList.add(
                        Conversation(
                            conversationId = doc.id,
                            otherUserId = otherUserId,
                            otherUserName = otherUserName,
                            lastMessage = lastMessage,
                            lastMessageTimestamp = lastMessageTimestamp,
                            hasNewMessages = unreadCount > 0,
                            unreadCount = unreadCount
                        )
                    )
                }
                
                _conversations.value = conversationList
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    fun refresh() {
        fetchConversations()
    }
}

