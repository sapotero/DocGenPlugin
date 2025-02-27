package docs.gen.actions

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.components.service
import docs.gen.actions.experimental.ImproveTextDialog
import docs.gen.service.GPTService

class InlineSearchAction : AnAction() {
    private val gptService = service<GPTService>()
    
    override fun actionPerformed(e: AnActionEvent) {
        val editor = e.getData(CommonDataKeys.EDITOR) ?: return
        val project = e.project ?: return
        val selectedText = editor.selectionModel.selectedText ?: return
        
        ImproveTextDialog(project, editor, selectedText, gptService).show()
    }
}
