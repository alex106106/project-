package com.example.savethem.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.example.savethem.R
import com.example.savethem.service.EmergencyService
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import java.util.*

class Notification : FirebaseMessagingService() {

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d("FCM", "Nuevo token generado: $token")
        updateTokenInFirebase(token)
    }

    private fun updateTokenInFirebase(token: String) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid
        if (uid != null) {
            val ref = FirebaseDatabase.getInstance().getReference("users/$uid/userData/token")
            ref.setValue(token).addOnSuccessListener {
                Log.d("FCM", "Token actualizado en la base de datos")
            }
        }
    }

    override fun onMessageReceived(message: RemoteMessage) {
        Log.d("FCM", "Mensaje recibido de: ${message.from}")

        val prefs = getSharedPreferences("settings", Context.MODE_PRIVATE)
        val isAntiTheftEnabled = prefs.getBoolean("anti_theft_remote_enabled", false)
        val secretKeyword = prefs.getString("anti_theft_remote_keyword", "#ACTIVAR_RASTREO") ?: "#ACTIVAR_RASTREO"

        // Extraer datos tanto de 'notification' como de 'data'
        val title = message.notification?.title ?: message.data["titulo"] ?: "Nuevo Mensaje"
        val body = message.notification?.body ?: message.data["detalle"] ?: "Has recibido un mensaje"
        val iconName = message.data["icon"] ?: "norma"
        val action = message.data["action"] ?: ""

        // Lógica de activación remota por palabra clave
        if (isAntiTheftEnabled && (action == "TRIGGER_REMOTE_EMERGENCY" || body.contains(secretKeyword, ignoreCase = true))) {
            Log.d("FCM_AntiTheft", "Comando secreto anti-robo detectado: $secretKeyword. Activando localización remota...")
            
            val serviceIntent = Intent(this, EmergencyService::class.java).apply {
                this.action = "START_EMERGENCY"
            }
            
            try {
                ContextCompat.startForegroundService(this, serviceIntent)
            } catch (e: Exception) {
                startService(serviceIntent)
            }
            
            // Retornamos sin mostrar notificación push visible para que el ladrón no sospeche
            return
        }

        showNotification(title, body, iconName)
    }

    private fun showNotification(titulo: String?, detalle: String?, iconName: String?) {
        val channelId = "Mensaje" 
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channelName = "Notificaciones de Chat"
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(channelId, channelName, importance).apply {
                description = "Canal para alertas y mensajes de SaveThem"
                enableLights(true)
                enableVibration(true)
            }
            nm.createNotificationChannel(channel)
        }

        val resourceId = if (!iconName.isNullOrEmpty()) {
            val id = resources.getIdentifier(iconName, "drawable", packageName)
            if (id != 0) id else R.drawable.otro
        } else {
            R.drawable.otro
        }

        val builder = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(resourceId)
            .setContentTitle(titulo)
            .setContentText(detalle)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setWhen(System.currentTimeMillis())

        val idNotify = Random().nextInt(8000)
        nm.notify(idNotify, builder.build())
    }
}
