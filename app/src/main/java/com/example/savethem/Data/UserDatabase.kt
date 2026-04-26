package com.example.savethem.Data

import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import android.content.Context
import com.example.savethem.DAO.UserDao
import com.example.savethem.DAO.ChatDao
import com.example.savethem.DAO.NoteDao
import com.example.savethem.Model.*

@Database(
    entities = [
        UserEntity::class, 
        FriendEntity::class, 
        ChatEntity::class, 
        LocationEntity::class, 
        NoteEntity::class,
        MapStateEntity::class // Añadida nueva entidad
    ], 
    version = 4, // Subimos versión para el estado del mapa
    exportSchema = false
)
abstract class UserDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun chatDao(): ChatDao
    abstract fun noteDao(): NoteDao

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
