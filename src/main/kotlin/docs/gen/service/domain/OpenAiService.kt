package docs.gen.service.domain

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonIgnoreUnknownKeys

@Serializable
@JsonIgnoreUnknownKeys
data class OpenAiModelsResponse(val data: List<OpenAiModel>)

@Serializable
@JsonIgnoreUnknownKeys
data class OpenAiModel(val id: String)

@Serializable
@JsonIgnoreUnknownKeys
data class ChatRequest(
    val model: String,
    val messages: List<Message>
)

@Serializable
@JsonIgnoreUnknownKeys
data class Message(
    val role: String, // "user", "assistant", or "system"
    val content: String
)

@Serializable
@JsonIgnoreUnknownKeys
data class OpenAiError(
    val message: String,
    val type: String? = null,
    val code: String? = null,
    val param: String? = null
)

@JsonIgnoreUnknownKeys
@Serializable
data class ChatCompletionResponse(
    val id: String,
    val created: Long,
    val model: String,
    val choices: List<Choice>
)

@JsonIgnoreUnknownKeys
@Serializable
data class Choice(
    val message: Message,
)