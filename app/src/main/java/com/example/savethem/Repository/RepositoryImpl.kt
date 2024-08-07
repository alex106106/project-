package com.example.savethem.Repository

import androidx.lifecycle.map
import com.example.savethem.DAO.UserDao
import com.example.savethem.Model.FriendEntity
import com.example.savethem.Model.registerModel

class RepositoryImpl(private val userDao: UserDao) : RepositoryFriends {
	override suspend fun addFriend(friend: registerModel, idFriend: String) {
		val friendEntity = FriendEntity(
			UUID = friend.UUID ?: "",
			name = friend.name,
			email = friend.email,
			token = friend.token
		)
		userDao.insertFriend(friendEntity)
	}

	override suspend fun getFriends(): List<registerModel> {
		return userDao.getAllFriends().map { friendEntity ->
			registerModel(
				UUID = friendEntity.UUID,
				name = friendEntity.name,
				email = friendEntity.email,
				token = friendEntity.token
			)
		}
	}
}
