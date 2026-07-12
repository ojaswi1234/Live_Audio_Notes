package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "book_sessions")
data class BookSession(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val author: String,
    val purpose: String,
    val depth: String,
    val focus: String,
    val masterNotes: String,
    val lastTopic: String,
    val lastReadTime: Long = System.currentTimeMillis(),
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "text_chunks", indices = [androidx.room.Index(value = ["sessionId"])])
data class TextChunk(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val sessionId: Int,
    val chunkNumber: Int,
    val inputText: String,
    val summary: String,
    val keyPointsJson: String, // JSON array of points
    val connectionsJson: String, // JSON array of connections
    val questionsJson: String, // JSON array of questions
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "study_cards", indices = [androidx.room.Index(value = ["sessionId"])])
data class StudyCard(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val sessionId: Int,
    val front: String, // Question
    val back: String, // Answer
    val isMastered: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)
