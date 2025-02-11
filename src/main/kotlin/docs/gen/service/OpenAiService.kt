package docs.gen.service

import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.thisLogger
import docs.gen.service.domain.ChatCompletionResponse
import docs.gen.service.domain.ChatRequest
import docs.gen.service.domain.OpenAiError
import docs.gen.service.domain.OpenAiModelsResponse
import docs.gen.settings.PluginSettings
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.net.HttpURLConnection
import java.net.URI
import java.net.URL
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse

@Service
class OpenAiService {
    companion object {
        const val BASE_URL = "https://api.openai.com/v1"
    }
    
    private val settings = service<PluginSettings>().state
    private val client = HttpClient.newHttpClient()
    
    fun validateApiKey(apiKey: String): Boolean =
        runCatching {
            with(URL("BASE_URL/models").openConnection() as HttpURLConnection) {
                requestMethod = "HEAD"
                setRequestProperty("Authorization", "Bearer $apiKey")
                connectTimeout = 5000
                readTimeout = 5000
                responseCode == 200
            }
        }.getOrDefault(false)
    
    /**
     * The function fetchAvailableModels is responsible for fetching a list of available models using an API call to a given base URL.
     * The request is created with the GET method to the '/models' endpoint using an Authorization token provided in the settings.
     * The response from the API call is then parsed to extract the model IDs from the received JSON data.
     * If the status code of the response is not 200, the function logs the error and returns an empty list. It similarly handles exceptions.
     *
     * @return A {@code List<String>} containing the IDs of the available models. It can be an empty list in case of an error or unsuccessful API call.
     *
     * Throws:
     * If an error occurs during the API call or while processing the response data, such as connectivity issues, server faults, etc.,
     * it throws a {@code CompletionException} wrapped around the original exception like {@code HttpTimeoutException}, {@code IOException}, and others.
     * The detailed error message will be logged and displayed in the console.
     */
    fun fetchAvailableModels(): List<String> =
        runCatching {
            with(URL("BASE_URL/models").openConnection() as HttpURLConnection) {
                requestMethod = "GET"
                setRequestProperty("Authorization", "Bearer ${settings.apiKey}")
                connectTimeout = 5000
                readTimeout = 5000
                
                if (responseCode == 200) {
                    inputStream.bufferedReader().use { reader ->
                        Json.decodeFromString<OpenAiModelsResponse>(reader.readText()).data.map { it.id }
                    }
                } else {
                    thisLogger().error("API Error: $responseCode")
                    emptyList()
                }
            }
        }.getOrElse { throwable ->
            thisLogger().error("Error fetching models: ${throwable.message}")
            emptyList()
        }
    
    /**
     * Sends a chat request to the server using an asynchronous HTTP request and retrieves the response.
     * This function specifically communicates with a chat API, constructs the HTTP request including the necessary headers,
     * sends it asynchronously, and processes the received response. If the response is successful, it extracts and
     * returns the chat message from the response. In case of an error in the response, it extracts and logs the error detail.
    *
     * @param chatRequest An instance of [ChatRequest] containing all the required data to make a chat API request.
     * @return A [String] object representing the chat response message if successful, or an error message if the server responds with an error.
     *         Returns `null` when the response lacks a valid chat completion or if an unhandled exception occurs.
     * @throws IllegalStateException if the API key has not been set or is blank.
     * @throws Throwable if an error occurs during the asynchronous handling of the response or if the future execution is interrupted.
     * The thrown throwable provides details on what went wrong during the HTTP communication or processing.
    */
    fun sendRequest(chatRequest: ChatRequest): String? {
        if (settings.apiKey.isBlank()) {
            throw IllegalStateException("API Error: apiKey is blank")
        }
        
        val requestBody = Json.encodeToString(chatRequest)
        
        val request = HttpRequest.newBuilder()
            .uri(URI.create("$BASE_URL/chat/completions"))
            .header("Authorization", "Bearer ${settings.apiKey}")
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(requestBody))
            .build()
        
        return client.sendAsync(request, HttpResponse.BodyHandlers.ofString())
            .thenApply { response ->
                val responseBody = response.body()
                if (response.statusCode() == 200) {
                    val parsedResponse = Json.decodeFromString<ChatCompletionResponse>(responseBody)
                    parsedResponse.choices.firstOrNull()?.message?.content ?: ""
                } else {
                    println("API Error: ${response.statusCode()}")
                    val errorResponse = Json.decodeFromString<OpenAiError>(responseBody)
                    println("Error: ${errorResponse.message}")
                    errorResponse.message
                }
            }
            .whenComplete { result, throwable ->
                if (throwable != null) {
                    throw throwable
                } else {
                    result
                }
            }.get()
    }
}