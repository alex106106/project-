package com.example.savethem.ui.Screens

import android.content.Intent
import android.os.Build
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.navigation.NavController
import coil.compose.rememberAsyncImagePainter
import com.example.savethem.Model.SafePlaceModel
import com.example.savethem.Model.registerModel
import com.example.savethem.R
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
    
    val context = LocalContext.current
    
    val activeEmergencies by viewModel.activeEmergencies.collectAsState()
    val friends by friendsViewModel.friends.collectAsState()
    val safePlaces by viewModel.safePlaces.collectAsState()
    val userName by viewModel.name.collectAsState()
    val isDarkMode by viewModel.isDarkMode.collectAsState()
    val userLocation by viewModel.currentUserLocation.collectAsState()
    
    // Control del Tour
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

    // MOSTRAR EL TOUR SI ES LA PRIMERA VEZ
    if (showTour) {
        AppTourDialog(onDismiss = { viewModel.dismissTour() })
    }

    Scaffold(
        backgroundColor = if (isDarkMode) Color(0xFF121212) else Color(0xFFF8F9FA),
        topBar = {
            Surface(elevation = 2.dp, color = if (isDarkMode) Color(0xFF1E1E1E) else Color.White) {
                Row(
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp).fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Active Protection", style = TextStyle(fontSize = 12.sp, color = if (isDarkMode) Color.LightGray else Color.Gray))
                        Text(text = userName.ifEmpty { "Welcome" }, style = TextStyle(fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, color = if (isDarkMode) Color.White else colorResource(id = R.color.md_purple_900)))
                    }
                    
                    Row {
                        // Botón para repetir el tour si el usuario quiere
                        IconButton(onClick = { viewModel.dismissTour() }) {
                            Icon(Icons.Default.Home, contentDescription = "Help", tint = if (isDarkMode) Color.White else colorResource(id = R.color.md_purple_800))
                        }
                        IconButton(onClick = { showSOSConfigDialog = true }) {
                            Icon(Icons.Default.Settings, contentDescription = "SOS Settings", tint = if (isDarkMode) Color.White else colorResource(id = R.color.md_purple_800))
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
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
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
                    SafePlacePromptCard(onClick = { 
                        viewModel.updateUserLocation()
                        showSafePlaceDialog = true 
                    }, isDarkMode)
                }

                if (safePlaces.isNotEmpty()) {
                    item {
                        Text("Your Safe Zones", style = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = if (isDarkMode) Color.LightGray else Color.Gray))
                        LazyRow(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(safePlaces) { place -> SafePlaceItem(place, isDarkMode) }
                        }
                    }
                }

                item {
                    Text("Recent Contacts", style = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = if (isDarkMode) Color.LightGray else Color.Gray))
                }

                items(friends) { friend ->
                    FriendListItem(friend, navController, isDarkMode)
                }
                
                item { 
                    IconButton(onClick = { showAddDialog = true }, modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Add, contentDescription = null, tint = colorResource(id = R.color.md_purple_800))
                            Spacer(Modifier.width(8.dp))
                            Text("Add new contact", color = colorResource(id = R.color.md_purple_800))
                        }
                    }
                }
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
                        Toast.makeText(context, "Safe place saved: $name", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(context, "Wait for GPS signal...", Toast.LENGTH_LONG).show()
                        viewModel.updateUserLocation()
                    }
                    showSafePlaceDialog = false
                })
            }
        }
    }
}

@Composable
fun AppTourDialog(onDismiss: () -> Unit) {
    var step by remember { mutableStateOf(0) }
    val steps = listOf(
        TourData("Welcome to SaveThem", "Your safety circle is now active. Let's see how it works.", Icons.Default.Build),
        TourData("Activate SOS", "In an emergency, press the SOS button. It notifies your entire circle immediately.", Icons.Default.Warning),
        TourData("Safe Zones", "Register locations like your home or work. Tracking stops automatically when you arrive.", Icons.Default.Home),
        TourData("Stealth Mode", "Go to settings to make your SOS notification look like a regular system update.", Icons.Default.Settings)
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
                
                Text(steps[step].title, fontWeight = FontWeight.Bold, fontSize = 22.sp, textAlign = TextAlign.Center)
                Spacer(modifier = Modifier.height(12.dp))
                Text(steps[step].description, fontSize = 14.sp, color = Color.Gray, textAlign = TextAlign.Center)
                
                Spacer(modifier = Modifier.height(32.dp))
                
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    TextButton(onClick = onDismiss) { Text("SKIP", color = Color.Gray) }
                    
                    Button(
                        onClick = { 
                            if (step < steps.size - 1) step++ else onDismiss()
                        },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(backgroundColor = colorResource(id = R.color.md_purple_700))
                    ) {
                        Text(if (step < steps.size - 1) "NEXT" else "GOT IT!", color = Color.White)
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
        "battery" to Icons.Default.Person,
        "cloud" to Icons.Default.AccountBox,
        "wifi" to Icons.Default.AccountCircle
    )

    Dialog(onDismissRequest = onDismiss) {
        Card(shape = RoundedCornerShape(28.dp), elevation = 12.dp) {
            Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Default.Notifications, contentDescription = null, tint = colorResource(id = R.color.md_purple_700), modifier = Modifier.size(48.dp))
                Spacer(modifier = Modifier.height(16.dp))
                Text("Customize SOS Notify", fontWeight = FontWeight.Bold, fontSize = 20.sp)
                Text("Edit the notification title, text, and icon.", fontSize = 12.sp, color = Color.Gray, textAlign = TextAlign.Center)
                Spacer(modifier = Modifier.height(24.dp))

                OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Notification Title") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp))
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(value = text, onValueChange = { text = it }, label = { Text("Notification Content") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp))

                Spacer(modifier = Modifier.height(16.dp))
                Text("Select Fake Icon:", modifier = Modifier.align(Alignment.Start), fontSize = 14.sp, fontWeight = FontWeight.Bold)
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
                    Text("SAVE SETTINGS", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun SafePlacePromptCard(onClick: () -> Unit, isDarkMode: Boolean) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable { onClick() }, 
        shape = RoundedCornerShape(16.dp), 
        elevation = 2.dp, 
        backgroundColor = if (isDarkMode) Color(0xFF1E1E1E) else Color.White
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(40.dp).clip(CircleShape).background(if (isDarkMode) Color(0xFF2C2C2C) else colorResource(id = R.color.md_purple_50)), contentAlignment = Alignment.Center) {
                Icon(Icons.Default.Home, contentDescription = null, tint = colorResource(id = R.color.md_purple_700))
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text("Is this a safe place?", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = if (isDarkMode) Color.White else Color.Black)
                Text("Save this location to auto-stop tracking.", fontSize = 12.sp, color = if (isDarkMode) Color.LightGray else Color.Gray)
            }
        }
    }
}

@Composable
fun SafePlaceItem(place: SafePlaceModel, isDarkMode: Boolean) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = if (isDarkMode) Color(0xFF1E1E1E) else Color.White,
        border = BorderStroke(1.dp, if (isDarkMode) Color(0xFF2C2C2C) else colorResource(id = R.color.md_purple_100)),
        modifier = Modifier.padding(4.dp)
    ) {
        Row(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp), tint = colorResource(id = R.color.md_purple_700))
            Spacer(Modifier.width(8.dp))
            Text(place.name, fontSize = 12.sp, fontWeight = FontWeight.Medium, color = if (isDarkMode) Color.White else Color.Black)
        }
    }
}

@Composable
fun SaveSafePlaceDialog(onDismiss: () -> Unit, onSave: (String) -> Unit) {
    var name by remember { mutableStateOf("") }
    Dialog(onDismissRequest = onDismiss) {
        Card(shape = RoundedCornerShape(28.dp), elevation = 12.dp) {
            Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Default.AddCircle, contentDescription = null, tint = colorResource(id = R.color.md_purple_700), modifier = Modifier.size(48.dp))
                Spacer(modifier = Modifier.height(16.dp))
                Text("Register Safe Place", fontWeight = FontWeight.Bold, fontSize = 20.sp)
                Spacer(modifier = Modifier.height(24.dp))
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Place Name") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp))
                Spacer(modifier = Modifier.height(24.dp))
                Button(onClick = { if(name.isNotEmpty()) onSave(name) }, modifier = Modifier.fillMaxWidth().height(50.dp), shape = RoundedCornerShape(12.dp), colors = ButtonDefaults.buttonColors(backgroundColor = colorResource(id = R.color.md_purple_700))) {
                    Text("SAVE LOCATION", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun EmergencyControlCard(isActive: Boolean, onClick: () -> Unit) {
    val brush = if (isActive) Brush.linearGradient(listOf(Color.White, Color.White))
                else Brush.horizontalGradient(listOf(Color(0xFFFF5252), Color(0xFFFF1744)))
    Card(modifier = Modifier.fillMaxWidth().clickable { onClick() }, shape = RoundedCornerShape(24.dp), elevation = 6.dp) {
        Row(modifier = Modifier.background(brush).padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(52.dp).clip(CircleShape).background(if (isActive) Color.Red.copy(alpha = 0.1f) else Color.White.copy(alpha = 0.2f)), contentAlignment = Alignment.Center) {
                Icon(if (isActive) Icons.Default.LocationOn else Icons.Default.Warning, contentDescription = null, tint = if (isActive) Color.Red else Color.White, modifier = Modifier.size(28.dp))
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(if (isActive) "STOP SOS" else "ACTIVATE SOS", fontWeight = FontWeight.Black, fontSize = 18.sp, color = if (isActive) Color.Red else Color.White)
                Text(if (isActive) "Tracking is currently active" else "Notify all friends immediately", fontSize = 12.sp, color = if (isActive) Color.Gray else Color.White.copy(alpha = 0.8f) )
            }
        }
    }
}

@Composable
fun FriendListItem(friend: registerModel, navController: NavController, isDarkMode: Boolean) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable { navController.navigate(Screens.Chat.route + "/${friend.UUID}") }, 
        shape = RoundedCornerShape(20.dp), 
        elevation = 0.dp, 
        backgroundColor = if (isDarkMode) Color(0xFF1E1E1E) else Color.White
    ) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Surface(modifier = Modifier.size(54.dp), shape = CircleShape, color = if (isDarkMode) Color(0xFF2C2C2C) else colorResource(id = R.color.md_purple_50)) {
                Box(contentAlignment = Alignment.Center) { Text((friend.name?.take(1) ?: "U").uppercase(), fontWeight = FontWeight.Bold, color = colorResource(id = R.color.md_purple_800), fontSize = 20.sp) }
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(friend.name ?: "User", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = if (isDarkMode) Color.White else Color.Black)
                Text(friend.email ?: "", fontSize = 13.sp, color = if (isDarkMode) Color.LightGray else Color.Gray, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Icon(Icons.Default.Email, contentDescription = null, modifier = Modifier.size(20.dp), tint = if (isDarkMode) Color.Gray else colorResource(id = R.color.md_purple_100))
        }
    }
}

@Composable
fun BottomNavigationBarDesign(navController: NavController, isDarkMode: Boolean = false) {
    var selectedItem by remember { mutableStateOf(0) }
    BottomNavigation(
        backgroundColor = if (isDarkMode) Color(0xFF1E1E1E) else Color.White, 
        elevation = 16.dp, 
        modifier = Modifier.height(70.dp).clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
    ) {
        BottomNavigationItem(icon = { Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null) }, label = { Text("Chats") }, selected = selectedItem == 0, onClick = { selectedItem = 0 }, selectedContentColor = colorResource(id = R.color.md_purple_800), unselectedContentColor = Color.LightGray)
        BottomNavigationItem(icon = { Icon(Icons.Default.Info, contentDescription = null) }, label = { Text("Security") }, selected = selectedItem == 1, onClick = { selectedItem = 1; navController.navigate(Screens.SafetyMap.route) }, selectedContentColor = colorResource(id = R.color.md_purple_800), unselectedContentColor = Color.LightGray)
        BottomNavigationItem(icon = { Icon(Icons.Default.AccountCircle, contentDescription = null) }, label = { Text("Profile") }, selected = selectedItem == 2, onClick = { selectedItem = 2; navController.navigate(Screens.Profile.route) }, selectedContentColor = colorResource(id = R.color.md_purple_800), unselectedContentColor = Color.LightGray)
    }
}

@Composable
fun AddFriendDialogDesign(onDismiss: () -> Unit, onAdd: (String) -> Unit) {
    var email by remember { mutableStateOf("") }
    Dialog(onDismissRequest = onDismiss) {
        Card(shape = RoundedCornerShape(28.dp), elevation = 12.dp) {
            Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Default.Person, contentDescription = null, tint = colorResource(id = R.color.md_purple_700), modifier = Modifier.size(48.dp))
                Spacer(modifier = Modifier.height(16.dp))
                Text("Add Contact", style = TextStyle(fontSize = 22.sp, fontWeight = FontWeight.Bold))
                Spacer(modifier = Modifier.height(24.dp))
                OutlinedTextField(value = email, onValueChange = { email = it }, label = { Text("Friend's Email") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), colors = TextFieldDefaults.outlinedTextFieldColors(focusedBorderColor = colorResource(id = R.color.md_purple_700)))
                Spacer(modifier = Modifier.height(24.dp))
                Button(onClick = { if (email.isNotEmpty()) onAdd(email) }, modifier = Modifier.fillMaxWidth().height(56.dp), shape = RoundedCornerShape(16.dp), colors = ButtonDefaults.buttonColors(backgroundColor = colorResource(id = R.color.md_purple_700))) {
                    Text("ADD TO CIRCLE", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
