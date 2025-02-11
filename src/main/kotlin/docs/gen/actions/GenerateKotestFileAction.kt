package docs.gen.actions

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.service
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.testFramework.LightVirtualFile
import docs.gen.service.GPTService
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtNamedFunction


class GenerateKotestFileAction : AnAction() {
    private val gptService = service<GPTService>()
    
    override fun update(event: AnActionEvent) {
        val selectedElement = event.getData(CommonDataKeys.PSI_ELEMENT)
        event.presentation.isEnabledAndVisible = selectedElement is KtNamedFunction
    }
    
    /**
     * Handles the action triggered when a user attempts to generate Kotlin test specifications for a function within a Kotlin project.
     * This function is utilized typically through user interaction with UI components, like menus or buttons within an IDE.
     *
     * The function identifies the selected Kotlin function within a project, generates documentation or a testing spec for it using
     * GPT-based services or tools, and creates a scratch file named as `<ClassName>Test.kt` which contains the test specifications.
     *
     * @param event An instance of [AnActionEvent], which provides context about the action performed including the associated data
     *              and the environment in which the action is executed. It's expected to contain the project and the relevant PSI
     *              data for the function and file.
     *              - `project`: the current open project in the IDE where the action is performed.
     *              - `PSI_ELEMENT`: the PSI element linked to the function where the action was invoked.
     *              - `PSI_FILE`: the PSI file which contains the PSI element (function).
     *
     * Operation involves several key steps:
     * 1. Extraction of necessary elements like project, PSI element of the function, and the PSI file.
     * 2. Defining names for the test specification file based on the PSI file name.
     * 3. Generation of test specifications asynchronously via `ProgressManager` with an indeterminate progress.
     * 4. Handling success by writing the generated specifications to a new scratch file.
     * 5. Handling failure by displaying an error message dialog to the user.
     *
     * It's important that the event provides all necessary data. If any of the required data (project, PSI element, or PSI file)
     * is missing, the function will return early without performing any operations.
     *
     * @throws IllegalStateException if there's an issue in fetching required data from `event` or during execution of
     *         asynchronous tasks, typically caused by missing or null obligatory fields within `event`.
     * @throws PsiInvalidElementAccessException if the PSI elements accessed during background execution are no longer valid.
     * @throws Exception for general failures, particularly when performing read/write operations or interacting with external services
     *         for documentation generation. Errors are caught and communicated through a graphical user interface error dialog.
     */
    override fun actionPerformed(event: AnActionEvent) {
        val project = event.project ?: return
        val function = event.getData(CommonDataKeys.PSI_ELEMENT) as? KtNamedFunction ?: return
        val psiFile = event.getData(CommonDataKeys.PSI_FILE) as? KtFile ?: return
        
        val className = psiFile.name.removeSuffix(".kt")
        val testFileName = "${className}Test.kt"
        
        ProgressManager.getInstance().run(object : Task.Backgroundable(project, "Generating Kotest Spec...") {
            override fun run(indicator: ProgressIndicator) {
                indicator.isIndeterminate = true
                try {
                    val functionText = ApplicationManager.getApplication().runReadAction<String> {
                        function.text
                    }
                    
                    val documentation = gptService.generateTestCase(functionText).toString()
                    
                    ApplicationManager.getApplication().invokeLater {
                        createScratchFile(project, testFileName, documentation)
                    }
                } catch (e: Exception) {
                    ApplicationManager.getApplication().invokeLater {
                        Messages.showErrorDialog(
                            project,
                            "Failed to generate test spec: ${e.message}",
                            "Error"
                        )
                    }
                }
            }
        })
    }
    
    private fun createScratchFile(project: Project, fileName: String, content: String) {
        val scratchFile = LightVirtualFile(fileName, content.removeCodeLines())
        VfsUtil.markDirtyAndRefresh(true, true, true, scratchFile)
        FileEditorManager.getInstance(project).openFile(scratchFile, true)
    }
    
    private fun String.removeCodeLines(): String =
        lineSequence()
            .filterNot { it.trimStart().startsWith("```") }
            .joinToString("\n")
}