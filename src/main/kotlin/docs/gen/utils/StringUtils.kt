package docs.gen.utils

fun String.removeCodeLines(): String =
    lineSequence()
        .filterNot { it.trimStart().startsWith("```") }
        .joinToString("\n")
        .removeThinkBlocks()


fun String.removeThinkBlocks(): String =
    replace(Regex("<think>.*?</think>", RegexOption.DOT_MATCHES_ALL), "")
