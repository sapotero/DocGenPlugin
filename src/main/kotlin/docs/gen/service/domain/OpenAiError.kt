package docs.gen.service.domain

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonIgnoreUnknownKeys

@Serializable
@JsonIgnoreUnknownKeys
data class OpenAiError(
    val message: String,
    val type: String? = null,
    val code: String? = null,
    val param: String? = null
)