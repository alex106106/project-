package com.example.savethem.ViewModel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.savethem.Model.ChatModel
import com.example.savethem.Model.registerModel
import com.example.savethem.Repository.Repository
import com.example.savethem.Repository.UserRepository
import com.example.savethem.util.EncryptionUtils
import com.example.savethem.util.FcmUtil
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class FriendsViewModel @Inject constructor(
    private val repository: Repository,
    private val userRepository: UserRepository,
    @ApplicationContext private val context: Context // Inyectamos el contexto para FCM
) : ViewModel() {

    private val _friends = MutableStateFlow<List<registerModel>>(emptyList())
    val friends: StateFlow<List<registerModel>> = _friends.asStateFlow()

    private val _toastMessage = MutableSharedFlow<String>()
    val toastMessage: SharedFlow<String> = _toastMessage.asSharedFlow()

    init {
        observeFriends()
    }

    private fun observeFriends() {
        viewModelScope.launch(Dispatchers.IO) {
            repository.getFriends().collectLatest { friendList ->
                _friends.value = friendList
            }
        }
    }

    fun addFriend(friendEmail: String) {
        if (friendEmail.isEmpty()) {
            viewModelScope.launch { _toastMessage.emit("Please enter an email") }
            return
        }

        val currentUserEmail = FirebaseAuth.getInstance().currentUser?.email
        if (friendEmail == currentUserEmail) {
            viewModelScope.launch { _toastMessage.emit("You cannot add yourself") }
            return
        }

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val result = repository.addFriendWithValidation(friendEmail)
                if (result) {
                    _toastMessage.emit("Friend added successfully")
                } else {
                    _toastMessage.emit("User not found or already added")
                }
            } catch (e: Exception) {
                _toastMessage.emit("Error: ${e.message}")
            }
        }
    }

    fun getAllFriends() {
        observeFriends()
    }

    fun sendEmergencyMessage() {
        val currentUser = FirebaseAuth.getInstance().currentUser ?: return
        val currentFriends = _friends.value
        val emergencyText = "🚨 EMERGENCIA: ¡Necesito ayuda! Esta es una alerta automática de WomenSafe."
        val encryptedMessage = EncryptionUtils.encrypt(emergencyText)

        viewModelScope.launch(Dispatchers.IO) {
            val senderName = repository.getUserData()?.name ?: "Un usuario"
            currentFriends.forEach { friend ->
                val friendId = friend.UUID ?: return@forEach
                val messageID = UUID.randomUUID().toString()
                val chatModel = ChatModel(
                    IDMessage = messageID,
                    message = encryptedMessage,
                    UUIDSender = currentUser.uid,
                    timestamp = System.currentTimeMillis(),
                    seen = false
                )
                repository.sendMessage(chatModel, friendId)
                
                // Enviar notificación FCM V1 (incluyendo el contexto)
                friend.token?.let { token ->
                    FcmUtil.sendNotification(
                        context = context,
                        token = token,
                        title = "ALERTA SOS",
                        body = "$senderName ha activado el servicio de emergencia.",
                        icon = "otro"
                    )
                }
            }
        }
    }

    fun enviarNotificaciones(tokens: List<String>) {
        viewModelScope.launch {
            tokens.forEach { token ->
                FcmUtil.sendNotification(
                    context = context,
                    token = token,
                    title = "Notificación Masiva",
                    body = "Este es un mensaje de prueba para todos los amigos."
                )
            }
        }
    }
}
