package com.example.savethem.Model

import com.google.android.gms.maps.model.LatLng

//data class LatLngWrapper(
//	var latitude: Double? = 0.0,
//	var longitude: Double? = 0.0
//) {
//	constructor() : this(0.0, 0.0)
//}

data class ChatModel(
	var IDMessage: String = "",
	var UUIDSender: String = "",
	var message: String = "",
	var timestamp: Long = 0L,
	var seen: Boolean = false
) {
	fun toMap(): Map<String, Any?> {
		return mapOf(
			"IDMessage" to IDMessage,
			"UUIDSender" to UUIDSender,
			"message" to message,
			"timestamp" to timestamp
		)
	}
}

data class ChatLocationModel(
	var IDMessage: String? = "",
	var UUIDSender: String? = "",
	var message: String? = "",
	var location: LatLngWrapper? = null,
	var timestamp: Long? = 0
)

