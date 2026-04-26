package com.example.savethem.ViewModel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.savethem.DAO.UserDao
import com.example.savethem.Data.UserDatabase
import com.example.savethem.Model.UserEntity
import com.example.savethem.Model.registerModel
import com.example.savethem.util.EncryptionUtils
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class RegisterViewModel(application: Application) : AndroidViewModel(application) {

    private val userDao: UserDao = UserDatabase.getDatabase(application).userDao()
    val registeredUser: LiveData<UserEntity> = userDao.getUserById(1)

    private fun generateCustomUid(email: String): String {
        val prefix = email.substringBefore("@")
        val namePart = prefix.filter { it.isLetter() }.take(4).lowercase()
        val digitPart = prefix.filter { it.isDigit() }.takeLast(4)
        return namePart + digitPart
    }

    fun registerWithGoogle(idToken: String, onSuccess: () -> Unit) {
        val auth = FirebaseAuth.getInstance()
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val result = auth.signInWithCredential(credential).await()
                val user = result.user
                if (user != null) {
                    val rawName = user.displayName ?: "User"
                    val rawEmail = user.email ?: ""
                    val customUid = generateCustomUid(rawEmail)

                    val registerModel = registerModel(
                        email = EncryptionUtils.encrypt(rawEmail),
                        name = EncryptionUtils.encrypt(rawName),
                        UUID = customUid,
                        token = ""
                    )
                    
                    val databaseRef = FirebaseDatabase.getInstance().getReference("users/${user.uid}/userData")
                    // Habilitar sincronización offline para este nodo específico
                    databaseRef.keepSynced(true)
                    
                    databaseRef.setValue(registerModel).await()
                    
                    val fcmToken = FirebaseMessaging.getInstance().token.await()
                    databaseRef.child("token").setValue(fcmToken).await()
                    
                    userDao.insert(UserEntity(
                        email = rawEmail,
                        pass = "google_auth",
                        name = rawName,
                        UUID = customUid,
                        token = fcmToken
                    ))
                    
                    withContext(Dispatchers.Main) { onSuccess() }
                }
            } catch (e: Exception) {
                Log.e("RegisterVM", "Google Auth Error", e)
            }
        }
    }

    // El registro manual queda deshabilitado
    fun registerUser(registerModel: registerModel): LiveData<String> = MutableLiveData("disabled")
}
