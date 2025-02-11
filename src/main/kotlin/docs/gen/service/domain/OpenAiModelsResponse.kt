package docs.gen.service.domain

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonIgnoreUnknownKeys

@Serializable
@JsonIgnoreUnknownKeys
data class OpenAiModelsResponse(val data: List<OpenAiModel>)