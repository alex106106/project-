package com.example.savethem.App

import android.app.Activity
import android.app.Application
import android.content.Intent
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.common.api.ApiException
import com.google.firebase.FirebaseApp
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class App : Application() {
	override fun onCreate() {
		super.onCreate()
		FirebaseApp.initializeApp(this)
		FirebaseDatabase.getInstance().setPersistenceEnabled(true)	}
}
