package com.example.savethem.util

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import androidx.compose.material.darkColors
import androidx.compose.material.lightColors
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import com.google.firebase.auth.FirebaseAuth
import java.util.*


class constants {
    object Screens{
        const val LOGIN_SCREEN = "login_screen"
        const val MAIN_SCREEN = "main_screen"
        const val MAP_SCREEN = "map_screen"
        const val DETAILS_SCREEN = "details_screen"
        const val REGISTER_SCREEN = "register_screen"
        const val KEY_GAME_ID = "com.example.savethem"
        const val CALL_SCREEN = "call_screen"
        const val CHAT_SCREEN = "chat_screen"
        const val TEST_SCREEN = "test_screen"
        const val GLOBAL_CHAT_SCREEN = "global_chat_Screen"
//        val messageID = UUID.randomUUID().toString()
        val time = System.currentTimeMillis()
    }
    object Notification{
        const val CHANNEL = "channel"
        const val ID = 1
        const val EXTRA_MESSAGE = "extra_message"

        const val INTENT_ACTION_DETAILS = "OPEN_DETAILS"
    }
    object const{
        val AUTH = FirebaseAuth.getInstance()
    }
    object AppTheme {
        val Dark = darkColors(
            primary = Color.White,
            onPrimary = Color.Black,
            secondary = Color.Yellow,
            onSecondary = Color.Black,
            background = Color.Black,
            onBackground = Color.White
        )

        val Light = lightColors(
            primary = Color.Black,
            onPrimary = Color.White,
            secondary = Color.Yellow,
            onSecondary = Color.Black,
            background = Color.White,
            onBackground = Color.Black
        )
    }

}
class DatabaseHelper(context: Context): SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION){
    override fun onCreate(db: SQLiteDatabase?) {
        db?.execSQL(CREATE_TABLE_USER)
        db?.execSQL(CREATE_TABLE_FRIENDS)
    }

    override fun onUpgrade(db: SQLiteDatabase?, oldVersion: Int, newVersion: Int) {
        db?.execSQL("DROP TABLE IF EXISTS users")
        db?.execSQL("DROP TABLE IF EXISTS friends")
        onCreate(db)
    }
    companion object tables{
        private const val DATABASE_VERSION = 1
        private const val DATABASE_NAME = "app_database.db"
        private const val CREATE_TABLE_USER = """ 
            CREATE TABLE users (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
        name TEXT,
        email TEXT,
        UUID TEXT,
        token TEXT
            )
        """

        private const val CREATE_TABLE_FRIENDS = """
            CREATE TABLE friends (
        id INTEGER PRIMARY KEY AUTOINCREMENT,
        user_id INTEGER,
        friend_id INTEGER,
        friend_name TEXT,
        friend_email TEXT,
        friend_UUID TEXT,
        friend_token TEXT,
        FOREIGN KEY(user_id) REFERENCES users(id)
    )
        """
    }


}