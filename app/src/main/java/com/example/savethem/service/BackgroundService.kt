package com.example.savethem.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.navigation.NavController
import com.example.savethem.DAO.DAO
import com.example.savethem.R
import com.example.savethem.Repository.Repository
import com.example.savethem.Repository.UserRepository
import com.example.savethem.ViewModel.ChatViewModel
import com.example.savethem.ViewModel.FriendsViewModel
import com.example.savethem.call.enviar
import kotlin.random.Random

class MyBackgroundService() : Service() {
	private val CHANNEL_ID = "ForegroundServiceChannel"
	private var counter = 0
	private lateinit var friendsViewModel: FriendsViewModel

	override fun onCreate() {
		super.onCreate()
		createNotificationChannel()
		friendsViewModel = FriendsViewModel(Repository(DAO()), UserRepository(this))
		friendsViewModel.getAllFriends()
	}

	override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
		val notification = NotificationCompat.Builder(this, CHANNEL_ID)
			.setContentTitle("Background Service")
			.setContentText("Service is running")
			.setSmallIcon(R.drawable.norma)
			.build()

		startForeground(1, notification)

		Thread {
			while (true) {
				Thread.sleep(5000) // Sleep for 5 seconds
				val randomNumber = Random.nextInt(0, 200)
				if (randomNumber in 100..150) {
					friendsViewModel.friends.value.forEach { friend ->
						friend.token?.let { token ->
							if (token.isNotEmpty()){
								enviar(this, token)
							}else{
								Log.d("TOKEN", "Token is empty for friend: ${friend.name}")
							}
						} ?: Log.e("FCM ERROR", "token is null for friend ${friend.name}")
					}

				}
			}
		}.start()

		return START_STICKY
	}


	private fun sendNotification(counter: Int) {
		val notification = NotificationCompat.Builder(this, CHANNEL_ID)
			.setContentTitle("Background Service")
			.setContentText("Notification $counter")
			.setSmallIcon(R.drawable.norma)
			.setPriority(NotificationCompat.PRIORITY_HIGH)
			.build()

		with(NotificationManagerCompat.from(this)) {
			notify(counter, notification)
		}
	}

	override fun onBind(intent: Intent?): IBinder? {
		return null
	}

	private fun createNotificationChannel() {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
			val serviceChannel = NotificationChannel(
				CHANNEL_ID,
				"Foreground Service Channel",
				NotificationManager.IMPORTANCE_DEFAULT
			)
			val manager = getSystemService(NotificationManager::class.java)
			manager.createNotificationChannel(serviceChannel)
		}
	}
}



//private fun tes() {
//	val friendsViewModel: FriendsViewModel
//	val context = LocalContext
//
//	val friends by friendsViewModel.friends.collectAsState()
//	friendsViewModel.getAllFriends()
//			Log.d("TOKENS", friends.map { it.token }.toString())
//			// Aquí iteramos sobre todos los amigos en la lista y enviamos notificación a cada uno
//			friends.forEach { friend ->
//				friend.token?.let { token ->
//					if (token.isNotEmpty()) {
//						enviar(context = context, token = token)
//					} else {
////						Log.e("FCM_ERROR", "Token is empty for friend: ${friend.name}")
//					}
//				}
//			}
//}
