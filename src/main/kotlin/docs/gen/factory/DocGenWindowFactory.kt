package docs.gen.factory


import com.intellij.icons.AllIcons
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBPanel
import com.intellij.ui.content.ContentFactory
import docs.gen.settings.TextBundle
import javax.swing.JButton

class DocGenWindowFactory : ToolWindowFactory {
    
    init {
        thisLogger().warn("Don't forget to remove all non-needed sample code files with their corresponding registration entries in `plugin.xml`.")
    }
    
    override fun createToolWindowContent(project: Project, toolWindow: com.intellij.openapi.wm.ToolWindow) {
        val window = ToolWindow(toolWindow)
        
        val content = ContentFactory.getInstance()
            .createContent(window.getContent(), "", false)
        
        toolWindow.setIcon(AllIcons.Actions.GC)
        toolWindow.stripeTitle = "Stripe title"
        
        toolWindow.contentManager.addContent(content)
        
        
    }
    
    override fun shouldBeAvailable(project: Project) = true
    
    class ToolWindow(toolWindow: com.intellij.openapi.wm.ToolWindow) {
        
//        private val service = toolWindow.project.service<RandomNumberProjectService>()
        
        fun getContent() = JBPanel<JBPanel<*>>().apply {
            val label = JBLabel(TextBundle.message("randomLabel", "?"))
            
            add(label)
            add(JButton(TextBundle.message("shuffle")).apply {
                addActionListener {
                    label.text = TextBundle.message("randomLabel", "test")
                }
            })
        }
    }
}