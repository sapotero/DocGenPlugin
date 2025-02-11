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
import com.intellij.psi.PsiFile
import com.intellij.psi.util.PsiTreeUtil
import docs.gen.service.GPTService
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.psi.KtPsiFactory

class DescribeSelectionDialogAction : AnAction() {
    private val gptService = service<GPTService>()
    
    override fun update(event: AnActionEvent) {
        val editor = event.getRequiredData(CommonDataKeys.EDITOR)
        val selectionModel: SelectionModel = editor.selectionModel
        event.presentation.isEnabledAndVisible =
            selectionModel.hasSelection() && selectionModel.selectedText.toString().length >= 16
    }
    
    /**
     * Responds to an action event by generating a detailed function comment based on the selected
     * portion of text within the editor in an IntelliJ platform-based IDE. This method is intended
     * to be used as part of an IntelliJ plugin that automates documentation generation.
     * When triggered, it extracts the selected text from the active editor, invokes an external
     * service to generate the documentation, and inserts this documentation back into the file as
     * a KDoc comment at the start of the selected text.
     *
     * @param event AnActionEvent provided by the action's invocation, containing context like
     *        the current project and data specific to the IDE environment such as the editor and
     *        file currently being edited.
     *
     * This function does not return a value as its type signature is `Unit` (Kotlin's equivalent of `void` in Java).
     *
     * @throws Exception in the following cases:
     *        - If no project is associated with the event (`event.project` is null), it simply returns without executing.
     *        - `event.getRequiredData(CommonDataKeys.EDITOR)` or `event.getRequiredData(CommonDataKeys.PSI_FILE)`
     *          might throw if the required data is not present.
     *        - Document generation or insertion fails. Exceptions within these processes are caught and result in
     *          an error dialog being shown to the user with the exception message.
     */
    override fun actionPerformed(event: AnActionEvent) {
        val currentProject = event.project ?: return
        val editor = event.getRequiredData(CommonDataKeys.EDITOR)
        val psiFile = event.getRequiredData(CommonDataKeys.PSI_FILE)
        val selectionModel: SelectionModel = editor.selectionModel
        
        ProgressManager.getInstance().run(object : Task.Backgroundable(currentProject, "Generating function comment") {
            override fun run(indicator: ProgressIndicator) {
                indicator.isIndeterminate = true
                try {
                    val functionText =
                        ApplicationManager.getApplication().runReadAction(Computable { selectionModel.selectedText.toString() })
                    
                    val documentation = gptService.describeSelection(functionText)
                    
                    ApplicationManager.getApplication().invokeLater {
                        WriteCommandAction.runWriteCommandAction(project) {
                            insertKDocComment(psiFile, selectionModel.selectionStart, documentation)
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
    
    private fun insertKDocComment(psiFile: PsiFile, offset: Int, documentation: String?) {
        val psiFactory = KtPsiFactory(psiFile.project)
        val elementAtOffset = psiFile.findElementAt(offset) ?: return
        val function = PsiTreeUtil.getParentOfType(elementAtOffset, KtNamedFunction::class.java) ?: return
        
        val kdocComment = psiFactory.createComment("$documentation")
        
        WriteCommandAction.runWriteCommandAction(psiFile.project) {
            function.parent.addBefore(kdocComment, function)
        }
    }
}
