package docs.gen.service.domain


enum class Role {
    USER, ASSISTANT, SYSTEM;
    
    override fun toString(): String {
        return name.lowercase()
    }
}

data class Message(
    val role: Role,
    val content: String
)