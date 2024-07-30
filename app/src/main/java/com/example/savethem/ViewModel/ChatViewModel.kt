package com.example.savethem.ViewModel

import android.content.Context
import android.util.Log
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.*
import com.example.savethem.Model.ChatLocationModel
import com.example.savethem.Model.ChatModel
import com.example.savethem.Model.LocationModel
import com.example.savethem.Model.registerModel
import com.example.savethem.Repository.Repository
import com.example.savethem.call.enviar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.google.firebase.messaging.FirebaseMessaging
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.channels.trySendBlocking
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.tasks.await
import javax.inject.Inject


@HiltViewModel
class ChatViewModel @Inject constructor(
	private val DAO: Repository
): ViewModel(){

	private val _selectedFriend = MutableStateFlow<registerModel?>(null)
	val selectedFriend: StateFlow<registerModel?> = _selectedFriend.asStateFlow()

	private val _addMessage = MutableStateFlow<List<ChatModel>>(emptyList())
	val addMessage: StateFlow<List<ChatModel>> = _addMessage.asStateFlow()

	private val _addLocation = MutableStateFlow(emptyList<LocationModel>())
	val addLocation: StateFlow<List<LocationModel>> = _addLocation.asStateFlow()

	private val _locationById = MutableStateFlow(emptyList<LocationModel>())
	val locationById: StateFlow<List<LocationModel>> = _locationById.asStateFlow()

	private val _updateUserData = MutableStateFlow<registerModel?>(null)
	val updateUserData: StateFlow<registerModel?> = _updateUserData.asStateFlow()

	private val _updateLocationResult = MediatorLiveData<LocationModel>()
	val updateLocationResult: LiveData<LocationModel> = _updateLocationResult


//	private val _addLocation = MutableStateFlow(emptyList<ChatLocationModel>())
//	val addLocation: StateFlow<List<ChatLocationModel>> = _addLocation.asStateFlow()





	private val _name = MutableStateFlow("")
	val name: StateFlow<String> = _name

	fun friendID(id: String){
		viewModelScope.launch {
			withContext(Dispatchers.IO){
				val friend = DAO.getFriendById(id)
				_selectedFriend.value = friend
			}
		}
	}
	fun getUserData(){
		viewModelScope.launch {
			val userData = DAO.getUserData()
			userData?.let {
				_name.value = it.name.toString()
			}
		}
	}
	fun getUserData(idFriend: String){
		viewModelScope.launch {
			val userData = DAO.getFriendData(idFriend)
			_name.value = userData.toString()
		}
	}
	fun getSelectedFriendToken(context: Context) {
		FirebaseMessaging.getInstance().token
			.addOnCompleteListener { task ->
				if (task.isSuccessful) {
					val token = task.result
//				enviar(context, token)

					Log.d("TOKEN del usuario", token)
				} else {
					Log.e("TOKEN del usuario", "Error al obtener el token: ${task.exception}")
				}
			}
	}

	fun addMessage(addMessage: ChatModel, id: String, messageID: String, idUser: String){
		viewModelScope.launch {
			val message = DAO.addMessage(addMessage, id, messageID)
			withContext(Dispatchers.IO){
				val comments = DAO.getLocation(idUser,id)
				_name.value = comments.toString()
			}
//			getAllMessage(idUser, id)
		}
	}
	fun addLocation(addMessage: LocationModel, id: String, messageID: String, idUser: String){
		viewModelScope.launch {
			val message = DAO.addLocation(addMessage, id, messageID)
			withContext(Dispatchers.IO){
				message.value?.let {
					_addLocation.value = _addLocation.value + it
					getAllMessage(idUser,id)
					message.value!!
				}
			}
			getAllMessage(idUser, id)
		}
	}
	fun addLocation2(addMessage: LocationModel, id: String, idUser: String, messageID: String){
		viewModelScope.launch {
			val message = DAO.addLocation2(addMessage, id, idUser, messageID)
			withContext(Dispatchers.IO){
				message.value?.let {
					_addLocation.value = _addLocation.value + it
					getAllMessage(idUser, id)
					message.value!!
				}
			}
			getAllMessage(idUser, id)
		}
	}
	fun updateLocation(addMessage: LocationModel, id: String, idUser: String, messageID: String) {
		viewModelScope.launch {
			val result = DAO.updateLocation(addMessage, id, idUser, messageID)
			_updateLocationResult.addSource(result) { updatedLocation ->
				_updateLocationResult.value = updatedLocation
			}
		}
	}
	fun getLocation(idUser: String, id: String) {
		viewModelScope.launch {
			withContext(Dispatchers.IO) {
				DAO.getLocation(idUser, id).collect { messages ->
					_addLocation.value = messages
				}
			}
		}
	}

	fun locationID(idUser: String, id: String, latitude: Double, longitude: Double, context: Context,
	               token: String){
		viewModelScope.launch {
			DAO.getLocationById(idUser, id, latitude, longitude, context, token)
				.flowOn(Dispatchers.IO)
				.collect{ location ->
					_locationById.value = location
				}
		}
	}
	fun getUserDataUpdate(idUser: String, id: String, token: String){
		viewModelScope.launch {
			withContext(Dispatchers.IO){
				val location = DAO.getUserDataUpdate(idUser, id, token)
				_updateUserData.value = location
			}
		}
	}
	fun addMessage2(addMessage: ChatModel, id: String, idUser: String, messageID: String) {
		viewModelScope.launch(Dispatchers.IO) {
			val message = DAO.addMessage2(addMessage, id, idUser, messageID)
			message.value?.let {
				// Acumula mensajes y actualiza en lote
				val currentMessages = _addMessage.value.toMutableList()
				currentMessages.add(it)
				_addMessage.value = currentMessages
			}
		}
	}
	fun addMessageGlobal(addMessage: ChatModel, messageID: String) {
		viewModelScope.launch(Dispatchers.IO) {
			val message = addMessageGlobalChat(addMessage, messageID)
			message.let {
				// Acumula mensajes y actualiza en lote
				val currentMessages = _addMessage.value.toMutableList()
//				currentMessages.add()
				_addMessage.value = currentMessages
			}
		}

	}
	fun getAllMessage(idUser: String, id: String) {
		viewModelScope.launch {
			DAO.getAllMessage(idUser, id)
				.flowOn(Dispatchers.IO)
				.collect { messages ->
					_addMessage.value = messages
				}
		}
	}

	fun getGlobalChatMessage() {
		viewModelScope.launch {
			getAllMessagesFromGlobalChat()
				.flowOn(Dispatchers.IO)
				.collect { messages ->
					_addMessage.value = messages
				}
		}
	}

	fun markMessageAsSeen(id: String, idUser: String, messageID: String) {
		viewModelScope.launch {
			withContext(Dispatchers.IO) {
				DAO.markMessageAsSeen(id, idUser, messageID)
			}
		}
	}

	fun getAllMessage2(id: String, idUser: String) {
		viewModelScope.launch {
			withContext(Dispatchers.IO) {
				val message = DAO.getAllMessage2(id,idUser) // Obtener mensajes del usuario receptor
				_addMessage.value = message
			}
		}
	}
	private val _friends = MutableStateFlow(emptyList<registerModel>())
	val friends: StateFlow<List<registerModel>> = _friends.asStateFlow()

	fun addFriend(addFriend: registerModel, uid: String){
		viewModelScope.launch {
			val add = DAO.addFriendChat(addFriend, uid)
			add
		}
	}

}
fun addMessageGlobalChat(addMessage: ChatModel, messageID: String): LiveData<ChatModel> {
	val result = MutableLiveData<ChatModel>()
	val currentUser = FirebaseAuth.getInstance().currentUser ?: return result
	addMessage.IDMessage = messageID
	addMessage.UUIDSender = currentUser.uid
	addMessage.timestamp = -1L // Valor temporal para el timestamp

	// Ruta centralizada para el mensaje en el chat global
	val centralMessageRef = FirebaseDatabase.getInstance().getReference("globalChatMessages").child(messageID)

	// Usar ServerValue.TIMESTAMP para obtener el timestamp del servidor
	val messageData = addMessage.toMap().plus("timestamp" to ServerValue.TIMESTAMP)

	centralMessageRef.setValue(messageData).addOnSuccessListener {
		// Obtener el timestamp del servidor
		centralMessageRef.child("timestamp").addListenerForSingleValueEvent(object : ValueEventListener {
			override fun onDataChange(snapshot: DataSnapshot) {
				val serverTimestamp = snapshot.value as? Long
				serverTimestamp?.let {
					addMessage.timestamp = it
					result.value = addMessage
				}
			}

			override fun onCancelled(error: DatabaseError) {
				Log.e("addMessageGlobalChat", "Failed to get server timestamp: ${error.message}")
			}
		})
	}.addOnFailureListener {
		Log.e("addMessageGlobalChat", "Failed to send message: ${it.message}")
	}

	return result
}
suspend fun getAllMessagesFromGlobalChat(): Flow<List<ChatModel>> = callbackFlow {
	val globalMessagesRef = FirebaseDatabase.getInstance().getReference("globalChatMessages").orderByKey()

	val listener = object : ValueEventListener {
		override fun onDataChange(snapshot: DataSnapshot) {
			val messages = snapshot.children.mapNotNull { it.getValue(ChatModel::class.java) }
			trySend(messages.sortedBy { it.timestamp }).isSuccess
		}

		override fun onCancelled(error: DatabaseError) {
			close(error.toException())
		}
	}

	globalMessagesRef.addValueEventListener(listener)

	// Cancelar el listener cuando el flow es cancelado
	awaitClose { globalMessagesRef.removeEventListener(listener) }
}
