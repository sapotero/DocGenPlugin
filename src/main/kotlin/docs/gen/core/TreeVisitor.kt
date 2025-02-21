package docs.gen.core

import com.intellij.psi.util.parentOfTypes
import org.jetbrains.kotlin.descriptors.CallableDescriptor
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.name.FqNameUnsafe
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.psi.KtTreeVisitorVoid
import org.jetbrains.kotlin.renderer.DescriptorRenderer
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.BindingContext.FQNAME_TO_CLASS_DESCRIPTOR
import org.jetbrains.kotlin.resolve.calls.callUtil.getResolvedCall
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe
import org.jetbrains.kotlin.resolve.source.KotlinSourceElement
import org.jetbrains.kotlin.resolve.source.getPsi
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.utils.IDEAPluginsCompatibilityAPI

@OptIn(IDEAPluginsCompatibilityAPI::class)
@Suppress("UnstableApiUsage")
class TreeVisitor(private val bindingContext: BindingContext, val maxDepth: Int = 2) : KtTreeVisitorVoid() {
    private val treeBuilder = StringBuilder()
    private var indentLevel = 0
    private val distinctTypes = mutableSetOf<Pair<String, String?>>()
    private val dataClassStructures = mutableMapOf<String, String>()
    private val functionReferences = mutableMapOf<String, MutableList<Pair<String, String>>>()
    private val processedFunctions = mutableSetOf<String>()
    
    override fun visitCallExpression(expression: KtCallExpression) {
        val resolvedCall = expression.getResolvedCall(bindingContext) ?: return
        processFunctionCall(resolvedCall.resultingDescriptor, expression.containingKtFile.name, 0)
        super.visitCallExpression(expression)
    }
    
    private fun processFunctionCall(functionDescriptor: CallableDescriptor, containingFile: String, depth: Int) {
        if (depth >= maxDepth) return
        
        val functionName = functionDescriptor.name.asString()
        val className = functionDescriptor.containingDeclaration.fqNameSafe.asString()
        val simpleClassName = className.substringAfterLast('.', "Extensions")
        val fullFunctionName = "$simpleClassName.$functionName"
        val returnType = functionDescriptor.returnType
        
        if (!processedFunctions.add(fullFunctionName)) return
        
        val functionDeclaration = resolveFunctionBody(functionDescriptor)
        returnType?.let { collectType(it) }
        functionReferences.computeIfAbsent(containingFile) { mutableListOf() }
            .add(fullFunctionName to functionDeclaration)
        
        appendTreeLine("$fullFunctionName${formatReturnType(returnType)}")
        indentLevel++
        
        if (functionDescriptor is FunctionDescriptor) {
            functionDescriptor.overriddenDescriptors.forEach {
                processFunctionCall(it, containingFile, depth + 1)
            }
        }
        
        indentLevel--
    }
    
    override fun visitNamedFunction(function: KtNamedFunction) {
        val functionName = function.name ?: return
        val className = function.parentOfTypes<KtClass>()?.name ?: "Extensions"
        val fullFunctionName = "$className.$functionName"
        val returnType = bindingContext[BindingContext.FUNCTION, function]?.returnType
        val containingFile = function.containingKtFile.name
        val functionDeclaration = function.text
        
        returnType?.let { collectType(it) }
        functionReferences.computeIfAbsent(containingFile) { mutableListOf() }
            .add(fullFunctionName to functionDeclaration)
        appendTreeLine("$fullFunctionName${formatReturnType(returnType)}")
        indentLevel++
        super.visitNamedFunction(function)
        indentLevel--
    }
    
    private fun resolveFunctionBody(functionDescriptor: CallableDescriptor): String {
        val functionPsi = functionDescriptor.source.getPsi() as? KtNamedFunction
        return functionPsi?.text ?: DescriptorRenderer.FQ_NAMES_IN_TYPES.render(functionDescriptor)
    }
    
    private fun appendTreeLine(line: String) {
        treeBuilder.append("\t".repeat(indentLevel)).append("-> ").append(line).append("\n")
    }
    
    private fun formatReturnType(type: KotlinType?): String {
        return type?.let {
            val formattedType = formatNestedType(it)
            ": $formattedType"
        } ?: ""
    }
    
    private fun formatNestedType(type: KotlinType): String {
        val typeFqn = type.constructor.declarationDescriptor?.fqNameSafe?.asString() ?: "?"
        val typeArgs = type.arguments.joinToString(", ") { formatNestedType(it.type) }
        
        return if (type.arguments.isNotEmpty()) "$typeFqn<$typeArgs>" else typeFqn
    }
    
    private fun collectType(type: KotlinType) {
        
        val data = (type.constructor.declarationDescriptor?.source as? KotlinSourceElement)?.psi?.text
        
        val name = type.constructor.declarationDescriptor?.fqNameSafe?.asString() ?: "?"
        distinctTypes.add(
            getClassSignature(name) to data
        )
        type.arguments.forEach { collectType(it.type) }
        resolveDataClassStructure(type)
    }
    
    private fun resolveDataClassStructure(type: KotlinType) {
        val descriptor = type.constructor.declarationDescriptor as? ClassDescriptor ?: return
        if (!descriptor.isData) return
        if (dataClassStructures.containsKey(descriptor.fqNameSafe.asString())) return
        
        val primaryConstructor = descriptor.constructors.firstOrNull() ?: return
        val properties = primaryConstructor.valueParameters
            .joinToString(", ") { "${it.name.asString()}: ${formatNestedType(it.type)}" }
        
        dataClassStructures[descriptor.fqNameSafe.asString()] = "($properties)"
    }
    
    fun generateFunctionReferenceReport(): String {
        val reportBuilder = StringBuilder()
        functionReferences.forEach { (fileName, functions) ->
            val groupedFunctions = functions.groupBy { it.first.substringBeforeLast('.') }
            groupedFunctions.forEach { (className, functionList) ->
                if (className == "Extensions")
                    reportBuilder.append("class Main {\n")
                else
                    reportBuilder.append("interface $className {\n")
                functionList.forEach { (functionName, functionDeclaration) ->
                    reportBuilder.append("\t$functionDeclaration\n\n")
                }
                reportBuilder.append("}\n\n")
            }
        }
        return reportBuilder.toString()
    }
    
    fun getCallTree() = """
        Call Tree
        ---
        ```text
        $treeBuilder
        ```
    """.trimIndent()
    
    fun generateFullReport(): String {
        return buildString {
            append("\n## Classes:\n")
            append("\n```kotlin\n")
            append(generateFunctionReferenceReport())
            append("\n```\n")
            
            append("\n## Data Types:\n")
            append("\n```kotlin\n")
            distinctTypes.mapNotNull { it.second }.forEach { append("$it\n") }
            append("\n```\n")
            
        }
    }
    
    private fun getClassSignature(fqn: String): String =
        (bindingContext[FQNAME_TO_CLASS_DESCRIPTOR, FqNameUnsafe(fqn)]?.source as? KotlinSourceElement)
            ?.psi
            ?.text ?: fqn
    
}