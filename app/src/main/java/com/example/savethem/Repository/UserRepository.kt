package com.example.savethem.Repository

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.example.savethem.Interface.UserDAO
import com.example.savethem.Model.registerModel
import com.example.savethem.util.DatabaseHelper
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

class UserRepository @Inject constructor(
	@ApplicationContext private val context: Context,
//	private val userDAO: UserDAO

) {

//	private val dbHelper = DatabaseHelper(context)
//	fun getUserById(userId: Int): LiveData<registerModel?> {
//		val result = MutableLiveData<registerModel?>()
//		viewModelScope.launch {
//			result.postValue(userRepository.getUserById(userId))
//		}
//		return result
//	}
//	fun addFriend(currentUserId: Long, friend: registerModel): Long {
//		val db = dbHelper.writableDatabase
//		val values = ContentValues().apply {
//			put("user_id", currentUserId)
//			put("friend_id", friend.UUID)
//			put("friend_name", friend.name)
//			put("friend_email", friend.email)
//			put("friend_UUID", friend.UUID)
//			put("friend_token", friend.token)
//		}
//		return db.insert("friends", null, values)
//	}

//	fun getFriends(currentUserId: Long): List<registerModel> {
//		val db = dbHelper.readableDatabase
//		val cursor: Cursor = db.query(
//			"friends",
//			arrayOf("friend_id", "friend_name", "friend_email", "friend_UUID", "friend_token"),
//			"user_id = ?",
//			arrayOf(currentUserId.toString()),
//			null, null, null
//		)
//		val friends = mutableListOf<registerModel>()
//		with(cursor) {
//			while (moveToNext()) {
//				val friend = registerModel(
//					getLong(getColumnIndexOrThrow("friend_id")).toString(),
//					getString(getColumnIndexOrThrow("friend_name")),
//					getString(getColumnIndexOrThrow("friend_email")),
//					getString(getColumnIndexOrThrow("friend_UUID")),
//					getString(getColumnIndexOrThrow("friend_token"))
//				)
//				friends.add(friend)
//			}
//		}
//		cursor.close()
//		return friends
//	}
	fun addFriend(currentUserId: Long, friend: registerModel) {
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
			.equalTo(friend.email)

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
								friend.email,
								friendToken
							)
						}
					}
				} else {
					Log.d("AddFriend", "No user found with email: ${friend.email}")
				}
			}

			override fun onCancelled(error: DatabaseError) {
				Log.e("AddFriend", "DatabaseError: ${error.message}")
			}
		})
	}

}
