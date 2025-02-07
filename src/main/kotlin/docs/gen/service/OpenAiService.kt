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
    
    /**
    * This function `describeFunction` is used to generate a detailed description of a given function in the form of a comment block.
    * It follows the KDoc (for Kotlin) or Javadoc (for Java) style of documentation.
    * It not only provides a description of the function but also details about its parameters,
    * return type, and any exceptions that the function might throw.
    *
    * @param function The function that needs to be described. This is a `String` representation of the function.
    *                 This parameter should contain all the details of the function including the function name, parameters, return type, etc.
    *                 The function is processed and used to generate a detailed comment block description.
    *
    * @return The return value is a call to the `execute` function on a `ChatRequest` object.
    *         It contains, among other settings, a list of `Message` objects that symbolize the interaction required for the function generation process.
    *         The return type is the result of the `execute` function which is an implementation detail and depends on the behavior of the `execute` method itself.
    *
    * @throws No specific exceptions are expected to be thrown by this function.
    *         However, since it calls the `execute` function on a `ChatRequest` object, any exceptions that this function could throw are potential exceptions for `describeFunction` as well.
    */
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
    
    /**
    *  This function named 'describeSelection' uses the input string and the current selected model
    *  settings to construct a chat request that contains system and user-messages then executes it.
    *
    *  @param function, a String representing the function for which documentation should be generated.
    *  The role of the function parameter is to generate a chat request that includes
    *  the system and user-messages related to this input.
    *
    *  @return a 'ChatRequestResult', which offers a ChatRequest object after execution. The returned object
    *  is formed using the provided string and the current model selected from settings. This object represents the
    *  complete chat between the user and the system where the user message is basically the content of
    *  the provided function and the system message is a hard-coded string mentioning the requirement of generating KDoc or Javadoc comments.
    *
    *  There isn't any specific exception thrown by this function.
    *  However, failures could occur during the execution of the ChatRequest, such as network errors or
    *  problems with the server response. These should be handled in the code using the ChatRequest
    *  Execute() function.
    */
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
    
    /**
    * The `generateCode` function is used to process a block of code, replacing any comments prefixed with 'IMPL' with the correct implementation.
    * The newly implemented code is then used to execute a chat request.
    *
    * @param codeBlock A String representing a block of Kotlin code. This is the user message which
    * may include possible comments prefixed with 'IMPL' for replacement with the correct implementation.
    *
    * @return The result of ChatRequest execution. This will include the model and messages used for the chat request.
    * The model is selected based on the current settings, and the messages is a list built from a system message and
    * the user message which is the provided code block
    *
    * @throws IllegalStateException if selectedModel in settings is null.
    * @throws RuntimeException if it fails to execute the ChatRequest.
    * Please note that any syntax errors or semantic issues with the 'codeBlock' content would cause the execution to fail.
    */
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
    
    /**
    * This function describes and sends an asynchronous HTTP request for chatting message to a specific API,
    * then handle the responses and exceptions.
    *
    * @param chatRequest The request object to be sent as JSON payload.
    * Includes properties such as messages, model, options, etc.,
    * which are used to represent chat dialogue and openAI API preferences.
    *
    * @return A nullable string obtained from the API response after parsed.
    * If the request is successful, it returns the content of the first message from the API's choices.
    * If it fails, it prints the status code and message from the error response and returns the error message.
    * If it catches an exception, it rethrows the exception and ends the function execution.
    *
    * @throw IllegalStateException If the API key is blank.
    * This is a runtime exception as a result of illegal or inappropriate function condition.
    *
    * @throw Throwable If any exception is encountered during executing the request and handling responses.
    * The throwable instance captures the exception and get threw.
    */
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
    
    /**
    * Executes a ChatRequest and returns the description of the request.
    *
    * This function is part of the Chat functionality, and its main purpose is to encapsulate
    * the process of executing a ChatRequest and describing the results in a readable manner.
    *
    * @receiver ChatRequest - an instance on which this extension function is invoked. This ChatRequest object
    * should already have all needed properties set such as request details, user information etc.
    *
    * @return String - The detailed description of the executed chat request.
    * This description is generated by the function 'describe', and it typically includes details
    * about what actions were performed during execution.
    *
    * @throws IllegalStateException - If the ChatRequest could not be executed due to some invalid state,
    * this exception is thrown. The corresponding error message will contain details about the illegal state.
    *
    */
    private fun ChatRequest.execute() =
        describe(this)
}