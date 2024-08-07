package com.example.savethem.Repository

import com.example.savethem.Model.registerModel

interface RepositoryFriends {
	suspend fun addFriend(friend: registerModel, idFriend: String)
	suspend fun getFriends(): List<registerModel>
}
