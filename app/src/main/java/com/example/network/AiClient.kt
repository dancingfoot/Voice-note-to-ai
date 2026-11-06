package com.example.network

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit

sealed class AiResponse {
    data class Success(val text: String, val latencyMs: Long, val model: String) : AiResponse()
    data class Error(val message: String, val statusCode: Int? = null) : AiResponse()
}

class AiClient {
    private val client = OkHttpClient.Builder()
        .connectTimeout(12, TimeUnit.SECONDS)
        .readTimeout(35, TimeUnit.SECONDS)
        .writeTimeout(15, TimeUnit.SECONDS)
        .build()

    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    suspend fun query(endpoint: EndpointConfig, prompt: String): AiResponse = withContext(Dispatchers.IO) {
        val startTime = System.currentTimeMillis()
        try {
            when (endpoint.type) {
                EndpointType.LOCAL_NOTES_ONLY -> {
                    AiResponse.Success("Voice note captured locally.", 0, "Local Trigger")
                }
                EndpointType.LOCAL_DEEPSEEK -> {
                    queryOpenAiCompatible(endpoint, prompt, startTime)
                }
                EndpointType.GOOGLE_AI_STUDIO -> {
                    queryGemini(endpoint, prompt, startTime)
                }
                EndpointType.CUSTOM_AGENT -> {
                    queryCustomAgent(endpoint, prompt, startTime)
                }
            }
        } catch (e: java.net.ConnectException) {
            val suggestion = if (endpoint.baseUrl.contains("10.0.2.2")) {
                "Connection refused. Ensure your local container/server is running and listening on 0.0.0.0 (not just 127.0.0.1)."
            } else {
                "Unable to reach ${endpoint.baseUrl}. Verify IP address and network connectivity."
            }
            AiResponse.Error("Network error: $suggestion")
        } catch (e: java.net.SocketTimeoutException) {
            AiResponse.Error("Request timed out. The local model might be loading or busy.")
        } catch (e: Exception) {
            AiResponse.Error("Failed to communicate with ${endpoint.name}: ${e.localizedMessage ?: e.message}")
        }
    }

    suspend fun testConnection(endpoint: EndpointConfig): Pair<Boolean, String> = withContext(Dispatchers.IO) {
        val startTime = System.currentTimeMillis()
        if (endpoint.type == EndpointType.LOCAL_NOTES_ONLY) {
            return@withContext Pair(true, "Ready (On-device engine, 0ms)")
        }
        try {
            val url = cleanUrl(endpoint.baseUrl)
            val request = Request.Builder()
                .url(url)
                .apply {
                    if (endpoint.apiKey.isNotBlank()) {
                        addHeader("Authorization", "Bearer ${endpoint.apiKey}")
                    }
                    endpoint.customHeaders.forEach { (k, v) -> addHeader(k, v) }
                }
                .get()
                .build()

            client.newCall(request).execute().use { response ->
                val duration = System.currentTimeMillis() - startTime
                // Any response (even 401 or 404) proves server reachability
                if (response.isSuccessful || response.code in 400..404) {
                    Pair(true, "Connected (${duration}ms, HTTP ${response.code})")
                } else {
                    Pair(false, "Server replied with HTTP ${response.code} (${duration}ms)")
                }
            }
        } catch (e: Exception) {
            Pair(false, "Unreachable: ${e.message ?: "Check URL and container"}")
        }
    }

    private fun queryOpenAiCompatible(endpoint: EndpointConfig, prompt: String, startTime: Long): AiResponse {
        val base = cleanUrl(endpoint.baseUrl)
        val fullUrl = if (base.endsWith("/chat/completions")) base else "$base/chat/completions"

        val rootJson = JSONObject().apply {
            put("model", endpoint.modelName.ifBlank { "deepseek-chat" })
            put("stream", false)
            val messages = JSONArray().apply {
                if (endpoint.systemPrompt.isNotBlank()) {
                    put(JSONObject().apply {
                        put("role", "system")
                        put("content", endpoint.systemPrompt)
                    })
                }
                put(JSONObject().apply {
                    put("role", "user")
                    put("content", prompt)
                })
            }
            put("messages", messages)
        }

        val requestBuilder = Request.Builder()
            .url(fullUrl)
            .post(rootJson.toString().toRequestBody(jsonMediaType))

        if (endpoint.apiKey.isNotBlank()) {
            requestBuilder.addHeader("Authorization", "Bearer ${endpoint.apiKey}")
        }
        endpoint.customHeaders.forEach { (k, v) -> requestBuilder.addHeader(k, v) }

        client.newCall(requestBuilder.build()).execute().use { response ->
            val latency = System.currentTimeMillis() - startTime
            val body = response.body?.string().orEmpty()
            if (!response.isSuccessful) {
                return AiResponse.Error("HTTP ${response.code}: $body", response.code)
            }
            try {
                val parsed = JSONObject(body)
                val choices = parsed.optJSONArray("choices")
                if (choices != null && choices.length() > 0) {
                    val firstChoice = choices.getJSONObject(0)
                    val message = firstChoice.optJSONObject("message")
                    val content = message?.optString("content") ?: ""
                    return AiResponse.Success(content.trim(), latency, endpoint.modelName)
                }
                // Fallback for custom servers returning "response"
                val altResponse = parsed.optString("response")
                if (altResponse.isNotBlank()) {
                    return AiResponse.Success(altResponse.trim(), latency, endpoint.modelName)
                }
                return AiResponse.Success(body, latency, endpoint.modelName)
            } catch (e: Exception) {
                return AiResponse.Success(body, latency, endpoint.modelName)
            }
        }
    }

    private fun queryGemini(endpoint: EndpointConfig, prompt: String, startTime: Long): AiResponse {
        var model = endpoint.modelName.ifBlank { "gemini-3.8-flash" }
        if (model == "gemini-2.5-flash") {
            model = "gemini-3.8-flash"
        }
        val base = cleanUrl(endpoint.baseUrl)
        val fullUrl = "$base/models/$model:generateContent?key=${endpoint.apiKey}"

        val rootJson = JSONObject().apply {
            val contents = JSONArray().apply {
                val parts = JSONArray().apply {
                    put(JSONObject().apply { put("text", prompt) })
                }
                put(JSONObject().apply {
                    put("role", "user")
                    put("parts", parts)
                })
            }
            put("contents", contents)
            if (endpoint.systemPrompt.isNotBlank()) {
                val sysInstruction = JSONObject().apply {
                    val parts = JSONArray().apply {
                        put(JSONObject().apply { put("text", endpoint.systemPrompt) })
                    }
                    put("parts", parts)
                }
                put("systemInstruction", sysInstruction)
            }
        }

        val request = Request.Builder()
            .url(fullUrl)
            .post(rootJson.toString().toRequestBody(jsonMediaType))
            .build()

        client.newCall(request).execute().use { response ->
            val latency = System.currentTimeMillis() - startTime
            val body = response.body?.string().orEmpty()
            if (!response.isSuccessful) {
                return AiResponse.Error("Gemini Error (${response.code}): $body", response.code)
            }
            try {
                val parsed = JSONObject(body)
                val candidates = parsed.optJSONArray("candidates")
                if (candidates != null && candidates.length() > 0) {
                    val candidate = candidates.getJSONObject(0)
                    val content = candidate.optJSONObject("content")
                    val parts = content?.optJSONArray("parts")
                    if (parts != null && parts.length() > 0) {
                        val text = parts.getJSONObject(0).optString("text")
                        return AiResponse.Success(text.trim(), latency, model)
                    }
                }
                return AiResponse.Success(body, latency, model)
            } catch (e: Exception) {
                return AiResponse.Error("Failed to parse Gemini response: ${e.message}")
            }
        }
    }

    private fun queryCustomAgent(endpoint: EndpointConfig, prompt: String, startTime: Long): AiResponse {
        val base = cleanUrl(endpoint.baseUrl)
        // Check if OpenAI style or generic webhook
        val rootJson = JSONObject().apply {
            put("query", prompt)
            put("prompt", prompt)
            put("message", prompt)
            put("model", endpoint.modelName)
        }

        val requestBuilder = Request.Builder()
            .url(base)
            .post(rootJson.toString().toRequestBody(jsonMediaType))

        if (endpoint.apiKey.isNotBlank()) {
            requestBuilder.addHeader("Authorization", "Bearer ${endpoint.apiKey}")
        }
        endpoint.customHeaders.forEach { (k, v) -> requestBuilder.addHeader(k, v) }

        client.newCall(requestBuilder.build()).execute().use { response ->
            val latency = System.currentTimeMillis() - startTime
            val body = response.body?.string().orEmpty()
            if (!response.isSuccessful) {
                return AiResponse.Error("Agent Error (${response.code}): $body", response.code)
            }
            return AiResponse.Success(body.trim(), latency, endpoint.modelName)
        }
    }

    private fun cleanUrl(url: String): String {
        return url.trim().removeSuffix("/")
    }
}
