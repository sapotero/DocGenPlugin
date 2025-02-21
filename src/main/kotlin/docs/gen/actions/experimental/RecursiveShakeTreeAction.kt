package docs.gen.actions.experimental

import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.components.service
import com.intellij.openapi.ui.Messages
import docs.gen.core.RecursiveTreeVisitor
import docs.gen.service.GPTService
import docs.gen.settings.PluginSettings
import docs.gen.settings.features.TreeShakingMode.DISABLED
import docs.gen.utils.invokeLater
import docs.gen.utils.openScratchFile
import docs.gen.utils.readAction
import docs.gen.utils.runAsyncBackgroundTask
import org.jetbrains.kotlin.idea.base.plugin.KotlinPluginMode
import org.jetbrains.kotlin.idea.base.plugin.KotlinPluginModeProvider
import org.jetbrains.kotlin.psi.KtNamedFunction

class RecursiveShakeTreeAction : AnAction() {
    private val gptService = service<GPTService>()
    private val settings = service<PluginSettings>().state
    
    override fun update(event: AnActionEvent) {
        val selectedElement = event.getData(CommonDataKeys.NAVIGATABLE)
        val isVisible = selectedElement is KtNamedFunction
        val isEnabled = settings.experimentalFeaturesEnabled && settings.treeShakingMode != DISABLED
        event.presentation.isEnabledAndVisible = isEnabled && isVisible && !isK2Enabled()
    }
    
    override fun actionPerformed(event: AnActionEvent) {
        val project = event.project ?: return
        val selectedElement = event.getData(CommonDataKeys.NAVIGATABLE) as? KtNamedFunction ?: return
        
        runAsyncBackgroundTask(project, "Tree-shaking the file") {
            runCatching {
                buildTree(selectedElement)
            }.onSuccess { content ->
                project.openScratchFile("Scratch.md", content)
            }.onFailure {
                invokeLater {
                    Messages.showErrorDialog(
                        project,
                        "Failed to generate documentation: ${it.message}",
                        "Error"
                    )
                }
            }
        }
    }
    
    override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.BGT
    
    private fun buildTree(function: KtNamedFunction): String =
        readAction {
            RecursiveTreeVisitor()
                .apply { function.accept(this) }
                .report()
        }
    
    private fun isK2Enabled(): Boolean {
        return KotlinPluginModeProvider.currentPluginMode == KotlinPluginMode.K2
    }
}

