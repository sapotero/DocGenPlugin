package docs.gen.settings.features

enum class OpenApiProvider(val host: String) {
    OpenAI("https://api.openai.com/v1"),
    Azure("https://your-resource.openai.azure.com/openai"),
    GoogleGemini("https://generativelanguage.googleapis.com/v1"),
    HuggingFace("https://api-inference.huggingface.co"),
    MistralAI("https://api.mistral.ai"),
    Groq("https://api.groq.com"),
    PerplexityAI("https://api.perplexity.ai"),
    Custom("Custom");
}