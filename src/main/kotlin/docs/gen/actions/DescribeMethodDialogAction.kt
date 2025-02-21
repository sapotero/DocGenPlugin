package docs.gen.actions

import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.components.service
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiElementFactory
import docs.gen.actions.experimental.AbstractAction
import docs.gen.actions.experimental.ShakeResult
import docs.gen.service.GPTService
import docs.gen.utils.removeCodeLines
import org.jetbrains.kotlin.psi.KtClassBody
import org.jetbrains.kotlin.psi.KtNamedFunction

class DescribeMethodDialogAction : AbstractAction() {
    private val gptService = service<GPTService>()
    
    override fun isEnabled(event: AnActionEvent): Boolean {
        val selectedElement = event.getData(CommonDataKeys.NAVIGATABLE)
        return selectedElement is KtNamedFunction && selectedElement.parent is KtClassBody
    }
    
    override fun afterShake(event: AnActionEvent, rawData: String) =
        ShakeResult(
            code = gptService.describeFunction(rawData).toString(),
            removeCodeLines = true
        )
    
    override fun onSuccess(event: AnActionEvent, result: ShakeResult) {
        val currentProject = event.project ?: return
        val selectedElement = event.getData(CommonDataKeys.NAVIGATABLE) as? PsiElement ?: return
        
        WriteCommandAction.runWriteCommandAction(currentProject) {
            val factory = PsiElementFactory.getInstance(currentProject)
            val comment = factory.createDocCommentFromText(result.code.removeCodeLines(), selectedElement)
            selectedElement.parent.addBefore(comment, selectedElement)
        }
    }
    
    override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.BGT
    
}