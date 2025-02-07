package docs.gen.settings

import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage

@State(
    name = "PluginSettings",
    storages = [Storage("docsbuilder.xml")]
)
@Service
class PluginSettings : PersistentStateComponent<PluginSettings.State> {
    
    data class State(
        var apiKey: String = "",
        var selectedModel: String = "gpt-4",
        var maxTokens: Int = 500,
    )
    
    private var _state = State()
    
    override fun getState(): State = _state
    
    override fun loadState(state: State) {
        _state = state
    }
    
    
}