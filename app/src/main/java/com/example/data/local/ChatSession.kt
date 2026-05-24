package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "chat_sessions")
data class ChatSession(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val systemInstruction: String = "You are a helpful assistant.",
    val modelName: String = "gemini-3.5-flash",
    val temperature: Float = 0.7f,
    val timestamp: Long = System.currentTimeMillis()
)
