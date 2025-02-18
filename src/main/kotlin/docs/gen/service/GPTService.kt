package docs.gen.service

import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import docs.gen.service.domain.ChatRequest
import docs.gen.service.domain.Message
import docs.gen.settings.PluginSettings

@Service
class GPTService {
    
    private val openAiService = service<OpenAiService>()
    private val settings = service<PluginSettings>().state
    
    /**
     * Generates and executes a ChatRequest to a designated model based on a provided function signature and description prompt.
     * This function primarily aids in creating more interactive development environments where automated generation of documentation is needed.
     *
     * @param function the signature or name of the function for which documentation is to be generated. It is used within the ChatRequest to specify the content of the user message.
     * @return a response from the execution of the ChatRequest containing the generated documentation or other relevant information as determined by the API or system logic. The return type is inferred from the context but generally includes details about the API's response to the constructed request.
     * @throws HTTPException if the request fails due to network issues, server errors, or incorrect API usage.
     * @throws IllegalArgumentException if the function parameter is null or improperly formatted, given that a valid function signature or description is required for correct processing.
     */
    fun describeFunction(function: String) =
        ChatRequest(
            model = settings.selectedModel,
            messages = listOf(
                Message(
                    role = "system",
                    content = "Generate a function comment with detailed documentation in the style of KDoc (for Kotlin) or Javadoc (for Java) for the following function;" +
                        "Please include the purpose of the function, parameters with descriptions, the return type with a description, and any possible exceptions thrown." +
                        "Be specific about each part of the documentation. Return only comment block without any markdown markup"
                ),
                Message(role = "user", content = function)
            )
        ).execute()
    
    
    /**
     * Generates a test case template for a given Kotlin function using the Kotest BehaviorSpec style.
     * This function specifically prepares a chat request to simulate a conversation where the function
     * description is processed to create an appropriate test structure.
     *
     * @param function a string that contains the Kotlin function for which the test case needs to be generated.
     *        This string should include the function's signature and its body.
     *
     * @return a ChatRequest object set up to execute the testing assistant simulation, which upon execution,
     *         should yield a test structure. The returned ChatRequest contains messages that direct the testing
     *         assistant to generate the test structure based on the provided function.
     *
     * @throws IllegalArgumentException if the function parameter is an empty string or not properly formatted as
     *         a valid Kotlin function definition, which is necessary for the test generation simulation to work correctly.
     */
    fun generateTestCase(function: String) =
        ChatRequest(
            model = settings.selectedModel,
            messages = listOf(
                Message(
                    role = "system",
                    content = "You are an expert Kotlin developer and testing assistant." +
                        " Given a Kotlin function, generate only the corresponding Kotest test structure using the BehaviorSpec style." +
                        " The output should contain only empty given-when-then blocks with meaningful and precise naming based on the function’s logic." +
                        " Do not include explanations, comments, markdown-markup or any extra text—just the raw test structure."
                ),
                Message(role = "user", content = function)
            )
        ).execute()
    
    /**
     *  This function named 'describeSelection' uses the input string and the current selected model
     *  settings to construct a chat request that contains system and user-messages then executes it.
     *
     *  @param function, a String representing the function for which documentation should be generated.
     *  The role of the function parameter is to generate a chat request that includes
     *  the system and user-messages related to this input.
     *
     *  @return a 'ChatRequestResult', which offers a ChatRequest object after execution. The returned object
     *  is formed using the provided string and the current model selected from settings. This object represents the
     *  complete chat between the user and the system where the user message is basically the content of
     *  the provided function and the system message is a hard-coded string mentioning the requirement of generating KDoc or Javadoc comments.
     *
     *  There isn't any specific exception thrown by this function.
     *  However, failures could occur during the execution of the ChatRequest, such as network errors or
     *  problems with the server response. These should be handled in the code using the ChatRequest
     *  Execute() function.
     */
    fun describeSelection(function: String) =
        ChatRequest(
            model = settings.selectedModel,
            messages = listOf(
                Message(
                    role = "system",
                    content = "Generate a comment with detailed documentation in the style of KDoc (for Kotlin) or Javadoc (for Java) for the following code fragment;" +
                        "Return only comment block without any markdown markup"
                ),
                Message(role = "user", content = function)
            )
        ).execute()
    
    /**
     * The `generateCode` function is used to process a block of code, replacing any comments prefixed with 'IMPL' with the correct implementation.
     * The newly implemented code is then used to execute a chat request.
     *
     * @param codeBlock A String representing a block of Kotlin code. This is the user message which
     * may include possible comments prefixed with 'IMPL' for replacement with the correct implementation.
     *
     * @return The result of ChatRequest execution. This will include the model and messages used for the chat request.
     * The model is selected based on the current settings, and the messages is a list built from a system message and
     * the user message which is the provided code block
     *
     * @throws IllegalStateException if selectedModel in settings is null.
     * @throws RuntimeException if it fails to execute the ChatRequest.
     * Please note that any syntax errors or semantic issues with the 'codeBlock' content would cause the execution to fail.
     */
    fun generateCode(codeBlock: String) =
        ChatRequest(
            model = settings.selectedModel,
            messages = listOf(
                Message(
                    role = "system",
                    content = """
                        You are an expert Kotlin developer.
                        Given the following Kotlin code, replace all comments that start with 'IMPL ' with the appropriate implementation.
                        The implementation should follow best practices, be idiomatic, and ensure correctness.
                        Do not modify any other part of the code
                    """.trimIndent()
                ),
                Message(role = "user", content = codeBlock)
            )
        ).execute()
    
    
    fun generateTreeSaknKotestTests(callTree: String, addExampleImplementation: Boolean = false) =
        ChatRequest(
            model = settings.selectedModel,
            messages = listOf(
                Message(
                    role = "system",
                    content = """
                    You are an expert Kotlin developer and testing assistant.
                    
                    Given a Main class with one function and interfaces with function, that it use. Create a valid Kotest test file.
                    Starts from the Main class's entry function. Follows its complete call tree through dependencies.Tests all functions in the execution chain
                    
                    Strict Rules:
                    - Create nested test only for function defined in Main class.
                    - Does not include test for other classes, use them for better test generation in Main class.
                    - Group related functions into logical categories using `Given` blocks.
                    - Each function must have a `When` block.
                    - Each `When` block must contain at least one `Then` block.
                    - If a function can return an error, add a separate `When` block explicitly handling errors.
                    - Function names must be based on the provided call tree; do not create or infer new functions.
                    - Only generate test cases for functions explicitly defined in the call tree.
                    - Use only the return types defined in the struct definitions; do not introduce new types.
                    - Do not include comments, markdown, or any formatting outside of valid Kotlin syntax.
                    
                    ${
                        "- All given-when-then blocks must be empty, but contain verbose valid name".includeIf(!addExampleImplementation)
                    }
                    
                    
                    Expected Output:
                    - A valid Kotest test class with `BehaviorSpec` style.
                    - Logical `Given` blocks for function groupings.
                    - `When` blocks for each function scenario.
                    
                    ${
                        "- `Then` blocks with empty bodies.".includeIf(!addExampleImplementation)
                    }
                    ${
                        "- Add implementation for each case with kotlin comments using mockk".includeIf(
                            addExampleImplementation
                        )
                    }
                    
                """.trimIndent()
                ),
                Message(
                    role = "user", content = callTree
                )
            )
        ).execute()
    
    fun generateTestPlanBack(callTree: String, functionName: String?) =
        ChatRequest(
            model = settings.selectedModel,
            messages = listOf(
                Message(
                    role = "system",
                    content = """
                    # QA Report for $functionName 🚀

                    ## Enhancements Made:
                    - Added executive summary for quick overview.
                    - Expanded security, performance, and logging sections.
                    - Included test prioritization, idempotency, and environment parity.
                    - Added examples and explicit edge cases.
                    
                    ## Executive Summary 🌟
                    Briefly summarize the function’s purpose, testing scope, critical risks.
                    
                    ### 1. Test Objective 📝
                    
                    #### Function Description
                    - Clearly define the function’s role in the system (e.g., "Processes user payments and updates order status").
                    - Mention business rules it enforces (e.g., "Applies discounts for bulk orders").
                    - List key dependencies (e.g., libraries, SDK versions).
                    
                    #### Expected Behavior
                    - Provide input/output examples (e.g., input: `{ "userId": 123 }`, output: `{ "status": "success" }`).
                    - Detail side effects (e.g., "Updates orders table and sends a confirmation email").
                    
                    ### 2. Test Cases 🧪
                    
                    #### Successful Execution ✅
                    - **Test Case Name**: "Should process valid input within 200ms"
                    - **Test Priority**: Critical/High/Medium
                    - **Examples**: Valid payloads, edge values (e.g., maximum allowed integers).
                    
                    #### Error Handling ⚠️
                    - **Test Case Name**: "Should return 401 if auth token is invalid"
                    - **Validate**: HTTP status codes, error message consistency, and alert triggers (e.g., PagerDuty).
                    
                    #### Edge Case Handling 🔲
                    - **Explicit Scenarios**: Empty arrays, null/undefined inputs, timezone-sensitive dates.
                    - **Boundaries**: Strings at max allowed length, negative values.
                    
                    #### Integration with External Services 🔗
                    - Test: Rate-limiting, idempotency keys, and mock responses (e.g., 5xx errors).
                    
                    ### 3. Test Tools 🛠️
                    - **API Testing**: Postman (for manual tests), REST Assured (automation).
                    - **Contract Testing**: Pact to validate API agreements.
                    - **Performance**: k6 (scriptable load testing).
                    - **Security**: OWASP ZAP for vulnerability scans.
                    
                    ### 4. Test Coverage 📊
                    - **Idempotency**: Ensure duplicate requests don’t cause side effects.
                    - **Backward Compatibility**: Test with older API versions if applicable.
                    
                    ### 5. Performance Considerations ⚡
                    - **Baseline Metrics**: Define acceptable response times (e.g., p95 < 300ms).
                    - **Endurance Testing**: Run for 24hrs to detect memory leaks.
                    - **Resource Usage**: Monitor CPU/memory during load tests.
                    
                    ### 6. Security Considerations 🔒
                    - **Validate**: Encryption (TLS for APIs), OAuth scopes, and input sanitization.
                    - **Compliance**: GDPR, HIPAA, or PCI-DSS requirements.
                    
                    ### 7. Logging and Monitoring 📡
                    - **Log Structure**: JSON format with traceId for cross-service tracing.
                    - **Sensitive Data**: Ensure no PII is logged (e.g., mask credit card numbers).
                    
                    ### 8. Additional Notes 💡
                    - **Environment Parity**: Test environment should mirror production (e.g., same DB version).
                    - **Audit Trails**: Log critical actions (e.g., `order_updated`).
                    - **Test Data Strategy**: Use synthetic data generation tools (e.g., Faker).
                    
                    **Final Tip**: Add a Risk Assessment Matrix (e.g., likelihood vs. impact) to prioritize fixes! 🔥
                    Strict rule - generate report only for $functionName defined in Main class
                """.trimIndent()
                ),
                Message(
                    role = "user", content = callTree
                )
            )
        ).execute()
    
    /**
     * Executes a ChatRequest and returns the description of the request.
     *
     * This function is part of the Chat functionality, and its main purpose is to encapsulate
     * the process of executing a ChatRequest and describing the results in a readable manner.
     *
     * @receiver ChatRequest - an instance on which this extension function is invoked. This ChatRequest object
     * should already have all needed properties set such as request details, user information etc.
     *
     * @return String - The detailed description of the executed chat request.
     * This description is generated by the function 'describe', and it typically includes details
     * about what actions were performed during execution.
     *
     * @throws IllegalStateException - If the ChatRequest could not be executed due to some invalid state,
     * this exception is thrown. The corresponding error message will contain details about the illegal state.
     *
     */
    private fun ChatRequest.execute() = openAiService.sendRequest(this)
    
    fun String.includeIf(condition: Boolean) =
        when {
            condition -> this
            else -> "\n"
        }
}