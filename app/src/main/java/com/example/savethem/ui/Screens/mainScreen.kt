package com.example.savethem.ui.Screens

import android.content.Context
import android.graphics.BitmapFactory
import android.location.Geocoder
import android.location.Location
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.animation.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.savethem.Model.*
import com.example.savethem.R
import com.example.savethem.Repository.UserRepository
import com.example.savethem.ViewModel.FriendsViewModel
import com.example.savethem.ViewModel.mainViewModel
import com.example.savethem.call.boton
import com.example.savethem.call.tokensFriends
import com.example.savethem.navigation.Screens
import com.example.savethem.notification.createNotificationChannel
import com.example.savethem.notification.showSimpleNotificationWithTapAction
import com.example.savethem.util.constants.Notification.CHANNEL
import com.example.savethem.util.constants.Notification.ID
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.ktx.Firebase
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.rememberCameraPositionState
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.*

@Composable
fun add(viewModel: mainViewModel, navController: NavController, friendsViewModel: FriendsViewModel) {

    Scaffold(
        topBar = { TopAppBar(friendsViewModel)},
        content = { MapView(viewModel = viewModel, navController = navController) },
        floatingActionButton = {

        },
        bottomBar = {

        }
    )





}
@Composable
fun list(viewModel: mainViewModel) {
    val markerInfoList by viewModel.markerInfoList.collectAsState(emptyList())
    var stateName by rememberSaveable { mutableStateOf("") }
    val context = LocalContext.current


    val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)
    fusedLocationClient.lastLocation.addOnSuccessListener { location: Location? ->
        if (location != null) {
            // Obtener el nombre del estado a partir de la ubicación actual
            val geoCoder = Geocoder(context, Locale.getDefault())
            val addressList = geoCoder.getFromLocation(location.latitude, location.longitude, 1)
            if (addressList?.isNotEmpty()!!) {
                stateName = addressList?.get(0)!!.adminArea ?: ""
            }
        }
    }


    TextField(
        value = stateName,
        onValueChange = { stateName = it },
        label = { Text("search for location") },
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
    )
    LazyColumn {
        items(markerInfoList) { feed ->
            Column {
                Card(
                    elevation = 7.dp,
                    shape = RoundedCornerShape(7.dp),
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(top = 7.dp, bottom = 7.dp, start = 14.dp, end = 14.dp)
                ) {
                    Row {
                        Column(
                            modifier = Modifier
                                .padding(10.dp)
                                .align(Alignment.CenterVertically)
                        ) {
                            Text(
                                text = feed.title.toString(),
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = feed.gLevel.toString(),
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = feed.position?.longitude.toString(),
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = feed.position?.latitude.toString(),
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
                val gLevel = feed.gLevel.toString()
                if (stateName.isNotBlank() && feed.title!!.contains(stateName, ignoreCase = true)) {
                    if (gLevel == "ALTO") {
                        println("ALTO")
                    }
                }
            }
        }
    }

}



@Composable
fun MapView(viewModel: mainViewModel, navController: NavController) {
    val markerInfoList by viewModel.markerInfoList.collectAsState(emptyList())
    var isMapReady by remember { mutableStateOf(false) }
    var selectedMarkerId by remember { mutableStateOf("") }

    var stateName by rememberSaveable { mutableStateOf("") }
    val context = LocalContext.current

    val channelId = CHANNEL
    val notificationId = ID
    var stopLoop by remember { mutableStateOf(false) }

    var selectedRiskLevel by remember { mutableStateOf("") }
    val riskLevels = listOf("ALTO", "MEDIO", "BAJO")
    var expanded by remember { mutableStateOf(false) }




    val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)

    fusedLocationClient.lastLocation.addOnSuccessListener { location: Location? ->
        if (location != null) {
            // Obtener el nombre del estado a partir de la ubicación actual
            val geoCoder = Geocoder(context, Locale.getDefault())
            val addressList = geoCoder.getFromLocation(location.latitude, location.longitude, 1)
            if (addressList?.isNotEmpty()!!) {
                stateName = addressList?.get(0)!!.adminArea ?: ""
            }
        }
    }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colorResource(id = R.color.md_pink_A200))
    ) {

        Column() {

//            // Filtra los marcadores en base al nivel de riesgo seleccionado
            val filteredMarkers = if (selectedRiskLevel.isNotEmpty()) {
                markerInfoList.filter { it.gLevel == selectedRiskLevel }
            } else {
                markerInfoList
            }

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight()
                    .padding(10.dp),
                elevation = 18.dp,
                shape = RoundedCornerShape(25.dp),
                backgroundColor = Color.DarkGray
            ) {
                GoogleMap(
                    modifier = Modifier
                        .fillMaxSize()
                        .onGloballyPositioned {
                            isMapReady = true
                        },
                ) {
                    if (isMapReady) {

                        filteredMarkers.forEach { markerInfo ->
                            val gLevel = markerInfo.gLevel.toString()

                            // Filtrar markers en base al nivel de riesgo seleccionado
                            if (selectedRiskLevel.isNotEmpty() && gLevel != selectedRiskLevel) {
                                return@forEach // Saltar a la siguiente iteración
                            }


                            Marker(
                                state = MarkerState(position = markerInfo.position!!),
                                title = markerInfo.title,
                                snippet = markerInfo.gLevel,
                                onClick = {
                                    selectedMarkerId = markerInfo.ID!!
                                    true
                                },
                                visible = selectedRiskLevel.isEmpty() || gLevel == selectedRiskLevel,
                            )

                        }
                    }
                }
            }


        }
    }
    LaunchedEffect(selectedMarkerId) {
        if (selectedMarkerId.isNotEmpty()) {
            navController.navigate(Screens.Details.route + "/$selectedMarkerId")
        }
    }

}



@OptIn(ExperimentalMaterialApi::class)
@Composable
fun TopAppBar(friendsViewModel: FriendsViewModel) {
    var searchText by remember { mutableStateOf("") }
    var isSearchExpanded by remember { mutableStateOf(false) }
    var expandedMenu by remember { mutableStateOf(false) }
//    var idToFriend = ""
    val focusRequester = remember { FocusRequester() }

    TopAppBar(
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (isSearchExpanded) {
                    AnimatedVisibility(
                        visible = isSearchExpanded,
                        enter = fadeIn() + slideInHorizontally(),
                        exit = fadeOut() + slideOutHorizontally()
                    ) {
                        OutlinedTextField(
                            value = searchText,
                            onValueChange = { searchText = it },
                            placeholder = { Text(text = "Add") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .focusRequester(focusRequester),
                            textStyle = TextStyle(color = Color.Black, fontSize = 14.sp),
                            colors = TextFieldDefaults.outlinedTextFieldColors(
                                cursorColor = Color.Black,
                                focusedBorderColor = Color.Transparent,
                                unfocusedBorderColor = Color.Transparent
                            ),
                            keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Done),
                            keyboardActions = KeyboardActions(
                                onDone = {
                                    val currentUserId = FirebaseAuth.getInstance().currentUser
                                    friendsViewModel.addFriend(registerModel(), searchText)
//                                    // Ocultar el teclado después de enviar el texto
                                    focusRequester.freeFocus()
//                                    val friend = registerModel(
//                                        searchText, // Debes manejar el ID correctamente
//                                        name = "", // Deberías obtener el nombre y otros datos del amigo aquí
//                                        UUID = "",
//                                        token = ""
//                                    )
//                                    friendsViewModel.addFriend(1, friend)
                                    focusRequester.freeFocus()
                                    searchText = ""
                                    isSearchExpanded = false
                                }
                            )
                        )
                    }
                } else {
                    Box(
                        modifier = Modifier.padding(start = 5.dp)
                    ) {
                        BasicText(
                            text = "WomenSafe",
                            style = TextStyle(
                                fontFamily = FontFamily(Font(R.font.signikasemibold)),
                                fontSize = 25.sp,
                                fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                                color = colorResource(id = R.color.md_pink_50) // Cambia el color aquí
                            ),
                            modifier = Modifier
                                .padding(5.dp)
                                .graphicsLayer(
                                    scaleX = 1.1f // Ajusta este valor para estirar más o menos horizontalmente
                                )
                        )
                    }

                }
//                Button(onClick = {
//                    friendsViewModel.addFriend(registerModel(), "alexisgalindo108@gmail.com")
//                }) {
//                    Text(text = "add")
//                }
            }
        },

        actions = {
            Row() {
                IconButton(onClick = { isSearchExpanded = !isSearchExpanded }) {
                    Icon(
                        imageVector = if (isSearchExpanded) Icons.Default.Close else Icons.Default.Search,
                        contentDescription = "Search",
                        tint = colorResource(id = R.color.md_purple_800)
                    )
                }
                IconButton(onClick = { expandedMenu = true }) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = "",
                    tint = colorResource(id = R.color.md_purple_800)
                    )
                    DropdownMenu(
                        expanded = expandedMenu,
                        onDismissRequest = { expandedMenu = false },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        // Crea el botón dentro del menú desplegable
                        Box(
                            modifier = Modifier.height(56.dp)
                        ) {
                            Button(
                                onClick = { expandedMenu = true },
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(
                                    backgroundColor = Color.DarkGray, // Cambia el color de fondo del botón
                                    contentColor = Color.White // Cambia el color del contenido del botón (texto/icono)
                                ),
                                content = {
                                    Text("")
                                }
                            )
                        }

                        // Agrega los elementos del menú desplegable
                        DropdownMenuItem(onClick = {

                        }) {
                            Text("ALTO")
                        }
                        DropdownMenuItem(onClick = {
//                            selectedRiskLevel = "MEDIO"
//                            expanded = false
                        }) {
                            Text("MEDIO")
                        }
                        DropdownMenuItem(onClick = {
//                            selectedRiskLevel = "BAJO"
//                            expanded = false
                        }) {
                            Text("BAJO")
                        }
                    }

                }
            }
        },
        modifier = Modifier.height(52.dp),
        backgroundColor = colorResource(id = R.color.md_purple_200),
        elevation = 0.dp
    )
}



@OptIn(ExperimentalMaterialApi::class)
@Composable
fun friendList(viewModel: mainViewModel, navController: NavController, friendsViewModel: FriendsViewModel) {
    val sheetState = rememberModalBottomSheetState(ModalBottomSheetValue.Hidden)
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    FirebaseApp.initializeApp(context)

    ModalBottomSheetLayout(
            sheetState = sheetState,
            sheetContent = {
                // Contenido de la pantalla emergente
                Card(
                    modifier = Modifier
                        .padding(top = 30.dp, bottom = 30.dp, start = 14.dp, end = 14.dp),
                    elevation = 8.dp,
                    shape = RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp, bottomEnd = 8.dp, bottomStart = 8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.White)
                    ) {
                        var id = ""

//                        tokensFriends(friendsViewModel = friendsViewModel, navController = navController)

//                        boton(friendsViewModel = friendsViewModel, navController)
                    }
                }
            },
            sheetBackgroundColor = Color.Transparent,

            content = {
                // Contenido de la pantalla principal
                Scaffold(
                    topBar = { TopAppBar(friendsViewModel) },
                    content = {
                        // Contenido principal
                        boton(friendsViewModel = friendsViewModel, navController)
                    },
                    bottomBar = {BottomNavigationBar(navController)}
                )
            }
        )
}



@Composable
fun BottomNavigationBar(navController: NavController) {
    var selectedItem by remember { mutableStateOf(0) }

    BottomNavigation(
        backgroundColor = Color.Transparent,
        elevation = 0.dp // Elimina la sombra
    ) {
        BottomNavigationItem(
            icon = { Icon(imageVector = Icons.Default.Home, contentDescription = "Home") },
            label = { Text("Home") },
            selected = selectedItem == 0,
            onClick = {
                selectedItem = 0
                // Navegar a la pantalla de inicio
            },
            selectedContentColor = colorResource(id = R.color.md_purple_800),
            unselectedContentColor = Color.Gray
        )
        BottomNavigationItem(
            icon = { Icon(imageVector = Icons.Default.AccountCircle, contentDescription = "Profile") },
            label = { Text("Profile") },
            selected = selectedItem == 2,
            onClick = {
                selectedItem = 2
                // Navegar a la pantalla de perfil
            },
            selectedContentColor = colorResource(id = R.color.md_purple_800),
            unselectedContentColor = Color.Gray
        )
    }
}



