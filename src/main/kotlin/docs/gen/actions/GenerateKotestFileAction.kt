package docs.gen.actions

import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.components.service
import docs.gen.actions.experimental.AbstractAction
import docs.gen.actions.experimental.ShakeResult
import docs.gen.service.GPTService
import docs.gen.utils.removeCodeLines
import org.jetbrains.kotlin.psi.KtDeclarationWithBody
import org.jetbrains.kotlin.psi.KtFile


class GenerateKotestFileAction : AbstractAction() {
    private val gptService = service<GPTService>()
    
    override fun isEnabled(event: AnActionEvent): Boolean {
        val selectedElement = event.getData(CommonDataKeys.NAVIGATABLE)
        return (selectedElement as? KtDeclarationWithBody)?.hasBlockBody() == false
    }
    
    override fun afterShake(event: AnActionEvent, rawData: String) =
        ShakeResult(
            code = gptService.generateTest(rawData).toString(),
            fileFormat = "kt",
            removeCodeLines = true
        )
    
    override fun onSuccess(event: AnActionEvent, result: ShakeResult) {
        val psiFile = event.getData(CommonDataKeys.PSI_FILE) as? KtFile ?: return
        openScratchFile("${psiFile.name}.${result.fileFormat}", result.code.removeCodeLines(), event.project!!)
    }
    
    override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.BGT
}