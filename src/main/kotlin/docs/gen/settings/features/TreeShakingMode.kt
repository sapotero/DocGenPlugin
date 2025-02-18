package docs.gen.settings.features

enum class TreeShakingMode(private val displayName: String) {
    DISABLED("Disabled"),
    JUST_BUILD_TREE("Just build tree"),
    GENERATE_EMPTY_TEST("Generate empty Kotest file"),
    GENERATE_TEST_WITH_IMPLEMENTATION("Generate Kotest with implementation"),
    GENERATE_QA_REPORT("Generate QA report");
    
    override fun toString(): String = displayName
}
