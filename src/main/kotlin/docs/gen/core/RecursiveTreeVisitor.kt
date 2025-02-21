package docs.gen.core

import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.psi.KtDotQualifiedExpression
import org.jetbrains.kotlin.psi.KtImportDirective
import org.jetbrains.kotlin.psi.KtLambdaExpression
import org.jetbrains.kotlin.psi.KtNamedDeclaration
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.psi.KtProperty
import org.jetbrains.kotlin.psi.KtReferenceExpression
import org.jetbrains.kotlin.psi.KtTreeVisitorVoid
import org.jetbrains.kotlin.psi.KtWhenExpression

class RecursiveTreeVisitor : KtTreeVisitorVoid() {
    val usedClasses = mutableSetOf<String>()
    val usedFunctions = mutableSetOf<String>()
    val usedProperties = mutableSetOf<String>()
    val usedExpressions = mutableSetOf<String>()
    val usedFunctionParams = mutableSetOf<String>()
    val usedLambdas = mutableSetOf<String>()
    val importAliases = mutableMapOf<String, String>()
    
    override fun visitImportDirective(importDirective: KtImportDirective) {
        super.visitImportDirective(importDirective)
        val alias = importDirective.aliasName
        val importedFqName = importDirective.importedFqName?.asString()
        if (alias != null && importedFqName != null) {
            importAliases[alias] = importedFqName
        }
    }
    
    override fun visitReferenceExpression(expression: KtReferenceExpression) {
        super.visitReferenceExpression(expression)
        
        val resolvedElement = expression.reference?.resolve()
        val name = expression.text
        val fqName = importAliases[name] ?: (resolvedElement as? KtNamedDeclaration)?.fqName?.toString()
        
        when (resolvedElement) {
            is KtNamedFunction -> usedFunctions.add(fqName ?: name)
            is KtClassOrObject -> usedClasses.add(fqName ?: name)
            is KtProperty -> usedProperties.add(fqName ?: name)
        }
    }
    
    override fun visitDotQualifiedExpression(expression: KtDotQualifiedExpression) {
        super.visitDotQualifiedExpression(expression)
        expression.selectorExpression?.accept(this)
        usedExpressions.add(expression.text)
    }
    
    override fun visitCallExpression(expression: KtCallExpression) {
        super.visitCallExpression(expression)
        expression.calleeExpression?.accept(this)
        expression.valueArguments.forEach { it.accept(this) }
        expression.lambdaArguments.forEach { it.getLambdaExpression()?.accept(this) }
        usedExpressions.add(expression.text)
    }
    
    override fun visitLambdaExpression(lambdaExpression: KtLambdaExpression) {
        super.visitLambdaExpression(lambdaExpression)
        usedLambdas.add(lambdaExpression.text)
        lambdaExpression.bodyExpression?.acceptChildren(this)
        lambdaExpression.bodyExpression?.accept(this) // Ensure lambda body is fully processed
    }
    
    override fun visitNamedFunction(function: KtNamedFunction) {
        super.visitNamedFunction(function)
        function.valueParameters.forEach { param ->
            val fqType = param.typeReference?.let { typeRef ->
                (typeRef.reference?.resolve() as? KtClassOrObject)?.fqName?.toString() ?: typeRef.text
            }
            fqType?.let { usedClasses.add(it) }
            usedFunctionParams.add("${param.name}: ${fqType}")
        }
        function.bodyExpression?.acceptChildren(this)
    }
    
    override fun visitWhenExpression(whenExpression: KtWhenExpression) {
        super.visitWhenExpression(whenExpression)
        whenExpression.entries.forEach { it.expression?.acceptChildren(this) }
    }
    
    fun report() = buildString {
        appendSorted("Used Classes", usedClasses)
        appendSorted("Used Functions", usedFunctions)
        appendSorted("Used Expressions", usedExpressions)
        appendSorted("Function Parameters", usedFunctionParams)
        appendSorted("Lambdas", usedLambdas)
        appendSorted("Alias", importAliases.entries.map { "${it.key} -> ${it.value}" }.toSet())
    }
    
    private fun StringBuilder.appendSorted(title: String, list: Set<Any>) {
        append("# $title:\n\n")
        append("```\n")
        list.sortedBy { "$it" }.forEach { append(" -> $it\n") }
        append("```\n\n")
    }
}
