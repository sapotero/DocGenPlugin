package docs.gen.service

import com.intellij.openapi.diagnostic.logger
import kotlinx.serialization.json.Json
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse

class HttpClientService {
    
    private val client = HttpClient.newHttpClient()
    private val log = logger<HttpClientService>()
    
    /**
     * Sends an HTTP request to a specified URI using the provided method and includes an API key for authorization.
     * The request can optionally include a body for methods that support body data (e.g., POST). The function
     * handles response processing, including error handling through status codes and logs an error message on failure.
     *
     * @param uri The URI to which the request will be sent. It must be a properly formatted URL.
     * @param method The HTTP method to be used for the request, with a default of "GET". Supported methods include "GET", "POST", and "HEAD".
     * @param apiKey The API key to be included in the request header for authorization. It must be a valid bearer token.
     * @param body An optional JSON formatted string to be sent as the request's body content. This parameter is relevant only for requests that submit data to the server like POST.
     * @return A `Result<String>` which, on success, contains the response body as a string if the status code is 200.
     *         On error, the function logs the error detail and returns a `Result` containing the respective exception.
     * @throws IllegalArgumentException If the HTTP method provided is not supported.
     * @throws IllegalStateException If the server responds with a non-200 status code, indicating a call failure with description provided by the API error message response.
     */
    fun sendRequest(
        uri: String,
        method: String = "GET",
        apiKey: String,
        body: String? = null
    ): Result<String> {
        val requestBuilder = HttpRequest.newBuilder()
            .uri(URI.create(uri))
            .header("Authorization", "Bearer $apiKey")
            .header("Content-Type", "application/json")
        
        when (method) {
            "GET" -> requestBuilder.GET()
            "POST" -> requestBuilder.POST(HttpRequest.BodyPublishers.ofString(body ?: ""))
            "HEAD" -> requestBuilder.method("HEAD", HttpRequest.BodyPublishers.noBody())
            else -> throw IllegalArgumentException("Unsupported HTTP method: $method")
        }
        
        return runCatching {
            val response = client.send(requestBuilder.build(), HttpResponse.BodyHandlers.ofString())
            if (response.statusCode() == 200) {
                response.body()
            } else {
                val error = Json.decodeFromString<Map<String, String>>(response.body())
                log.error("API Error ${response.statusCode()}: ${error["message"]}")
                throw IllegalStateException("API Error: ${error["message"]}")
            }
        }
    }
}
