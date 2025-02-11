package docs.gen.service.domain

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonIgnoreUnknownKeys

@Serializable
@JsonIgnoreUnknownKeys
data class Message(
    val role: String, // "user", "assistant", or "system"
    val content: String
)