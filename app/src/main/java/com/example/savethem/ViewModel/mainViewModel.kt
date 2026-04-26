package com.example.savethem.ViewModel

import android.annotation.SuppressLint
import android.app.Application
import android.content.Context
import android.location.Location
import android.os.Looper
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.savethem.Model.*
import com.example.savethem.Repository.Repository
import com.google.android.gms.location.*
import com.google.android.gms.maps.model.LatLng
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.FirebaseApp
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import javax.inject.Inject

@HiltViewModel
class mainViewModel @Inject constructor(application: Application,
    private val repository: Repository
) : AndroidViewModel(application) {
    val context: Context = application.applicationContext
    
    private val prefs = context.getSharedPreferences("settings", Context.MODE_PRIVATE)
    private val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)

    private val _isDarkMode = MutableStateFlow(prefs.getBoolean("dark_mode", false))
    val isDarkMode: StateFlow<Boolean> = _isDarkMode.asStateFlow()

    private val _currentUserLocation = MutableStateFlow<LatLng?>(null)
    val currentUserLocation: StateFlow<LatLng?> = _currentUserLocation.asStateFlow()

    private val _safetyAlerts = MutableStateFlow<List<SafetyAlertModel>>(emptyList())
    private val _radiusFilter = MutableStateFlow<Double?>(null)
    val radiusFilter: StateFlow<Double?> = _radiusFilter.asStateFlow()

    private val _name = MutableStateFlow("")
    val name: StateFlow<String> = _name

    private val _profilePic = MutableStateFlow("")
    val profilePic: StateFlow<String> = _profilePic.asStateFlow()

    private val _stealthTitle = MutableStateFlow("System Update")
    val stealthTitle: StateFlow<String> = _stealthTitle.asStateFlow()

    private val _stealthText = MutableStateFlow("Checking for new software updates...")
    val stealthText: StateFlow<String> = _stealthText.asStateFlow()

    private val _stealthIcon = MutableStateFlow("settings") 
    val stealthIcon: StateFlow<String> = _stealthIcon.asStateFlow()

    private val _zoomToUserEvent = MutableSharedFlow<LatLng>(extraBufferCapacity = 1)
    val zoomToUserEvent = _zoomToUserEvent.asSharedFlow()

    private val _activeEmergencies = MutableStateFlow<List<EmergencyModel>>(emptyList())
    val activeEmergencies: StateFlow<List<EmergencyModel>> = _activeEmergencies.asStateFlow()

    private val _showTour = MutableStateFlow(prefs.getBoolean("first_run_tour", true))
    val showTour: StateFlow<Boolean> = _showTour.asStateFlow()

    private val _safePlaces = MutableStateFlow<List<SafePlaceModel>>(emptyList())
    val safePlaces: StateFlow<List<SafePlaceModel>> = _safePlaces.asStateFlow()

    val filteredSafetyAlerts = combine(_safetyAlerts, _currentUserLocation, _radiusFilter) { alerts, userLoc, radius ->
        if (userLoc == null || radius == null) {
            alerts
        } else {
            alerts.filter { alert ->
                val alertLoc = alert.location ?: return@filter false
                val distance = calculateDistance(userLoc.latitude, userLoc.longitude, alertLoc.latitude, alertLoc.longitude)
                distance <= radius
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        FirebaseApp.initializeApp(context)
        getUserData()
        observeSafetyAlerts()
        observeEmergencies()
        observeSafePlaces()
        startLocationUpdates()
        loadStealthSettings()
    }

    @SuppressLint("MissingPermission")
    private fun startLocationUpdates() {
        val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 10000)
            .setMinUpdateIntervalMillis(5000)
            .build()

        val locationCallback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                result.lastLocation?.let {
                    val latLng = LatLng(it.latitude, it.longitude)
                    _currentUserLocation.value = latLng
                }
            }
        }
        fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper())
    }

    @SuppressLint("MissingPermission")
    fun updateUserLocation() {
        fusedLocationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null)
            .addOnSuccessListener { location: Location? ->
                location?.let {
                    val latLng = LatLng(it.latitude, it.longitude)
                    _currentUserLocation.value = latLng
                    _zoomToUserEvent.tryEmit(latLng)
                }
            }
    }

    private fun observeSafetyAlerts() {
        viewModelScope.launch(Dispatchers.IO) {
            repository.getSafetyAlerts().collectLatest { alerts -> _safetyAlerts.value = alerts }
        }
    }

    private fun observeEmergencies() {
        viewModelScope.launch(Dispatchers.IO) {
            repository.getActiveEmergencies().collectLatest { emergencies -> _activeEmergencies.value = emergencies }
        }
    }

    private fun observeSafePlaces() {
        viewModelScope.launch(Dispatchers.IO) {
            repository.getSafePlaces().collectLatest { places -> _safePlaces.value = places }
        }
    }

    private fun loadStealthSettings() {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        FirebaseDatabase.getInstance().getReference("users/$uid/stealthSettings")
            .addValueEventListener(object : com.google.firebase.database.ValueEventListener {
                override fun onDataChange(snapshot: com.google.firebase.database.DataSnapshot) {
                    _stealthTitle.value = snapshot.child("title").getValue(String::class.java) ?: "System Update"
                    _stealthText.value = snapshot.child("text").getValue(String::class.java) ?: "Checking for updates..."
                    _stealthIcon.value = snapshot.child("icon").getValue(String::class.java) ?: "settings"
                }
                override fun onCancelled(error: com.google.firebase.database.DatabaseError) {}
            })
    }

    fun saveStealthSettings(title: String, text: String, icon: String, imageUri: String = "") {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val settings = mapOf("title" to title, "text" to text, "icon" to icon, "imageUri" to imageUri)
        FirebaseDatabase.getInstance().getReference("users/$uid/stealthSettings").setValue(settings)
    }

    fun getUserData() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                repository.getUserData()?.let {
                    _name.value = it.name ?: ""
                    _profilePic.value = it.profilePic ?: ""
                }
            } catch (e: Exception) { Log.e("mainViewModel", "Error fetching user data", e) }
        }
    }

    fun stopEmergency() {
        viewModelScope.launch(Dispatchers.IO) {
            val user = FirebaseAuth.getInstance().currentUser ?: return@launch
            repository.stopEmergency(user.uid)
        }
    }

    fun setRadiusFilter(km: Double?) { _radiusFilter.value = km }

    fun reportIncident(alert: SafetyAlertModel) {
        viewModelScope.launch(Dispatchers.IO) { repository.reportSafetyAlert(alert) }
    }

    fun addSafePlace(name: String, location: LatLng) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.addSafePlace(SafePlaceModel(
                name = name,
                location = LatLngWrapper(location.latitude, location.longitude),
                userId = FirebaseAuth.getInstance().currentUser?.uid ?: ""
            ))
        }
    }

    fun dismissTour() {
        _showTour.value = false
        prefs.edit().putBoolean("first_run_tour", false).apply()
    }

    fun toggleDarkMode(enabled: Boolean) {
        _isDarkMode.value = enabled
        prefs.edit().putBoolean("dark_mode", enabled).apply()
    }

    private fun calculateDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val results = FloatArray(1)
        Location.distanceBetween(lat1, lon1, lat2, lon2, results)
        return (results[0] / 1000).toDouble()
    }
}
