package com.example.savethem.Model

import android.content.Context
import androidx.lifecycle.LiveData
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

interface AccessDAO {
//    Maps
    suspend fun getAllCountries(): List<MarkerModel>
    fun addPlace(merkerModel: MarkerModel): LiveData<MarkerModel>
    suspend fun getCountryById(id: String): MarkerModel?
    fun addComment(commentsModel: CommentsModel, id: String): LiveData<CommentsModel>
    suspend fun getAllComments(id: String): List<CommentsModel>
    suspend fun getUserData(): registerModel?
    suspend fun getFriendData(idFriend: String): registerModel?
    fun getLikes(commentId: String): LiveData<Pair<List<String>, Boolean>>
//    Friends
    fun addFriend(addFriend: registerModel, idFriend: String)
//    ADDFRIEND SQLITE
    fun addFriendSQL(currentUser: Long, friend: registerModel): Long
    fun addFriendChat(addFriend: registerModel, uid: String)
    suspend fun getFriends(): List<registerModel>
    suspend fun getFriendById(id: String): registerModel?
    suspend fun getUserDataUpdate(idUser: String, id: String, token: String): registerModel?
//    Chats
    fun addMessage(addMessage: ChatModel, id: String, messageID: String): LiveData<ChatModel>
    fun addLocation(addMessage: LocationModel, id: String, messageID: String): LiveData<LocationModel>
    suspend fun getLocation(idUser: String, id: String): Flow<List<LocationModel>>
    fun addLocation2(addMessage: LocationModel, id: String, idUser: String, messageID: String): LiveData<LocationModel>
    suspend fun getLocationById(idUser: String, id: String, latitude: Double, longitude: Double, context: Context,
                                token: String): Flow<List<LocationModel>>
    fun updateLocationById(location: LocationModel, id: String, idUser: String, messageID: String): LiveData<LocationModel>
    fun addMessage2(addMessage: ChatModel, id: String, idUser: String, messageID: String): LiveData<ChatModel>
    suspend fun getAllMessage(idUser: String, id:String): Flow<List<ChatModel>>
    suspend fun getAllMessage2(id:String,  idUser: String):List<ChatModel>

}