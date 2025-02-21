package docs.gen.actions.experimental

import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.service
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.testFramework.LightVirtualFile
import docs.gen.core.TreeShaker
import docs.gen.utils.invokeLater
import docs.gen.utils.removeCodeLines
import docs.gen.utils.runAsyncBackgroundTask
import org.jetbrains.kotlin.idea.base.plugin.KotlinPluginMode
import org.jetbrains.kotlin.idea.base.plugin.KotlinPluginModeProvider
import org.jetbrains.kotlin.psi.KtNamedFunction

data class ShakeResult(
    val code: String,
    val fileFormat: String = "md",
    val removeCodeLines: Boolean = false,
)

abstract class AbstractAction : AnAction() {
    private val treeShaker = service<TreeShaker>()
    
    abstract fun isEnabled(event: AnActionEvent): Boolean
    abstract fun afterShake(event: AnActionEvent, rawData: String): ShakeResult
    abstract fun onSuccess(event: AnActionEvent, result: ShakeResult)
    
    override fun update(event: AnActionEvent) {
        val selectedElement = event.getData(CommonDataKeys.NAVIGATABLE)
        val isVisible = selectedElement is KtNamedFunction
        event.presentation.isEnabledAndVisible = isEnabled(event) && isVisible && !isK2Enabled()
    }
    
    override fun actionPerformed(event: AnActionEvent) {
        val project = event.project ?: return
        val selectedElement = event.getData(CommonDataKeys.NAVIGATABLE) as? KtNamedFunction ?: return
        
        runAsyncBackgroundTask(project, "Tree-shaking the file") {
            runCatching {
                afterShake(
                    event,
                    treeShaker.buildTree(selectedElement)
                )
            }.onSuccess { result ->
                val content = result.code.apply {
                    if (result.removeCodeLines) removeCodeLines()
                }
                onSuccess(event, result.copy(code = content))
            }.onFailure {
                invokeLater {
                    Messages.showErrorDialog(
                        project,
                        "Failed to generate: ${it.message}",
                        "Error"
                    )
                }
            }
        }
    }
    
    
    protected fun openScratchFile(rawfileName: String, raw: String, project: Project) {
        ApplicationManager.getApplication().invokeLater {
            val rawScratchFile = LightVirtualFile(rawfileName, raw)
            VfsUtil.markDirtyAndRefresh(true, false, true, rawScratchFile)
            FileEditorManager.getInstance(project).openFile(rawScratchFile, true)
        }
    }
    
    override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.BGT
    
    private fun isK2Enabled(): Boolean =
        KotlinPluginModeProvider.currentPluginMode == KotlinPluginMode.K2
}