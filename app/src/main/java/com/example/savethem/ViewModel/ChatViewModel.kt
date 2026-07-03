package com.example.savethem.ViewModel

import android.content.Context
import android.util.Log
import androidx.lifecycle.*
import com.example.savethem.DAO.ChatDao
import com.example.savethem.Model.*
import com.example.savethem.Repository.Repository
import com.example.savethem.util.EncryptionUtils
import com.example.savethem.util.FcmUtil
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.*
import java.util.*
import javax.inject.Inject

@HiltViewModel
class ChatViewModel @Inject constructor(
    private val repository: Repository,
    private val chatDao: ChatDao,
    @ApplicationContext private val context: Context
): ViewModel() {

    private val _selectedFriend = MutableStateFlow<registerModel?>(null)
    val selectedFriend: StateFlow<registerModel?> = _selectedFriend.asStateFlow()

    private val _addMessage = MutableStateFlow<List<ChatModel>>(emptyList())
    val addMessage: StateFlow<List<ChatModel>> = _addMessage.asStateFlow()

    private val _locationById = MutableStateFlow(emptyList<LocationModel>())
    val locationById: StateFlow<List<LocationModel>> = _locationById.asStateFlow()

    private val _user = MutableStateFlow<registerModel?>(null)
    val user: StateFlow<registerModel?> = _user

    private val _profileUser = MutableStateFlow<registerModel?>(null)
    val profileUser: StateFlow<registerModel?> = _profileUser.asStateFlow()

    private var chatJob: Job? = null
    private var locationJob: Job? = null

    fun friendID(id: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val friend = repository.getFriendById(id)
            _selectedFriend.value = friend
        }
    }

    fun getUserData() {
        viewModelScope.launch {
            try {
                val userData = repository.getUserData()
                _user.value = userData
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun loadProfile(userId: String?) {
        viewModelScope.launch(Dispatchers.IO) {
            if (userId == null) {
                getUserData()
                _profileUser.value = _user.value
            } else {
                try {
                    val friendData = repository.getFriendData(userId)
                    _profileUser.value = friendData
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    fun getAllMessage(currentUserId: String, friendId: String) {
        chatJob?.cancel()
        _addMessage.value = emptyList()

        chatJob = viewModelScope.launch {
            launch {
                chatDao.getMessages(friendId)
                    .map { entities ->
                        entities.map { entity ->
                            ChatModel(entity.idMessage, entity.uuidSender, entity.message, entity.timestamp, entity.seen)
                        }
                    }
                    .collect { messages ->
                        _addMessage.value = messages
                    }
            }

            repository.getAllMessage(currentUserId, friendId)
                .flowOn(Dispatchers.IO)
                .collect { firebaseMessages ->
                    withContext(Dispatchers.IO) {
                        val entities = firebaseMessages.map { model ->
                            ChatEntity(
                                idMessage = model.IDMessage,
                                message = EncryptionUtils.decrypt(model.message),
                                uuidSender = model.UUIDSender,
                                timestamp = model.timestamp,
                                seen = model.seen,
                                friendId = friendId
                            )
                        }
                        chatDao.insertMessages(entities)
                    }
                }
        }
    }

    fun locationID(idUser: String, friendId: String, latitude: Double, longitude: Double, context: Context, token: String) {
        locationJob?.cancel()
        _locationById.value = emptyList()

        locationJob = viewModelScope.launch {
            launch {
                chatDao.getLocations(friendId)
                    .map { entities ->
                        entities.map { entity ->
                            LocationModel(entity.idMessage, entity.uuidSender, LatLngWrapper(entity.latitude, entity.longitude), entity.timestamp)
                        }
                    }
                    .collect { locations ->
                        _locationById.value = locations
                    }
            }

            repository.getLocation(idUser, friendId)
                .flowOn(Dispatchers.IO)
                .collect { firebaseLocations ->
                    withContext(Dispatchers.IO) {
                        val entities = firebaseLocations.map { model ->
                            LocationEntity(
                                idMessage = model.IDMessage ?: UUID.randomUUID().toString(),
                                latitude = model.location?.latitude ?: 0.0,
                                longitude = model.location?.longitude ?: 0.0,
                                timestamp = model.timestamp ?: 0L,
                                uuidSender = model.UUIDSender ?: "",
                                friendId = friendId
                            )
                        }
                        chatDao.insertLocations(entities)
                    }
                }
        }
    }

    fun sendMessage(messageText: String, receiverId: String) {
        val currentUser = FirebaseAuth.getInstance().currentUser ?: return
        val messageID = UUID.randomUUID().toString()
        val timestamp = System.currentTimeMillis()
        val encryptedMessage = EncryptionUtils.encrypt(messageText)

        val chatModelForFirebase = ChatModel(
            IDMessage = messageID,
            message = encryptedMessage,
            UUIDSender = currentUser.uid,
            timestamp = timestamp,
            seen = false,
            receiverId = receiverId
        )

        viewModelScope.launch(Dispatchers.IO) {
            try {
                // 1. Guardar localmente y en Firebase
                chatDao.insertMessages(listOf(
                    ChatEntity(messageID, messageText, currentUser.uid, timestamp, false, receiverId)
                ))
                repository.sendMessage(chatModelForFirebase, receiverId)

                // 2. Obtener el nombre del remitente (desencriptado) para la notificación
                val senderName = repository.getUserData()?.name ?: "Alguien"

                // 3. Enviar Notificación al receptor
                // Buscamos el token más reciente del receptor directamente desde su nodo principal
                val recipientToken = FirebaseDatabase.getInstance()
                    .getReference("users/$receiverId/userData/token")
                    .get().addOnSuccessListener { snapshot ->
                        val token = snapshot.getValue(String::class.java)
                        if (!token.isNullOrEmpty()) {
                            FcmUtil.sendNotification(
                                context = context,
                                token = token,
                                title = senderName,
                                body = messageText, // El texto original sin cifrar para la vista previa
                                icon = "norma"
                            )
                        }
                    }
            } catch (e: Exception) {
                Log.e("ChatVM", "Error enviando mensaje o notificación", e)
            }
        }
    }

    fun getGlobalChatMessage() {
        viewModelScope.launch {
            getAllMessagesFromGlobalChat()
                .flowOn(Dispatchers.IO)
                .collect { messages ->
                    val decrypted = messages.map { it.copy(message = EncryptionUtils.decrypt(it.message)) }
                    _addMessage.value = decrypted
                }
        }
    }

    private fun getAllMessagesFromGlobalChat(): Flow<List<ChatModel>> = callbackFlow {
        val globalMessagesRef = FirebaseDatabase.getInstance().getReference("globalChatMessages").orderByKey()
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val messages = snapshot.children.mapNotNull { it.getValue(ChatModel::class.java) }
                trySend(messages.sortedBy { it.timestamp }).isSuccess
            }
            override fun onCancelled(error: DatabaseError) { close(error.toException()) }
        }
        globalMessagesRef.addValueEventListener(listener)
        awaitClose { globalMessagesRef.removeEventListener(listener) }
    }

    fun addMessageGlobal(addMessage: ChatModel, messageID: String) {
        val currentUser = FirebaseAuth.getInstance().currentUser ?: return
        val encrypted = EncryptionUtils.encrypt(addMessage.message)
        addMessage.IDMessage = messageID
        addMessage.UUIDSender = currentUser.uid
        addMessage.message = encrypted
        val centralMessageRef = FirebaseDatabase.getInstance().getReference("globalChatMessages").child(messageID)
        centralMessageRef.setValue(addMessage.toMap().plus("timestamp" to ServerValue.TIMESTAMP))
    }

    fun addLocation(addMessage: LocationModel, id: String, messageID: String, idUser: String) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.addLocation(addMessage, id, messageID)
        }
    }
}
