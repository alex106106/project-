package com.example.savethem.ui.Screens

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
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
import androidx.core.content.ContextCompat
import androidx.navigation.NavController
import com.example.savethem.R
import com.example.savethem.ViewModel.ChatViewModel
import com.example.savethem.ViewModel.mainViewModel
import com.example.savethem.navigation.Screens
import com.example.savethem.service.MyBackgroundService
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.*
import com.google.firebase.auth.FirebaseAuth
import com.google.maps.android.compose.*
import java.text.SimpleDateFormat
import java.util.*

@SuppressLint("UnusedMaterialScaffoldPaddingParameter")
@Composable
fun ChatMainScreen(id: String, chatViewModel: ChatViewModel, mainViewModel: mainViewModel, navController: NavController) {
    var isServiceRunning by remember { mutableStateOf(false) }
    val isDarkMode by mainViewModel.isDarkMode.collectAsState()

    Scaffold(
        topBar = { 
            TopAppBarChat(
                chatViewModel = chatViewModel, 
                navController = navController,
                friendId = id,
                isServiceRunning = isServiceRunning,
                onToggleService = { isServiceRunning = it },
                isDarkMode = isDarkMode
            ) 
        },
        backgroundColor = if (isDarkMode) Color(0xFF121212) else Color(0xFFF8F9FD)
    ) { paddingValues ->
        Box(modifier = Modifier.padding(paddingValues).fillMaxSize()) {
            ChatScreen(
                id = id, 
                chatViewModel = chatViewModel,
                isDarkMode = isDarkMode
            ) 
        }
    }
}

@Composable
fun TopAppBarChat(
    chatViewModel: ChatViewModel, 
    navController: NavController,
    friendId: String,
    isServiceRunning: Boolean,
    onToggleService: (Boolean) -> Unit,
    isDarkMode: Boolean
) {
    val context = LocalContext.current
    val selectedFriend by chatViewModel.selectedFriend.collectAsState(null)
    val locations by chatViewModel.locationById.collectAsState()
    var showMenu by remember { mutableStateOf(false) }

    val startTracking = {
        val intent = Intent(context, MyBackgroundService::class.java).apply {
            putExtra("FRIEND_ID", friendId)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(intent)
        } else {
            context.startService(intent)
        }
        onToggleService(true)
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val fineLocationGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] ?: false
        if (fineLocationGranted) {
            startTracking()
        }
    }
    
    Surface(
        elevation = 8.dp,
        color = if (isDarkMode) Color(0xFF1E1E1E) else Color.White
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp)
                .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { navController.popBackStack() }) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "Back",
                    tint = if (isDarkMode) Color.White else colorResource(id = R.color.md_purple_800)
                )
            }
            
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(if (isDarkMode) Color(0xFF333333) else colorResource(id = R.color.md_purple_100)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = (selectedFriend?.name?.take(1) ?: "U").uppercase(),
                    style = TextStyle(
                        fontWeight = FontWeight.Bold,
                        color = colorResource(id = R.color.md_purple_800)
                    )
                )
            }
            
            Spacer(modifier = Modifier.width(12.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = selectedFriend?.name ?: "Chat",
                    style = TextStyle(
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isDarkMode) Color.White else Color.Black
                    )
                )
                Text(
                    text = if (isServiceRunning) "Rastreo activo" else "En línea",
                    style = TextStyle(
                        fontSize = 11.sp,
                        color = if (isServiceRunning) Color.Red else Color.Gray
                    )
                )
            }

            IconButton(
                onClick = {
                    if (isServiceRunning) {
                        val intent = Intent(context, MyBackgroundService::class.java)
                        context.stopService(intent)
                        onToggleService(false)
                    } else {
                        val hasLocationPermission = ContextCompat.checkSelfPermission(
                            context, Manifest.permission.ACCESS_FINE_LOCATION
                        ) == PackageManager.PERMISSION_GRANTED

                        if (hasLocationPermission) {
                            startTracking()
                        } else {
                            val permissionsToRequest = mutableListOf(
                                Manifest.permission.ACCESS_FINE_LOCATION,
                                Manifest.permission.ACCESS_COARSE_LOCATION
                            )
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                permissionsToRequest.add(Manifest.permission.POST_NOTIFICATIONS)
                            }
                            permissionLauncher.launch(permissionsToRequest.toTypedArray())
                        }
                    }
                }
            ) {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = "Tracking",
                    tint = if (isServiceRunning) Color.Red else if (isDarkMode) Color.White else colorResource(id = R.color.md_purple_800)
                )
            }
            
            Box {
                IconButton(onClick = { showMenu = true }) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = "Options",
                        tint = if (isDarkMode) Color.White else Color.Gray
                    )
                }
                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false },
                    modifier = Modifier.background(if (isDarkMode) Color(0xFF1E1E1E) else Color.White)
                ) {
                    DropdownMenuItem(onClick = { 
                        showMenu = false
                        navController.navigate(Screens.Profile.route + "?userId=${friendId}")
                    }) {
                        Icon(Icons.Default.Person, contentDescription = null, tint = if (isDarkMode) Color.White else Color.Black)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Ver Perfil", color = if (isDarkMode) Color.White else Color.Black)
                    }
                    DropdownMenuItem(onClick = { 
                        showMenu = false
                        val lastLoc = locations.lastOrNull { it.UUIDSender == friendId }
                        val shareText = if (lastLoc != null) {
                            "Ubicación de ${selectedFriend?.name}: https://www.google.com/maps/search/?api=1&query=${lastLoc.location?.latitude},${lastLoc.location?.longitude}"
                        } else {
                            "Compartiendo contacto: ${selectedFriend?.name}"
                        }
                        val sendIntent: Intent = Intent().apply {
                            action = Intent.ACTION_SEND
                            putExtra(Intent.EXTRA_TEXT, shareText)
                            type = "text/plain"
                        }
                        context.startActivity(Intent.createChooser(sendIntent, "Compartir vía"))
                    }) {
                        Icon(Icons.Default.Share, contentDescription = null, tint = if (isDarkMode) Color.White else Color.Black)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Compartir", color = if (isDarkMode) Color.White else Color.Black)
                    }
                }
            }
        }
    }
}

@Composable
fun ChatScreen(id: String, chatViewModel: ChatViewModel, isDarkMode: Boolean) {
    val context = LocalContext.current
    val currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: return
    val locationList by chatViewModel.locationById.collectAsState(emptyList())
    val selectedFriend by chatViewModel.selectedFriend.collectAsState(null)
    
    var isChatVisible by remember { mutableStateOf(false) }

    LaunchedEffect(id) {
        chatViewModel.getAllMessage(currentUserId, id)
        chatViewModel.locationID(currentUserId, id, 0.0, 0.0, context, "")
        chatViewModel.friendID(id)
    }

    Box(modifier = Modifier.fillMaxSize()) {
        val cameraPositionState = rememberCameraPositionState()
        var hasInitiallyMovedCamera by remember { mutableStateOf(false) }

        LaunchedEffect(locationList) {
            if (locationList.isNotEmpty() && !hasInitiallyMovedCamera) {
                val lastLoc = locationList.lastOrNull { it.UUIDSender != currentUserId } ?: locationList.last()
                lastLoc.location?.let {
                    if (it.latitude != 0.0 && it.longitude != 0.0) {
                        cameraPositionState.position = CameraPosition.fromLatLngZoom(LatLng(it.latitude, it.longitude), 15f)
                        hasInitiallyMovedCamera = true
                    }
                }
            }
        }

        GoogleMap(
            modifier = Modifier.fillMaxSize(),
            cameraPositionState = cameraPositionState,
            uiSettings = MapUiSettings(zoomControlsEnabled = false, compassEnabled = true)
        ) {
            if (locationList.isNotEmpty()) {
                val locationsByUser = remember(locationList) {
                    locationList.groupBy { it.UUIDSender }
                }

                locationsByUser.forEach { (senderId, userLocations) ->
                    // FILTROS:
                    // 1. Ordenar por tiempo
                    // 2. Eliminar coordenadas invalidas (0.0, 0.0)
                    // 3. Tomar solo los últimos 15 puntos para evitar "spaghetti" de líneas
                    val path = userLocations
                        .sortedBy { it.timestamp }
                        .mapNotNull { it.location?.let { loc -> 
                            if (loc.latitude != 0.0 && loc.longitude != 0.0) LatLng(loc.latitude, loc.longitude) else null 
                        }}
                        .takeLast(15) 

                    if (path.isNotEmpty()) {
                        val isMe = senderId == currentUserId
                        val polylineColor = if (isMe) colorResource(id = R.color.md_purple_800) else Color(0xFFFF5252)
                        
                        Polyline(
                            points = path, 
                            color = polylineColor, 
                            width = 8f, // Un poco más delgada para verse mejor
                            pattern = if (isMe) null else listOf(Dash(20f), Gap(10f)),
                            jointType = JointType.ROUND,
                            startCap = RoundCap(),
                            endCap = RoundCap()
                        )
                        
                        val lastPoint = path.last()
                        Marker(
                            state = MarkerState(position = lastPoint),
                            title = if(isMe) "Tú" else selectedFriend?.name ?: "Amigo",
                            icon = BitmapDescriptorFactory.defaultMarker(
                                if(isMe) BitmapDescriptorFactory.HUE_VIOLET else BitmapDescriptorFactory.HUE_RED
                            )
                        )
                    }
                }
            }
        }

        Surface(
            modifier = Modifier.align(Alignment.TopEnd).padding(16.dp).padding(top = 16.dp),
            shape = RoundedCornerShape(16.dp),
            elevation = 6.dp,
            color = if (isDarkMode) Color(0xFF1E1E1E).copy(alpha = 0.9f) else Color.White.copy(alpha = 0.9f)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text("Leyenda", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = if(isDarkMode) Color.White else Color.Black)
                Spacer(modifier = Modifier.height(8.dp))
                LegendItem(color = colorResource(id = R.color.md_purple_800), label = "Tú", isDashed = false, isDarkMode)
                Spacer(modifier = Modifier.height(4.dp))
                LegendItem(color = Color(0xFFFF5252), label = selectedFriend?.name ?: "Amigo", isDashed = true, isDarkMode)
            }
        }

        ExtendedFloatingActionButton(
            text = { Text(if(isChatVisible) "Cerrar" else "Chat") },
            icon = { Icon(if(isChatVisible) Icons.Default.KeyboardArrowDown else Icons.Default.Menu, contentDescription = null) },
            onClick = { isChatVisible = !isChatVisible },
            modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp).padding(bottom = if(isChatVisible) 340.dp else 0.dp),
            backgroundColor = colorResource(id = R.color.md_purple_800),
            contentColor = Color.White
        )

        AnimatedVisibility(
            visible = isChatVisible,
            enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            Surface(
                modifier = Modifier.fillMaxWidth().height(350.dp).padding(8.dp),
                shape = RoundedCornerShape(28.dp),
                elevation = 20.dp,
                color = if (isDarkMode) Color(0xFF1E1E1E).copy(alpha = 0.98f) else Color.White.copy(alpha = 0.98f)
            ) {
                Column {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(4.dp)
                            .padding(top = 12.dp)
                            .align(Alignment.CenterHorizontally)
                            .width(40.dp)
                            .background(Color.LightGray, CircleShape)
                    )
                    
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "Mensajes con ${selectedFriend?.name ?: "..."}",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = if (isDarkMode) Color.White else Color.Black
                        )
                    }
                    
                    Box(modifier = Modifier.weight(1f)) {
                        ChatList(chatViewModel = chatViewModel, isDarkMode = isDarkMode)
                    }
                    
                    MessageInput(chatViewModel, selectedFriend?.UUID ?: "", isDarkMode = isDarkMode)
                }
            }
        }
    }
}

@Composable
fun LegendItem(color: Color, label: String, isDashed: Boolean, isDarkMode: Boolean) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(width = 20.dp, height = 4.dp)
                .background(color, if (isDashed) RoundedCornerShape(1.dp) else CircleShape)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(label, fontSize = 11.sp, fontWeight = FontWeight.Medium, color = if(isDarkMode) Color.White else Color.Black)
    }
}

@Composable
fun MessageInput(chatViewModel: ChatViewModel, friendUuid: String, isDarkMode: Boolean) {
    var comment by remember { mutableStateOf("") }
    Row(
        modifier = Modifier.padding(16.dp).fillMaxWidth().navigationBarsPadding(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        TextField(
            value = comment,
            onValueChange = { comment = it },
            modifier = Modifier.weight(1f),
            placeholder = { Text("Mensaje...", color = Color.Gray) },
            shape = RoundedCornerShape(24.dp),
            colors = TextFieldDefaults.textFieldColors(
                backgroundColor = if (isDarkMode) Color(0xFF2C2C2C) else Color(0xFFF1F3F4),
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
                textColor = if (isDarkMode) Color.White else Color.Black
            )
        )
        
        Spacer(modifier = Modifier.width(8.dp))
        
        IconButton(
            onClick = {
                if (comment.isNotEmpty()) {
                    chatViewModel.sendMessage(comment, friendUuid)
                    comment = ""
                }
            },
            modifier = Modifier
                .size(48.dp)
                .background(
                    if (comment.isNotEmpty()) colorResource(id = R.color.md_purple_800) else Color.LightGray,
                    CircleShape
                )
        ) {
            Icon(
                painter = painterResource(id = R.drawable.send),
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
fun ChatList(chatViewModel: ChatViewModel, isDarkMode: Boolean) {
    val messageList by chatViewModel.addMessage.collectAsState(emptyList())
    val listState = rememberLazyListState()
    val currentUser = FirebaseAuth.getInstance().currentUser

    LaunchedEffect(messageList.size) {
        if (messageList.isNotEmpty()) {
            listState.animateScrollToItem(0)
        }
    }

    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        reverseLayout = true 
    ) {
        items(
            items = messageList,
            key = { message -> message.IDMessage }
        ) { message ->
            val isMe = message.UUIDSender == currentUser?.uid
            ChatBubbleItem(message.message, message.timestamp, isMe, isDarkMode)
        }
    }
}

@Composable
fun ChatBubbleItem(message: String, timestamp: Long, isMe: Boolean, isDarkMode: Boolean) {
    val alignment = if (isMe) Alignment.CenterEnd else Alignment.CenterStart
    val bubbleColor = if (isMe) colorResource(id = R.color.md_purple_800) else if (isDarkMode) Color(0xFF333333) else Color(0xFFEEEEEE)
    val textColor = if (isMe) Color.White else if (isDarkMode) Color.White else Color.Black
    
    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = alignment
    ) {
        Surface(
            shape = RoundedCornerShape(
                topStart = 16.dp, topEnd = 16.dp,
                bottomStart = if (isMe) 16.dp else 4.dp,
                bottomEnd = if (isMe) 4.dp else 16.dp
            ),
            elevation = 1.dp,
            color = bubbleColor
        ) {
            Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                Text(
                    text = message,
                    fontSize = 15.sp,
                    color = textColor,
                    lineHeight = 20.sp
                )
                Text(
                    text = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(timestamp)),
                    fontSize = 10.sp,
                    color = if (isMe) Color.White.copy(alpha = 0.7f) else Color.Gray,
                    modifier = Modifier.align(Alignment.End).padding(top = 2.dp)
                )
            }
        }
    }
}
