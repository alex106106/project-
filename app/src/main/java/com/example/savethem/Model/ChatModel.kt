package com.example.savethem.Model

data class ChatModel(
    var IDMessage: String = "",
    var UUIDSender: String = "",
    var message: String = "",
    var timestamp: Long = 0L,
    var seen: Boolean = false,
    var receiverId: String = "" // Nuevo campo para que la Cloud Function sepa a quién notificar
) {
    fun toMap(): Map<String, Any?> {
        return mapOf(
            "IDMessage" to IDMessage,
            "UUIDSender" to UUIDSender,
            "message" to message,
            "timestamp" to timestamp,
            "receiverId" to receiverId
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
