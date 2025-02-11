package docs.gen.settings

import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage

@State(
    name = "KDocGenPluginSettings",
    storages = [Storage("kdocgen.xml")]
)
@Service
class PluginSettings : PersistentStateComponent<PluginSettings.State> {
    
    companion object {
        const val DEFAULT_MODEL = "gpt-4-turbo"
    }
    
    data class State(
        var apiKey: String = "",
        var selectedModel: String = "",
        var availableModels: List<String> = mutableListOf(),
    )
    
    private var _state = State()
    
    override fun getState(): State = _state
    
    override fun loadState(state: State) {
        _state = state
    }
    
    
}