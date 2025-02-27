package docs.gen.actions.experimental

import com.intellij.icons.AllIcons
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.fileTypes.FileTypes
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.ui.EditorTextField
import com.intellij.ui.JBSplitter
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBList
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.components.fields.ExtendableTextComponent
import com.intellij.ui.components.fields.ExtendableTextField
import com.intellij.ui.dsl.builder.Align
import com.intellij.ui.dsl.builder.panel
import com.intellij.util.ui.JBUI
import docs.gen.service.GPTService
import docs.gen.utils.removeCodeLines
import docs.gen.utils.removeThinkBlocks
import docs.gen.utils.runAsyncBackgroundTask
import kotlinx.coroutines.runBlocking
import java.awt.Dimension
import java.awt.event.KeyAdapter
import java.awt.event.KeyEvent
import javax.swing.JComponent
import javax.swing.JProgressBar
import javax.swing.ListSelectionModel
import javax.swing.ScrollPaneConstants
import javax.swing.SwingUtilities

class ImproveTextDialog(
    private val project: Project,
    private val editor: Editor,
    selectedText: String,
    private val gptService: GPTService
) : DialogWrapper(project) {
    private var improvedText = selectedText
    private var progressBar: JProgressBar
    private var preview: EditorTextField
    private var searchTextField: ExtendableTextField
    private var historyList: JBList<String>
    private val historyData = mutableListOf<Pair<String, String>>()
    private var lastSelectedIndex = -1
    
    init {
        title = "Echidna with AI"
        
        searchTextField = ExtendableTextField().apply {
            toolTipText = "Describe what you need done"
            emptyText.text = "What needs improvement?"
            
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
        
        preview = EditorTextField(selectedText, project, editor.virtualFile?.fileType ?: FileTypes.PLAIN_TEXT)
            .apply {
                setFontInheritedFromLAF(true)
                isFocusable = false
                autoscrolls = true
                isResizable = true
                setOneLineMode(false)
            }
        
        progressBar = JProgressBar().apply {
            isIndeterminate = true
            isVisible = false
        }
        
        historyList = JBList<String>().apply {
            selectionMode = ListSelectionModel.SINGLE_SELECTION
            emptyText.appendLine("Your recent queries")
            emptyText.appendLine("will appear here")
            addListSelectionListener {
                val index = selectedIndex
                if (index != -1) {
                    lastSelectedIndex = index
                    searchTextField.text = historyData[index].first
                    preview.text = historyData[index].second
                }
            }
            cellRenderer = SearchHistoryCellRenderer(130).apply {
                preferredSize = Dimension(JBUI.scale(150), 0)
            }
        }
        
        init()
        setFocusToSearch()
    }
    
    private fun setFocusToSearch() {
        SwingUtilities.invokeLater {
            searchTextField.requestFocusInWindow()
        }
    }
    
    override fun createCenterPanel(): JComponent {
        val leftPanel = panel {
            row {
                cell(
                    JBLabel("Search query").apply {
                        border = JBUI.Borders.empty(8)
                    }
                ).align(Align.FILL)
            }
            row {
                cell(
                    JBScrollPane(historyList)
                ).applyToComponent {
                    preferredSize = Dimension(150, 300)
                    isOpaque = true
                }.align(Align.FILL)
            }
        }.apply {
            preferredSize = Dimension(150, 300)
        }
        
        val rightPanel = panel {
            row {
                cell(searchTextField)
                    .align(Align.FILL)
                    .focused()
                    .resizableColumn()
            }
            row {
                cell(
                    JBScrollPane(preview).apply {
                        horizontalScrollBarPolicy = ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED
                        verticalScrollBarPolicy = ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED
                    }.apply {
                        preferredSize = Dimension(800, 400)
                    }
                )
                    .align(Align.FILL)
                    .resizableColumn()
            }
            row {
                cell(progressBar).align(Align.FILL)
            }
        }
        
        return JBSplitter(false, 0.2f).apply {
            firstComponent = leftPanel
            secondComponent = rightPanel
            
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
        
        searchTextField.isEnabled = false
        preview.isEnabled = false
        progressBar.isVisible = true
        
        runAsyncBackgroundTask(project, "Loading...") {
            val result = runBlocking {
                fetchImprovedText(searchQuery, preview.text)
            }
            
            ApplicationManager.getApplication().invokeLater {
                improvedText = result
                preview.text = result
                progressBar.isVisible = false
                preview.isEnabled = true
                searchTextField.isEnabled = true
                
                val existingIndex = historyData.indexOfFirst { it.first == searchQuery }
                if (existingIndex != -1) {
                    historyData[existingIndex] = searchQuery to result
                } else {
                    historyData.add(searchQuery to result)
                }
                updateHistoryList()
                setFocusToSearch()
            }
        }
    }
    
    private fun updateHistoryList() {
        historyList.setListData(historyData.map { it.first }.toTypedArray())
        if (historyData.isEmpty()) {
            historyList.emptyText.text = "No search history available"
        }
        if (lastSelectedIndex != -1 && lastSelectedIndex < historyData.size) {
            historyList.selectedIndex = lastSelectedIndex
        }
    }
    
    private suspend fun fetchImprovedText(query: String, text: String) =
        gptService
            .enhance(query, text, editor.virtualFile?.fileType ?: FileTypes.PLAIN_TEXT)
            .toString()
            .removeCodeLines()
            .removeThinkBlocks()
}
