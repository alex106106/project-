package com.example.savethem.ViewModel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.savethem.Model.MarkerModel
import com.example.savethem.Model.registerModel

import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID
import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.savethem.DAO.UserDao
import com.example.savethem.Data.UserDatabase
import com.example.savethem.Model.UserEntity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class RegisterViewModel(application: Application) : AndroidViewModel(application) {

    private val userDao: UserDao = UserDatabase.getDatabase(application).userDao()

    val registeredUser: LiveData<UserEntity> = userDao.getUserById(1) // Suponiendo que el ID del usuario sea 1

    fun registerUser(registerModel: registerModel): LiveData<String> {
        val auth = FirebaseAuth.getInstance()
        val result = MutableLiveData<String>()
        val database = FirebaseDatabase.getInstance()

        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                auth.createUserWithEmailAndPassword(
                    registerModel.email.toString(),
                    registerModel.pass.toString()
                )
                    .addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            val current = FirebaseAuth.getInstance().currentUser
                            registerModel.UUID = current?.uid

                            // Guardar los datos del usuario en Realtime Database
                            val userDatabaseRef = database.getReference("users/${current?.uid!!}/userData")
                            userDatabaseRef.setValue(registerModel)
                                .addOnSuccessListener {
                                    // Obtener el token de registro de Firebase Cloud Messaging
                                    FirebaseMessaging.getInstance().token.addOnSuccessListener { token ->
                                        // Asignar el token a la propiedad "token" del modelo
                                        registerModel.token = token

                                        // Actualizar los datos del usuario en Realtime Database con el token
                                        userDatabaseRef.setValue(registerModel)
                                            .addOnSuccessListener {

                                                result.postValue("success")
                                                // Guardar en SQLite
                                                viewModelScope.launch {
                                                    val userEntity = UserEntity(
                                                        email = registerModel.email!!,
                                                        pass = registerModel.pass!!,
                                                        name = registerModel.name!!,
                                                        UUID = registerModel.UUID!!,
                                                        token = registerModel.token!!
                                                    )
                                                    userDao.insert(userEntity)
                                                }
                                            }
                                            .addOnFailureListener {
                                                result.postValue("error")
                                            }
                                    }
                                }
                                .addOnFailureListener {
                                    result.postValue("error")
                                }
                        } else {
                            result.postValue("error")
                        }
                    }
            }
        }

        return result
    }
}



