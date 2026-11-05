package com.example.network

enum class EndpointType(val displayName: String, val badge: String) {
    LOCAL_NOTES_ONLY("Quick Notes Only", "LOCAL"),
    LOCAL_DEEPSEEK("Local DeepSeek", "DEEPSEEK"),
    GOOGLE_AI_STUDIO("Google AI Studio", "GEMINI"),
    CUSTOM_AGENT("Custom Agent / Harness", "AGENT")
}

data class EndpointConfig(
    val id: String,
    val name: String,
    val type: EndpointType,
    val baseUrl: String,
    val apiKey: String = "",
    val modelName: String = "",
    val systemPrompt: String = "You are an efficient, concise AI assistant for voice interaction. Keep responses brief, direct, and conversational.",
    val customHeaders: Map<String, String> = emptyMap()
) {
    val badge: String get() = type.badge

    companion object {
        fun defaultPresets(): List<EndpointConfig> = listOf(
            EndpointConfig(
                id = "local_notes",
                name = "Local Notes & Checklists",
                type = EndpointType.LOCAL_NOTES_ONLY,
                baseUrl = "",
                modelName = "On-Device",
                systemPrompt = ""
            ),
            EndpointConfig(
                id = "deepseek_lan",
                name = "DeepSeek (Local LAN / Podman)",
                type = EndpointType.LOCAL_DEEPSEEK,
                baseUrl = "http://192.168.1.100:11434/v1",
                modelName = "deepseek-r1:latest",
                apiKey = ""
            ),
            EndpointConfig(
                id = "deepseek_emulator",
                name = "DeepSeek (Host: 10.0.2.2)",
                type = EndpointType.LOCAL_DEEPSEEK,
                baseUrl = "http://10.0.2.2:11434/v1",
                modelName = "deepseek-chat",
                apiKey = ""
            ),
            EndpointConfig(
                id = "google_ai_studio",
                name = "Google AI Studio",
                type = EndpointType.GOOGLE_AI_STUDIO,
                baseUrl = "https://generativelanguage.googleapis.com/v1beta",
                modelName = "gemini-2.5-flash",
                apiKey = ""
            ),
            EndpointConfig(
                id = "custom_agent",
                name = "Agent Harness / Custom",
                type = EndpointType.CUSTOM_AGENT,
                baseUrl = "http://10.0.2.2:8000/api/v1/chat",
                modelName = "agent-v1",
                apiKey = ""
            )
        )
    }
}
