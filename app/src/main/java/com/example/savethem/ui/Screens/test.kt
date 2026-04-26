package com.example.savethem.ui.Screens

import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.savethem.ViewModel.FriendsViewModel
import com.example.savethem.call.enviarNotificacionV1
import com.example.savethem.service.MyBackgroundService

@Composable
fun test() {
	val context = LocalContext.current

	Column(
		modifier = Modifier
			.fillMaxSize()
			.padding(16.dp),
		verticalArrangement = Arrangement.Center,
		horizontalAlignment = Alignment.CenterHorizontally
	) {
		Button(onClick = {
			val intent = Intent(context, MyBackgroundService::class.java)
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
				context.startForegroundService(intent)
			} else {
				context.startService(intent)
			}
		}) {
			Text(text = "Start Background Service")
		}
	}
}
@Composable
fun tests(friendsViewModel: FriendsViewModel, navController: NavController) {
	val context = LocalContext.current

	val friends by friendsViewModel.friends.collectAsState()
	friendsViewModel.getAllFriends()
	Column(modifier = Modifier.fillMaxSize()) {
		Button(onClick = {
			Log.d("TOKENS", friends.map { it.token }.toString())
			// Aquí iteramos sobre todos los amigos en la lista y enviamos notificación a cada uno
			friends.forEach { friend ->
				friend.token?.let { token ->
					if (token.isNotEmpty()) {
						enviarNotificacionV1(context = context, token = token, titulo = "Test V1", mensaje = "Probando FCM V1")
					} else {
						Log.e("FCM_ERROR", "Token is empty for friend: ${friend.name}")
					}
				} ?: Log.e("FCM_ERROR", "Token is null for friend: ${friend.name}")
			}
		}) {
			Text(text = "Send Notifications")
		}

		friends.forEach { friend ->
			Text(text = "Friend: ${friend.name}, Token: ${friend.token}")
		}
	}
}
