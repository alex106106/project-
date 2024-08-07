package com.example.savethem.Model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "friends")
data class FriendEntity(
	@PrimaryKey val UUID: String,
	val name: String?,
	val email: String?,
	val token: String?
)
