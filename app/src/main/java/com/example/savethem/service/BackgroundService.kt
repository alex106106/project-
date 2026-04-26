package com.example.savethem.service

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.location.Location
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.savethem.DAO.DAO
import com.example.savethem.Model.LocationModel
import com.example.savethem.R
import com.google.android.gms.location.*
import com.google.firebase.auth.FirebaseAuth
import java.util.*

class MyBackgroundService : Service() {
    private val CHANNEL_ID = "ForegroundServiceChannel"
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var dao: DAO
    private var friendId: String? = null
    
    // Referencia al callback para poder detenerlo
    private var locationCallback: LocationCallback? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        dao = DAO()
    }

    @SuppressLint("ForegroundServiceType")
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        friendId = intent?.getStringExtra("FRIEND_ID")

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("SaveThem Tracking")
            .setContentText("Compartiendo tu ubicación en tiempo real...")
            .setSmallIcon(R.drawable.norma)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(1, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION)
        } else {
            startForeground(1, notification)
        }

        startLocationUpdates()

        return START_STICKY
    }

    @SuppressLint("MissingPermission")
    private fun startLocationUpdates() {
        // Evitar múltiples suscripciones
        if (locationCallback != null) return

        val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 5000)
            .setWaitForAccurateLocation(false)
            .setMinUpdateIntervalMillis(3000)
            .setMaxUpdateDelayMillis(10000)
            .build()

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                for (location in locationResult.locations) {
                    saveLocationToFirebase(location)
                }
            }
        }

        locationCallback?.let {
            fusedLocationClient.requestLocationUpdates(locationRequest, it, null)
        }
    }

    private fun saveLocationToFirebase(location: Location) {
        val currentUser = FirebaseAuth.getInstance().currentUser ?: return
        val currentFriendId = friendId ?: return

        val messageID = UUID.randomUUID().toString()
        val locationModel = LocationModel(
            IDMessage = messageID,
            UUIDSender = currentUser.uid,
            location = com.example.savethem.Model.LatLngWrapper(location.latitude, location.longitude),
            timestamp = -1L
        )

        dao.addLocation(locationModel, currentFriendId, messageID)
        Log.d("SERVICE", "Ubicación guardada: ${location.latitude}, ${location.longitude}")
    }

    override fun onDestroy() {
        super.onDestroy()
        // DETENER ACTUALIZACIONES DE UBICACIÓN
        locationCallback?.let {
            fusedLocationClient.removeLocationUpdates(it)
            Log.d("SERVICE", "Actualizaciones de ubicación detenidas.")
        }
        locationCallback = null
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val serviceChannel = NotificationChannel(
                CHANNEL_ID,
                "Foreground Service Channel",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(serviceChannel)
        }
    }
}
