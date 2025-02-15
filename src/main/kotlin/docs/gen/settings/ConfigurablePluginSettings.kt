package docs.gen.settings

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.service
import com.intellij.openapi.options.Configurable
import com.intellij.openapi.ui.ComboBox
import com.intellij.openapi.ui.Messages
import com.intellij.util.ui.JBUI
import docs.gen.service.OpenAiService
import docs.gen.settings.PluginSettings.Companion.DEFAULT_MODEL
import docs.gen.settings.features.TreeShakingMode
import java.awt.BorderLayout
import java.awt.Dimension
import java.awt.GridBagConstraints
import java.awt.GridBagLayout
import javax.swing.JButton
import javax.swing.JCheckBox
import javax.swing.JComboBox
import javax.swing.JComponent
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.JSeparator
import javax.swing.JTextField
import javax.swing.SwingConstants
import javax.swing.SwingUtilities

class ConfigurablePluginSettings : Configurable {
    private val pluginSettings = service<PluginSettings>().state
    private val gptService = service<OpenAiService>()
    
    private lateinit var panel: JPanel
    private lateinit var apiKeyField: JTextField
    private lateinit var checkKeyButton: JButton
    private lateinit var modelComboBox: JComboBox<String>
    private lateinit var loadingLabel: JLabel
    
    private lateinit var experimentalFeaturesCheckbox: JCheckBox
    private lateinit var treeShakingComboBox: JComboBox<TreeShakingMode>
    
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
        modelComboBox = ComboBox<String>().apply {
            isEnabled = pluginSettings.apiKey.isNotBlank()
            pluginSettings.availableModels.forEach { addItem(it) }
            selectedItem = pluginSettings.selectedModel
        }
        
        panel.add(JLabel("Model:"), gbc.apply { gridx = 0; gridy = 1; weightx = 0.0 })
        panel.add(modelComboBox, gbc.apply { gridx = 1; weightx = 1.0 })
        
        // Loading Indicator
        loadingLabel = JLabel("Loading...").apply {
            isVisible = false
            horizontalAlignment = SwingConstants.CENTER
        }
        panel.add(loadingLabel, gbc.apply { gridx = 0; gridy = 2; gridwidth = 2 })
        
        panel.add(JSeparator(SwingConstants.HORIZONTAL), gbc.apply { gridx = 0; gridy = 3; gridwidth = 2 })
        
        // Experimental Features Section
        experimentalFeaturesCheckbox =
            JCheckBox("[K1] Enable Experimental Features", pluginSettings.experimentalFeaturesEnabled).apply {
                addActionListener { treeShakingComboBox.isEnabled = isSelected }
            }
        
        panel.add(experimentalFeaturesCheckbox, gbc.apply { gridx = 0; gridy = 4; gridwidth = 2 })
        
        treeShakingComboBox = ComboBox(
            arrayOf(
                TreeShakingMode.DISABLED,
                TreeShakingMode.JUST_BUILD_TREE,
                TreeShakingMode.GENERATE_EMPTY_TEST,
                TreeShakingMode.GENERATE_TEST_WITH_IMPLEMENTATION,
            )
        ).apply {
            selectedItem = pluginSettings.treeShakingMode
            isEnabled = pluginSettings.experimentalFeaturesEnabled
        }
        
        val treeShakingLabel = JLabel("Tree-shaking").apply {
            horizontalAlignment = SwingConstants.LEFT
            preferredSize = Dimension(150, preferredSize.height) // Ensures it has enough width
        }
        
        panel.add(experimentalFeaturesCheckbox, gbc.apply { gridx = 0; gridy = 4; gridwidth = 2 })
        panel.add(treeShakingLabel, gbc.apply { gridx = 0; gridy = 5; weightx = 0.5 })
        panel.add(treeShakingComboBox, gbc.apply { gridx = 1; gridy = 5; weightx = 0.7 })
        
        return panel
    }
    
    private fun validateApiKey() {
        val apiKey = apiKeyField.text.trim()
        if (apiKey.isEmpty()) {
            Messages.showErrorDialog(panel, "API Key cannot be empty.", "Validation Error")
            return
        }
        
        setLoading(true)
        ApplicationManager.getApplication().executeOnPooledThread {
            val isValid = gptService.validateApiKey(apiKey)
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
        ApplicationManager.getApplication().executeOnPooledThread {
            val models = gptService.fetchAvailableModels()
            SwingUtilities.invokeLater {
                setLoading(false)
                if (models.isNotEmpty()) {
                    pluginSettings.availableModels = models
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
    }
    
    override fun isModified(): Boolean =
        apiKeyField.text.trim() != pluginSettings.apiKey ||
            modelComboBox.selectedItem?.toString() != pluginSettings.selectedModel ||
            experimentalFeaturesCheckbox.isSelected != pluginSettings.experimentalFeaturesEnabled ||
            treeShakingComboBox.selectedItem != pluginSettings.treeShakingMode
    
    override fun apply() {
        pluginSettings.apiKey = apiKeyField.text.trim()
        pluginSettings.selectedModel = modelComboBox.selectedItem?.toString() ?: DEFAULT_MODEL
        pluginSettings.experimentalFeaturesEnabled = experimentalFeaturesCheckbox.isSelected
        pluginSettings.treeShakingMode = (treeShakingComboBox.selectedItem as? TreeShakingMode)
            ?: TreeShakingMode.DISABLED
    }
    
    override fun reset() {
        apiKeyField.text = pluginSettings.apiKey
        modelComboBox.removeAllItems()
        pluginSettings.availableModels.forEach { modelComboBox.addItem(it) }
        modelComboBox.selectedItem = pluginSettings.selectedModel
        modelComboBox.isEnabled = pluginSettings.apiKey.isNotBlank()
        
        experimentalFeaturesCheckbox.isSelected = pluginSettings.experimentalFeaturesEnabled
        treeShakingComboBox.selectedItem = pluginSettings.treeShakingMode
        treeShakingComboBox.isEnabled = pluginSettings.experimentalFeaturesEnabled
    }
}
