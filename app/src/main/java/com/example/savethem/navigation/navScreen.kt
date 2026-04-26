package com.example.savethem.navigation

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.example.savethem.ViewModel.*
import com.example.savethem.ui.Screens.*
import com.example.savethem.util.constants.Screens.CALL_SCREEN
import com.example.savethem.util.constants.Screens.CHAT_SCREEN
import com.example.savethem.util.constants.Screens.DETAILS_SCREEN
import com.example.savethem.util.constants.Screens.GLOBAL_CHAT_SCREEN
import com.example.savethem.util.constants.Screens.KEY_GAME_ID
import com.example.savethem.util.constants.Screens.LOGIN_SCREEN
import com.example.savethem.util.constants.Screens.MAIN_SCREEN
import com.example.savethem.util.constants.Screens.PROFILE_SCREEN
import com.example.savethem.util.constants.Screens.REGISTER_SCREEN
import com.example.savethem.util.constants.Screens.TEST_SCREEN

sealed class Screens(val route: String){
    object Main: Screens(route = MAIN_SCREEN)
    object Login: Screens(route = LOGIN_SCREEN)
    object Call: Screens(route = CALL_SCREEN)
    object Details: Screens(route = DETAILS_SCREEN)
    object Register: Screens(route = REGISTER_SCREEN)
    object Chat: Screens(route = CHAT_SCREEN)
    object Test: Screens(route = TEST_SCREEN)
    object GlobalChat: Screens(route = GLOBAL_CHAT_SCREEN)
    object Profile: Screens(route = PROFILE_SCREEN)
    object SafetyMap: Screens(route = "safety_map_screen") // Nueva ruta
}

@Composable
fun SetupNavHost(
    navHostController: NavHostController,
    viewModel: mainViewModel,
    registerViewModel: RegisterViewModel,
    loginViewModel: LoginViewModel,
    detailsViewModel: detailsViewModel,
    friendsViewModel: FriendsViewModel,
    chatViewModel: ChatViewModel,
    idToFriend: String,
    context: Context
){
    val startRoute = if (com.google.firebase.auth.FirebaseAuth.getInstance().currentUser != null) {
        Screens.Main.route
    } else {
        Screens.Login.route
    }

    NavHost(navController = navHostController, startDestination = startRoute){
        composable(route = Screens.Login.route){
            loginScreen(loginViewModel = loginViewModel, navController = navHostController, context = context)
        }
        composable(route = Screens.Main.route){
            MainScreen(viewModel = viewModel, navController = navHostController, friendsViewModel = friendsViewModel)
        }
        composable(route = Screens.Register.route){
            loginScreen(loginViewModel = loginViewModel, navController = navHostController, context = context)
        }
        composable(route = Screens.Details.route + "/{$KEY_GAME_ID}"){backStackEntry ->
            Comments(id = backStackEntry.arguments?.getString(KEY_GAME_ID) ?: "", mainViewModel = detailsViewModel)
        }
        composable(route = Screens.Chat.route + "/{$KEY_GAME_ID}"){backStackEntry ->
            ChatMainScreen(id = backStackEntry.arguments?.getString(KEY_GAME_ID) ?: "", chatViewModel = chatViewModel, mainViewModel = viewModel, navHostController)
        }
        composable(route = Screens.GlobalChat.route){
            GlobalChatMainScreen(chatViewModel = chatViewModel, navController = navHostController)
        }
        composable(
            route = Screens.Profile.route + "?userId={userId}",
            arguments = listOf(navArgument("userId") {
                type = NavType.StringType
                nullable = true
                defaultValue = null
            })
        ){ backStackEntry ->
            val userId = backStackEntry.arguments?.getString("userId")
            ProfileScreen(chatViewModel, viewModel, navHostController, userId)
        }
        composable(route = Screens.SafetyMap.route) {
            SafetyMapScreen(viewModel = viewModel, navController = navHostController)
        }
        // AGREGAR RUTA DE PRUEBA FCM
        composable(route = Screens.Test.route) {
            tests(friendsViewModel = friendsViewModel, navController = navHostController)
        }
    }
}
