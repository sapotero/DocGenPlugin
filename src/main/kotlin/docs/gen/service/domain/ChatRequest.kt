package docs.gen.service.domain

data class ChatRequest(
    val model: String,
    val messages: List<Message>
)