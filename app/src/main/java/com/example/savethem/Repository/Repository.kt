package com.example.savethem.Repository

import android.content.Context
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.example.savethem.DAO.DAO
import com.example.savethem.Model.*
import com.google.firebase.database.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import javax.inject.Inject

class Repository @Inject constructor(val DAO: DAO) {
    suspend fun getAllCountries(): List<MarkerModel> {
        return DAO.getAllCountries()
    }
    suspend fun addCountries(markerModel: MarkerModel): LiveData<MarkerModel>{
        return DAO.addPlace(markerModel)
    }
    suspend fun getCountryById(id: String): MarkerModel{
        return DAO.getCountryById(id)!!
    }
    fun addComment(commentsModel: CommentsModel, id: String): LiveData<CommentsModel>{
        return DAO.addComment(id = id, commentsModel = commentsModel)
    }
    suspend fun getAllComments(id: String): List<CommentsModel>{
        return DAO.getAllComments(id)
    }
    suspend fun getUserData(): registerModel?{
        return DAO.getUserData()
    }
    fun getLikes(commentId: String): LiveData<Pair<List<String>, Boolean>>{
        return DAO.getLikes(commentId)
    }

    fun markMessageAsSeen(id: String, idUser: String, messageID: String) {
        val centralMessageRef = FirebaseDatabase.getInstance().getReference("messages").child(messageID)
        val senderChatRef = FirebaseDatabase.getInstance().getReference("users").child(id).child("chats").child(idUser).child(messageID)
        val receiverChatRef = FirebaseDatabase.getInstance().getReference("users").child(idUser).child("chats").child(id).child(messageID)

        // Marcamos el mensaje como visto y actualizamos el timestamp
        val updates = hashMapOf(
            "seen" to true,
            "seenTimestamp" to ServerValue.TIMESTAMP
        )

        // Ejecutamos las actualizaciones atómicamente
        centralMessageRef.updateChildren(updates).addOnSuccessListener {
            senderChatRef.updateChildren(updates).addOnSuccessListener {
                receiverChatRef.updateChildren(updates).addOnSuccessListener {
                    Log.d("DAO", "Message marked as seen")
                }.addOnFailureListener {
                    Log.e("DAO", "Failed to mark message as seen in receiver's chat: ${it.message}")
                }
            }.addOnFailureListener {
                Log.e("DAO", "Failed to mark message as seen in sender's chat: ${it.message}")
            }
        }.addOnFailureListener {
            Log.e("DAO", "Failed to mark message as seen: ${it.message}")
        }
    }



    fun addLike(commentId: String, userId: String, liked: Boolean): LiveData<Boolean> {
        val result = MutableLiveData<Boolean>()
        val commentRef = FirebaseDatabase.getInstance().getReference("place/comments/$commentId/likes/$userId")
        if (liked) {
            commentRef.setValue(true).addOnSuccessListener {
                result.value = true
            }.addOnFailureListener {
                result.value = false
            }
        } else {
            commentRef.removeValue().addOnSuccessListener {
                result.value = true
            }.addOnFailureListener {
                result.value = false
            }
        }
        return result
    }

    fun addFriend(addFriend: registerModel, idFriend: String){
        return DAO.addFriend(addFriend,idFriend)
    }

    fun addFriendChat(addFriend: registerModel, uid: String){
        return DAO.addFriendChat(addFriend, uid)
    }
    suspend fun getFriends(): List<registerModel>{
        return DAO.getFriends()
    }
    suspend fun getFriendById(id: String): registerModel?{
        return DAO.getFriendById(id)
    }
    suspend fun getFriendData(idFriend: String): registerModel?{
        return DAO.getFriendData(idFriend)
    }
    suspend fun getUserDataUpdate(idUser: String, id: String, token: String): registerModel? {
        return DAO.getUserDataUpdate(idUser, id, token)
    }
        fun addMessage(addMessage: ChatModel, id: String, messageID: String): LiveData<ChatModel>{
        return DAO.addMessage(addMessage, id, messageID)
    }
    fun addLocation(addMessage: LocationModel, id: String, messageID: String): LiveData<LocationModel>{
        return DAO.addLocation(addMessage, id, messageID)
    }
    fun addLocation2(addMessage: LocationModel, id: String, idUser: String, messageID: String): LiveData<LocationModel>{
        return DAO.addLocation2(addMessage, id, idUser, messageID)
    }
    fun updateLocation(addMessage: LocationModel, id: String, idUser: String, messageID: String): LiveData<LocationModel>{
        return DAO.updateLocationById(addMessage, id, idUser, messageID)
    }
    suspend fun getLocationById(idUser: String, id: String, latitude: Double, longitude: Double, context: Context,
                                token: String):Flow<List<LocationModel>>{
        return DAO.getLocationById(idUser, id, latitude, longitude, context, token)
    }
    suspend fun getLocation(idUser: String, id: String): Flow<List<LocationModel>>{
        return DAO.getLocation(idUser, id)
    }
    fun addMessage2(addMessage: ChatModel, id: String, idUser: String, messageID: String): LiveData<ChatModel>{
        return DAO.addMessage2(addMessage, id, idUser, messageID)
    }
    suspend fun getAllMessage(idUser: String,id: String): Flow<List<ChatModel>> {
        return DAO.getAllMessage(idUser,id)
    }
    suspend fun getAllMessage2(id: String, idUser: String): List<ChatModel>{
        return DAO.getAllMessage2(id, idUser = idUser)
    }





//    fun addLike(commentId: String, userId: String, liked: Boolean): LiveData<Boolean> {
//        val result = MutableLiveData<Boolean>()
//        val commentRef = FirebaseDatabase.getInstance().getReference("place/comments/$commentId")
//        commentRef.child("likes").child(userId).setValue(liked)
//            .addOnSuccessListener {
//                // obtener el valor actualizado de likes
//                commentRef.child("likes")
//                    .addListenerForSingleValueEvent(object : ValueEventListener {
//                        override fun onDataChange(snapshot: DataSnapshot) {
//                            // verificar si el comentario tiene likes
//                            val likes = snapshot.children.mapNotNull { it.value as? Boolean }
//                            result.value = likes.contains(true)
//                        }
//
//                        override fun onCancelled(error: DatabaseError) {
//                            result.value = false
//                        }
//                    })
//            }.addOnFailureListener {
//                result.value = false
//            }
//        return result
//    }










}