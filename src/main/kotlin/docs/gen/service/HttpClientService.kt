package docs.gen.service

import com.intellij.openapi.diagnostic.logger
import kotlinx.serialization.json.Json
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpClient.Version
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration
import kotlin.math.min
import kotlin.math.pow

class HttpClientService {
    
    private val client = HttpClient.newBuilder()
        .version(Version.HTTP_2)
        .connectTimeout(Duration.ofSeconds(10))
        .build()
    
    private val log = logger<HttpClientService>()
    
    fun sendRequest(
        uri: String,
        method: String = "GET",
        apiKey: String,
        body: String? = null,
        maxRetries: Int = 3,
        initialDelayMillis: Long = 500
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
        
        var attempt = 0
        while (attempt < maxRetries) {
            try {
                val response = client.send(requestBuilder.build(), HttpResponse.BodyHandlers.ofString())
                
                if (response.statusCode() == 200) {
                    return Result.success(response.body())
                } else {
                    val error = Json.decodeFromString<Map<String, String>>(response.body())
                    log.error("API Error ${response.statusCode()}: ${error["message"]}")
                    return Result.failure(IllegalStateException("API Error: ${error["message"]}"))
                }
            } catch (e: Exception) {
                log.warn("Request failed (attempt $attempt/${maxRetries - 1}): ${e.message}")
                if (attempt == maxRetries - 1) return Result.failure(e)
                
                // Exponential backoff
                val delay = min(initialDelayMillis * 2.0.pow(attempt.toDouble()).toLong(), 5000L)
                Thread.sleep(delay)
                attempt++
            }
        }
        
        return Result.failure(IllegalStateException("Request failed after $maxRetries attempts"))
    }
}
