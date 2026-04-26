package com.example.savethem.ui.Screens

import android.app.Activity
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.savethem.Model.registerModel
import com.example.savethem.R
import com.example.savethem.ViewModel.RegisterViewModel
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException

@Composable
fun RegisterScreen(registerViewModel: RegisterViewModel = viewModel(), navController: NavController) {
    var name by remember { mutableStateOf("") }
    var emailUser by remember { mutableStateOf("") }
    var passUser by remember { mutableStateOf("") }
    var confirmPass by remember { mutableStateOf("") }

    val context = LocalContext.current
    val registeredUser by registerViewModel.registeredUser.observeAsState()

    // OBTENCIÓN SEGURA DEL WEB CLIENT ID
    val webClientId = remember {
        try {
            // Intentamos obtenerlo de los recursos automáticos de Firebase
            val id = context.getString(R.string.default_web_client_id)
            if (id.isEmpty()) throw Exception("ID vacío")
            id
        } catch (e: Exception) {
            // ID MANUAL DE RESPALDO (Asegúrate de que este coincida con tu consola de Firebase)
            "419233537452-4vduhkq4bj7k7bm45e9fhfjiohjqvive.apps.googleusercontent.com"
        }
    }

    val gso = remember(webClientId) {
        GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(webClientId)
            .requestEmail()
            .build()
    }
    
    val googleSignInClient = remember(gso) { GoogleSignIn.getClient(context, gso) }

    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
            try {
                val account = task.getResult(ApiException::class.java)
                account.idToken?.let { idToken ->
                    registerViewModel.registerWithGoogle(idToken) {
                        navController.navigate("main_screen") {
                            popUpTo("register_screen") { inclusive = true }
                        }
                    }
                }
            } catch (e: ApiException) {
                Log.e("GoogleSignIn", "Error code: ${e.statusCode}")
                Toast.makeText(context, "Error de autenticación (Cód: ${e.statusCode})", Toast.LENGTH_LONG).show()
            }
        } else {
            // Si el código es 0, suele ser por SHA-1 o configuración de red
            Log.e("GoogleSignIn", "Cancelado o error de configuración. Código: ${result.resultCode}")
            Toast.makeText(context, "Google Sign-In cancelado. Revisa tu SHA-1 en Firebase.", Toast.LENGTH_LONG).show()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        colorResource(id = R.color.md_purple_700),
                        colorResource(id = R.color.md_purple_400)
                    )
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 30.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "Join WomenSafe",
                style = MaterialTheme.typography.h4,
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Register to start protecting",
                style = MaterialTheme.typography.body1,
                color = Color.White.copy(alpha = 0.8f)
            )

            Spacer(modifier = Modifier.height(30.dp))

            // BOTÓN DE GOOGLE MEJORADO
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .clickable { 
                        // Forzamos el cierre de sesión previo para limpiar estados corruptos
                        googleSignInClient.signOut().addOnCompleteListener {
                            launcher.launch(googleSignInClient.signInIntent)
                        }
                    },
                shape = RoundedCornerShape(16.dp),
                elevation = 4.dp,
                backgroundColor = Color.White
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier.padding(horizontal = 16.dp)
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.otro), // Puedes cambiar por un icono de Google
                        contentDescription = "Google Logo",
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "Sign up with Google",
                        color = Color.Black,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 16.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Divider(modifier = Modifier.weight(1f), color = Color.White.copy(alpha = 0.3f))
                Text(
                    text = " OR ",
                    color = Color.White.copy(alpha = 0.6f),
                    modifier = Modifier.padding(horizontal = 10.dp),
                    fontSize = 12.sp
                )
                Divider(modifier = Modifier.weight(1f), color = Color.White.copy(alpha = 0.3f))
            }

            Spacer(modifier = Modifier.height(20.dp))

            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Full Name", color = Color.White) },
                leadingIcon = { Icon(Icons.Default.Person, contentDescription = null, tint = Color.White) },
                colors = TextFieldDefaults.outlinedTextFieldColors(
                    focusedBorderColor = Color.White,
                    unfocusedBorderColor = Color.White.copy(alpha = 0.6f),
                    textColor = Color.White,
                    cursorColor = Color.White
                ),
                shape = RoundedCornerShape(16.dp),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = emailUser,
                onValueChange = { emailUser = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Email Address", color = Color.White) },
                leadingIcon = { Icon(Icons.Default.Email, contentDescription = null, tint = Color.White) },
                colors = TextFieldDefaults.outlinedTextFieldColors(
                    focusedBorderColor = Color.White,
                    unfocusedBorderColor = Color.White.copy(alpha = 0.6f),
                    textColor = Color.White,
                    cursorColor = Color.White
                ),
                shape = RoundedCornerShape(16.dp),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = passUser,
                onValueChange = { passUser = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Password", color = Color.White) },
                leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null, tint = Color.White) },
                visualTransformation = PasswordVisualTransformation(),
                colors = TextFieldDefaults.outlinedTextFieldColors(
                    focusedBorderColor = Color.White,
                    unfocusedBorderColor = Color.White.copy(alpha = 0.6f),
                    textColor = Color.White,
                    cursorColor = Color.White
                ),
                shape = RoundedCornerShape(16.dp),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = {
                    if (name.isNotEmpty() && emailUser.isNotEmpty()) {
                        registerViewModel.registerUser(
                            registerModel = registerModel(email = emailUser, pass = passUser, UUID = "", name = name)
                        )
                    }
                },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(backgroundColor = Color.White, contentColor = colorResource(id = R.color.md_purple_700))
            ) {
                Text("CREATE ACCOUNT", fontSize = 16.sp, fontWeight = FontWeight.ExtraBold)
            }

            Spacer(modifier = Modifier.height(20.dp))

            Row(modifier = Modifier.clickable { navController.popBackStack() }) {
                Text("Back to ", color = Color.White.copy(alpha = 0.8f), fontSize = 14.sp)
                Text("Login", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            }
        }
    }
}
