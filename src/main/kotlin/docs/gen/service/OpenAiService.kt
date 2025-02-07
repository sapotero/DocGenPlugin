package docs.gen.service

import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.thisLogger
import docs.gen.service.domain.ChatCompletionResponse
import docs.gen.service.domain.ChatRequest
import docs.gen.service.domain.Message
import docs.gen.service.domain.OpenAiError
import docs.gen.service.domain.OpenAiModelsResponse
import docs.gen.settings.PluginSettings
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse

@Service
class OpenAiService {
    
    private val baseUrl = "https://api.openai.com/v1"
    private val client = HttpClient.newHttpClient()
    
    private val settings = service<PluginSettings>().state
    
    
    fun fetchAvailableModels(): List<String> {
        val request = HttpRequest.newBuilder()
            .uri(URI.create("$baseUrl/models"))
            .header("Authorization", "Bearer ${settings.apiKey}")
            .GET()
            .build()
        
        return client.sendAsync(request, HttpResponse.BodyHandlers.ofString())
            .thenApply { response ->
                val responseBody = response.body()
                if (response.statusCode() == 200) {
                    Json.decodeFromString<OpenAiModelsResponse>(responseBody).data.map { it.id }
                } else {
                    thisLogger().error("API Error: ${response.statusCode()}")
                    println("API Error: ${response.statusCode()}")
                    emptyList()
                }
            }
            .whenComplete { result, throwable ->
                if (throwable != null) {
                    thisLogger().error("Error fetching models: ${throwable.message}")
                    println("Error fetching models: ${throwable.message}")
                    emptyList()
                } else {
                    result
                }
            }.get()
    }
    
    fun describeFunction(function: String) =
        ChatRequest(
            model = settings.selectedModel,
            messages = listOf(
                Message(
                    role = "system",
                    content = "Generate a function comment with detailed documentation in the style of KDoc (for Kotlin) or Javadoc (for Java) for the following function;" +
                        "Please include the purpose of the function, parameters with descriptions, the return type with a description, and any possible exceptions thrown." +
                        "Be specific about each part of the documentation. Return only comment block without any markdown markup"
                ),
                Message(role = "user", content = function)
            )
        ).execute()
    
    fun describeSelection(function: String) =
        ChatRequest(
            model = settings.selectedModel,
            messages = listOf(
                Message(
                    role = "system",
                    content = "Generate a comment with detailed documentation in the style of KDoc (for Kotlin) or Javadoc (for Java) for the following code fragment;" +
                        "Return only comment block without any markdown markup"
                ),
                Message(role = "user", content = function)
            )
        ).execute()
    
    fun generateCode(codeBlock: String) =
        ChatRequest(
            model = settings.selectedModel,
            messages = listOf(
                Message(
                    role = "system",
                    content = """
                        You are an expert Kotlin developer.
                        Given the following Kotlin code, replace all comments that start with 'IMPL ' with the appropriate implementation.
                        The implementation should follow best practices, be idiomatic, and ensure correctness.
                        Do not modify any other part of the code
                    """.trimIndent()
                ),
                Message(role = "user", content = codeBlock)
            )
        ).execute()
    
    private fun describe(chatRequest: ChatRequest): String? {
        if (settings.apiKey.isBlank()) {
            throw IllegalStateException("API Error: apiKey is blank")
        }
        
        val requestBody = Json.encodeToString(chatRequest)
        
        val request = HttpRequest.newBuilder()
            .uri(URI.create("$baseUrl/chat/completions"))
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
    
    private fun ChatRequest.execute() =
        describe(this)
}