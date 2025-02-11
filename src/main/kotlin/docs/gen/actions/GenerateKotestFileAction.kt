package docs.gen.actions

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.service
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.testFramework.LightVirtualFile
import docs.gen.service.GPTService
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtNamedFunction


class GenerateKotestFileAction : AnAction() {
    private val gptService = service<GPTService>()
    
    override fun update(event: AnActionEvent) {
        val selectedElement = event.getData(CommonDataKeys.PSI_ELEMENT)
        event.presentation.isEnabledAndVisible = selectedElement is KtNamedFunction
    }
    
    override fun actionPerformed(event: AnActionEvent) {
        val project = event.project ?: return
        val function = event.getData(CommonDataKeys.PSI_ELEMENT) as? KtNamedFunction ?: return
        val psiFile = event.getData(CommonDataKeys.PSI_FILE) as? KtFile ?: return
        
        val className = psiFile.name.removeSuffix(".kt")
        val testFileName = "${className}Test.kt"
        
        ProgressManager.getInstance().run(object : Task.Backgroundable(project, "Generating Kotest Spec...") {
            override fun run(indicator: ProgressIndicator) {
                indicator.isIndeterminate = true
                try {
                    val functionText = ApplicationManager.getApplication().runReadAction<String> {
                        function.text
                    }
                    
                    val documentation = gptService.generateTestCase(functionText).toString()
                    
                    ApplicationManager.getApplication().invokeLater {
                        createScratchFile(project, testFileName, documentation)
                    }
                } catch (e: Exception) {
                    ApplicationManager.getApplication().invokeLater {
                        Messages.showErrorDialog(
                            project,
                            "Failed to generate test spec: ${e.message}",
                            "Error"
                        )
                    }
                }
            }
        })
    }
    
    private fun createScratchFile(project: Project, fileName: String, content: String) {
        val scratchFile = LightVirtualFile(fileName, content.removeCodeLines())
        VfsUtil.markDirtyAndRefresh(true, true, true, scratchFile)
        FileEditorManager.getInstance(project).openFile(scratchFile, true)
    }
    
    private fun String.removeCodeLines(): String =
        lineSequence()
            .filterNot { it.trimStart().startsWith("```") }
            .joinToString("\n")
}