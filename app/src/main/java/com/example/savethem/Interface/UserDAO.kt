package com.example.savethem.Interface

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.savethem.Model.registerModel

@Dao
interface UserDAO {
	@Insert(onConflict = OnConflictStrategy.REPLACE)
	suspend fun insertUser(user: registerModel)

	@Query("SELECT * FROM users WHERE id = :userId")
	suspend fun getUserById(userId: Int): registerModel?
}