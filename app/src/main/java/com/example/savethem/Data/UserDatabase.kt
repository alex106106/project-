package com.example.savethem.Data

import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import android.content.Context
import com.example.savethem.DAO.UserDao
import com.example.savethem.Model.FriendEntity
import com.example.savethem.Model.UserEntity

@Database(entities = [UserEntity::class, FriendEntity::class], version = 2)
abstract class UserDatabase : RoomDatabase() {
	abstract fun userDao(): UserDao

	companion object {
		@Volatile
		private var INSTANCE: UserDatabase? = null

		fun getDatabase(context: Context): UserDatabase {
			return INSTANCE ?: synchronized(this) {
				val instance = Room.databaseBuilder(
					context.applicationContext,
					UserDatabase::class.java,
					"user_database"
				).fallbackToDestructiveMigration().build()
				INSTANCE = instance
				instance
			}
		}
	}
}

