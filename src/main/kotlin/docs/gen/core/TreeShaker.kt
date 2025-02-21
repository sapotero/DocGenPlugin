package docs.gen.core

import com.intellij.openapi.components.Service
import docs.gen.utils.readAction
import org.jetbrains.kotlin.idea.caches.resolve.analyze
import org.jetbrains.kotlin.psi.KtNamedFunction

@Suppress("UnstableApiUsage")
@Service(Service.Level.APP)
class TreeShaker {
    fun buildTree(function: KtNamedFunction): String =
        readAction {
            TreeVisitor(function.analyze())
                .apply { function.accept(this) }
                .generateFullReport()
        }
}