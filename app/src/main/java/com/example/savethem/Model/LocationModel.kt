package com.example.savethem.Model

data class LocationModel(
	var IDMessage: String? = "",
	var UUIDSender: String? = "",
	var location: LatLngWrapper? = null,
	var timestamp: Long? = 0
) {
	fun toMap(): Map<String, Any?> {
		return mapOf(
			"IDMessage" to IDMessage,
			"UUIDSender" to UUIDSender,
			"location" to location?.toMap(),
			"timestamp" to timestamp
		)
	}
}

data class LatLngWrapper(
	var latitude: Double = 0.0,
	var longitude: Double = 0.0
) {
	fun toMap(): Map<String, Double> {
		return mapOf(
			"latitude" to latitude,
			"longitude" to longitude
		)
	}
}
