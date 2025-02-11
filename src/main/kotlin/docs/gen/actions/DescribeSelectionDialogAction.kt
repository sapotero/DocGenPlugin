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
