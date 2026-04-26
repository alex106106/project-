package com.example.savethem.call

import android.content.Context
import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.savethem.R
import com.example.savethem.ViewModel.FriendsViewModel
import com.example.savethem.navigation.Screens
import com.google.auth.oauth2.GoogleCredentials
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.InputStream

// CONFIGURACIÓN FCM V1
private const val SERVICE_ACCOUNT_FILE = "savethem-30714-firebase-adminsdk-nbmeq-7342bacbd3.json"
private const val PROJECT_ID = "savethem-30714"
private const val FCM_V1_URL = "https://fcm.googleapis.com/v1/projects/$PROJECT_ID/messages:send"

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun boton(friendsViewModel: FriendsViewModel, navController: NavController) {
    val friends by friendsViewModel.friends.collectAsState()
    var selectedFriend by remember { mutableStateOf("") }
    val context = androidx.compose.ui.platform.LocalContext.current
    
    friendsViewModel.getAllFriends()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colorResource(id = R.color.white))
    ) {
        Column {
            Row(
                modifier = Modifier
                    .clickable { navController.navigate(Screens.GlobalChat.route) }
                    .padding(top = 7.dp, bottom = 7.dp, start = 14.dp, end = 14.dp)
                    .fillMaxWidth()
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Image(
                    painter = painterResource(id = R.drawable.chatgroup),
                    contentDescription = null,
                    modifier = Modifier
                        .size(50.dp)
                        .clip(CircleShape)
                        .border(2.dp, colorResource(id = R.color.md_purple_800), CircleShape),
                    contentScale = ContentScale.Crop
                )
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 8.dp)
                ) {
                    Text(
                        text = "Global Chat",
                        color = colorResource(id = R.color.md_pink_900),
                        style = TextStyle(
                            fontFamily = FontFamily(Font(R.font.signikaregular)),
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold
                        ),
                        modifier = Modifier.padding(bottom = 3.dp)
                    )
                    Text(
                        text = "Write something",
                        color = colorResource(id = R.color.md_pink_A200),
                        style = TextStyle(
                            fontFamily = FontFamily(Font(R.font.josefinsanslight)),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                        )
                    )
                }
            }
            LazyColumn {
                friends.forEach { friend ->
                    item {
                        Row(
                            modifier = Modifier
                                .clickable {
                                    selectedFriend = friend.UUID!!
                                    // Ejemplo: Enviar notificación al hacer clic
                                    friend.token?.let { token ->
                                        enviarNotificacionV1(context, token, "Hola ${friend.name}", "Te han enviado un mensaje")
                                    }
                                }
                                .padding(top = 7.dp, bottom = 7.dp, start = 14.dp, end = 14.dp)
                                .fillMaxWidth()
                                .padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Image(
                                painter = painterResource(id = R.drawable.imgprof),
                                contentDescription = null,
                                modifier = Modifier
                                    .size(50.dp)
                                    .clip(CircleShape)
                                    .border(2.dp, colorResource(id = R.color.md_purple_800), CircleShape),
                                contentScale = ContentScale.Crop
                            )
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(start = 8.dp)
                            ) {
                                Text(
                                    text = friend.name.toString(),
                                    color = colorResource(id = R.color.md_pink_900),
                                    style = TextStyle(
                                        fontFamily = FontFamily(Font(R.font.signikaregular)),
                                        fontSize = 20.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                )
                                Text(
                                    text = friend.email.toString(),
                                    color = colorResource(id = R.color.md_pink_A200),
                                    style = TextStyle(
                                        fontFamily = FontFamily(Font(R.font.josefinsanslight)),
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold,
                                    )
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    LaunchedEffect(selectedFriend) {
        if (selectedFriend.isNotEmpty()) {
            navController.navigate(Screens.Chat.route + "/$selectedFriend")
        }
    }
}

/**
 * Función para enviar notificaciones usando FCM V1
 */
fun enviarNotificacionV1(context: Context, token: String, titulo: String, mensaje: String) {
    CoroutineScope(Dispatchers.IO).launch {
        try {
            val accessToken = getAccessToken(context) ?: return@launch
            val client = OkHttpClient()
            val mediaType = "application/json; charset=utf-8".toMediaTypeOrNull()

            val jsonBody = JSONObject().apply {
                val message = JSONObject().apply {
                    put("token", token)
                    put("notification", JSONObject().apply {
                        put("title", titulo)
                        put("body", mensaje)
                    })
                    put("data", JSONObject().apply {
                        put("titulo", titulo)
                        put("detalle", mensaje)
                        put("icon", "norma")
                    })
                }
                put("message", message)
            }

            val request = Request.Builder()
                .url(FCM_V1_URL)
                .post(jsonBody.toString().toRequestBody(mediaType))
                .addHeader("Authorization", "Bearer $accessToken")
                .build()

            client.newCall(request).execute().use { response ->
                val resText = response.body?.string()
                if (response.isSuccessful) {
                    Log.d("FCM_V1", "Enviado con éxito: $resText")
                } else {
                    Log.e("FCM_V1", "Error ${response.code}: $resText")
                }
            }
        } catch (e: Exception) {
            Log.e("FCM_V1", "Excepción al enviar: ${e.message}")
        }
    }
}

private suspend fun getAccessToken(context: Context): String? = withContext(Dispatchers.IO) {
    return@withContext try {
        val inputStream: InputStream = context.assets.open(SERVICE_ACCOUNT_FILE)
        val googleCredentials = GoogleCredentials.fromStream(inputStream)
            .createScoped(listOf("https://www.googleapis.com/auth/firebase.messaging"))
        
        googleCredentials.refreshIfExpired()
        googleCredentials.accessToken.tokenValue
    } catch (e: Exception) {
        Log.e("FCM_TOKEN", "Error obteniendo token: ${e.message}")
        null
    }
}
