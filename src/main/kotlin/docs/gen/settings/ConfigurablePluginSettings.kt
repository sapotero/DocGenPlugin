package docs.gen.settings

import com.intellij.openapi.components.service
import com.intellij.openapi.options.Configurable
import com.intellij.openapi.ui.Messages
import docs.gen.service.OpenAiService
import java.awt.BorderLayout
import java.awt.GridBagConstraints
import java.awt.GridBagLayout
import java.awt.Insets
import javax.swing.JButton
import javax.swing.JComboBox
import javax.swing.JComponent
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.JSeparator
import javax.swing.JTextField
import javax.swing.SwingConstants
import javax.swing.SwingUtilities
import javax.swing.border.EmptyBorder

class ConfigurablePluginSettings : Configurable {
    private val pluginSettings = service<PluginSettings>().state
    private val gptService = service<OpenAiService>()
    
    private lateinit var panel: JPanel
    private lateinit var apiKeyField: JTextField
    private lateinit var checkKeyButton: JButton
    private lateinit var modelComboBox: JComboBox<String>
    private lateinit var loadingLabel: JLabel
    
    override fun getDisplayName(): String = "KDocGen Settings"
    
    override fun createComponent(): JComponent {
        panel = JPanel(GridBagLayout()).apply {
            border = EmptyBorder(10, 10, 10, 10)
        }
        
        val gbc = GridBagConstraints().apply {
            insets = Insets(5, 5, 5, 5)
            fill = GridBagConstraints.HORIZONTAL
            anchor = GridBagConstraints.WEST
        }
        
        // API Key Input + Check Key Button
        apiKeyField = JTextField(25).apply { text = pluginSettings.apiKey }
        checkKeyButton = JButton("Check Key").apply {
            addActionListener { validateApiKey() }
        }
        
        val apiKeyPanel = JPanel(BorderLayout(5, 0)).apply {
            add(apiKeyField, BorderLayout.CENTER)
            add(checkKeyButton, BorderLayout.EAST)
        }
        
        panel.add(JLabel("OpenAI API Key:"), gbc.apply { gridx = 0; gridy = 0; weightx = 0.0 })
        panel.add(apiKeyPanel, gbc.apply { gridx = 1; weightx = 1.0 })
        
        // Model Selection
        modelComboBox = JComboBox<String>().apply {
            isEnabled = pluginSettings.apiKey.isNotBlank()
            pluginSettings.availableModels.forEach { addItem(it) }
            selectedItem = pluginSettings.selectedModel
        }
        
        panel.add(JLabel("Model:"), gbc.apply { gridx = 0; gridy = 1; weightx = 0.0 })
        panel.add(modelComboBox, gbc.apply { gridx = 1; weightx = 1.0 })
        
        // Loading Indicator (Spinner Text)
        loadingLabel = JLabel("Loading...").apply {
            isVisible = false
            horizontalAlignment = SwingConstants.CENTER
        }
        panel.add(loadingLabel, gbc.apply { gridx = 0; gridy = 2; gridwidth = 2 })
        
        panel.add(JSeparator(SwingConstants.HORIZONTAL), gbc.apply { gridx = 0; gridy = 3; gridwidth = 2 })
        
        return panel
    }
    
    private fun validateApiKey() {
        val apiKey = apiKeyField.text.trim()
        if (apiKey.isEmpty()) {
            Messages.showErrorDialog(panel, "API Key cannot be empty.", "Validation Error")
            return
        }
        
        setLoading(true)
        SwingUtilities.invokeLater {
            val isValid = gptService.validateApiKey(apiKey) // Simulated API call
            SwingUtilities.invokeLater {
                if (isValid) {
                    pluginSettings.apiKey = apiKey
                    Messages.showInfoMessage(panel, "API Key is valid!", "Success")
                    loadAvailableModels()
                } else {
                    setLoading(false)
                    Messages.showErrorDialog(panel, "Invalid API Key. Please check and try again.", "Error")
                }
            }
        }
    }
    
    private fun loadAvailableModels() {
        setLoading(true)
        SwingUtilities.invokeLater {
            val models = gptService.fetchAvailableModels() // Simulated API call
            SwingUtilities.invokeLater {
                setLoading(false)
                if (models.isNotEmpty()) {
                    pluginSettings.availableModels = models
                    modelComboBox.apply {
                        removeAllItems()
                        models.forEach { addItem(it) }
                        selectedItem = pluginSettings.selectedModel
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
    }
    
    override fun isModified(): Boolean =
        apiKeyField.text.trim() != pluginSettings.apiKey ||
            modelComboBox.selectedItem?.toString() != pluginSettings.selectedModel
    
    override fun apply() {
        pluginSettings.apiKey = apiKeyField.text.trim()
        pluginSettings.selectedModel = modelComboBox.selectedItem?.toString() ?: "gpt-4-turbo"
    }
    
    override fun reset() {
        apiKeyField.text = pluginSettings.apiKey
        modelComboBox.removeAllItems()
        pluginSettings.availableModels.forEach { modelComboBox.addItem(it) }
        modelComboBox.selectedItem = pluginSettings.selectedModel
        modelComboBox.isEnabled = pluginSettings.apiKey.isNotBlank()
    }
}