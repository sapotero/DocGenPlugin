package docs.gen.settings.features

enum class OpenApiProvider(val host: String) {
    OpenAI("https://api.openai.com/v1"),
    
    //    GoogleGemini("https://generativelanguage.googleapis.com/v1"),
//    HuggingFace("https://api-inference.huggingface.co"),
//    MistralAI("https://api.mistral.ai/v1"),
    Groq("https://api.groq.com/openai/v1"),
    
    //    PerplexityAI("https://api.perplexity.ai", false),
    Custom("Custom");
}