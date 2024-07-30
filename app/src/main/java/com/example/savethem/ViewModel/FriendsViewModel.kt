package com.example.savethem.ViewModel

import android.content.Context
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.savethem.Model.registerModel
import com.example.savethem.Repository.Repository
import com.example.savethem.Repository.UserRepository
import com.example.savethem.call.enviar
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class FriendsViewModel @Inject constructor(
	private val DAO: Repository,
	private val userRepository: UserRepository
): ViewModel()
{
	private val _friends = MutableStateFlow(emptyList<registerModel>())
	val friends: StateFlow<List<registerModel>> = _friends.asStateFlow()
	fun addFriends(currentUserId: Long, friend: registerModel) {
		viewModelScope.launch {
			userRepository.addFriend(currentUserId, friend)
			DAO.addFriend(friend, friend.UUID ?: "")
			getAllFriends()
		}
	}

	fun addFriend(addFriend: registerModel, idFriend: String){
		viewModelScope.launch {
			val add = DAO.addFriend(addFriend, idFriend)
			add
		}
	}
	fun getAllFriends(){
		viewModelScope.launch {
			withContext(Dispatchers.IO){
				val friends = DAO.getFriends()
				_friends.value = friends
			}
		}
	}
	fun enviarNotificaciones(context: Context, tokens: List<String>){
		viewModelScope.launch {
			tokens.forEach { token ->
//				enviar(context, token)
			}
		}
	}
}