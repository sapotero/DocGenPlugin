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
import docs.gen.service.GPTService

class ImplementMissingCodeInSelectionDialogAction : AnAction() {
    private val gptService = service<GPTService>()

    override fun update(event: AnActionEvent) {
        val editor = event.getRequiredData(CommonDataKeys.EDITOR)
        val selectionModel: SelectionModel = editor.selectionModel
        event.presentation.isEnabledAndVisible =
            selectionModel.hasSelection()
                && selectionModel.selectedText.toString().contains("IMPL")
    }
    
    /**
     * Handles the action of generating code based on the selected text in the editor when triggered by an event.
     * This function is typically bound to a specific action within an IDE's user interface. It extracts the text
     * currently selected by the user in the open editor, sends it to a code generation service, and replaces the
     * selected text with the generated code.
     *
     * The process is executed asynchronously, showing a progress indicator during the operation. Errors during the
     * code generation or replacement are handled gracefully by displaying an error dialog to the user.
     *
     * @param event An instance of [AnActionEvent] which holds the context in which the action was invoked.
     *            This includes references to the project, editor, and other context data.
     *
     * @throws IllegalStateException If the required data from `event` cannot be retrieved (for example, if the editor
     *         or project is not available), which prevents further execution.
     * @throws Exception Propagates exceptions thrown by the external code generation service or internal API errors.
     *         Such exceptions are caught and used to display an error message to the user.
     */
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
                    
                    val code = gptService.generateCode(codeBlock).toString()
                    
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