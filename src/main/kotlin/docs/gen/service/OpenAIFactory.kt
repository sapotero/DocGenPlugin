package docs.gen.service

import com.intellij.openapi.components.Service
import com.intellij.openapi.components.Service.Level
import com.intellij.openapi.components.service
import com.openai.client.OpenAIClient
import com.openai.client.okhttp.OpenAIOkHttpClient
import docs.gen.settings.PluginSettings
import java.lang.ref.SoftReference
import java.security.MessageDigest
import java.util.concurrent.ConcurrentHashMap

@Service(Level.APP)
class OpenAIFactory {
    private val settings = service<PluginSettings>().state
    private val instances = ConcurrentHashMap<String, SoftReference<OpenAIClient>>()
    
    // TODO тригериться на все изменения не только ключик
    fun get(): OpenAIClient =
        settings.apiKey.hash().let { key ->
            instances[key]?.get()
                ?: buildNewClient().also { instances[key] = SoftReference(it) }
        }
    
    private fun buildNewClient() =
        OpenAIOkHttpClient.builder()
            .baseUrl(settings.providerHost)
            .apiKey(settings.apiKey)
            .build()
    
    private fun String.hash(): String =
        MessageDigest.getInstance("SHA-256")
            .digest(toByteArray())
            .joinToString("") { "%02x".format(it) }
}