package com.example.savethem.ui.Main

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.navigation.compose.rememberNavController
import com.example.savethem.ViewModel.*
import com.example.savethem.navigation.SetupNavHost
import com.example.savethem.ui.theme.SaveThemTheme
import com.example.savethem.service.EmergencyService
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.messaging.FirebaseMessaging
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val fineLocationGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] ?: false
        if (fineLocationGranted) {
            // Una vez que tenemos permisos, iniciamos el servicio de monitoreo
            startEmergencyMonitoring()
        }
    }

    @RequiresApi(Build.VERSION_CODES.N)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        checkAndRequestPermissions()

        val viewModel by viewModels<mainViewModel>()
        val RegisterVM by viewModels<RegisterViewModel>()
        val LoginVM by viewModels<LoginViewModel>()
        val DetailsVM by viewModels<detailsViewModel>()
        val FriendsVM by viewModels<FriendsViewModel>()
        val ChatVM  by viewModels<ChatViewModel>()
        
        updateFCMToken()

        setContent {
            val isDarkMode by viewModel.isDarkMode.collectAsState()
            
            SaveThemTheme(darkTheme = isDarkMode) {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colors.background) {
                    val navController = rememberNavController()
                    SetupNavHost(
                        navHostController = navController,
                        viewModel = viewModel,
                        context = this,
                        loginViewModel = LoginVM,
                        registerViewModel = RegisterVM,
                        detailsViewModel = DetailsVM,
                        friendsViewModel = FriendsVM,
                        idToFriend = "",
                        chatViewModel = ChatVM
                    )
                }
            }
        }
    }

    private fun startEmergencyMonitoring() {
        val intent = Intent(this, EmergencyService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent)
        } else {
            startService(intent)
        }
    }

    private fun checkAndRequestPermissions() {
        val permissionsToRequest = mutableListOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.CAMERA
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissionsToRequest.add(Manifest.permission.POST_NOTIFICATIONS)
        }
        requestPermissionLauncher.launch(permissionsToRequest.toTypedArray())
    }

    private fun updateFCMToken() {
        FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
            if (!task.isSuccessful) return@addOnCompleteListener
            val token = task.result
            val uid = FirebaseAuth.getInstance().currentUser?.uid
            if (uid != null && token != null) {
                FirebaseDatabase.getInstance().getReference("users/$uid/userData/token").setValue(token)
            }
        }
    }
}
