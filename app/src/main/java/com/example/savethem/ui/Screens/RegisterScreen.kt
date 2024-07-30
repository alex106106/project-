package com.example.savethem.ui.Screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.ClickableText
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.savethem.Model.registerModel
import com.example.savethem.ViewModel.RegisterViewModel

@Composable
fun RegisterScreen(registerViewModel: RegisterViewModel = viewModel(), navController: NavController) {
    var name by remember { mutableStateOf("Niko Junior") }
    var emailUser by remember { mutableStateOf("alexisgalindo106@gmail.com") }
    var passUser by remember { mutableStateOf("juniorniko106") }
    var confirmPass by remember { mutableStateOf("juniorniko106") }

    // Observa el usuario registrado
    val registeredUser by registerViewModel.registeredUser.observeAsState()

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier.fillMaxSize()
    ) {
        Image(
            painter = painterResource(id = com.example.savethem.R.drawable.back),
            contentDescription = "Background Image",
            contentScale = ContentScale.FillBounds,
            modifier = Modifier.fillMaxSize()
        )   
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight()
                .padding(20.dp),
            elevation = 8.dp,
            shape = RoundedCornerShape(20.dp),
            backgroundColor = Color.Gray
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                OutlinedTextField(
                    modifier = Modifier
                        .padding(bottom = 5.dp),
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Name") }
                )
                OutlinedTextField(
                    modifier = Modifier
                        .padding(bottom = 5.dp),
                    value = emailUser,
                    onValueChange = { emailUser = it },
                    label = { Text("Email") }
                )
                OutlinedTextField(
                    modifier = Modifier
                        .padding(bottom = 5.dp),
                    value = passUser,
                    onValueChange = { passUser = it },
                    label = { Text("Password") }
                )
                OutlinedTextField(
                    modifier = Modifier
                        .padding(bottom = 5.dp),
                    value = confirmPass,
                    onValueChange = { confirmPass = it },
                    label = { Text("Confirm Password") }
                )

                Button(
                    modifier = Modifier
                        .padding(15.dp)
                        .align(Alignment.CenterHorizontally),
                    colors = ButtonDefaults.buttonColors(
                        backgroundColor = Color.DarkGray, // Cambia el color de fondo del botón
                        contentColor = Color.White // Cambia el color del contenido del botón (texto/icono)
                    ),
                    onClick = {

                        registerViewModel.registerUser(
                            registerModel = registerModel(
                                email = emailUser,
                                pass = passUser,
                                UUID = "",
                                name = name,
                            ))
                    }
                ) {
                    Text("Register")
                }

                // Mostrar los datos del usuario registrado
                registeredUser?.let { user ->
                    Text("Registered User: ${user.UUID}", modifier = Modifier.padding(top = 10.dp))
                    Text("Registered User: ${user.name}", modifier = Modifier.padding(top = 10.dp))
                    Text("Email: ${user.email}", modifier = Modifier.padding(top = 5.dp))
                }
            }
        }
    }
}