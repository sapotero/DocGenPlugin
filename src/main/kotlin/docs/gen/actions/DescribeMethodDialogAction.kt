package docs.gen.actions

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.components.service
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.util.Computable
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiElementFactory
import com.intellij.psi.util.elementType
import docs.gen.service.GPTService

class DescribeMethodDialogAction : AnAction() {
    private val gptService = service<GPTService>()
    
    override fun update(event: AnActionEvent) {
        val selectedElement = event.getData(CommonDataKeys.NAVIGATABLE)
        val isVisible = selectedElement is PsiElement && (
            selectedElement.elementType.toString() in listOf("METHOD", "FUN"))
        event.presentation.isEnabledAndVisible = isVisible
    }
    
    override fun actionPerformed(event: AnActionEvent) {
        val currentProject = event.project ?: return
        val selectedElement = event.getData(CommonDataKeys.NAVIGATABLE) as? PsiElement ?: return
        
        ProgressManager.getInstance().run(object : Task.Backgroundable(currentProject, "Generating Documentation") {
            override fun run(indicator: ProgressIndicator) {
                indicator.isIndeterminate = true
                try {
                    val functionText =
                        ApplicationManager.getApplication().runReadAction(Computable { selectedElement.text })
                    
                    val documentation = gptService.describeFunction(functionText).toString()
                    
                    ApplicationManager.getApplication().invokeLater {
                        WriteCommandAction.runWriteCommandAction(project) {
                            val factory = PsiElementFactory.getInstance(currentProject)
                            val comment = factory.createDocCommentFromText(documentation, selectedElement)
                            selectedElement.parent.addBefore(comment, selectedElement)
                        }
                    }
                } catch (e: Exception) {
                    ApplicationManager.getApplication().invokeLater {
                        Messages.showErrorDialog(
                            currentProject,
                            "Failed to generate documentation: ${e.message}",
                            "Error"
                        )
                    }
                }
            }
        })
    }
}