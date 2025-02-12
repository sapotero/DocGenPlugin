package docs.gen.utils


/**
 * Removes lines from the string that start with the markdown code block indicator "```".
 * This function is useful when processing markdown content where code blocks need to be excluded.
 *
 * The function operates by treating the input string as a sequence of lines, filtering out
 * lines that start with the markdown code block delimiter "```" after leading whitespace is trimmed.
 * Finally, the remaining lines are concatenated back into a single string with newline characters
 * between each line.
 *
 * @return A new string containing only the lines from the original string that do not begin
 *         with the code block delimiter "```", preserving the order and content of the other lines.
 *
 * Example:
 *   Input:  "Normal line\n```code line1\nmore code```\nAnother normal line"
 *   Output: "Normal line\nAnother normal line"
 *
 * Note that this function does not support nested or incomplete code blocks and is designed
 * for simple use cases. For more complex markdown parsing, consider using a dedicated library.
 */
fun String.removeCodeLines(): String =
    lineSequence()
        .filterNot { it.trimStart().startsWith("```") }
        .joinToString("\n")
