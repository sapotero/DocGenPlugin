package docs.gen.utils

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Computable

fun <T> runBackgroundTaskAndReturnResult(
    project: Project,
    title: String,
    cancellable: Boolean = true,
    action: () -> T
): Result<T> =
    BackgroundTask(project, title, cancellable, action)
        .let {
            ProgressManager.getInstance().run(it)
            it.result
        }

fun <T> runAsyncBackgroundTask(
    project: Project,
    title: String,
    action: () -> T
) =
    ProgressManager.getInstance().run(
        AsyncBackgroundTask(project, title, action)
    )

class BackgroundTask<T>(
    project: Project,
    title: String,
    cancellable: Boolean,
    private val action: () -> T
) : Task.WithResult<Result<T>, Exception>(project, title, cancellable) {
    
    private var result: Result<T> = Result.failure(IllegalStateException("Task did not execute"))
    
    override fun compute(indicator: ProgressIndicator): Result<T> {
        indicator.isIndeterminate = true
        result = runCatching { action() }
        return result
    }
}

class AsyncBackgroundTask<T>(
    project: Project,
    title: String,
    private val action: () -> T
) : Task.Backgroundable(project, title) {
    
    override fun run(indicator: ProgressIndicator) {
        indicator.isIndeterminate = true
        action()
    }
}

fun invokeLater(block: () -> Any) =
    ApplicationManager.getApplication().invokeLater {
        block()
    }

fun <T> readAction(block: () -> T) =
    ApplicationManager.getApplication().runReadAction(Computable { block() })

fun <T> writeAction(block: () -> T) =
    ApplicationManager.getApplication().runWriteAction(Computable { block() })