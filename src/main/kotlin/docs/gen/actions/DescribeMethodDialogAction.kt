package docs.gen.actions

import com.intellij.openapi.actionSystem.ActionUpdateThread
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
import docs.gen.service.GPTService
import org.jetbrains.kotlin.psi.KtClassBody
import org.jetbrains.kotlin.psi.KtNamedFunction

class DescribeMethodDialogAction : AnAction() {
    private val gptService = service<GPTService>()
    
    override fun update(event: AnActionEvent) {
        val selectedElement = event.getData(CommonDataKeys.NAVIGATABLE)
        val isVisible = selectedElement is KtNamedFunction && selectedElement.parent is KtClassBody
        event.presentation.isEnabledAndVisible = isVisible
    }
    
    /**
     * Handles the action of automatic documentation generation for a selected code element within the IDE.
     * This function responds to action events triggered in the IDE to generate and insert documentation comments
     * for a selected `PsiElement`, which represents an element of the code in IntelliJ Platform SDK.
     *
     * @param event An instance of `AnActionEvent` containing context data and necessary information such as the project,
     *        the current editor, and the selected code element. This context is used to determine where the documentation should be generated.
     *
     * Execution Flow:
     * - Retrieves the current project and the selected code element (PsiElement) from the event.
     * - Runs a background task to avoid blocking the main UI thread. This task:
     *   - Marks the progress indicator indeterminate as the duration is unknown.
     *   - Requests a read action to safely fetch the text of the selected element.
     *   - Uses an external service (`gptService`) to generate the documentation for the fetched code.
     *   - Schedules a write action to insert the generated documentation into the codebase as a documentation comment.
     *   - Handles any exceptions during the process by showing an error dialog in the IDE.
     *
     * Note:
     * - The function is designed to be used with coding environments supported by IntelliJ Platform SDK.
     * - It ensures thread-safety by properly managing read and write actions in the IDE's threading model.
     *
     * Throws:
     * - `Exception`: Propagates exceptions from the read or write operations or from the external documentation service.
     *    In such cases, an error dialog is displayed in the UI.
     *
     * @return Nothing is returned as this is an event handler method.
     */
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
    
    override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.BGT
    
}