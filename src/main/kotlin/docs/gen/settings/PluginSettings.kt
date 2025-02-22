package docs.gen.settings

import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import docs.gen.settings.features.OpenApiProvider

@State(
    name = "KDocGenPluginSettings",
    storages = [Storage("kdocgen.xml")]
)
@Service(Service.Level.APP)
class PluginSettings : PersistentStateComponent<PluginSettings.State> {
    
    companion object {
        const val DEFAULT_MODEL = "chatgpt-4o-latest"
    }
    
    data class State(
        var apiKey: String = "",
        var selectedModel: String = "",
        var availableModels: List<String> = mutableListOf(),
        var selectedProvider: OpenApiProvider = OpenApiProvider.OpenAI,
        var customProviderUrl: String? = null,
    ) {
        val providerHost: String
            get() =
                when (selectedProvider) {
                    OpenApiProvider.Custom -> customProviderUrl.orEmpty()
                    else -> selectedProvider.host
                }
    }
    
    private var _state = State()
    
    override fun getState(): State = _state
    
    override fun loadState(state: State) {
        _state = state
    }
}

