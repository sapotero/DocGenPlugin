package docs.gen.settings

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.service
import com.intellij.openapi.options.Configurable
import com.intellij.openapi.ui.ComboBox
import com.intellij.openapi.ui.Messages
import com.intellij.util.ui.JBUI
import docs.gen.service.OpenAiService
import docs.gen.settings.PluginSettings.Companion.DEFAULT_MODEL
import docs.gen.settings.features.OpenApiProvider
import java.awt.BorderLayout
import java.awt.GridBagConstraints
import java.awt.GridBagLayout
import javax.swing.JButton
import javax.swing.JComboBox
import javax.swing.JComponent
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.JSeparator
import javax.swing.JTextField
import javax.swing.SwingConstants
import javax.swing.SwingUtilities

class ConfigurablePluginSettings : Configurable {
    
    private lateinit var customHostField: JTextField
    private val settings = service<PluginSettings>().state
    private val gptService = service<OpenAiService>()
    
    private lateinit var panel: JPanel
    private lateinit var customHostPanel: JPanel
    private lateinit var apiKeyField: JTextField
    private lateinit var checkKeyButton: JButton
    private lateinit var modelComboBox: JComboBox<String>
    private lateinit var providerComboBox: ComboBox<OpenApiProvider>
    private lateinit var loadingLabel: JLabel
    
    override fun getDisplayName(): String = "KDocGen Settings"
    
    override fun createComponent(): JComponent {
        panel = JPanel(GridBagLayout()).apply {
            border = JBUI.Borders.empty(10)
        }
        
        val gbc = GridBagConstraints().apply {
            insets = JBUI.insets(5)
            fill = GridBagConstraints.HORIZONTAL
            anchor = GridBagConstraints.WEST
        }
        
        // API Key Input + Check Key Button
        apiKeyField = JTextField(25).apply { text = settings.apiKey }
        checkKeyButton = JButton("Check Key").apply {
            addActionListener { validateApiKey() }
        }
        
        val apiKeyPanel = JPanel(BorderLayout(5, 0)).apply {
            add(apiKeyField, BorderLayout.CENTER)
            add(checkKeyButton, BorderLayout.EAST)
        }
        
        panel.add(JLabel("OpenAI API Key:"), gbc.apply { gridx = 0; gridy = 0; weightx = 0.0 })
        panel.add(apiKeyPanel, gbc.apply { gridx = 1; weightx = 1.0 })
        
        // Provider Selection
        providerComboBox = ComboBox<OpenApiProvider>().apply {
            OpenApiProvider.entries.forEach { addItem(it) }
            selectedItem = settings.selectedProvider
            addActionListener { updateProviderState() }
        }
        
        panel.add(JLabel("Provider:"), gbc.apply { gridx = 0; gridy = 1; weightx = 0.0 })
        panel.add(providerComboBox, gbc.apply { gridx = 1; weightx = 1.0 })
        
        // Model Selection
        modelComboBox = ComboBox<String>().apply {
            isEnabled = settings.apiKey.isNotBlank()
            settings.availableModels.forEach { addItem(it) }
            selectedItem = settings.selectedModel
        }
        
        panel.add(JLabel("Model:"), gbc.apply { gridx = 0; gridy = 2; weightx = 0.0 })
        panel.add(modelComboBox, gbc.apply { gridx = 1; weightx = 1.0 })
        
        // Loading Indicator
        loadingLabel = JLabel("Loading...").apply {
            isVisible = false
            horizontalAlignment = SwingConstants.CENTER
        }
        panel.add(loadingLabel, gbc.apply { gridx = 0; gridy = 3; gridwidth = 2 })
        panel.add(JSeparator(SwingConstants.HORIZONTAL))
        
        customHostField = JTextField(25)
        customHostPanel = JPanel(BorderLayout(5, 0)).apply {
            add(JLabel("Host:"), BorderLayout.WEST)
            add(customHostField, BorderLayout.CENTER)
            isVisible = (providerComboBox.selectedItem == OpenApiProvider.Custom)
        }
        
        panel.add(customHostPanel, gbc.apply { gridx = 0; gridy = 4; gridwidth = 2 })
        
        return panel
    }
    
    private fun updateProviderState() {
        customHostPanel.isVisible = (providerComboBox.selectedItem == OpenApiProvider.Custom)
    }
    
    private fun validateApiKey() {
        val apiKey = apiKeyField.text.trim()
        if (apiKey.isEmpty()) {
            Messages.showErrorDialog(panel, "API Key cannot be empty.", "Validation Error")
            return
        }
        
        setLoading(true)
        ApplicationManager.getApplication().executeOnPooledThread {
            val providerHost = when (val selectedItem = providerComboBox.selectedItem as OpenApiProvider) {
                OpenApiProvider.Custom -> customHostField.text.orEmpty()
                else -> selectedItem.host
            }
            
            val isValid = gptService.validateApiKey(apiKey, providerHost)
            SwingUtilities.invokeLater {
                if (isValid) {
                    settings.apiKey = apiKey
                    Messages.showInfoMessage(panel, "API Key is valid!", "Success")
                    apply()
                    loadAvailableModels()
                } else {
                    setLoading(false)
                    Messages.showErrorDialog(panel, "Invalid API Key. Please check and try again.", "Error")
                    modelComboBox.removeAllItems()
                    modelComboBox.isEnabled = false
                }
            }
        }
    }
    
    private fun loadAvailableModels() {
        setLoading(true)
        ApplicationManager.getApplication().executeOnPooledThread {
            val models = gptService.fetchAvailableModels()
            SwingUtilities.invokeLater {
                setLoading(false)
                if (models.isNotEmpty()) {
                    settings.availableModels = models
                    modelComboBox.apply {
                        removeAllItems()
                        models.forEach { addItem(it) }
                        selectedItem = DEFAULT_MODEL
                        isEnabled = true
                    }
                } else {
                    Messages.showErrorDialog(
                        panel,
                        "Failed to load models. Please check your API Key and network connection.",
                        "Error"
                    )
                }
            }
        }
    }
    
    private fun setLoading(loading: Boolean) {
        loadingLabel.isVisible = loading
        checkKeyButton.isEnabled = !loading
        apiKeyField.isEnabled = !loading
        customHostField.isEnabled = !loading
        providerComboBox.isEnabled = !loading
        modelComboBox.isEnabled = !loading
    }
    
    override fun isModified(): Boolean =
        apiKeyField.text.trim() != settings.apiKey ||
            modelComboBox.selectedItem?.toString() != settings.selectedModel ||
            providerComboBox.selectedItem != settings.selectedProvider ||
            (providerComboBox.selectedItem == OpenApiProvider.Custom &&
                customHostField.text.trim() != settings.customProviderUrl)
    
    override fun apply() {
        settings.apiKey = apiKeyField.text.trim()
        settings.selectedModel = modelComboBox.selectedItem?.toString() ?: DEFAULT_MODEL
        settings.selectedProvider = providerComboBox.selectedItem as OpenApiProvider
        if (providerComboBox.selectedItem == OpenApiProvider.Custom) {
            settings.customProviderUrl = customHostField.text.trim()
        }
    }
    
    override fun reset() {
        apiKeyField.text = settings.apiKey
        modelComboBox.removeAllItems()
        settings.availableModels.forEach { modelComboBox.addItem(it) }
        modelComboBox.selectedItem = settings.selectedModel
        providerComboBox.selectedItem = settings.selectedProvider
        customHostField.text = settings.customProviderUrl
        modelComboBox.isEnabled = settings.apiKey.isNotBlank()
        updateProviderState()
    }
}
