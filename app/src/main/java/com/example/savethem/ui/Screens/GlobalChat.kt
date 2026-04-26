package com.example.savethem.ui.Screens

import android.annotation.SuppressLint
import android.location.Location
import android.util.Log
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
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
import com.google.android.gms.location.LocationServices
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.messaging.FirebaseMessaging
import java.text.SimpleDateFormat
import java.util.*

@SuppressLint("UnusedMaterialScaffoldPaddingParameter")
@Composable
fun GlobalChatMainScreen(chatViewModel: ChatViewModel, navController: NavController) {
	Scaffold(
		topBar = { TopAppBarGlobalChat( chatViewModel = chatViewModel, navController)},
		content = { GlobalChatScreen(chatViewModel = chatViewModel)}
	)
}

@SuppressLint("MissingPermission")
@Composable
fun GlobalChatScreen(chatViewModel: ChatViewModel) {
	val selectedFriend by chatViewModel.selectedFriend.collectAsState(null)
	val messageList by chatViewModel.addMessage.collectAsState()
	val context = LocalContext.current

	LaunchedEffect(Unit) {
		chatViewModel.getGlobalChatMessage()
	}

	var isButtonVisible by remember { mutableStateOf(false) }
	var comment by remember { mutableStateOf("") }

	Box(modifier = Modifier.fillMaxSize()) {
		Column(modifier = Modifier.fillMaxSize()) {
			Box(modifier = Modifier.weight(1f)) {
				GlobalChatList(chatViewModel = chatViewModel)
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
							.padding(horizontal = 15.dp),
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
										val idToFriend = FirebaseAuth.getInstance().currentUser?.uid
										val messageID = UUID.randomUUID().toString()
										val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)
										fusedLocationClient.lastLocation.addOnSuccessListener { location: Location? ->
											location?.let {
												if (it.latitude != 0.0 && it.longitude != 0.0) {
													chatViewModel.addLocation(
														LocationModel(
															location = LatLngWrapper(it.latitude, it.longitude)
														),
														selectedFriend?.UUID ?: "",
														messageID,
														idToFriend ?: ""
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
												messageID
											)
											isButtonVisible = false
											comment = ""
											FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
												if (task.isSuccessful) {
													Log.d("FCM", task.result)
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

		items(message) { msg ->
			val isMe = msg.UUIDSender == currentUser?.uid
			val cardShape = if (isMe) {
				RoundedCornerShape(topStart = 8.dp, topEnd = 18.dp, bottomStart = 18.dp)
			} else {
				RoundedCornerShape(topStart = 18.dp, topEnd = 8.dp, bottomEnd = 18.dp)
			}
			val pad = if (isMe) {
				PaddingValues(top = 7.dp, bottom = 7.dp, start = 150.dp, end = 14.dp)
			} else {
				PaddingValues(top = 7.dp, bottom = 7.dp, start = 14.dp, end = 150.dp)
			}

			Box(
				modifier = Modifier.fillMaxWidth(),
				contentAlignment = if (isMe) Alignment.CenterEnd else Alignment.CenterStart
			) {
				Card(
					modifier = Modifier.padding(pad),
					elevation = 8.dp,
					shape = cardShape,
					backgroundColor = if (isMe) colorResource(id = R.color.md_pink_100) else colorResource(id = R.color.md_pink_A100)
				) {
					Column(modifier = Modifier.padding(5.dp)) {
						Text(
							text = msg.message,
							color = colorResource(id = R.color.md_purple_900),
							textAlign = TextAlign.Start,
							style = TextStyle(
								fontFamily = FontFamily(Font(R.font.josefinsanslight)),
								fontSize = 16.sp,
								fontWeight = FontWeight.Bold
							)
						)
						val time = msg.timestamp
						val formattedTime = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(time))

						Row(verticalAlignment = Alignment.CenterVertically) {
							Text(
								text = formattedTime,
								fontWeight = FontWeight.Thin,
								color = colorResource(id = R.color.md_purple_800),
								style = TextStyle(
									fontFamily = FontFamily(Font(R.font.josefinsanslight)),
									fontSize = 12.sp,
									fontWeight = FontWeight.Bold
								),
								modifier = Modifier.padding(start = 5.dp, bottom = 3.dp)
							)
						}
					}
				}
			}
		}
	}
}

@Composable
fun TopAppBarGlobalChat(chatViewModel: ChatViewModel, navController: NavController) {
	val data by chatViewModel.user.collectAsState()
	LaunchedEffect(Unit) {
		chatViewModel.getUserData()
	}
	TopAppBar(
		navigationIcon = {
			IconButton(onClick = { navController.popBackStack() }) {
				Icon(
					painter = painterResource(id = R.drawable.arrowback),
					contentDescription = null,
					tint = colorResource(id = R.color.md_purple_800)
				)
			}
		},
		title = { Text(text = data.toString(), textAlign = TextAlign.End) },
		actions = {
			IconButton(onClick = { }) {
				Icon(
					painter = painterResource(id = R.drawable.menu),
					contentDescription = null,
					tint = colorResource(id = R.color.md_purple_800)
				)
			}
		},
		modifier = Modifier.height(52.dp),
		backgroundColor = colorResource(id = R.color.md_purple_200),
		elevation = 0.dp,
	)
}