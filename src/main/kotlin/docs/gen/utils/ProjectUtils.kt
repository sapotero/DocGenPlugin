package docs.gen.utils

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.testFramework.LightVirtualFile

fun Project.openScratchFile(fileName: String, content: String, stripCodeBlock: Boolean = false) {
    ApplicationManager.getApplication().invokeLater {
        val rawScratchFile = LightVirtualFile(fileName, content.apply {
            if (stripCodeBlock) removeCodeLines()
        })
        VfsUtil.markDirtyAndRefresh(true, false, true, rawScratchFile)
        FileEditorManager.getInstance(this).openFile(rawScratchFile, true)
    }
}