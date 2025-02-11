package docs.gen.service.domain

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonIgnoreUnknownKeys

@JsonIgnoreUnknownKeys
@Serializable
data class ChatCompletionResponse(
    val id: String,
    val created: Long,
    val model: String,
    val choices: List<Choice>
)