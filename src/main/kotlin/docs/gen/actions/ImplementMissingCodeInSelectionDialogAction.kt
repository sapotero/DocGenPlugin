package docs.gen.actions

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.components.service
import com.intellij.openapi.editor.SelectionModel
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.util.Computable
import docs.gen.service.OpenAiService


/**
 * test
 * */
class ImplementMissingCodeInSelectionDialogAction : AnAction() {
    override fun update(event: AnActionEvent) {
        val editor = event.getRequiredData(CommonDataKeys.EDITOR)
        val selectionModel: SelectionModel = editor.selectionModel
        event.presentation.isEnabledAndVisible =
            selectionModel.hasSelection()
                && selectionModel.selectedText.toString().contains("IMPL")
    }
    
    private val openAIService = service<OpenAiService>()
    
    override fun actionPerformed(event: AnActionEvent) {
        val currentProject = event.project ?: return
        val editor = event.getRequiredData(CommonDataKeys.EDITOR)
        val selectionModel: SelectionModel = editor.selectionModel
        val startOffset = selectionModel.selectionStart
        val endOffset = selectionModel.selectionEnd
        val document = editor.document
        
        ProgressManager.getInstance().run(object : Task.Backgroundable(currentProject, "Generating code") {
            override fun run(indicator: ProgressIndicator) {
                indicator.isIndeterminate = true
                try {
                    val codeBlock =
                        ApplicationManager.getApplication().runReadAction(Computable { selectionModel.selectedText.toString() })
                    
                    val code = openAIService.generateCode(codeBlock).toString()
                    
                    ApplicationManager.getApplication().invokeLater {
                        WriteCommandAction.runWriteCommandAction(project) {
                            document.deleteString(startOffset, endOffset)
                            document.insertString(startOffset, code)
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