package com.example.savethem.ui.Screens

import android.annotation.SuppressLint
import android.location.Location
import android.util.Log
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.savethem.Model.ChatModel
import com.example.savethem.Model.LatLngWrapper
import com.example.savethem.Model.LocationModel
import com.example.savethem.R
import com.example.savethem.ViewModel.ChatViewModel
import com.google.android.gms.location.LocationServices
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.messaging.FirebaseMessaging
import java.text.SimpleDateFormat
import java.util.*

@SuppressLint("UnusedMaterialScaffoldPaddingParameter")
@Composable
fun GlobalChatMainScreen(chatViewModel: ChatViewModel, navController: NavController) {
    Scaffold(
        topBar = { TopAppBarGlobalChat(chatViewModel = chatViewModel, navController = navController) },
        backgroundColor = Color(0xFFF8F9FD) 
    ) { paddingValues ->
        Box(modifier = Modifier.padding(paddingValues).fillMaxSize()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            listOf(Color(0xFFF8F9FD), Color(0xFFE8EAF6))
                        )
                    )
            )
            GlobalChatScreen(chatViewModel = chatViewModel)
        }
    }
}

@SuppressLint("MissingPermission")
@Composable
fun GlobalChatScreen(chatViewModel: ChatViewModel) {
    val selectedFriend by chatViewModel.selectedFriend.collectAsState(null)
    val context = LocalContext.current
    var comment by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        chatViewModel.getGlobalChatMessage()
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Box(modifier = Modifier.weight(1f)) {
            GlobalChatList(chatViewModel = chatViewModel)
        }

        Surface(
            elevation = 16.dp,
            shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
            color = Color.White,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .padding(horizontal = 20.dp, vertical = 16.dp)
                    .fillMaxWidth()
                    .navigationBarsPadding(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    modifier = Modifier.size(48.dp),
                    shape = CircleShape,
                    color = colorResource(id = R.color.md_purple_50),
                    elevation = 0.dp
                ) {
                    IconButton(
                        onClick = {
                            val idToFriend = FirebaseAuth.getInstance().currentUser?.uid
                            val messageID = UUID.randomUUID().toString()
                            val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)
                            fusedLocationClient.lastLocation.addOnSuccessListener { location: Location? ->
                                location?.let {
                                    if (it.latitude != 0.0 && it.longitude != 0.0) {
                                        chatViewModel.addLocation(
                                            LocationModel(location = LatLngWrapper(it.latitude, it.longitude)),
                                            selectedFriend?.UUID ?: "",
                                            messageID,
                                            idToFriend ?: ""
                                        )
                                    }
                                }
                            }
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.LocationOn,
                            contentDescription = "Location",
                            tint = colorResource(id = R.color.md_purple_800)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                TextField(
                    value = comment,
                    onValueChange = { comment = it },
                    placeholder = { 
                        Text(
                            "Escribe a la comunidad...", 
                            style = TextStyle(
                                fontFamily = FontFamily(Font(R.font.josefinsanslight)),
                                color = Color.Gray,
                                fontSize = 14.sp
                            )
                        ) 
                    },
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(24.dp))
                        .background(Color(0xFFF1F3F4)),
                    colors = TextFieldDefaults.textFieldColors(
                        backgroundColor = Color.Transparent,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        cursorColor = colorResource(id = R.color.md_purple_800),
                        textColor = Color.Black
                    ),
                    maxLines = 4
                )

                Spacer(modifier = Modifier.width(12.dp))

                IconButton(
                    onClick = {
                        if (comment.isNotEmpty()) {
                            val messageID = UUID.randomUUID().toString()
                            chatViewModel.addMessageGlobal(
                                ChatModel(message = comment),
                                messageID
                            )
                            comment = ""
                            FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
                                if (task.isSuccessful) {
                                    Log.d("FCM", task.result)
                                }
                            }
                        }
                    },
                    enabled = comment.isNotEmpty(),
                    modifier = Modifier
                        .size(48.dp)
                        .shadow(if (comment.isNotEmpty()) 4.dp else 0.dp, CircleShape)
                        .background(
                            if (comment.isNotEmpty()) colorResource(id = R.color.md_purple_800) else Color(0xFFE0E0E0),
                            CircleShape
                        )
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Send,
                        contentDescription = "SEND",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun GlobalChatList(chatViewModel: ChatViewModel) {
    val messages by chatViewModel.addMessage.collectAsState(emptyList())
    val listState = rememberLazyListState()

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        val currentUser = FirebaseAuth.getInstance().currentUser

        items(messages) { msg ->
            val isMe = msg.UUIDSender == currentUser?.uid
            ChatBubble(msg, isMe)
        }
    }
}

@Composable
fun ChatBubble(msg: ChatModel, isMe: Boolean) {
    val alignment = if (isMe) Alignment.End else Alignment.Start
    val bubbleColor = if (isMe) colorResource(id = R.color.md_purple_800) else Color.White
    val textColor = if (isMe) Color.White else Color(0xFF202124)
    
    val bubbleShape = RoundedCornerShape(
        topStart = 20.dp,
        topEnd = 20.dp,
        bottomStart = if (isMe) 20.dp else 4.dp,
        bottomEnd = if (isMe) 4.dp else 20.dp
    )

    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
        horizontalAlignment = alignment
    ) {
        if (!isMe) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(start = 4.dp, bottom = 6.dp)
            ) {
                Surface(
                    modifier = Modifier.size(24.dp),
                    shape = CircleShape,
                    color = colorResource(id = R.color.md_pink_100),
                    elevation = 2.dp
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = msg.UUIDSender.take(1).uppercase(),
                            style = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.Bold, color = colorResource(id = R.color.md_purple_900))
                        )
                    }
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Usuario ${msg.UUIDSender.take(4)}",
                    style = TextStyle(
                        fontFamily = FontFamily(Font(R.font.josefinsanslight)),
                        fontSize = 12.sp,
                        color = Color.DarkGray,
                        fontWeight = FontWeight.SemiBold
                    )
                )
            }
        }

        Surface(
            elevation = if (isMe) 4.dp else 2.dp,
            shape = bubbleShape,
            color = bubbleColor
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                horizontalAlignment = if (isMe) Alignment.End else Alignment.Start
            ) {
                Text(
                    text = msg.message,
                    color = textColor,
                    style = TextStyle(
                        fontFamily = FontFamily(Font(R.font.josefinsanslight)),
                        fontSize = 15.sp,
                        lineHeight = 22.sp
                    )
                )
                
                Spacer(modifier = Modifier.height(4.dp))
                
                val formattedTime = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(msg.timestamp))
                Text(
                    text = formattedTime,
                    style = TextStyle(
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Light,
                        color = if (isMe) Color.White.copy(alpha = 0.8f) else Color.Gray
                    )
                )
            }
        }
    }
}

@Composable
fun TopAppBarGlobalChat(chatViewModel: ChatViewModel, navController: NavController) {
    val userData by chatViewModel.user.collectAsState()
    
    LaunchedEffect(Unit) {
        chatViewModel.getUserData()
    }

    Surface(
        elevation = 8.dp,
        color = Color.White,
        modifier = Modifier.statusBarsPadding()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(68.dp)
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { navController.popBackStack() }) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = colorResource(id = R.color.md_purple_800)
                )
            }
            
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(colorResource(id = R.color.md_purple_100)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Public,
                    contentDescription = null,
                    tint = colorResource(id = R.color.md_purple_800),
                    modifier = Modifier.size(26.dp)
                )
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Comunidad Global",
                    style = TextStyle(
                        fontFamily = FontFamily(Font(R.font.signikaregular)),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = colorResource(id = R.color.md_purple_900)
                    )
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(Color(0xFF4CAF50)))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "En línea: ${userData?.name ?: "Usuario"}",
                        style = TextStyle(
                            fontFamily = FontFamily(Font(R.font.josefinsanslight)),
                            fontSize = 11.sp,
                            color = Color.Gray
                        )
                    )
                }
            }
            
            IconButton(onClick = { /* Search */ }) {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = "Search",
                    tint = Color.Gray
                )
            }
            
            IconButton(onClick = { /* Settings */ }) {
                Icon(
                    imageVector = Icons.Default.MoreVert,
                    contentDescription = "More",
                    tint = Color.Gray
                )
            }
        }
    }
}
