package com.example.savethem.DAO

import androidx.lifecycle.LiveData
import androidx.room.*
import com.example.savethem.Model.FriendEntity
import com.example.savethem.Model.UserEntity

@Dao
interface UserDao {
	@Insert(onConflict = OnConflictStrategy.REPLACE)
	suspend fun insert(user: UserEntity)

	@Update
	suspend fun update(user: UserEntity)

	@Delete
	suspend fun delete(user: UserEntity)

	@Query("SELECT * FROM user_table WHERE id = :id")
	fun getUserById(id: Int): LiveData<UserEntity>

	@Insert(onConflict = OnConflictStrategy.REPLACE)
	suspend fun insertFriend(friend: FriendEntity)

	@Query("SELECT * FROM friends")
	fun getAllFriends(): List<FriendEntity>

	@Query("DELETE FROM friends WHERE UUID = :uuid")
	suspend fun deleteFriend(uuid: String)
}
