package docs.gen.actions

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
import docs.gen.service.GPTService
import docs.gen.settings.PluginSettings
import docs.gen.settings.features.TreeShakingMode.DISABLED
import docs.gen.settings.features.TreeShakingMode.GENERATE_EMPTY_TEST
import docs.gen.settings.features.TreeShakingMode.JUST_BUILD_TREE
import docs.gen.utils.readAction
import docs.gen.utils.removeCodeLines
import docs.gen.utils.runAsyncBackgroundTask
import org.jetbrains.kotlin.idea.base.plugin.KotlinPluginMode
import org.jetbrains.kotlin.idea.base.plugin.KotlinPluginModeProvider
import org.jetbrains.kotlin.idea.caches.resolve.analyze
import org.jetbrains.kotlin.psi.KtDeclarationWithBody
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtNamedFunction

@Suppress("UnstableApiUsage")
class ShakeTreeAction : AnAction() {
    private val gptService = service<GPTService>()
    private val settings = service<PluginSettings>().state
    
    override fun update(event: AnActionEvent) {
        val selectedElement = event.getData(CommonDataKeys.NAVIGATABLE)
        val isVisible = selectedElement is KtNamedFunction
        val hasBody = (selectedElement as? KtDeclarationWithBody)?.hasBlockBody() == false
        val isEnabled = settings.experimentalFeaturesEnabled && settings.treeShakingMode != DISABLED
        event.presentation.isEnabledAndVisible = isEnabled && isVisible && hasBody && !isK2Enabled()
    }
    
    override fun actionPerformed(event: AnActionEvent) {
        val project = event.project ?: return
        val selectedElement = event.getData(CommonDataKeys.NAVIGATABLE) as? KtNamedFunction ?: return
        val psiFile = event.getData(CommonDataKeys.PSI_FILE) as? KtFile ?: return
        
        runAsyncBackgroundTask(project, "Tree-shaking the file") {
            runCatching {
                val rawTree = buildTree(selectedElement)
                
                val generateTreeShakenKotestTests =
                    when (settings.treeShakingMode) {
                        JUST_BUILD_TREE -> rawTree
                        GENERATE_EMPTY_TEST -> gptService.generateTreeSaknKotestTests(rawTree)
                        else -> gptService.generateTreeSaknKotestTests(rawTree, true)
                    }
                generateTreeShakenKotestTests
            }.onSuccess { content ->
                val className = psiFile.name.removeSuffix(".kt")
                
                if (content != null) {
                    
                    if (settings.treeShakingMode == JUST_BUILD_TREE) {
                        val fileName = "Tree shaked ${className}.md"
                        openScratchFile(fileName, content, project)
                        
                    } else {
                        val fileName = "${className}Test.kt"
                        openScratchFile(fileName, content.removeCodeLines(), project)
                    }
                }
            }.onFailure {
                ApplicationManager.getApplication().invokeLater {
                    Messages.showErrorDialog(
                        project,
                        "Failed to generate documentation: ${it.message}",
                        "Error"
                    )
                }
            }
        }
    }
    
    private fun openScratchFile(rawfileName: String, raw: String, project: Project) {
        ApplicationManager.getApplication().invokeLater {
            val rawScratchFile = LightVirtualFile(rawfileName, raw)
            VfsUtil.markDirtyAndRefresh(true, false, true, rawScratchFile)
            FileEditorManager.getInstance(project).openFile(rawScratchFile, true)
        }
    }
    
    override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.BGT
    
    private fun buildTree(function: KtNamedFunction): String =
        readAction {
            TreeVisitor(function.analyze())
                .apply { function.accept(this) }
                .generateFullReport()
        }
    
    private fun isK2Enabled(): Boolean {
        return KotlinPluginModeProvider.currentPluginMode == KotlinPluginMode.K2
    }
}

