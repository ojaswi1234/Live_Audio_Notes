package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "user_stats")
data class UserStats(
    @PrimaryKey val id: Int = 1,
    val totalXp: Int = 0,
    val level: Int = 1,
    val sessionsCompleted: Int = 0,
    val flashcardsMastered: Int = 0,
    val currentStreak: Int = 0,
    val longestStreak: Int = 0,
    val lastActiveDate: Long = 0L
)

@Entity(tableName = "achievements")
data class Achievement(
    @PrimaryKey val id: String,
    val name: String,
    val description: String,
    val iconName: String,
    val unlockedAt: Long = System.currentTimeMillis()
)
