package com.example.savethem.DAO

import android.content.Context
import android.util.Log
import android.widget.Toast
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.example.savethem.Model.*
import com.example.savethem.call.enviar
import com.example.savethem.util.DatabaseHelper
//import com.example.savethem.util.asda
//import com.example.savethem.util.constants.Screens.messageID
//import com.example.savethem.util.constants.Screens.messageID
import com.example.savethem.util.constants.Screens.time
import com.google.android.gms.maps.model.LatLng
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.channels.trySendBlocking
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.util.UUID
import java.util.concurrent.CountDownLatch
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.inject.Inject

class DAO @Inject constructor() : AccessDAO {

    val locations = FirebaseDatabase.getInstance().getReference("place")
    val currentUser = FirebaseAuth.getInstance().currentUser
    var messageID = UUID.randomUUID().toString()

//    val DBR = FirebaseDatabase.getInstance().getReference("users/"+currentUser?.uid + "/userData/")


    override suspend fun getAllCountries(): List<MarkerModel> {
        val markers = mutableListOf<MarkerModel>()
        val dataSnapshot = locations.get().await()
        for (marker in dataSnapshot.children) {
            val id = marker.child("id").getValue(String::class.java)
            val latitude = marker.child("position").child("latitude").getValue(Double::class.java)
            val longitude = marker.child("position").child("longitude").getValue(Double::class.java)
            val gLevel = marker.child("glevel").getValue(String::class.java)
            val title = marker.child("title").getValue(String::class.java)

            val mark = MarkerModel(
                id,
                homicide("", 0.0),
                kidnapping("", 0.0),
                vehicleTheft("", 0.0),
                rape("", 0.0),
                femicide("", 0.0),
                LatLng(latitude ?: 0.0, longitude ?: 0.0),
                title,
                gLevel
            )
            markers.add(mark)
        }
        return markers
    }

    override fun addPlace(markerModel: MarkerModel): LiveData<MarkerModel> {
        val result = MutableLiveData<MarkerModel>()
        val uuid = UUID.randomUUID().toString()
        markerModel.ID = uuid // Asignar el UUID a la propiedad ID del modelo
        val DBR = FirebaseDatabase.getInstance().getReference("place/$uuid")
        DBR.setValue(markerModel).addOnSuccessListener {
            result.value = markerModel
        }.addOnFailureListener { }
        return result
    }

    override suspend fun getCountryById(id: String): MarkerModel? {
        val dataSnapshot = locations.orderByChild("id").equalTo(id).get().await()
        return dataSnapshot.children.firstOrNull()?.let { marker ->
            val latitude = marker.child("position").child("latitude").getValue(Double::class.java)
            val longitude = marker.child("position").child("longitude").getValue(Double::class.java)
            val gLevel = marker.child("glevel").getValue(String::class.java)
            val femicideLevel = marker.child("femicide").child("level").getValue(String::class.java)
            val femicidePer = marker.child("femicide").child("percentage").getValue(String::class.java)
            val femiceP = femicidePer?.toDoubleOrNull() ?: 0.0

            val homicideLevel = marker.child("homicide").child("level").getValue(String::class.java)
            val homicidePer = marker.child("homicide").child("percentage").getValue(String::class.java)
            val homicideP = homicidePer?.toDoubleOrNull() ?: 0.0

            val kidnappingLevel = marker.child("kidnapping").child("level").getValue(String::class.java)
            val kidnappingPer = marker.child("kidnapping").child("percentage").getValue(String::class.java)
            val kidnappingP = kidnappingPer?.toDoubleOrNull() ?: 0.0

            val rapeLevel = marker.child("rape").child("level").getValue(String::class.java)
            val rapePer = marker.child("rape").child("percentage").getValue(String::class.java)
            val rapeP = rapePer?.toDoubleOrNull() ?: 0.0

            val vehicleTheftLevel = marker.child("vehicleTheft").child("level").getValue(String::class.java)
            val vehicleTheftPer = marker.child("vehicleTheft").child("percentage").getValue(String::class.java)
            val vehicleTheftP = vehicleTheftPer?.toDoubleOrNull() ?: 0.0

            val title = marker.child("title").getValue(String::class.java)
            MarkerModel(
                id,
                homicide(homicideLevel ?: "", homicideP),
                kidnapping(kidnappingLevel ?: "", kidnappingP),
                vehicleTheft(vehicleTheftLevel ?: "", vehicleTheftP),
                rape(rapeLevel ?: "", rapeP),
                femicide(femicideLevel ?: "", femiceP),
                LatLng(latitude ?: 0.0, longitude ?: 0.0),
                title,
                gLevel
            )
        }
    }

    override fun addComment(commentsModel: CommentsModel, id: String): LiveData<CommentsModel> {
        val result = MutableLiveData<CommentsModel>()
        val uuid = UUID.randomUUID().toString()
        commentsModel.IDComments = uuid
        commentsModel.UUIDComments = FirebaseAuth.getInstance().currentUser?.uid
        val commentsRef = FirebaseDatabase.getInstance().getReference("place/$id/comments/$uuid")
        commentsRef.setValue(commentsModel).addOnSuccessListener {
            result.value = commentsModel
        }.addOnFailureListener {
            // Manejar el error
        }
        return result
    }

    override suspend fun getAllComments(id: String): List<CommentsModel> {
        val comentarios = mutableListOf<CommentsModel>()
        val comentariosRef = FirebaseDatabase.getInstance().reference.child("place").child(id).child("comments")
        val dataSnapshot = comentariosRef.get().await()
        for (comentarioSnapshot in dataSnapshot.children) {
            val comentario = comentarioSnapshot.getValue(CommentsModel::class.java)
            comentarios.add(comentario!!)
        }
        return comentarios
    }

    val auth = FirebaseAuth.getInstance()
    val database = FirebaseDatabase.getInstance()
    override suspend fun getUserData(): registerModel? {
        return try {
            val cu = auth.currentUser
            val ref = database.getReference("users/${cu?.uid}/userData")
            val snapshot = ref.get().await()
            snapshot.getValue(registerModel::class.java)
        } catch (e: Exception) {
            null
        }
    }

    override suspend fun getFriendData(idFriend: String): registerModel? {
        return try {
            val cu = auth.currentUser
            val ref = database.getReference("users/${cu?.uid}/friends/$idFriend")
            val snapshot = ref.get().await()
            snapshot.getValue(registerModel::class.java)
        } catch (e: Exception) {
            null
        }
    }

    override fun getLikes(commentId: String): LiveData<Pair<List<String>, Boolean>> {
        val likesLiveData = MutableLiveData<Pair<List<String>, Boolean>>()
        val likesRef = FirebaseDatabase.getInstance().getReference("place/comments/$commentId/likes/")
        likesRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val likes = mutableListOf<String>()
                var currentUserLiked = false
                for (likeSnap in snapshot.children) {
                    val value = likeSnap.getValue(Boolean::class.java)
                    if (value == true) {
                        likes.add(likeSnap.key!!)
                    }
                    if (likeSnap.key == FirebaseAuth.getInstance().currentUser?.uid) {
                        currentUserLiked = likeSnap.getValue(Boolean::class.java) ?: false
                    }
                }

                likesLiveData.value = Pair(likes, currentUserLiked)
            }

            override fun onCancelled(error: DatabaseError) {
                // Manejar el error
            }
        })
        return likesLiveData
    }

    override fun addFriend(addFriend: registerModel, friendEmail: String) {
        val user = FirebaseAuth.getInstance().currentUser
        val uid = user?.uid ?: return

        fun addFriendCurrentUser(
            friendUid: String,
            friendName: String?,
            friendEmail: String?,
            friendToken: String?
        ) {
            val currentUserRef = FirebaseDatabase.getInstance().getReference("users").child(uid)
            val friendData = mapOf(
                "UUID" to friendUid,
                "email" to friendEmail,
                "name" to friendName,
                "token" to friendToken
            )
            currentUserRef.child("friends").child(friendUid).setValue(friendData)
        }

        val friendRef = FirebaseDatabase.getInstance().getReference("users")
            .orderByChild("userData/email")
            .equalTo(friendEmail)

        friendRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    for (friendSnapshot in snapshot.children) {
                        val friendUid = friendSnapshot.key
                        val friendName = friendSnapshot.child("userData/name").value as? String
                        val friendToken = friendSnapshot.child("userData/token").value as? String

                        if (friendUid != null) {
                            addFriendCurrentUser(
                                friendUid,
                                friendName,
                                friendEmail,
                                friendToken
                            )
                        }
                    }
                } else {
                    // No se encontró un usuario con el correo electrónico especificado
                    Log.d("AddFriend", "No user found with email: $friendEmail")
                }
            }

            override fun onCancelled(error: DatabaseError) {
                // Manejar la cancelación
                Log.e("AddFriend", "DatabaseError: ${error.message}")
            }
        })
    }

    //    SQLITE ADDFRIEND
    override fun addFriendSQL(currentUser: Long, friend: registerModel): Long {
        val context = LocalContext
//         val dbHelper = DatabaseHelper(context = context)
        return 1
    }


    override fun addFriendChat(addFriend: registerModel, uid: String) {
        val user = FirebaseAuth.getInstance().currentUser?.uid
        val uid = uid
        val idFriend = user ?: return

        fun addFriendCurrentUser(
            friendUid: String,
            friendName: String?,
            friendEmail: String?,
            friendToken: String?
        ){
            val currentUserRef = FirebaseDatabase.getInstance().getReference("users").child(uid)
            val friendData = mapOf(
                "UUID" to friendUid,
                "email" to friendEmail,
                "name" to friendName,
                "token" to friendToken
            )
            currentUserRef.child("friends").child(friendUid).setValue(friendData)
        }
        val friendRef = FirebaseDatabase.getInstance()
            .getReference("users")
            .child(idFriend.toString()).child("userData")
        friendRef.addListenerForSingleValueEvent(object : ValueEventListener{
            override fun onDataChange(snapshot: DataSnapshot) {

                val friendName = snapshot.child("name").value as? String
                val friendEmail = snapshot.child("email").value as? String
                val friendToken = snapshot.child("token").value as? String

                addFriendCurrentUser(
                    idFriend.toString(),
                    friendName,
                    friendEmail,
                    friendToken
                )
            }

            override fun onCancelled(error: DatabaseError) {
                TODO("Not yet implemented")
            }

        })
    }

    override suspend fun getFriends(): List<registerModel> {
        val friends = mutableListOf<registerModel>()
        val currentUser = FirebaseAuth.getInstance().currentUser
        val userId = currentUser?.uid

            val reference = FirebaseDatabase.getInstance().getReference("users/$userId/friends")
            val dataSnapshot = reference.get().await()
            for (snapshot in dataSnapshot.children) {
                val friend = snapshot.getValue(registerModel::class.java)
                friends.add(friend!!)
            }

        return friends
    }

    override suspend fun getFriendById(id: String): registerModel? {
        val currentUser = FirebaseAuth.getInstance().currentUser
        val userId = currentUser?.uid
        val friend = FirebaseDatabase.getInstance().getReference("users/$userId/friends")
        val dataSnapshot = friend.orderByChild("UUID").equalTo(id).get().await()
        return dataSnapshot.children.firstOrNull()?.let { friend ->
            val UUIDFriend = friend.child("UUID").getValue(String::class.java)
            val name = friend.child("name").getValue(String::class.java)
            val token = friend.child("token").getValue(String::class.java)

            registerModel(
                id,
                "",
                name ?: "",
                UUIDFriend ?: "",
                token ?: ""

            )
        }
    }

    override suspend fun getUserDataUpdate(idUser: String, id: String, token: String): registerModel? {
        val friendRef = FirebaseDatabase.getInstance().getReference("users/$idUser/friends")
        val friendSnapshot = friendRef.get().await()

        if (friendSnapshot.exists()) {
            val friend = friendSnapshot.children.first()
            val UUID = friend.child("UUID").getValue(String::class.java)
            val sender = friend.child("uuidsender").getValue(String::class.java)
            val times = friend.child("timestamp").getValue(Long::class.java)
            val lat = friend.child("location").child("latitude").getValue(Double::class.java)
            val lon = friend.child("location").child("longitude").getValue(Double::class.java)


            // Crear un Map con los campos que deseas actualizar y sus nuevos valores
            val updates = HashMap<String, Any>()
            updates["token"] = token
//            updates["location/longitude"] = longitude

//            updates["uuidsender"] = "nuevoValorUUIDSender"

            // Actualizar los datos utilizando updateChildren()
            friendRef.child(friend.key ?: "").updateChildren(updates)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
//                        Toast.makeText(context, "Please enter email and password", Toast.LENGTH_SHORT).show()
                    } else {
                        // Ocurrió un error al actualizar los datos
                    }
                }

            return registerModel(
                "",
                "",
                "",
                UUID ?: ""
            )
        }

        return null
    }

    override fun addMessage(addMessage: ChatModel, id: String, messageID: String): LiveData<ChatModel> {
        val result = MutableLiveData<ChatModel>()
        val cu = auth.currentUser
        addMessage.IDMessage = messageID
        addMessage.UUIDSender = FirebaseAuth.getInstance().currentUser?.uid!!
        val messageRef = FirebaseDatabase.getInstance().getReference("users/${cu?.uid}/friends/$id/chat/$messageID")
        val timestamp = addMessage.copy(timestamp = time)
        messageRef.setValue(addMessage).addOnSuccessListener {
            messageRef.setValue(timestamp).addOnSuccessListener{
                result.value = addMessage.copy(timestamp = time)
            }
        }.addOnFailureListener {

        }
        return result
    }



    override suspend fun getLocation(idUser: String, id: String): Flow<List<LocationModel>> = callbackFlow {
        val messagesRef = FirebaseDatabase.getInstance().getReference("users/$idUser/friends/$id/locationFriend")

        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val messages = mutableListOf<LocationModel>()
                for (messageSnapshot in snapshot.children) {
                    val message = messageSnapshot.getValue(LocationModel::class.java)
                    message?.let { messages.add(it) }
                }
                trySendBlocking(messages)
            }

            override fun onCancelled(error: DatabaseError) {
                close(error.toException())
            }
        }

        messagesRef.addValueEventListener(listener)

        awaitClose { messagesRef.removeEventListener(listener) }
    }

    override fun addLocation2(
        addMessage: LocationModel,
        id: String,
        idUser: String,
        messageID: String
    ): LiveData<LocationModel> {
        val result = MutableLiveData<LocationModel>()
        val currentUser = auth.currentUser
        val locationID = UUID.randomUUID()
        addMessage.IDMessage = messageID
        addMessage.UUIDSender = currentUser?.uid
        val userLocationRef = FirebaseDatabase.getInstance().getReference("users/${idUser}/friends/$id/locationFriend")
        userLocationRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val userHasLocation = snapshot.exists()

                // Permitir al usuario receptor agregar una ubicación si no tiene una ubicación previa
                if (!userHasLocation) {
                    val messageRef = userLocationRef.child(messageID)
                    val timestamp = addMessage.copy(timestamp = time)
                    messageRef.setValue(addMessage).addOnSuccessListener {
                        messageRef.setValue(timestamp).addOnSuccessListener {
                            result.value = addMessage.copy(timestamp = time)
                        }
                    }.addOnFailureListener {
                        // Handle failure
                    }
                } else {
                    // El usuario receptor ya tiene una ubicación en este chat, no realizar ninguna acción
                    result.value = null
                }
            }

            override fun onCancelled(error: DatabaseError) {
                // Handle cancellation
            }
        })

        return result
    }

    override fun addLocation(
        addMessage: LocationModel,
        id: String,
        messageID: String
    ): LiveData<LocationModel> {
        val result = MutableLiveData<LocationModel>()
        val currentUser = FirebaseAuth.getInstance().currentUser ?: return result
        addMessage.IDMessage = messageID
        addMessage.UUIDSender = currentUser.uid
        addMessage.timestamp = -1L // Valor temporal para el timestamp

        // Ruta centralizada para la ubicación
        val centralLocationRef = FirebaseDatabase.getInstance().getReference("locations").child(messageID)

        // Rutas para referenciar la ubicación en los perfiles de los usuarios
        val currentUserLocationRef = FirebaseDatabase.getInstance().getReference("users").child(currentUser.uid).child("locations").child(id).child(messageID)
        val userLocationRef = FirebaseDatabase.getInstance().getReference("users").child(id).child("locations").child(currentUser.uid).child(messageID)

        // Usar ServerValue.TIMESTAMP para obtener el timestamp del servidor
        val locationData = addMessage.toMap().plus("timestamp" to ServerValue.TIMESTAMP)

        centralLocationRef.setValue(locationData).addOnSuccessListener {
            val updates = hashMapOf<String, Any>(
                "/users/${currentUser.uid}/locations/$id/$messageID" to true,
                "/users/$id/locations/${currentUser.uid}/$messageID" to true
            )

            FirebaseDatabase.getInstance().reference.updateChildren(updates).addOnSuccessListener {
                // Obtener el timestamp del servidor
                centralLocationRef.child("timestamp").addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        val serverTimestamp = snapshot.value as? Long
                        serverTimestamp?.let {
                            addMessage.timestamp = it
                            result.value = addMessage
                        }
                    }

                    override fun onCancelled(error: DatabaseError) {
                        Log.e("addLocation", "Failed to get server timestamp: ${error.message}")
                    }
                })
            }.addOnFailureListener {
                Log.e("addLocation", "Failed to update location references: ${it.message}")
            }
        }.addOnFailureListener {
            Log.e("addLocation", "Failed to send location: ${it.message}")
        }

        return result
    }

    override suspend fun getLocationById(
        idUser: String,
        id: String,
        latitude: Double,
        longitude: Double,
        context: Context,
        token: String
    ): Flow<List<LocationModel>> = callbackFlow {
        val currentUser = FirebaseAuth.getInstance().currentUser ?: run {
            close(IllegalStateException("User not authenticated"))
            return@callbackFlow
        }

        val locationsRef = FirebaseDatabase.getInstance().getReference("users/${currentUser.uid}/locations/$id").orderByKey()

        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val locationIds = snapshot.children.mapNotNull { it.key }
                val locations = mutableListOf<LocationModel>()

                // Crear una lista de deferreds para las solicitudes de ubicaciones
                val deferreds = locationIds.map { locationId ->
                    async(Dispatchers.IO) {
                        val locationSnapshot = FirebaseDatabase.getInstance().getReference("locations/$locationId").get().await()
                        locationSnapshot.getValue(LocationModel::class.java)
                    }
                }

                // Esperar a que todas las solicitudes de ubicaciones se completen
                launch(Dispatchers.IO) {
                    val results = deferreds.awaitAll()
                    locations.addAll(results.filterNotNull())

                    // Enviar las ubicaciones al callbackFlow
                    trySend(locations.sortedBy { it.timestamp }).isSuccess
                }
            }

            override fun onCancelled(error: DatabaseError) {
                close(error.toException())
            }
        }

        locationsRef.addValueEventListener(listener)

        // Cancelar el listener cuando el flow es cancelado
        awaitClose { locationsRef.removeEventListener(listener) }
    }







    override fun updateLocationById(location: LocationModel, id: String, idUser: String, messageId: String): LiveData<LocationModel> {
        val result = MutableLiveData<LocationModel>()
        val currentUser = auth.currentUser
        location.IDMessage = messageId
        location.UUIDSender = currentUser?.uid
        val userLocationRef = FirebaseDatabase.getInstance().getReference("users/${idUser}/friends/$id/locationFriend/${messageId}")

        // Obtener la referencia al registro de ubicación específico por su ID
//        val locationRef = userLocationRef.child(messageId)
        userLocationRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    // Actualizar el registro de ubicación existente
                    val timestamp = location.copy(timestamp = time)
                    userLocationRef.setValue(timestamp)
                        .addOnSuccessListener {
                            result.value = location.copy(timestamp = time)
                        }
                        .addOnFailureListener {
                            // Manejar el error al actualizar el registro
                        }
                } else {
                    // El registro de ubicación no existe, manejar el caso según tus necesidades
                    result.value = null
                }
            }

            override fun onCancelled(error: DatabaseError) {
                // Manejar la cancelación
            }
        })

        return result
    }


    override fun addMessage2(addMessage: ChatModel, id: String, idUser: String, messageID: String): LiveData<ChatModel> {
        val result = MutableLiveData<ChatModel>()
        val currentUser = FirebaseAuth.getInstance().currentUser ?: return result
        addMessage.IDMessage = messageID
        addMessage.UUIDSender = currentUser.uid
        addMessage.timestamp = -1L // Valor temporal para el timestamp

        // Ruta centralizada para el mensaje
        val centralMessageRef = FirebaseDatabase.getInstance().getReference("messages").child(messageID)

        // Rutas para referenciar el mensaje en los perfiles de los usuarios
        val senderChatRef = FirebaseDatabase.getInstance().getReference("users").child(currentUser.uid).child("chats").child(idUser).child(messageID)
        val receiverChatRef = FirebaseDatabase.getInstance().getReference("users").child(idUser).child("chats").child(currentUser.uid).child(messageID)

        // Usar ServerValue.TIMESTAMP para obtener el timestamp del servidor
        val messageData = addMessage.toMap().plus("timestamp" to ServerValue.TIMESTAMP)

        centralMessageRef.setValue(messageData).addOnSuccessListener {
            val updates = hashMapOf<String, Any>(
                "/users/${currentUser.uid}/chats/$idUser/$messageID" to true,
                "/users/$idUser/chats/${currentUser.uid}/$messageID" to true
            )

            FirebaseDatabase.getInstance().reference.updateChildren(updates).addOnSuccessListener {
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
                        Log.e("addMessage2", "Failed to get server timestamp: ${error.message}")
                    }
                })
            }.addOnFailureListener {
                Log.e("addMessage2", "Failed to update chat references: ${it.message}")
            }
        }.addOnFailureListener {
            Log.e("addMessage2", "Failed to send message: ${it.message}")
        }

        return result
    }

    override suspend fun getAllMessage(idUser: String, id: String): Flow<List<ChatModel>> = callbackFlow {
        val currentUser = FirebaseAuth.getInstance().currentUser ?: run {
            close(IllegalStateException("User not authenticated"))
            return@callbackFlow
        }

        val messagesRef = FirebaseDatabase.getInstance().getReference("users/${currentUser.uid}/chats/$id").orderByKey()

        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val messageIds = snapshot.children.mapNotNull { it.key }
                val messages = mutableListOf<ChatModel>()

                // Crear una lista de deferreds para las solicitudes de mensajes
                val deferreds = messageIds.map { messageId ->
                    async(Dispatchers.IO) {
                        val messageSnapshot = FirebaseDatabase.getInstance().getReference("messages/$messageId").get().await()
                        messageSnapshot.getValue(ChatModel::class.java)
                    }
                }

                // Esperar a que todas las solicitudes de mensajes se completen
                launch(Dispatchers.IO) {
                    val results = deferreds.awaitAll()
                    messages.addAll(results.filterNotNull())

                    // Enviar los mensajes al callbackFlow
                    trySend(messages.sortedBy { it.timestamp }).isSuccess
                }
            }

            override fun onCancelled(error: DatabaseError) {
                close(error.toException())
            }
        }

        messagesRef.addValueEventListener(listener)

        // Cancelar el listener cuando el flow es cancelado
        awaitClose { messagesRef.removeEventListener(listener) }
    }

    override suspend fun getAllMessage2(id: String, idUser: String): List<ChatModel> {
        val messages = mutableListOf<ChatModel>()
        val cu = auth.currentUser
        val messageRef = FirebaseDatabase.getInstance().getReference("users/${idUser}/friends/$id/chat")
        val dataSnapshot = messageRef.get().await()
        for (messageSnapshot in dataSnapshot.children) {
            val message = messageSnapshot.getValue(ChatModel::class.java)
            messages.add(message!!)
        }
        return messages
    }

}

//fun generateAESKey(): SecretKey{
//    val keyGenerator = KeyGenerator.getInstance("AES")
//    keyGenerator.init(256)
//    return keyGenerator.generateKey()
//}
//fun encryptedMessageWithAES(message: String, secretKey: SecretKey): String{
//    val cipher = Cipher.getInstance("AES")
//    cipher.init(Cipher.ENCRYPT_MODE, secretKey)
//    val encryptedBytes = cipher.doFinal()
//}

//override fun addLocation(
//    addMessage: LocationModel,
//    id: String,
//    messageID: String
//): LiveData<LocationModel> {
//    val result = MutableLiveData<LocationModel>()
//    val currentUser = auth.currentUser
//    val locationID = UUID.randomUUID()
//    addMessage.IDMessage = messageID
//    addMessage.UUIDSender = currentUser?.uid
//    val currentUserLocationRef = FirebaseDatabase.getInstance().getReference("users/${currentUser?.uid}/friends/$id/location")
//    currentUserLocationRef.addListenerForSingleValueEvent(object : ValueEventListener {
//        override fun onDataChange(snapshot: DataSnapshot) {
//            val currentUserHasLocation = snapshot.exists()
//
//            // Verificar si el currentUser ya tiene una ubicación en este chat
//            if (!currentUserHasLocation) {
//                val messageRef = currentUserLocationRef.child(messageID)
//                val timestamp = addMessage.copy(timestamp = time)
//                messageRef.setValue(addMessage).addOnSuccessListener {
//                    messageRef.setValue(timestamp).addOnSuccessListener {
//                        result.value = addMessage.copy(timestamp = time)
//                    }
//                }.addOnFailureListener {
//                    // Handle failure
//                }
//            } else {
//                // El currentUser ya tiene una ubicación en este chat, no realizar ninguna acción
//                result.value = null
//            }
//        }
//
//        override fun onCancelled(error: DatabaseError) {
//            // Handle cancellation
//        }
//    })
//
//    return result
//}