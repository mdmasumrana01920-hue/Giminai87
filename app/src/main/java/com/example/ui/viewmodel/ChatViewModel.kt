package com.example.ui.viewmodel

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.BuildConfig
import com.example.data.local.AppDatabase
import com.example.data.local.ChatMessage
import com.example.data.local.ChatSession
import com.example.data.repository.ChatRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ChatViewModel(application: Application, private val repository: ChatRepository) : AndroidViewModel(application) {

    private val sharedPreferences: SharedPreferences =
        application.getSharedPreferences("gemini_assistant_settings", Context.MODE_PRIVATE)

    // Reactive lists of all saved database sessions
    val sessions: StateFlow<List<ChatSession>> = repository.allSessions
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    private val _currentSessionId = MutableStateFlow<Long?>(null)
    val currentSessionId: StateFlow<Long?> = _currentSessionId.asStateFlow()

    private val _currentSession = MutableStateFlow<ChatSession?>(null)
    val currentSession: StateFlow<ChatSession?> = _currentSession.asStateFlow()

    // Observe active messages reactively based on selected session ID
    @OptIn(ExperimentalCoroutinesApi::class)
    val currentMessages: StateFlow<List<ChatMessage>> = _currentSessionId
        .flatMapLatest { sessionId ->
            if (sessionId != null) {
                repository.getMessagesForSession(sessionId)
            } else {
                flowOf(emptyList())
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    private val _isGenerating = MutableStateFlow(false)
    val isGenerating: StateFlow<Boolean> = _isGenerating.asStateFlow()

    // Settings overrides
    private val _userApiKey = MutableStateFlow(sharedPreferences.getString("api_key", "") ?: "")
    val userApiKey: StateFlow<String> = _userApiKey.asStateFlow()

    private val _defaultModel = MutableStateFlow(sharedPreferences.getString("default_model", "gemini-3.5-flash") ?: "gemini-3.5-flash")
    val defaultModel: StateFlow<String> = _defaultModel.asStateFlow()

    private val _defaultSystemInstruction = MutableStateFlow(
        sharedPreferences.getString("default_system_instruction", "You are a professional assistant. Answer questions clearly and comprehensively.") ?: "You are a professional assistant."
    )
    val defaultSystemInstruction: StateFlow<String> = _defaultSystemInstruction.asStateFlow()

    private val _defaultTemperature = MutableStateFlow(sharedPreferences.getFloat("default_temperature", 0.7f))
    val defaultTemperature: StateFlow<Float> = _defaultTemperature.asStateFlow()

    init {
        // Initialize with default session if none exists, or load the latest session
        viewModelScope.launch {
            sessions.collect { list ->
                if (list.isNotEmpty() && _currentSessionId.value == null) {
                    selectSession(list.first().id)
                }
            }
        }
    }

    fun selectSession(sessionId: Long) {
        _currentSessionId.value = sessionId
        viewModelScope.launch {
            val session = repository.getSessionById(sessionId)
            _currentSession.value = session
        }
    }

    fun createNewSession(
        title: String,
        systemInstruction: String = _defaultSystemInstruction.value,
        modelName: String = _defaultModel.value,
        temperature: Float = _defaultTemperature.value
    ) {
        viewModelScope.launch {
            val finalTitle = title.ifBlank { "Session #${System.currentTimeMillis().toString().takeLast(4)}" }
            val newId = repository.createSession(
                title = finalTitle,
                systemInstruction = systemInstruction,
                modelName = modelName,
                temperature = temperature
            )
            selectSession(newId)
        }
    }

    fun updateSessionDetails(session: ChatSession) {
        viewModelScope.launch {
            repository.updateSession(session)
            if (_currentSessionId.value == session.id) {
                _currentSession.value = session
            }
        }
    }

    fun deleteSession(session: ChatSession) {
        viewModelScope.launch {
            repository.deleteSession(session)
            // If we deleted the active session, switch to the remaining first session, or clear
            if (_currentSessionId.value == session.id) {
                _currentSessionId.value = null
                _currentSession.value = null
                val remaining = sessions.value.filter { it.id != session.id }
                if (remaining.isNotEmpty()) {
                    selectSession(remaining.first().id)
                }
            }
        }
    }

    fun getActiveApiKey(): String {
        val customKey = _userApiKey.value.trim()
        if (customKey.isNotEmpty()) {
            return customKey
        }
        val buildKey = BuildConfig.GEMINI_API_KEY
        if (buildKey.isNotEmpty() && buildKey != "MY_GEMINI_API_KEY") {
            return buildKey
        }
        return ""
    }

    fun saveApiKey(key: String) {
        _userApiKey.value = key
        sharedPreferences.edit().putString("api_key", key).apply()
    }

    fun saveDefaultModel(model: String) {
        _defaultModel.value = model
        sharedPreferences.edit().putString("default_model", model).apply()
    }

    fun saveDefaultSystemInstruction(instruction: String) {
        _defaultSystemInstruction.value = instruction
        sharedPreferences.edit().putString("default_system_instruction", instruction).apply()
    }

    fun saveDefaultTemperature(temp: Float) {
        _defaultTemperature.value = temp
        sharedPreferences.edit().putFloat("default_temperature", temp).apply()
    }

    fun sendMessage(text: String) {
        val currentId = _currentSessionId.value
        val session = _currentSession.value
        
        if (text.isBlank()) return

        viewModelScope.launch {
            val targetId = if (currentId == null || session == null) {
                // Instantly auto-create a starting session if none is available
                val safeTitle = if (text.length > 20) text.take(20) + "..." else text
                val newId = repository.createSession(
                    title = safeTitle,
                    systemInstruction = _defaultSystemInstruction.value,
                    modelName = _defaultModel.value,
                    temperature = _defaultTemperature.value
                )
                selectSession(newId)
                newId
            } else {
                currentId
            }

            val activeSession = repository.getSessionById(targetId) ?: return@launch

            // 1. Insert User Message locally
            val userMsg = ChatMessage(
                sessionId = targetId,
                role = "user",
                content = text.trim()
            )
            repository.insertMessage(userMsg)

            // If the session was auto-created or holds a timestamp name, rename it according to the first message if appropriate
            if (activeSession.title.startsWith("Session #")) {
                val shortTitle = if (text.length > 25) text.take(25) + "..." else text
                updateSessionDetails(activeSession.copy(title = shortTitle))
            }

            // 2. Clear state, start model loading animation
            _isGenerating.value = true

            // Gather the conversation history for this session (excluding system instruction as we pass it as systemInstruction field)
            val history = currentMessages.value.filter { it.role == "user" || it.role == "model" }

            val apiKey = getActiveApiKey()
            if (apiKey.isBlank()) {
                // Safety callback if API Key is completely missing
                val botErrorMsg = ChatMessage(
                    sessionId = targetId,
                    role = "error",
                    content = "API Key error: No Gemini API Key provided! Please add an API Key inside the Settings panel or configure GEMINI_API_KEY inside the secrets panel to enable responses."
                )
                repository.insertMessage(botErrorMsg)
                _isGenerating.value = false
                return@launch
            }

            // 3. Request reply from repository
            val resultReply = repository.generateGeminiResponse(
                sessionId = targetId,
                apiKey = apiKey,
                prompt = text.trim(),
                history = history,
                modelName = activeSession.modelName,
                systemInstruction = activeSession.systemInstruction,
                temperature = activeSession.temperature
            )

            // 4. Save Gemini response
            val replyMsg = ChatMessage(
                sessionId = targetId,
                role = if (resultReply.startsWith("Failed to fetch response") || resultReply.startsWith("API Error")) "error" else "model",
                content = resultReply
            )
            repository.insertMessage(replyMsg)
            _isGenerating.value = false
        }
    }

    class Factory(private val application: Application, private val repository: ChatRepository) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(ChatViewModel::class.java)) {
                @Suppress("UNCHECKED_CAST")
                return ChatViewModel(application, repository) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
