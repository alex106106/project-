package com.example.savethem.ui.Screens

import android.app.Activity
import android.content.Context
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.savethem.R
import com.example.savethem.ViewModel.LoginViewModel
import com.example.savethem.ViewModel.RegisterViewModel
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException

@Composable
fun loginScreen(loginViewModel: LoginViewModel, navController: NavController, context: Context) {
    val registerViewModel: RegisterViewModel = viewModel()
    val isUserLoggedIn = loginViewModel.isUserLoggedIn
    val currentContext = LocalContext.current
    val isWearable = remember { isWearable(currentContext) }

    // OBTENCIÓN AUTOMÁTICA DEL WEB CLIENT ID DESDE GOOGLE SERVICES
    val webClientId = remember {
        val resourceId = currentContext.resources.getIdentifier("default_web_client_id", "string", currentContext.packageName)
        if (resourceId != 0) {
            currentContext.getString(resourceId)
        } else {
            // ID DE RESPALDO MANUAL (Asegúrate de cambiar este por el tuyo si falla)
            "419233537452-4vduhkq4bj7k7bm45e9fhfjiohjqvive.apps.googleusercontent.com"
        }
    }

    val gso = remember(webClientId) {
        GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(webClientId)
            .requestEmail()
            .build()
    }
    
    val googleSignInClient = remember(gso) { GoogleSignIn.getClient(currentContext, gso) }

    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
            try {
                val account = task.getResult(ApiException::class.java)
                account.idToken?.let { idToken ->
                    registerViewModel.registerWithGoogle(idToken) {
                        navController.navigate("main_screen") {
                            popUpTo("login_screen") { inclusive = true }
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e("GoogleSignIn", "Error en autenticación: ${e.message}")
            }
        } else {
            Log.e("GoogleSignIn", "Result code: ${result.resultCode}. Revisa SHA-1 en Firebase.")
        }
    }

    LaunchedEffect(isUserLoggedIn.value) {
        if (isUserLoggedIn.value == true) {
            val destination = if (isWearable) "test_screen" else "main_screen"
            navController.navigate(destination) {
                popUpTo("login_screen") { inclusive = true }
            }
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
            Surface(
                modifier = Modifier
                    .size(180.dp)
                    .clip(CircleShape),
                color = Color.White.copy(alpha = 0.2f)
            ) {
                Image(
                    painter = painterResource(id = R.drawable.otro),
                    contentDescription = "Logo",
                    modifier = Modifier.padding(30.dp),
                    contentScale = ContentScale.Fit
                )
            }

            Spacer(modifier = Modifier.height(40.dp))

            Text(
                text = "WomenSafe",
                style = MaterialTheme.typography.h3,
                color = Color.White,
                fontWeight = FontWeight.ExtraBold
            )
            Text(
                text = "Personal Safety & Connection",
                style = MaterialTheme.typography.body1,
                color = Color.White.copy(alpha = 0.8f),
                modifier = Modifier.padding(top = 8.dp)
            )

            Spacer(modifier = Modifier.height(80.dp))

            Button(
                onClick = { 
                    googleSignInClient.signOut().addOnCompleteListener {
                        launcher.launch(googleSignInClient.signInIntent)
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp),
                shape = RoundedCornerShape(30.dp),
                colors = ButtonDefaults.buttonColors(
                    backgroundColor = Color.White,
                    contentColor = colorResource(id = R.color.md_purple_800)
                ),
                elevation = ButtonDefaults.elevation(defaultElevation = 12.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Image(
                        painter = painterResource(id = R.drawable.otro),
                        contentDescription = null,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Text(
                        "CONTINUE WITH GOOGLE", 
                        fontSize = 16.sp, 
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(40.dp))
            
            Text(
                text = "By continuing you agree to our Terms of Service",
                style = MaterialTheme.typography.caption,
                color = Color.White.copy(alpha = 0.6f),
                textAlign = TextAlign.Center
            )
        }
    }
}
