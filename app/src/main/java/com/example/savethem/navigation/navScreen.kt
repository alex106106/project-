package com.example.savethem.navigation

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.example.savethem.ViewModel.*
import com.example.savethem.call.CallButton
import com.example.savethem.call.boton
import com.example.savethem.ui.Screens.*
import com.example.savethem.util.constants.Screens.CALL_SCREEN
import com.example.savethem.util.constants.Screens.CHAT_SCREEN
import com.example.savethem.util.constants.Screens.DETAILS_SCREEN
import com.example.savethem.util.constants.Screens.GLOBAL_CHAT_SCREEN
import com.example.savethem.util.constants.Screens.KEY_GAME_ID
//import com.example.savethem.util.constants.Screens.KEY_USER_ID
import com.example.savethem.util.constants.Screens.LOGIN_SCREEN
import com.example.savethem.util.constants.Screens.MAIN_SCREEN
import com.example.savethem.util.constants.Screens.MAP_SCREEN
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
    NavHost(navController = navHostController, startDestination = Screens.Login.route){
        composable(route = Screens.Login.route){
            loginScreen(loginViewModel = loginViewModel, navController = navHostController, context = context)
        }
        composable(route = Screens.Main.route){
            friendList(viewModel, navController = navHostController, friendsViewModel = friendsViewModel)
        }
        composable(route = Screens.Register.route){
            RegisterScreen(registerViewModel = registerViewModel, navController = navHostController)
        }
        composable(route = Screens.Details.route + "/{$KEY_GAME_ID}"){backStackEntry ->
            Comments(id = backStackEntry.arguments?.getString(KEY_GAME_ID) ?: "", mainViewModel = detailsViewModel)
        }
        composable(route = Screens.Chat.route + "/{$KEY_GAME_ID}"){backStackEntry ->
            ChatMainScreen(id = backStackEntry.arguments?.getString(KEY_GAME_ID) ?: "", chatViewModel = chatViewModel, navHostController)
        }
        composable(route = Screens.Test.route){
            test()
        }
        composable(route = Screens.GlobalChat.route){
            GlobalChatMainScreen(chatViewModel = chatViewModel, navController = navHostController)
        }

    }
}