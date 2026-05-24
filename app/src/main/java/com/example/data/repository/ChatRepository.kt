package com.example.data.repository

import com.example.data.local.ChatDao
import com.example.data.local.ChatMessage
import com.example.data.local.ChatSession
import com.example.data.remote.Content
import com.example.data.remote.GenerateContentRequest
import com.example.data.remote.GenerationConfig
import com.example.data.remote.Part
import com.example.data.remote.RetrofitClient
import kotlinx.coroutines.flow.Flow

class ChatRepository(private val chatDao: ChatDao) {

    val allSessions: Flow<List<ChatSession>> = chatDao.getAllSessions()

    fun getMessagesForSession(sessionId: Long): Flow<List<ChatMessage>> {
        return chatDao.getMessagesForSession(sessionId)
    }

    suspend fun getSessionById(sessionId: Long): ChatSession? {
        return chatDao.getSessionById(sessionId)
    }

    suspend fun createSession(
        title: String,
        systemInstruction: String = "You are a helpful assistant.",
        modelName: String = "gemini-3.5-flash",
        temperature: Float = 0.7f
    ): Long {
        val session = ChatSession(
            title = title,
            systemInstruction = systemInstruction,
            modelName = modelName,
            temperature = temperature
        )
        return chatDao.insertSession(session)
    }

    suspend fun updateSession(session: ChatSession) {
        chatDao.updateSession(session)
    }

    suspend fun deleteSession(session: ChatSession) {
        chatDao.deleteSession(session)
    }

    suspend fun insertMessage(message: ChatMessage): Long {
        return chatDao.insertMessage(message)
    }

    suspend fun getLatestMessage(sessionId: Long): ChatMessage? {
        return chatDao.getLatestMessageForSession(sessionId)
    }

    suspend fun generateGeminiResponse(
        sessionId: Long,
        apiKey: String,
        prompt: String,
        history: List<ChatMessage>,
        modelName: String,
        systemInstruction: String?,
        temperature: Float
    ): String {
        // Construct the multi-turn contents list
        val contentsList = mutableListOf<Content>()
        
        // Include loaded history messages before the active prompt
        history.forEach { msg ->
            if (msg.role == "user" || msg.role == "model") {
                contentsList.add(
                    Content(
                        role = msg.role,
                        parts = listOf(Part(text = msg.content))
                    )
                )
            }
        }
        
        // Add the current prompt message
        contentsList.add(
            Content(
                role = "user",
                parts = listOf(Part(text = prompt))
            )
        )

        try {
            val systemContent = if (!systemInstruction.isNullOrBlank()) {
                Content(parts = listOf(Part(text = systemInstruction)))
            } else null

            val req = GenerateContentRequest(
                contents = contentsList,
                systemInstruction = systemContent,
                generationConfig = GenerationConfig(temperature = temperature)
            )

            val cleanedModel = when (modelName.lowercase()) {
                "gemini pro" -> "gemini-3.1-pro-preview"
                "gemini-pro" -> "gemini-3.1-pro-preview"
                "gemini flash" -> "gemini-3.5-flash"
                "gemini-flash" -> "gemini-3.5-flash"
                else -> modelName
            }

            val response = RetrofitClient.service.generateContent(
                model = cleanedModel,
                apiKey = apiKey,
                request = req
            )

            val textResult = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
            if (!textResult.isNullOrEmpty()) {
                return textResult
            }

            if (response.error != null) {
                return "API Error (${response.error.code}): ${response.error.message}"
            }

            return "Received empty response from the AI. Please verify your settings or prompt."
        } catch (e: Exception) {
            e.printStackTrace()
            return "Failed to fetch response: ${e.localizedMessage ?: "Network connection error"}"
        }
    }
}
