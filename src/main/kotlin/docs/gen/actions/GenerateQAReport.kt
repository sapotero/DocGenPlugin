package docs.gen.actions

import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.components.service
import docs.gen.actions.experimental.AbstractAction
import docs.gen.actions.experimental.ShakeResult
import docs.gen.service.GPTService
import docs.gen.utils.readAction
import org.jetbrains.kotlin.psi.KtDeclarationWithBody
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtNamedFunction

class GenerateQAReport : AbstractAction() {
    private val gptService = service<GPTService>()
    
    override fun isEnabled(event: AnActionEvent): Boolean {
        val selectedElement = event.getData(CommonDataKeys.NAVIGATABLE)
        return (selectedElement as? KtDeclarationWithBody)?.hasBlockBody() == false
    }
    
    override fun afterShake(event: AnActionEvent, rawData: String): ShakeResult {
        val selectedElement = event.getData(CommonDataKeys.NAVIGATABLE) as KtNamedFunction
        
        val functionName = readAction { selectedElement.name }

//        return when (type) {
//            JUST_BUILD_TREE -> rawData to "md"
//            GENERATE_EMPTY_TEST -> gptService.generateTreeSaknKotestTests(rawData) to "kt"
//            GENERATE_QA_REPORT -> gptService.generateTestPlanBack(rawData, functionName) to "md"
//            else -> gptService.generateTreeSaknKotestTests(rawData, true) to "md"
//        }
        return ShakeResult(
            code = gptService.generateTestPlanBack(rawData, functionName).toString(),
            fileFormat = "md"
        )
    }
    
    override fun onSuccess(event: AnActionEvent, result: ShakeResult) {
        val psiFile = event.getData(CommonDataKeys.PSI_FILE) as? KtFile ?: return
        openScratchFile("${psiFile.name}.${result.fileFormat}", result.code, event.project!!)
    }
    
    override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.BGT
    
}

