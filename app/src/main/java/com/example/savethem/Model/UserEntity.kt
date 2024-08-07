package com.example.savethem.Model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "user_table")
data class UserEntity(
	@PrimaryKey(autoGenerate = true) val id: Int = 0,
	var email: String,
	var pass: String,
	var name: String,
	var UUID: String,
	var token: String
)

