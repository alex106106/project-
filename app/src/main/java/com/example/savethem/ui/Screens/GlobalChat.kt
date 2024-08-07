package com.example.savethem.ui.Screens

import android.content.Context
import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.location.Geocoder
import android.location.Location
import android.util.Log
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.savethem.Model.ChatModel
import com.example.savethem.Model.LatLngWrapper
import com.example.savethem.Model.LocationModel
import com.example.savethem.R
import com.example.savethem.ViewModel.ChatViewModel
import com.example.savethem.call.enviar
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.messaging.FirebaseMessaging
import com.google.maps.android.compose.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*


@Composable
fun GlobalChatMainScreen(chatViewModel: ChatViewModel, navController: NavController) {
	Scaffold(
		topBar = { TopAppBarGlobalChat( chatViewModel = chatViewModel, navController)},
		content = { GlobalChatScreen(chatViewModel = chatViewModel)}
	)
}



@Composable
fun GlobalChatScreen( chatViewModel: ChatViewModel) {
	val selectedFriend by chatViewModel.selectedFriend.collectAsState(null)
	val messageList by chatViewModel.addMessage.collectAsState()
	val locationList by chatViewModel.locationById.collectAsState(emptyList()) // Asegúrate de observar locationById
	val idToFriend = FirebaseAuth.getInstance().currentUser?.uid
	val context = LocalContext.current


	LaunchedEffect(Unit) {
		chatViewModel.getGlobalChatMessage()
	}

	var isButtonVisible by remember { mutableStateOf(false) }
	var comment by remember { mutableStateOf("") }

	Box(modifier = Modifier.fillMaxSize()) {
		Column(modifier = Modifier.fillMaxSize()) {
//			messageList?.let {
//				if (it.isNotEmpty()) {
//					ExpandableCard(chatViewModel = chatViewModel, context = context, locations = locationList)
//					Log.d("LOCATION", "$locationList")
//				}
//			}

			Box(modifier = Modifier.weight(1f)) {
				GlobalChatList(
					chatViewModel = chatViewModel
				)
			}

			Spacer(modifier = Modifier.height(60.dp))
		}

		Box(
			modifier = Modifier
				.fillMaxSize()
				.padding(bottom = 15.dp),
			contentAlignment = Alignment.BottomCenter
		) {
			Card {
				Column {
					OutlinedTextField(
						modifier = Modifier
							.fillMaxWidth()
							.padding(start = 15.dp, end = 15.dp),
						value = comment,
						onValueChange = {
							comment = it
							isButtonVisible = it.isNotEmpty()
						},
						label = { Text("Message") },
						trailingIcon = {
							Row {
								IconButton(
									onClick = {
										messageList?.forEach { message ->
											println("*********** ${message.IDMessage.toString()}")
										}
										val idToFriend = FirebaseAuth.getInstance().currentUser?.uid
										val messageID = UUID.randomUUID().toString()
										val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)
										fusedLocationClient.lastLocation.addOnSuccessListener { location: Location? ->
											location?.let {
												val latitude = it.latitude
												val longitude = it.longitude

												if (latitude != 0.0 && longitude != 0.0) {
													val locationWrapper = LatLngWrapper(latitude, longitude)
													chatViewModel.addLocation(
														LocationModel(
															"",
															"",
															location = locationWrapper
														),
														selectedFriend?.UUID.toString(),
														messageID,
														idToFriend.toString()
													)
												}
											}
										}
									}
								) { }

								AnimatedVisibility(
									visible = isButtonVisible,
									enter = fadeIn(),
									exit = fadeOut()
								) {
									IconButton(
										onClick = {
											val messageID = UUID.randomUUID().toString()
											chatViewModel.addMessageGlobal(
												ChatModel(message = comment),
//												idToFriend.toString(),
//												selectedFriend?.UUID.toString(),
												messageID
											)
											isButtonVisible = false
											comment = ""
											FirebaseMessaging.getInstance().token
												.addOnCompleteListener { task ->
													if (task.isSuccessful) {
														val token = task.result
														Log.d("TOKEN del usuario", token)
													} else {
														Log.e(
															"TOKEN del usuario",
															"Error al obtener el token: ${task.exception}"
														)
													}
												}
										}
									) {
										Icon(
											painter = painterResource(id = R.drawable.send),
											contentDescription = "SEND",
											tint = colorResource(id = R.color.md_purple_800)
										)
									}
								}
							}
						},
						colors = TextFieldDefaults.outlinedTextFieldColors(
							focusedBorderColor = colorResource(id = R.color.md_purple_800),
							unfocusedBorderColor = colorResource(id = R.color.md_purple_300),
							textColor = colorResource(id = R.color.md_purple_900),
							cursorColor = colorResource(id = R.color.md_purple_900),
							placeholderColor = colorResource(id = R.color.md_purple_300),
							focusedLabelColor = colorResource(id = R.color.md_purple_900),
							unfocusedLabelColor = colorResource(id = R.color.md_purple_300)
						)
					)
				}
			}
		}
	}
}



@Composable
fun GlobalChatList(chatViewModel: ChatViewModel) {
	val message by chatViewModel.addMessage.collectAsState(emptyList())
	val id = FirebaseAuth.getInstance().currentUser
	val listState = rememberLazyListState()

	LaunchedEffect(message) {
		if (message.isNotEmpty()) {
			listState.scrollToItem(message.size - 1)
		}
	}

	LazyColumn(
		state = listState,
		modifier = Modifier
			.fillMaxSize()
			.background(Color.White)
			.padding(bottom = 16.dp),
		verticalArrangement = Arrangement.spacedBy(4.dp)
	) {
		val currentUser = FirebaseAuth.getInstance().currentUser

		items(message) { message ->
			val cardShape = if (message.UUIDSender == currentUser?.uid) {
				RoundedCornerShape(topStart = 8.dp, topEnd = 18.dp, bottomStart = 18.dp)
			} else {
				RoundedCornerShape(topStart = 18.dp, topEnd = 8.dp, bottomEnd = 18.dp)
			}
			val pad = if (message.UUIDSender == currentUser?.uid) {
				PaddingValues(top = 7.dp, bottom = 7.dp, start = 150.dp, end = 14.dp)
			} else {
				PaddingValues(top = 7.dp, bottom = 7.dp, start = 14.dp, end = 150.dp)
			}

			Box(
				modifier = Modifier.fillMaxWidth(),
				contentAlignment = if (message.UUIDSender == currentUser?.uid) Alignment.CenterEnd else Alignment.CenterStart
			) {
				Card(
					modifier = Modifier.padding(pad),
					elevation = 8.dp,
					shape = cardShape,
					backgroundColor = if (message.UUIDSender == currentUser?.uid) colorResource(id = R.color.md_pink_100) else colorResource(id = R.color.md_pink_A100)
				) {
					Column(modifier = Modifier.padding(5.dp)) {
						Text(
							text = message.message.toString(),
							color = colorResource(id = R.color.md_purple_900),
							textAlign = TextAlign.Start,
							modifier = Modifier
								.padding(5.dp, top = 0.dp)
								.background(
									if (message.UUIDSender == currentUser?.uid) colorResource(
										id = R.color.md_pink_100
									) else colorResource(id = R.color.md_pink_A100)
								),
							style = TextStyle(
								fontFamily = FontFamily(Font(R.font.josefinsanslight)),
								fontSize = 16.sp,
								fontWeight = FontWeight.Bold
							)
						)
						val time = message.timestamp ?: 0L
						val date = Date(time)
						val dateFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
						val formattedTime = dateFormat.format(date)

						Row(
							verticalAlignment = Alignment.CenterVertically,
							modifier = Modifier.background(if (message.UUIDSender == currentUser?.uid) colorResource(id = R.color.md_pink_100) else colorResource(id = R.color.md_pink_A100))
						) {
							Text(
								text = formattedTime,
								fontWeight = FontWeight.Thin,
								color = colorResource(id = R.color.md_purple_800),
								textAlign = TextAlign.Start,
								style = TextStyle(
									fontFamily = FontFamily(Font(R.font.josefinsanslight)),
									fontSize = 12.sp,
									fontWeight = FontWeight.Bold
								),
								modifier = Modifier.padding(start = 5.dp, bottom = 3.dp)
							)

//							if (message.UUIDSender == currentUser?.uid && message.seen == true) {
//								Icon(
//									painter = painterResource(id = R.drawable.like),
//									contentDescription = "Message Seen",
//									modifier = Modifier
//										.size(16.dp)
//										.padding(start = 3.dp),
//									tint = colorResource(id = R.color.md_pink_900)
//								)
//							}
						}
					}
				}
			}

//			if (message.UUIDSender != currentUser?.uid && message.seen == false) {
//				LaunchedEffect(message.IDMessage) {
//					chatViewModel.markMessageAsSeen(currentUser?.uid.toString(),
//						id.toString(), message.IDMessage)
//					Log.d("GlobalChatList", "Mensaje marcado como visto: ${message.IDMessage}")
//				}
//			}
		}
	}
}

@Composable
fun TopAppBarGlobalChat(chatViewModel: ChatViewModel, navController: NavController) {
//	val selectedFriend by chatViewModel.selectedFriend.collectAsState(null)
//	chatViewModel.getUserData(id)
	val data by chatViewModel.name.collectAsState()
	LaunchedEffect(Unit) {
		chatViewModel.getUserData()
	}
//	Log.d("NAME", selectedFriend?.name.toString())
	androidx.compose.material.TopAppBar(
		navigationIcon = {
			IconButton(
				onClick = {
					navController.popBackStack()
				}) {
				Icon(
					painter = painterResource(id = R.drawable.arrowback),
					contentDescription = "",
					tint = colorResource(id = R.color.md_purple_800)
				)
			}
		},
		title = {
			Text(
				text = data,
				textAlign = TextAlign.End,
			)
		},
		actions = {
			IconButton(onClick = { /* Acción del segundo IconButton */ }) {
				Icon(
					painter = painterResource(id = R.drawable.menu),
					contentDescription = "",
					tint = colorResource(id = R.color.md_purple_800)
				)
			}
		},
		modifier = Modifier
			.height(52.dp),
		backgroundColor = colorResource(id = R.color.md_purple_200),
		elevation = 0.dp,

		)

}