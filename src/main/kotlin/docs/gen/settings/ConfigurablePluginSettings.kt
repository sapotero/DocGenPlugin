package docs.gen.settings

import com.intellij.openapi.options.Configurable
import javax.swing.JComponent

import com.intellij.openapi.ui.Messages
import com.intellij.openapi.components.service
import docs.gen.service.OpenAiService
import javax.swing.*
import java.awt.BorderLayout
import java.awt.GridBagConstraints
import java.awt.GridBagLayout
import java.awt.Insets
import javax.swing.border.EmptyBorder

class ConfigurablePluginSettings : Configurable {
    private val pluginSettings = service<PluginSettings>().state
    private val gtpService = service<OpenAiService>()
    
    private var panel: JPanel? = null
    private var apiKeyField: JTextField? = null
    private var modelComboBox: JComboBox<String>? = null
    private var loadModelsButton: JButton? = null
    
    
    override fun getDisplayName(): String = "Documentation Builder"
    
    override fun createComponent(): JComponent {
        panel = JPanel(GridBagLayout())
        panel!!.border = EmptyBorder(10, 10, 10, 10)
        
        val gbc = GridBagConstraints()
        gbc.insets = Insets(5, 5, 5, 5)
        gbc.fill = GridBagConstraints.HORIZONTAL
        gbc.anchor = GridBagConstraints.WEST
        
        gbc.gridx = 0
        gbc.gridy = 0
        gbc.weightx = 0.0
        panel!!.add(JLabel("OpenAI API Key:"), gbc)
        
        gbc.gridx = 1
        gbc.gridy = 0
        gbc.weightx = 1.0
        apiKeyField = JTextField(30)
        apiKeyField!!.text = pluginSettings.apiKey
        panel!!.add(apiKeyField, gbc)
        
        gbc.gridx = 0
        gbc.gridy = 1
        gbc.weightx = 0.0
        panel!!.add(JLabel("Model:"), gbc)
        
        gbc.gridx = 1
        gbc.gridy = 1
        gbc.weightx = 1.0
        val modelPanel = JPanel(BorderLayout(5, 0))
        modelComboBox = JComboBox()
        modelPanel.add(modelComboBox, BorderLayout.CENTER)
        loadModelsButton = JButton("Get Models")
        modelPanel.add(loadModelsButton, BorderLayout.EAST)
        panel!!.add(modelPanel, gbc)
        
        gbc.gridx = 1
        gbc.gridy = 2
        gbc.weightx = 1.0
        
        
        val separator = JSeparator(SwingConstants.VERTICAL)
        
        gbc.gridx = 0
        gbc.gridy = 3
        gbc.weightx = 1.0
        panel!!.add(separator, gbc)
        
        gbc.gridx = 0
        gbc.gridy = 4
        
        loadModelsButton!!.addActionListener {
            loadAvailableModels()
        }
        
        modelComboBox!!.isEnabled = pluginSettings.apiKey.isNotBlank()
        loadModelsButton!!.isEnabled = pluginSettings.apiKey.isNotBlank() || apiKeyField!!.text.isNotBlank()
        
        return panel!!
    }
    
    private fun saveSettings() {
        val apiKey = apiKeyField!!.text.trim()
        if (apiKey.isEmpty()) {
            Messages.showErrorDialog(panel, "API Key cannot be empty.", "Validation Error")
            return
        }
        val selectedModel = modelComboBox?.selectedItem?.toString() ?: "gpt-4"
        
        pluginSettings.selectedModel = selectedModel
        pluginSettings.apiKey = apiKey
    }
    
    private fun loadAvailableModels() {
        
        val apiKey = apiKeyField!!.text.trim()
        if (apiKey.isEmpty()) {
            modelComboBox!!.isEnabled = false
            Messages.showErrorDialog(panel, "Please enter a valid API Key first.", "Error")
            return
        }
        
        val models = gtpService.fetchAvailableModels()
        SwingUtilities.invokeLater {
            modelComboBox!!.removeAllItems()
            if (models.isNotEmpty()) {
                models.forEach { model ->
                    modelComboBox!!.addItem(model)
                }
                modelComboBox!!.selectedItem = pluginSettings.selectedModel
                modelComboBox!!.isEnabled = true
            } else {
                Messages.showErrorDialog(
                    panel,
                    "Failed to load models. Please check your API Key and network connection.",
                    "Error"
                )
            }
        }
    }
    
    override fun isModified(): Boolean {
        val currentApiKey = apiKeyField?.text?.trim() ?: ""
        val currentSelectedModel = modelComboBox?.selectedItem?.toString() ?: ""
        
        return currentApiKey != pluginSettings.apiKey ||
            currentSelectedModel != pluginSettings.selectedModel
    }
    
    override fun apply() {
        saveSettings()
    }
    
    override fun reset() {
        apiKeyField?.text = pluginSettings.apiKey
        modelComboBox?.removeAllItems()
    }
}