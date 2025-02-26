package docs.gen.actions.experimental

import com.intellij.icons.AllIcons
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.fileTypes.FileTypes
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.ui.EditorTextField
import com.intellij.ui.components.fields.ExtendableTextComponent
import com.intellij.ui.components.fields.ExtendableTextField
import com.intellij.ui.dsl.builder.Align
import com.intellij.ui.dsl.builder.panel
import docs.gen.utils.runAsyncBackgroundTask
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import java.awt.Dimension
import java.awt.event.KeyAdapter
import java.awt.event.KeyEvent
import javax.swing.JComponent
import javax.swing.JProgressBar

class ImproveTextDialog(
    private val project: Project,
    private val editor: Editor,
    private val selectedText: String
) :
    DialogWrapper(project) {
    private var improvedText = selectedText
    private var progressBar: JProgressBar
    private var preview: EditorTextField
    private var searchTextField: ExtendableTextField
    
    init {
        title = "Improve Text"
        
        searchTextField = ExtendableTextField().apply {
            toolTipText = "What do you want to improve?"
            addExtension(
                ExtendableTextComponent.Extension.create(
                    AllIcons.Actions.Find,
                    "Search"
                ) { search(text) }
            
            )
            addKeyListener(object : KeyAdapter() {
                override fun keyPressed(e: KeyEvent) {
                    if (e.keyCode == KeyEvent.VK_ENTER) {
                        e.consume()
                        search(text)
                    }
                }
            })
        }
        
        preview = EditorTextField(selectedText, project, editor.virtualFile?.fileType ?: FileTypes.PLAIN_TEXT).apply {
            setFontInheritedFromLAF(true)
            isFocusable = false
        }
        
        progressBar = JProgressBar().apply {
            isIndeterminate = true
            isVisible = false
        }
        
        init()
    }
    
    override fun createCenterPanel(): JComponent {
        return panel {
            row {
                cell(searchTextField)
                    .align(Align.FILL)
                    .focused()
                    .resizableColumn()
            }
            
            row {
                cell(progressBar)
                    .align(Align.FILL)
            }
            
            row {
                scrollCell(preview)
                    .align(Align.FILL)
                    .resizableColumn()
                    .applyToComponent {
                        preferredSize = Dimension(500, 250)
                    }
            }.resizableRow()
        }.apply {
            preferredSize = Dimension(500, 300)
        }
    }
    
    
    override fun doOKAction() {
        WriteCommandAction.runWriteCommandAction(project) {
            editor.document.replaceString(
                editor.selectionModel.selectionStart,
                editor.selectionModel.selectionEnd,
                improvedText
            )
        }
        super.doOKAction()
    }
    
    private fun search(searchQuery: String) {
        if (searchQuery.isBlank()) return
        
        println("Start searching for $searchQuery")
        
        searchTextField.isEnabled = false
        preview.isEnabled = false
        progressBar.isVisible = true
        
        println("Launching coroutine")
        
        runAsyncBackgroundTask(project, "Loading...") {
            val result = runBlocking {
                fetchImprovedText(searchQuery, preview.text)
            }
            println("Result: $result")
            
            ApplicationManager.getApplication().invokeLater {
                println("Invoke later")
                
                improvedText = result
                preview.text = result
                progressBar.isVisible = false
                preview.isEnabled = true
                searchTextField.isEnabled = true
            }
        }
    }
    
    private suspend fun fetchImprovedText(query: String, text: String): String {
        delay(500) // Simulate async operation
        println("Return from coroutine")
        return "Improved version of: \"$text\" based on \"$query\""
    }
}