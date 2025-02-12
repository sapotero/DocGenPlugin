package docs.gen.service

import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.logger
import docs.gen.service.domain.ChatCompletionResponse
import docs.gen.service.domain.ChatRequest
import docs.gen.service.domain.OpenAiModelsResponse
import docs.gen.settings.PluginSettings
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Service(Service.Level.APP)
class OpenAiService {
    
    companion object {
        private const val BASE_URL = "https://api.openai.com/v1"
        private val log = logger<OpenAiService>()
    }
    
    private val settings = service<PluginSettings>().state
    private val httpClient = HttpClientService()
    
    /**
     * Validates an API key by sending a HEAD request to a specified URL and checking the success status of the response.
     *
     * This function is useful for quickly verifying if the provided API key has access to the required resources
     * without transferring any data. It utilizes a HEAD HTTP method to minimize data exchange during the verification process.
     *
     * @param apiKey The API key to be validated. This should be a string format key used for authentication.
     * @return Boolean value indicating if the API key is valid (true) or not (false).
     * @throws HttpRequestException If there is an issue with the network or server during the request.
     * @throws InvalidApiKeyFormatException If the format of the API key is incorrect.
     */
    fun validateApiKey(apiKey: String): Boolean =
        httpClient.sendRequest("$BASE_URL/models", "HEAD", apiKey).isSuccess
    
    /**
     * Fetches a list of available models from a remote server using HTTP GET, deserializes the JSON response, and extracts model identifiers.
     *
     * This function uses the base URL specified in `BASE_URL` concatenated with "/models" to know which endpoint to request.
     * It sends an HTTP GET request utilizing an API key for authorization, which is provided via `settings.apiKey`.
     *
     * The response is expected to be JSON-formatted, specifically in the structure of `OpenAiModelsResponse`, which should contain
     * an array `data` of items where each item includes an identifier (`id`). The function maps these identifiers into a list of strings.
     *
     * In case of failure during the HTTP transaction or during JSON deserialization, an error message will be logged,
     * and an empty list will be returned to indicate that no models could be fetched.
     *
     * @return List<String> A list of model identifiers as strings. If the fetch operation or parsing fails, it returns an empty list.
     *
     * @throws JsonDecodingException If the JSON decoding process encounters incompatible or erroneous data formats.
     * @throws HttpRequestException If there are issues with the network request like connectivity problems or timeouts.
     */
    fun fetchAvailableModels(): List<String> =
        httpClient.sendRequest("$BASE_URL/models", "GET", settings.apiKey)
            .mapCatching { Json.decodeFromString<OpenAiModelsResponse>(it).data.map { model -> model.id } }
            .getOrElse {
                log.error("Failed to fetch models: ${it.message}", it)
                emptyList()
            }
    
    /**
     * Sends a chat request to the configured server using POST method, processes the response, and retrieves the chat completion message.
     * This function encodes the given [chatRequest] to JSON format to prepare the payload for HTTP request,
     * performs the API call through an HTTP client, and attempts to decode the JSON response into a [ChatCompletionResponse].
     * If successful, it retrieves the first choice's message content. In case of any failures during the request or response handling,
     * the error is logged and the function returns null.
    *
     * @param chatRequest The [ChatRequest] data object which contains all necessary information for the chat request.
     *                     This includes any parameters required by the chat API such as prompts, session tokens, etc.
     * @return The content of the first message choice from the chat completion response as a [String]? if present,
     *          or `null` if the request fails, if there is no message found, or if the response decoding fails.
     * @throws IllegalArgumentException If the API key in the settings is blank, this exception is thrown with a message.
    */
    fun sendRequest(chatRequest: ChatRequest): String? {
        require(settings.apiKey.isNotBlank()) { "API Error: apiKey is blank" }
        
        return httpClient.sendRequest(
            uri = "$BASE_URL/chat/completions",
            method = "POST",
            apiKey = settings.apiKey,
            body = Json.encodeToString(chatRequest)
        ).mapCatching { Json.decodeFromString<ChatCompletionResponse>(it).choices.firstOrNull()?.message?.content }
            .getOrElse {
                log.error("Failed to send request: ${it.message}", it)
                null
            }
    }
}
