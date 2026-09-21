package com.example.savethem.ui.Screens

import android.content.Intent
import android.os.Build
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import coil.compose.rememberAsyncImagePainter
import com.example.savethem.Model.SafePlaceModel
import com.example.savethem.Model.registerModel
import com.example.savethem.R
import com.example.savethem.ViewModel.ChatViewModel
import com.example.savethem.ViewModel.FriendsViewModel
import com.example.savethem.ViewModel.mainViewModel
import com.example.savethem.navigation.Screens
import com.example.savethem.service.EmergencyService
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.collectLatest

@Composable
fun MainScreen(viewModel: mainViewModel, navController: NavController, friendsViewModel: FriendsViewModel) {
    var showAddDialog by remember { mutableStateOf(false) }
    var showSafePlaceDialog by remember { mutableStateOf(false) }
    var showSOSConfigDialog by remember { mutableStateOf(false) }
    
    var friendToDelete by remember { mutableStateOf<registerModel?>(null) }
    val chatViewModel: ChatViewModel = hiltViewModel()
    
    val context = LocalContext.current
    
    val activeEmergencies by viewModel.activeEmergencies.collectAsState()
    val friends by friendsViewModel.friends.collectAsState()
    val safePlaces by viewModel.safePlaces.collectAsState()
    val userName by viewModel.name.collectAsState()
    val isDarkMode by viewModel.isDarkMode.collectAsState()
    val userLocation by viewModel.currentUserLocation.collectAsState()
    
    val showTour by viewModel.showTour.collectAsState()
    
    val currentUser = FirebaseAuth.getInstance().currentUser
    val isEmergencyActive = activeEmergencies.any { it.userId == currentUser?.uid }

    LaunchedEffect(Unit) {
        friendsViewModel.toastMessage.collectLatest { message ->
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        }
    }

    LaunchedEffect(Unit) {
        friendsViewModel.getAllFriends()
        viewModel.getUserData()
        viewModel.updateUserLocation()
    }

    if (showTour) {
        AppTourDialog(onDismiss = { viewModel.dismissTour() })
    }

    Scaffold(
        backgroundColor = if (isDarkMode) Color(0xFF121212) else Color(0xFFF5F7FB),
        topBar = {
            Surface(
                elevation = 4.dp, 
                color = if (isDarkMode) Color(0xFF1E1E1E) else Color.White,
                shape = RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp)
            ) {
                Row(
                    modifier = Modifier
                        .statusBarsPadding()
                        .padding(horizontal = 20.dp, vertical = 20.dp)
                        .fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Hola,", 
                            style = TextStyle(
                                fontSize = 14.sp, 
                                color = if (isDarkMode) Color.LightGray else Color.Gray
                            )
                        )
                        Text(
                            text = userName.ifEmpty { "Usuario" }, 
                            style = TextStyle(
                                fontSize = 24.sp, 
                                fontWeight = FontWeight.Black, 
                                color = if (isDarkMode) Color.White else colorResource(id = R.color.md_purple_900)
                            )
                        )
                    }
                    
                    Row {
                        IconButton(onClick = { showSOSConfigDialog = true }) {
                            Icon(Icons.Default.Tune, contentDescription = "Config", tint = if (isDarkMode) Color.White else Color.Gray)
                        }
                    }
                }
            }
        },
        bottomBar = { BottomNavigationBarDesign(navController, isDarkMode) }
    ) { padding ->
        Box(modifier = Modifier.padding(padding).fillMaxSize()) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    EmergencyControlCard(
                        isActive = isEmergencyActive,
                        onClick = {
                            val intent = Intent(context, EmergencyService::class.java)
                            if (isEmergencyActive) {
                                intent.action = "STOP_EMERGENCY"
                                context.startService(intent)
                                viewModel.stopEmergency()
                            } else {
                                intent.action = "START_EMERGENCY"
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                    context.startForegroundService(intent)
                                } else {
                                    context.startService(intent)
                                }
                                friendsViewModel.sendEmergencyMessage()
                            }
                        }
                    )
                }

                item {
                    SectionHeader("Mis Lugares Seguros", isDarkMode)
                }

                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            modifier = Modifier
                                .size(60.dp)
                                .clickable { 
                                    viewModel.updateUserLocation()
                                    showSafePlaceDialog = true 
                                },
                            shape = RoundedCornerShape(16.dp),
                            color = colorResource(id = R.color.md_purple_50),
                            elevation = 2.dp
                        ) {
                            Icon(Icons.Default.AddLocationAlt, contentDescription = null, tint = colorResource(id = R.color.md_purple_800), modifier = Modifier.padding(16.dp))
                        }
                        
                        if (safePlaces.isEmpty()) {
                            Text(
                                "Registra un lugar seguro para dejar de rastrear automáticamente.",
                                fontSize = 13.sp,
                                color = Color.Gray,
                                modifier = Modifier.weight(1f)
                            )
                        } else {
                            LazyRow(
                                modifier = Modifier.weight(1f),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                items(safePlaces) { place -> SafePlaceItem(place, isDarkMode) }
                            }
                        }
                    }
                }

                item {
                    SectionHeader("Círculo de Confianza", isDarkMode)
                }

                if (friends.isEmpty()) {
                    item {
                        EmptyStateCard(
                            title = "Aún no tienes amigos",
                            description = "Añade a alguien de confianza para que pueda ver tu ubicación en emergencias.",
                            onAdd = { showAddDialog = true },
                            isDarkMode = isDarkMode
                        )
                    }
                } else {
                    items(friends) { friend ->
                        FriendListItem(
                            friend = friend,
                            navController = navController,
                            isDarkMode = isDarkMode,
                            onLongClick = { friendToDelete = friend }
                        )
                    }
                    
                    item {
                        TextButton(
                            onClick = { showAddDialog = true },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.PersonAdd, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("AÑADIR CONTACTO")
                        }
                    }
                }
            }

            if (friendToDelete != null) {
                AlertDialog(
                    onDismissRequest = { friendToDelete = null },
                    title = { Text("Eliminar de tus contactos") },
                    text = { Text("¿Estás seguro de que deseas eliminar a ${friendToDelete?.name} de tu círculo de confianza? Esto borrará el chat e historial de ubicaciones compartidas por completo tanto de este dispositivo como de la nube.") },
                    confirmButton = {
                        TextButton(onClick = {
                            val targetId = friendToDelete?.UUID
                            if (targetId != null) {
                                chatViewModel.deleteChatAndFriend(targetId) {
                                    friendsViewModel.getAllFriends()
                                    Toast.makeText(context, "Contacto eliminado correctamente", Toast.LENGTH_SHORT).show()
                                }
                            }
                            friendToDelete = null
                        }) {
                            Text("ELIMINAR", color = Color.Red, fontWeight = FontWeight.Bold)
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { friendToDelete = null }) {
                            Text("CANCELAR", color = Color.Gray)
                        }
                    },
                    backgroundColor = if (isDarkMode) Color(0xFF1E1E1E) else Color.White,
                    contentColor = if (isDarkMode) Color.White else Color.Black
                )
            }

            if (showSOSConfigDialog) {
                CustomizeSOSDialog(viewModel = viewModel, onDismiss = { showSOSConfigDialog = false })
            }

            if (showAddDialog) {
                AddFriendDialogDesign(onDismiss = { showAddDialog = false }, onAdd = { email ->
                    friendsViewModel.addFriend(email)
                    showAddDialog = false
                })
            }

            if (showSafePlaceDialog) {
                SaveSafePlaceDialog(onDismiss = { showSafePlaceDialog = false }, onSave = { name ->
                    val location = userLocation
                    if (location != null) {
                        viewModel.addSafePlace(name, location)
                        Toast.makeText(context, "Lugar guardado: $name", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(context, "Obteniendo ubicación...", Toast.LENGTH_LONG).show()
                        viewModel.updateUserLocation()
                    }
                    showSafePlaceDialog = false
                })
            }
        }
    }
}

@Composable
fun SectionHeader(title: String, isDarkMode: Boolean) {
    Text(
        text = title,
        style = TextStyle(
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = if (isDarkMode) Color.White else colorResource(id = R.color.md_purple_900)
        ),
        modifier = Modifier.padding(vertical = 4.dp)
    )
}

@Composable
fun EmptyStateCard(title: String, description: String, onAdd: () -> Unit, isDarkMode: Boolean) {
    Card(
        shape = RoundedCornerShape(24.dp),
        backgroundColor = if (isDarkMode) Color(0xFF1E1E1E) else Color.White,
        elevation = 0.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(Icons.Default.GroupOff, contentDescription = null, modifier = Modifier.size(48.dp), tint = Color.LightGray)
            Spacer(modifier = Modifier.height(16.dp))
            Text(title, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = if (isDarkMode) Color.White else Color.Black)
            Spacer(modifier = Modifier.height(8.dp))
            Text(description, textAlign = TextAlign.Center, fontSize = 13.sp, color = Color.Gray)
            Spacer(modifier = Modifier.height(20.dp))
            Button(
                onClick = onAdd,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(backgroundColor = colorResource(id = R.color.md_purple_800))
            ) {
                Text("AÑADIR AHORA", color = Color.White, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun EmergencyControlCard(isActive: Boolean, onClick: () -> Unit) {
    val backgroundColor = if (isActive) Color(0xFFFFEBEE) else colorResource(id = R.color.md_purple_800)
    val contentColor = if (isActive) Color(0xFFD32F2F) else Color.White

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(if (isActive) 12.dp else 4.dp, RoundedCornerShape(28.dp))
            .clickable { onClick() },
        shape = RoundedCornerShape(28.dp),
        backgroundColor = backgroundColor,
        elevation = 0.dp
    ) {
        Row(
            modifier = Modifier.padding(24.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(contentColor.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isActive) Icons.Default.GppBad else Icons.Default.Shield,
                    contentDescription = null,
                    tint = contentColor,
                    modifier = Modifier.size(32.dp)
                )
            }
            Spacer(modifier = Modifier.width(20.dp))
            Column {
                Text(
                    text = if (isActive) "SOS ACTIVADO" else "ESTÁS SEGURO",
                    style = TextStyle(
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 20.sp,
                        color = contentColor,
                        letterSpacing = 1.sp
                    )
                )
                Text(
                    text = if (isActive) "Presiona para detener alerta" else "Toca para enviar alerta SOS",
                    style = TextStyle(
                        fontSize = 13.sp,
                        color = contentColor.copy(alpha = 0.8f)
                    )
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun FriendListItem(friend: registerModel, navController: NavController, isDarkMode: Boolean, onLongClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(2.dp, RoundedCornerShape(20.dp))
            .clip(RoundedCornerShape(20.dp))
            .combinedClickable(
                onClick = { navController.navigate(Screens.Chat.route + "/${friend.UUID}") },
                onLongClick = onLongClick
            ), 
        shape = RoundedCornerShape(20.dp), 
        elevation = 0.dp,
        backgroundColor = if (isDarkMode) Color(0xFF1E1E1E) else Color.White
    ) {
        Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Surface(
                modifier = Modifier.size(50.dp), 
                shape = CircleShape, 
                color = colorResource(id = R.color.md_purple_50)
            ) {
                Box(contentAlignment = Alignment.Center) { 
                    Text(
                        text = (friend.name?.take(1) ?: "U").uppercase(), 
                        fontWeight = FontWeight.Bold, 
                        color = colorResource(id = R.color.md_purple_800), 
                        fontSize = 18.sp
                    ) 
                }
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    friend.name ?: "Usuario", 
                    fontWeight = FontWeight.Bold, 
                    fontSize = 16.sp, 
                    color = if (isDarkMode) Color.White else Color.Black
                )
                Text(
                    friend.email ?: "", 
                    fontSize = 12.sp, 
                    color = Color.Gray, 
                    maxLines = 1, 
                    overflow = TextOverflow.Ellipsis
                )
            }
            Icon(
                Icons.AutoMirrored.Filled.KeyboardArrowRight, 
                contentDescription = null, 
                tint = Color.LightGray
            )
        }
    }
}

@Composable
fun SafePlaceItem(place: SafePlaceModel, isDarkMode: Boolean) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = if (isDarkMode) Color(0xFF2C2C2C) else Color.White,
        border = BorderStroke(1.dp, Color.LightGray.copy(alpha = 0.5f)),
        elevation = 1.dp
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp), 
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.Home, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color(0xFF4CAF50))
            Spacer(Modifier.width(8.dp))
            Text(place.name, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = if (isDarkMode) Color.White else Color.Black)
        }
    }
}

@Composable
fun BottomNavigationBarDesign(navController: NavController, isDarkMode: Boolean = false) {
    var selectedItem by remember { mutableStateOf(0) }
    Surface(
        elevation = 20.dp,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        color = if (isDarkMode) Color(0xFF1E1E1E) else Color.White
    ) {
        BottomNavigation(
            backgroundColor = Color.Transparent, 
            elevation = 0.dp, 
            modifier = Modifier
                .height(72.dp)
                .navigationBarsPadding()
        ) {
            BottomNavigationItem(
                icon = { Icon(Icons.Default.Security, contentDescription = null) },
                label = { Text("Seguridad", fontSize = 10.sp) }, 
                selected = selectedItem == 0, 
                onClick = { selectedItem = 0 }, 
                selectedContentColor = colorResource(id = R.color.md_purple_800), 
                unselectedContentColor = Color.LightGray
            )
            BottomNavigationItem(
                icon = { Icon(Icons.Default.Map, contentDescription = null) },
                label = { Text("Mapa", fontSize = 10.sp) }, 
                selected = selectedItem == 1, 
                onClick = { selectedItem = 1; navController.navigate(Screens.SafetyMap.route) }, 
                selectedContentColor = colorResource(id = R.color.md_purple_800), 
                unselectedContentColor = Color.LightGray
            )
            BottomNavigationItem(
                icon = { Icon(Icons.Default.AccountCircle, contentDescription = null) }, 
                label = { Text("Perfil", fontSize = 10.sp) }, 
                selected = selectedItem == 2, 
                onClick = { selectedItem = 2; navController.navigate(Screens.Profile.route) }, 
                selectedContentColor = colorResource(id = R.color.md_purple_800), 
                unselectedContentColor = Color.LightGray
            )
        }
    }
}

@Composable
fun CustomizeSOSDialog(viewModel: mainViewModel, onDismiss: () -> Unit) {
    val currentTitle by viewModel.stealthTitle.collectAsState()
    val currentText by viewModel.stealthText.collectAsState()
    val currentIcon by viewModel.stealthIcon.collectAsState()

    var title by remember { mutableStateOf(currentTitle) }
    var text by remember { mutableStateOf(currentText) }
    var selectedIcon by remember { mutableStateOf(currentIcon) }

    val iconOptions = listOf(
        "settings" to Icons.Default.Settings,
        "battery" to Icons.Default.BatteryStd,
        "cloud" to Icons.Default.Cloud,
        "wifi" to Icons.Default.Wifi
    )

    Dialog(onDismissRequest = onDismiss) {
        Card(shape = RoundedCornerShape(28.dp), elevation = 12.dp) {
            Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Default.Tune, contentDescription = null, tint = colorResource(id = R.color.md_purple_700), modifier = Modifier.size(48.dp))
                Spacer(modifier = Modifier.height(16.dp))
                Text("Camuflar SOS", fontWeight = FontWeight.Bold, fontSize = 20.sp)
                Text("Cambia cómo se ve la notificación de emergencia.", fontSize = 12.sp, color = Color.Gray, textAlign = TextAlign.Center)
                Spacer(modifier = Modifier.height(24.dp))

                OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Título falso") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp))
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(value = text, onValueChange = { text = it }, label = { Text("Mensaje falso") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp))

                Spacer(modifier = Modifier.height(16.dp))
                Text("Icono de camuflaje:", modifier = Modifier.align(Alignment.Start), fontSize = 14.sp, fontWeight = FontWeight.Bold)
                Row(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
                    iconOptions.forEach { (name, icon) ->
                        IconButton(
                            onClick = { selectedIcon = name },
                            modifier = Modifier.clip(CircleShape).background(if (selectedIcon == name) colorResource(id = R.color.md_purple_100) else Color.Transparent)
                        ) {
                            Icon(icon, contentDescription = null, tint = if (selectedIcon == name) colorResource(id = R.color.md_purple_700) else Color.Gray)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
                Button(onClick = { viewModel.saveStealthSettings(title, text, selectedIcon); onDismiss() }, modifier = Modifier.fillMaxWidth().height(56.dp), shape = RoundedCornerShape(16.dp), colors = ButtonDefaults.buttonColors(backgroundColor = colorResource(id = R.color.md_purple_700))) {
                    Text("GUARDAR CAMBIOS", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun SaveSafePlaceDialog(onDismiss: () -> Unit, onSave: (String) -> Unit) {
    var name by remember { mutableStateOf("") }
    Dialog(onDismissRequest = onDismiss) {
        Card(shape = RoundedCornerShape(28.dp), elevation = 12.dp) {
            Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Default.AddLocationAlt, contentDescription = null, tint = colorResource(id = R.color.md_purple_700), modifier = Modifier.size(48.dp))
                Spacer(modifier = Modifier.height(16.dp))
                Text("Nuevo Lugar Seguro", fontWeight = FontWeight.Bold, fontSize = 20.sp)
                Spacer(modifier = Modifier.height(24.dp))
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Nombre (Ej: Casa)") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp))
                Spacer(modifier = Modifier.height(24.dp))
                Button(onClick = { if(name.isNotEmpty()) onSave(name) }, modifier = Modifier.fillMaxWidth().height(50.dp), shape = RoundedCornerShape(12.dp), colors = ButtonDefaults.buttonColors(backgroundColor = colorResource(id = R.color.md_purple_700))) {
                    Text("GUARDAR LUGAR", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun AddFriendDialogDesign(onDismiss: () -> Unit, onAdd: (String) -> Unit) {
    var email by remember { mutableStateOf("") }
    Dialog(onDismissRequest = onDismiss) {
        Card(shape = RoundedCornerShape(28.dp), elevation = 12.dp) {
            Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Default.PersonAdd, contentDescription = null, tint = colorResource(id = R.color.md_purple_700), modifier = Modifier.size(48.dp))
                Spacer(modifier = Modifier.height(16.dp))
                Text("Añadir al Círculo", style = TextStyle(fontSize = 22.sp, fontWeight = FontWeight.Bold))
                Spacer(modifier = Modifier.height(24.dp))
                OutlinedTextField(value = email, onValueChange = { email = it }, label = { Text("Correo del amigo") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), colors = TextFieldDefaults.outlinedTextFieldColors(focusedBorderColor = colorResource(id = R.color.md_purple_700)))
                Spacer(modifier = Modifier.height(24.dp))
                Button(onClick = { if (email.isNotEmpty()) onAdd(email) }, modifier = Modifier.fillMaxWidth().height(56.dp), shape = RoundedCornerShape(16.dp), colors = ButtonDefaults.buttonColors(backgroundColor = colorResource(id = R.color.md_purple_700))) {
                    Text("ENVIAR INVITACIÓN", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun AppTourDialog(onDismiss: () -> Unit) {
    var step by remember { mutableStateOf(0) }
    val steps = listOf(
        TourData("Bienvenido a SaveThem", "Tu círculo de seguridad está activo. Veamos cómo funciona.", Icons.Default.WavingHand),
        TourData("Botón SOS", "En una emergencia, presiona el botón SOS. Notifica a todo tu círculo al instante.", Icons.Default.GppBad),
        TourData("Zonas Seguras", "Registra lugares como tu casa. El rastreo se detiene solo al llegar.", Icons.Default.Map),
        TourData("Modo Invisible", "Configura la alerta para que parezca una actualización de sistema.", Icons.Default.VisibilityOff)
    )

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false)
    ) {
        Card(
            shape = RoundedCornerShape(28.dp),
            elevation = 8.dp,
            modifier = Modifier.fillMaxWidth().padding(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier.size(100.dp).clip(CircleShape).background(colorResource(id = R.color.md_purple_50)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(steps[step].icon, contentDescription = null, modifier = Modifier.size(48.dp), tint = colorResource(id = R.color.md_purple_700))
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                Text(steps[step].title, fontWeight = FontWeight.Bold, fontSize = 20.sp, textAlign = TextAlign.Center)
                Spacer(modifier = Modifier.height(12.dp))
                Text(steps[step].description, fontSize = 14.sp, color = Color.Gray, textAlign = TextAlign.Center)
                
                Spacer(modifier = Modifier.height(32.dp))
                
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    TextButton(onClick = onDismiss) { Text("SALTAR", color = Color.Gray) }
                    
                    Button(
                        onClick = { 
                            if (step < steps.size - 1) step++ else onDismiss()
                        },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(backgroundColor = colorResource(id = R.color.md_purple_700))
                    ) {
                        Text(if (step < steps.size - 1) "SIGUIENTE" else "ENTENDIDO", color = Color.White)
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Row {
                    steps.forEachIndexed { index, _ ->
                        Box(
                            modifier = Modifier
                                .padding(4.dp)
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(if (index == step) colorResource(id = R.color.md_purple_700) else Color.LightGray)
                        )
                    }
                }
            }
        }
    }
}

data class TourData(val title: String, val description: String, val icon: ImageVector)
