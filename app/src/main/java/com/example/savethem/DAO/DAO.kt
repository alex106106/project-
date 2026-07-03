package com.example.savethem.DAO

import android.content.Context
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.example.savethem.Model.*
import com.example.savethem.util.EncryptionUtils
import com.google.android.gms.maps.model.LatLng
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.channels.trySendBlocking
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.util.UUID
import javax.inject.Inject

class DAO @Inject constructor() : AccessDAO {

    private val auth = FirebaseAuth.getInstance()
    private val database = FirebaseDatabase.getInstance()
    private val locationsRef = database.getReference("place")

    override suspend fun getAllCountries(): List<MarkerModel> {
        val markers = mutableListOf<MarkerModel>()
        val dataSnapshot = locationsRef.get().await()
        for (marker in dataSnapshot.children) {
            val id = marker.child("id").getValue(String::class.java)
            val latitude = marker.child("position").child("latitude").getValue(Double::class.java)
            val longitude = marker.child("position").child("longitude").getValue(Double::class.java)
            val gLevel = marker.child("glevel").getValue(String::class.java)
            val title = marker.child("title").getValue(String::class.java)

            markers.add(MarkerModel(id, homicide("", 0.0), kidnapping("", 0.0), vehicleTheft("", 0.0), rape("", 0.0), femicide("", 0.0), LatLng(latitude ?: 0.0, longitude ?: 0.0), title, gLevel))
        }
        return markers
    }

    override fun addPlace(merkerModel: MarkerModel): LiveData<MarkerModel> {
        val result = MutableLiveData<MarkerModel>()
        val uuid = UUID.randomUUID().toString()
        merkerModel.ID = uuid
        database.getReference("place/$uuid").setValue(merkerModel).addOnSuccessListener { result.value = merkerModel }
        return result
    }

    override suspend fun getCountryById(id: String): MarkerModel? {
        val dataSnapshot = locationsRef.orderByChild("id").equalTo(id).get().await()
        return dataSnapshot.children.firstOrNull()?.let { marker ->
            val latitude = marker.child("position").child("latitude").getValue(Double::class.java)
            val longitude = marker.child("position").child("longitude").getValue(Double::class.java)
            val gLevel = marker.child("glevel").getValue(String::class.java)
            val title = marker.child("title").getValue(String::class.java)
            MarkerModel(id, homicide("", 0.0), kidnapping("", 0.0), vehicleTheft("", 0.0), rape("", 0.0), femicide("", 0.0), LatLng(latitude ?: 0.0, longitude ?: 0.0), title, gLevel)
        }
    }

    override fun addComment(commentsModel: CommentsModel, id: String): LiveData<CommentsModel> {
        val result = MutableLiveData<CommentsModel>()
        val uuid = UUID.randomUUID().toString()
        commentsModel.IDComments = uuid
        commentsModel.UUIDComments = auth.currentUser?.uid
        database.getReference("place/$id/comments/$uuid").setValue(commentsModel).addOnSuccessListener { result.value = commentsModel }
        return result
    }

    override suspend fun getAllComments(id: String): List<CommentsModel> {
        val comments = mutableListOf<CommentsModel>()
        val dataSnapshot = database.getReference("place/$id/comments").get().await()
        for (snapshot in dataSnapshot.children) { snapshot.getValue(CommentsModel::class.java)?.let { comments.add(it) } }
        return comments
    }

    override suspend fun getUserData(): registerModel? {
        return try {
            val cu = auth.currentUser
            val snapshot = database.getReference("users/${cu?.uid}/userData").get().await()
            val user = snapshot.getValue(registerModel::class.java)
            user?.apply {
                name = EncryptionUtils.decrypt(name ?: "")
                email = EncryptionUtils.decrypt(email ?: "")
                UUID = cu?.uid // Aseguramos el Auth UID para rutas
            }
        } catch (e: Exception) { null }
    }

    override suspend fun getFriendData(idFriend: String): registerModel? {
        return try {
            val userId = auth.currentUser?.uid
            val snapshot = database.getReference("users/$userId/friends/$idFriend").get().await()
            val friend = snapshot.getValue(registerModel::class.java)
            friend?.apply {
                name = EncryptionUtils.decrypt(name ?: "")
                email = EncryptionUtils.decrypt(email ?: "")
                UUID = idFriend // El ID usado como key es el Auth UID
            }
        } catch (e: Exception) { null }
    }

    override fun getLikes(commentId: String): LiveData<Pair<List<String>, Boolean>> {
        val result = MutableLiveData<Pair<List<String>, Boolean>>()
        database.getReference("place/comments/$commentId/likes/").addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val likes = mutableListOf<String>()
                var currentLiked = false
                snapshot.children.forEach { if (it.getValue(Boolean::class.java) == true) { likes.add(it.key!!); if (it.key == auth.currentUser?.uid) currentLiked = true } }
                result.value = Pair(likes, currentLiked)
            }
            override fun onCancelled(error: DatabaseError) {}
        })
        return result
    }

    override fun addFriend(addFriend: registerModel, idFriend: String) {
        val currentUser = auth.currentUser ?: return
        val uid = currentUser.uid
        val encryptedFriendEmail = EncryptionUtils.encrypt(idFriend)
        database.getReference("users/$uid/userData").addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(userSnapshot: DataSnapshot) {
                val myData = userSnapshot.getValue(registerModel::class.java) ?: return
                database.getReference("users").orderByChild("userData/email").equalTo(encryptedFriendEmail)
                    .addListenerForSingleValueEvent(object : ValueEventListener {
                        override fun onDataChange(snapshot: DataSnapshot) {
                            if (snapshot.exists()) {
                                for (friendSnapshot in snapshot.children) {
                                    val friendUid = friendSnapshot.key ?: continue
                                    val friendData = friendSnapshot.child("userData").getValue(registerModel::class.java) ?: continue
                                    database.getReference("users/$uid/friends/$friendUid").setValue(friendData)
                                    database.getReference("users/$friendUid/friends/$uid").setValue(myData)
                                }
                            }
                        }
                        override fun onCancelled(error: DatabaseError) {}
                    })
            }
            override fun onCancelled(error: DatabaseError) {}
        })
    }

    override suspend fun addFriendWithValidation(friendEmail: String): Boolean {
        val currentUser = auth.currentUser ?: return false
        val uid = currentUser.uid
        val encryptedFriendEmail = EncryptionUtils.encrypt(friendEmail)
        
        return try {
            val userSnapshot = database.getReference("users/$uid/userData").get().await()
            val myData = userSnapshot.getValue(registerModel::class.java) ?: return false
            
            val snapshot = database.getReference("users").orderByChild("userData/email").equalTo(encryptedFriendEmail).get().await()
            
            if (snapshot.exists()) {
                var added = false
                for (friendSnapshot in snapshot.children) {
                    val friendUid = friendSnapshot.key ?: continue
                    if (friendUid == uid) continue // No agregarse a sí mismo
                    
                    val friendData = friendSnapshot.child("userData").getValue(registerModel::class.java) ?: continue
                    
                    database.getReference("users/$uid/friends/$friendUid").setValue(friendData).await()
                    database.getReference("users/$friendUid/friends/$uid").setValue(myData).await()
                    added = true
                }
                added
            } else {
                false
            }
        } catch (e: Exception) {
            Log.e("DAO", "Error adding friend", e)
            false
        }
    }

    override fun addFriendSQL(currentUser: Long, friend: registerModel): Long = 1

    override fun addFriendChat(addFriend: registerModel, uid: String) {
        val currentUserId = auth.currentUser?.uid ?: return
        database.getReference("users/$currentUserId/userData").addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(userSnapshot: DataSnapshot) {
                val myData = userSnapshot.getValue(registerModel::class.java) ?: return
                database.getReference("users/$uid/userData").addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(friendSnapshot: DataSnapshot) {
                        val friendData = friendSnapshot.getValue(registerModel::class.java) ?: return
                        database.getReference("users/$currentUserId/friends/$uid").setValue(friendData)
                        database.getReference("users/$uid/friends/$currentUserId").setValue(myData)
                    }
                    override fun onCancelled(error: DatabaseError) {}
                })
            }
            override fun onCancelled(error: DatabaseError) {}
        })
    }

    override fun getFriends(): Flow<List<registerModel>> = callbackFlow {
        val userId = auth.currentUser?.uid ?: run { close(); return@callbackFlow }
        val ref = database.getReference("users/$userId/friends")
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val friends = mutableListOf<registerModel>()
                for (snap in snapshot.children) {
                    snap.getValue(registerModel::class.java)?.let { friend ->
                        friend.name = EncryptionUtils.decrypt(friend.name ?: "")
                        friend.email = EncryptionUtils.decrypt(friend.email ?: "")
                        friend.UUID = snap.key // Usamos la key (Auth UID) para navegación y rutas
                        friends.add(friend)
                    }
                }
                trySendBlocking(friends)
            }
            override fun onCancelled(error: DatabaseError) { close(error.toException()) }
        }
        ref.addValueEventListener(listener)
        awaitClose { ref.removeEventListener(listener) }
    }

    override suspend fun getFriendsListOnce(): List<registerModel> {
        val userId = auth.currentUser?.uid ?: return emptyList()
        val snapshot = database.getReference("users/$userId/friends").get().await()
        return snapshot.children.mapNotNull { snap ->
            snap.getValue(registerModel::class.java)?.apply {
                UUID = snap.key // Aseguramos el Auth UID
            }
        }
    }

    override suspend fun getFriendById(id: String): registerModel? {
        val userId = auth.currentUser?.uid ?: return null
        val snapshot = database.getReference("users/$userId/friends/$id").get().await()
        val friend = snapshot.getValue(registerModel::class.java)
        friend?.apply { 
            name = EncryptionUtils.decrypt(name ?: "")
            email = EncryptionUtils.decrypt(email ?: "")
            UUID = id // id es el Auth UID
        }
        return friend
    }

    override suspend fun getUserDataUpdate(idUser: String, id: String, token: String): registerModel? {
        val updates = hashMapOf<String, Any>("token" to token)
        database.getReference("users/$idUser/friends/$id").updateChildren(updates)
        return registerModel(UUID = id)
    }

    override fun addMessage(addMessage: ChatModel, id: String, messageID: String): LiveData<ChatModel> {
        val result = MutableLiveData<ChatModel>()
        val uid = auth.currentUser?.uid ?: return result
        addMessage.IDMessage = messageID
        addMessage.UUIDSender = uid
        database.getReference("users/$uid/chats/$id/$messageID").setValue(true)
        database.getReference("users/$id/chats/$uid/$messageID").setValue(true)
        database.getReference("messages/$messageID").setValue(addMessage).addOnSuccessListener { result.value = addMessage }
        return result
    }

    override suspend fun getLocation(idUser: String, id: String): Flow<List<LocationModel>> = callbackFlow {
        val ref = database.getReference("users/$idUser/friends/$id/locationFriend")
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val list = snapshot.children.mapNotNull { it.getValue(LocationModel::class.java) }
                trySendBlocking(list.sortedBy { it.timestamp })
            }
            override fun onCancelled(error: DatabaseError) { close(error.toException()) }
        }
        ref.addValueEventListener(listener)
        awaitClose { ref.removeEventListener(listener) }
    }

    override fun addLocation2(addMessage: LocationModel, id: String, idUser: String, messageID: String): LiveData<LocationModel> {
        val result = MutableLiveData<LocationModel>()
        val uid = auth.currentUser?.uid ?: return result
        addMessage.IDMessage = messageID
        addMessage.UUIDSender = uid
        
        val timestampToUse = if (addMessage.timestamp != null && addMessage.timestamp!! > 0) addMessage.timestamp else ServerValue.TIMESTAMP
        val locationData = addMessage.toMap().plus("timestamp" to timestampToUse)
        
        // idUser es el Auth UID del amigo, id es el Auth UID del sender
        val updates = hashMapOf<String, Any>(
            "users/$uid/friends/$idUser/locationFriend/$messageID" to locationData,
            "users/$idUser/friends/$uid/locationFriend/$messageID" to locationData
        )
        
        database.reference.updateChildren(updates).addOnSuccessListener { 
            result.value = addMessage 
        }
        return result
    }

    override fun addLocation(addMessage: LocationModel, id: String, messageID: String): LiveData<LocationModel> {
        val result = MutableLiveData<LocationModel>()
        val uid = auth.currentUser?.uid ?: return result
        addMessage.IDMessage = messageID
        addMessage.UUIDSender = uid
        
        // Respetamos el timestamp del GPS si viene configurado
        val timestampToUse = if (addMessage.timestamp != null && addMessage.timestamp!! > 0) addMessage.timestamp else ServerValue.TIMESTAMP
        val locationData = addMessage.toMap().plus("timestamp" to timestampToUse)
        
        val updates = hashMapOf<String, Any>(
            "users/$uid/friends/$id/locationFriend/$messageID" to locationData,
            "users/$id/friends/$uid/locationFriend/$messageID" to locationData,
            "locations/$messageID" to locationData,
            "users/$uid/locations/$id/$messageID" to true,
            "users/$id/locations/$uid/$messageID" to true
        )
        
        database.reference.updateChildren(updates).addOnSuccessListener { 
            result.value = addMessage 
        }
        return result
    }

    override suspend fun getLocationById(idUser: String, id: String, latitude: Double, longitude: Double, context: Context, token: String): Flow<List<LocationModel>> = callbackFlow {
        val ref = database.getReference("users/$idUser/locations/$id")
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val ids = snapshot.children.mapNotNull { it.key }
                launch {
                    val locationsList = ids.mapNotNull { locId ->
                        database.getReference("locations/$locId").get().await().getValue(LocationModel::class.java)
                    }
                    trySend(locationsList.sortedBy { it.timestamp ?: 0L })
                }
            }
            override fun onCancelled(error: DatabaseError) { close(error.toException()) }
        }
        ref.addValueEventListener(listener)
        awaitClose { ref.removeEventListener(listener) }
    }

    override fun updateLocationById(location: LocationModel, id: String, idUser: String, messageId: String): LiveData<LocationModel> {
        val result = MutableLiveData<LocationModel>()
        val timestampToUse = if (location.timestamp != null && location.timestamp!! > 0) location.timestamp else ServerValue.TIMESTAMP
        val locationData = location.toMap().plus("timestamp" to timestampToUse)
        database.getReference("users/$idUser/friends/$id/locationFriend/$messageId").setValue(locationData).addOnSuccessListener { result.value = location }
        return result
    }

    override fun addMessage2(addMessage: ChatModel, id: String, idUser: String, messageID: String): LiveData<ChatModel> {
        val result = MutableLiveData<ChatModel>()
        val uid = auth.currentUser?.uid ?: return result
        addMessage.IDMessage = messageID
        addMessage.UUIDSender = uid
        val messageData = addMessage.toMap().plus("timestamp" to ServerValue.TIMESTAMP)
        database.getReference("messages/$messageID").setValue(messageData).addOnSuccessListener {
            val updates = hashMapOf<String, Any>("/users/$uid/chats/$idUser/$messageID" to true, "/users/$idUser/chats/$uid/$messageID" to true)
            database.reference.updateChildren(updates).addOnSuccessListener { result.value = addMessage }
        }
        return result
    }

    override suspend fun sendMessage(message: ChatModel, receiverId: String) {
        val uid = auth.currentUser?.uid ?: return
        val msgId = message.IDMessage
        val data = message.toMap().plus("timestamp" to ServerValue.TIMESTAMP)
        database.getReference("messages/$msgId").setValue(data).await()
        val updates = hashMapOf<String, Any>("users/$uid/chats/$receiverId/$msgId" to true, "users/$receiverId/chats/$uid/$msgId" to true)
        database.reference.updateChildren(updates).await()
    }

    override suspend fun getAllMessage(idUser: String, id: String): Flow<List<ChatModel>> = callbackFlow {
        val ref = database.getReference("users/$idUser/chats/$id")
        val listener = object : ChildEventListener {
            val list = mutableListOf<ChatModel>()
            override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
                val msgId = snapshot.key ?: return
                database.getReference("messages/$msgId").addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(msgSnap: DataSnapshot) {
                        msgSnap.getValue(ChatModel::class.java)?.let { it.message = EncryptionUtils.decrypt(it.message); list.add(it); trySend(list.sortedBy { m -> m.timestamp ?: 0L }) }
                    }
                    override fun onCancelled(error: DatabaseError) {}
                })
            }
            override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) {}
            override fun onChildRemoved(snapshot: DataSnapshot) {}
            override fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?) {}
            override fun onCancelled(error: DatabaseError) { close(error.toException()) }
        }
        ref.addChildEventListener(listener)
        awaitClose { ref.removeEventListener(listener) }
    }

    override suspend fun listenMessages(idUser: String, chatId: String): Flow<List<ChatModel>> = callbackFlow {
        val ref = database.getReference("users/$idUser/chats/$chatId")
        val listener = object : ChildEventListener {
            override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
                val msgId = snapshot.key ?: return
                database.getReference("messages/$msgId").get().addOnSuccessListener { it.getValue(ChatModel::class.java)?.let { m -> m.message = EncryptionUtils.decrypt(m.message); trySend(listOf(m)) } }
            }
            override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) {}
            override fun onChildRemoved(snapshot: DataSnapshot) {}
            override fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?) {}
            override fun onCancelled(error: DatabaseError) { close(error.toException()) }
        }
        ref.addChildEventListener(listener)
        awaitClose { ref.removeEventListener(listener) }
    }

    override suspend fun getAllMessage2(id: String, idUser: String): List<ChatModel> {
        val snapshot = database.getReference("users/$idUser/friends/$id/chat").get().await()
        return snapshot.children.mapNotNull { it.getValue(ChatModel::class.java) }
    }
    
    override suspend fun reportSafetyAlert(alert: SafetyAlertModel) {
        val ref = database.getReference("safety_alerts").push()
        val alertId = ref.key ?: UUID.randomUUID().toString()
        val finalAlert = alert.copy(id = alertId, date = System.currentTimeMillis())
        ref.setValue(finalAlert.toMap()).await()
    }

    override fun getSafetyAlerts(): Flow<List<SafetyAlertModel>> = callbackFlow {
        val ref = database.getReference("safety_alerts")
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val alerts = snapshot.children.mapNotNull { it.getValue(SafetyAlertModel::class.java) }
                trySendBlocking(alerts.sortedByDescending { it.date })
            }
            override fun onCancelled(error: DatabaseError) { close(error.toException()) }
        }
        ref.addValueEventListener(listener)
        awaitClose { ref.removeEventListener(listener) }
    }

    override suspend fun startEmergency(emergency: EmergencyModel) {
        database.getReference("active_emergencies/${emergency.userId}").setValue(emergency).await()
    }

    override suspend fun stopEmergency(userId: String) {
        database.getReference("active_emergencies/$userId").removeValue().await()
    }

    override fun getActiveEmergencies(): Flow<List<EmergencyModel>> = callbackFlow {
        val ref = database.getReference("active_emergencies")
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val list = snapshot.children.mapNotNull { it.getValue(EmergencyModel::class.java) }
                trySendBlocking(list)
            }
            override fun onCancelled(error: DatabaseError) { close(error.toException()) }
        }
        ref.addValueEventListener(listener)
        awaitClose { ref.removeEventListener(listener) }
    }

    // SAFE PLACES IMPLEMENTATION CON CIFRADO
    override suspend fun addSafePlace(safePlace: SafePlaceModel) {
        val uid = auth.currentUser?.uid ?: return
        val ref = database.getReference("users/$uid/safe_places").push()
        
        // CIFRAMOS EL NOMBRE DEL LUGAR
        val encryptedName = EncryptionUtils.encrypt(safePlace.name)
        val finalPlace = safePlace.copy(
            id = ref.key ?: UUID.randomUUID().toString(), 
            name = encryptedName,
            userId = uid
        )
        ref.setValue(finalPlace.toMap()).await()
    }

    override fun getSafePlaces(): Flow<List<SafePlaceModel>> = callbackFlow {
        val uid = auth.currentUser?.uid ?: run { close(); return@callbackFlow }
        val ref = database.getReference("users/$uid/safe_places")
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val list = snapshot.children.mapNotNull { snap ->
                    snap.getValue(SafePlaceModel::class.java)?.let { place ->
                        place.copy(name = EncryptionUtils.decrypt(place.name))
                    }
                }
                trySendBlocking(list)
            }
            override fun onCancelled(error: DatabaseError) { close(error.toException()) }
        }
        ref.addValueEventListener(listener)
        awaitClose { ref.removeEventListener(listener) }
    }

    override suspend fun getSafePlacesOnce(): List<SafePlaceModel> {
        val uid = auth.currentUser?.uid ?: return emptyList()
        val snapshot = database.getReference("users/$uid/safe_places").get().await()
        return snapshot.children.mapNotNull { snap ->
            snap.getValue(SafePlaceModel::class.java)?.let { place ->
                place.copy(name = EncryptionUtils.decrypt(place.name))
            }
        }
    }
}
