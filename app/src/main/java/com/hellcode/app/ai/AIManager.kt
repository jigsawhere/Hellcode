package com.hellcode.app.ai

import android.util.Log
import com.google.ai.client.generativeai.GenerativeModel
import com.hellcode.app.HellApp
import com.hellcode.app.ai.models.AIChatMessage
import com.hellcode.app.ai.models.OpenRouterCompletionRequest
import com.hellcode.app.ai.models.OpenRouterMessage
import com.hellcode.app.utils.ErrorHandler
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST
import java.util.Properties
import java.util.concurrent.TimeUnit

// Interface for OpenRouter API
interface OpenRouterApiService {
    @POST("api/v1/chat/completions")
    suspend fun getChatCompletion(
        @Header("Authorization") apiKey: String,
        @Body request: OpenRouterCompletionRequest
    ): Response<OpenRouterCompletionResponse>
}

data class OpenRouterCompletionRequest(
    val model: String,
    val messages: List<OpenRouterMessage>
)

data class OpenRouterMessage(
    val role: String,
    val content: String
)

data class OpenRouterCompletionResponse(
    val id: String,
    val choices: List<OpenRouterChoice>,
    val usage: OpenRouterUsage? = null
)

data class OpenRouterChoice(
    val message: OpenRouterMessage,
    val finish_reason: String? = null
)

data class OpenRouterUsage(
    val prompt_tokens: Int,
    val completion_tokens: Int,
    val total_tokens: Int
)

data class AIChatMessage(
    val text: String,
    val isUser: Boolean,
    val timestamp: Long = System.currentTimeMillis()
)

class AIManager(private val application: HellApp) {

    private val _chatMessages = MutableStateFlow<List<AIChatMessage>>(emptyList())
    val chatMessages: StateFlow<List<AIChatMessage>> = _chatMessages.asStateFlow()

    private val apiKeys: Properties by lazy {
        val decryptedContent = application.cryptoStore.loadSecrets()
        val properties = Properties()
        decryptedContent.reader().use { reader ->
            properties.load(reader)
        }
        properties
    }

    private val geminiModel: GenerativeModel by lazy {
        GenerativeModel(
            modelName = "gemini-pro",
            apiKey = apiKeys.getProperty("GEMINI_API_KEY") ?: ""
        )
    }

    private val openRouterApiService: OpenRouterApiService by lazy {
        val logging = HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BODY }
        val httpClient = OkHttpClient.Builder()
            .addInterceptor(logging)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build()

        Retrofit.Builder()
            .baseUrl("https://openrouter.ai/")
            .addConverterFactory(GsonConverterFactory.create())
            .client(httpClient)
            .build()
            .create(OpenRouterApiService::class.java)
    }

    suspend fun getAIResponse(
        prompt: String,
        useOpenRouter: Boolean = false,
        modelName: String = "openrouter/gpt-4.1"
    ): String {
        _chatMessages.value = _chatMessages.value + AIChatMessage(prompt, isUser = true)
        val aiResponse: String
        try {
            if (useOpenRouter) {
                aiResponse = getOpenRouterResponse(prompt, modelName)
            } else {
                aiResponse = getGeminiResponse(prompt)
            }
        } catch (e: Exception) {
            ErrorHandler.log(e, "Error getting AI response")
            aiResponse = ErrorHandler.showUserFriendlyMessage(e)
        }
        _chatMessages.value = _chatMessages.value + AIChatMessage(aiResponse, isUser = false)
        return aiResponse
    }

    private suspend fun getGeminiResponse(prompt: String): String {
        return try {
            val response = geminiModel.generateContent(prompt)
            response.text ?: "No response from Gemini."
        } catch (e: Exception) {
            ErrorHandler.log(e, "Gemini API error")
            "Gemini API Error: ${e.message}"
        }
    }

    private suspend fun getOpenRouterResponse(prompt: String, model: String): String {
        return try {
            val messages = listOf(
                OpenRouterMessage("system", "You are an AI coding assistant for the Hellcode app. Provide concise and helpful code-related responses."),
                OpenRouterMessage("user", prompt)
            )
            val request = OpenRouterCompletionRequest(model, messages)
            val response = openRouterApiService.getChatCompletion(
                "Bearer ${apiKeys.getProperty("OPENROUTER_KEY")}", request
            )

            if (response.isSuccessful) {
                response.body()?.choices?.firstOrNull()?.message?.content ?: "No response from OpenRouter."
            } else {
                val errorBody = response.errorBody()?.string()
                ErrorHandler.log(Exception(errorBody), "OpenRouter API error: ${response.code()}")
                "OpenRouter API Error: ${response.code()} - ${errorBody ?: "Unknown error"}"
            }
        } catch (e: Exception) {
            ErrorHandler.log(e, "OpenRouter API network error")
            "OpenRouter API Network Error: ${e.message}"
        }
    }

    fun clearChatHistory() {
        _chatMessages.value = emptyList()
    }
}