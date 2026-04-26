package com.example.savethem.Repository

import android.content.Context
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.example.savethem.DAO.DAO
import com.example.savethem.Model.*
import com.google.firebase.database.*
import kotlinx.coroutines.flow.Flow
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

        val updates = hashMapOf(
            "seen" to true,
            "seenTimestamp" to ServerValue.TIMESTAMP
        )

        centralMessageRef.updateChildren(updates).addOnSuccessListener {
            senderChatRef.updateChildren(updates).addOnSuccessListener {
                receiverChatRef.updateChildren(updates).addOnSuccessListener {
                    Log.d("DAO", "Message marked as seen")
                }
            }
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

    suspend fun addFriendWithValidation(friendEmail: String): Boolean {
        return DAO.addFriendWithValidation(friendEmail)
    }

    fun addFriendChat(addFriend: registerModel, uid: String){
        return DAO.addFriendChat(addFriend, uid)
    }
    
    fun getFriends(): Flow<List<registerModel>> {
        return DAO.getFriends()
    }

    suspend fun getFriendsListOnce(): List<registerModel> {
        return DAO.getFriendsListOnce()
    }
    
    suspend fun getFriendById(id: String): registerModel? {
        return DAO.getFriendById(id)
    }
    suspend fun getFriendData(idFriend: String): registerModel? {
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
    fun updateLocation(addMessage: LocationModel, id: String, idUser: String, messageId: String): LiveData<LocationModel>{
        return DAO.updateLocationById(addMessage, id, idUser, messageId)
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

    suspend fun sendMessage(
        message: ChatModel,
        receiverId: String
    ) {
        DAO.sendMessage(message, receiverId)
    }

    suspend fun getAllMessage(idUser: String,id: String): Flow<List<ChatModel>> {
        return DAO.getAllMessage(idUser,id)
    }
    suspend fun listenMessages(
        idUser: String,
        chatId: String
    ): Flow<List<ChatModel>> {
        return DAO.listenMessages(idUser, chatId)
    }

    suspend fun getAllMessage2(id: String, idUser: String): List<ChatModel>{
        return DAO.getAllMessage2(id, idUser = idUser)
    }

    // Safety Alerts
    suspend fun reportSafetyAlert(alert: SafetyAlertModel) {
        DAO.reportSafetyAlert(alert)
    }

    fun getSafetyAlerts(): Flow<List<SafetyAlertModel>> {
        return DAO.getSafetyAlerts()
    }

    // Emergency
    suspend fun startEmergency(emergency: EmergencyModel) {
        DAO.startEmergency(emergency)
    }

    suspend fun stopEmergency(userId: String) {
        DAO.stopEmergency(userId)
    }

    fun getActiveEmergencies(): Flow<List<EmergencyModel>> {
        return DAO.getActiveEmergencies()
    }

    // Safe Places
    suspend fun addSafePlace(safePlace: SafePlaceModel) {
        DAO.addSafePlace(safePlace)
    }

    fun getSafePlaces(): Flow<List<SafePlaceModel>> {
        return DAO.getSafePlaces()
    }

    suspend fun getSafePlacesOnce(): List<SafePlaceModel> {
        return DAO.getSafePlacesOnce()
    }
}
