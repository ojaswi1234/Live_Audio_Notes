package com.example.viewmodel

import com.example.data.Achievement
import com.example.data.SessionRepository
import com.example.data.UserStats
import kotlinx.coroutines.flow.firstOrNull
import java.util.Calendar

// XP required to go from currentLevel to currentLevel + 1
fun getXpRequiredForLevelUp(currentLevel: Int): Int {
    return 100 + ((currentLevel - 1) * 50)
}

private fun isSameDay(date1: Long, date2: Long): Boolean {
    val cal1 = Calendar.getInstance().apply { timeInMillis = date1 }
    val cal2 = Calendar.getInstance().apply { timeInMillis = date2 }
    return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
           cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR)
}

private fun isYesterday(yesterdayTime: Long, todayTime: Long): Boolean {
    val cal1 = Calendar.getInstance().apply { timeInMillis = yesterdayTime }
    val cal2 = Calendar.getInstance().apply { timeInMillis = todayTime }
    cal2.add(Calendar.DAY_OF_YEAR, -1)
    return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
           cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR)
}

suspend fun awardXp(repository: SessionRepository, amount: Int, reason: String = "") {
    val currentStats = repository.userStats.firstOrNull() ?: UserStats()
    var currentXpInLevel = currentStats.totalXp + amount
    var currentLevel = currentStats.level
    
    // Level up logic
    var xpNeededForNextLevel = getXpRequiredForLevelUp(currentLevel)
    while (currentXpInLevel >= xpNeededForNextLevel) {
        currentXpInLevel -= xpNeededForNextLevel
        currentLevel++
        xpNeededForNextLevel = getXpRequiredForLevelUp(currentLevel)
    }
    
    // Streak logic
    val now = System.currentTimeMillis()
    var newStreak = currentStats.currentStreak
    var newLongestStreak = currentStats.longestStreak
    
    if (currentStats.lastActiveDate == 0L) {
        newStreak = 1
        newLongestStreak = 1
    } else if (!isSameDay(currentStats.lastActiveDate, now)) {
        if (isYesterday(currentStats.lastActiveDate, now)) {
            newStreak++
            if (newStreak > newLongestStreak) newLongestStreak = newStreak
        } else {
            newStreak = 1
        }
    }
    
    val newStats = currentStats.copy(
        totalXp = currentXpInLevel,
        level = currentLevel,
        currentStreak = newStreak,
        longestStreak = newLongestStreak,
        lastActiveDate = now
    )
    repository.saveUserStats(newStats)
    com.example.network.FirebaseManager.syncUserStats(newStats.level, newStats.totalXp, newStats.currentStreak, newStats.longestStreak, newStats.flashcardsMastered, newStats.sessionsCompleted)
    
    // Check achievements
    val currentAchievements = repository.achievements.firstOrNull() ?: emptyList()
    
    if (currentLevel >= 5 && currentAchievements.none { it.id == "lvl_5" }) {
        repository.unlockAchievement(Achievement(id = "lvl_5", name = "Dedicated Reader", description = "Reach Level 5", iconName = "MenuBook"))
    }
    if (currentLevel >= 10 && currentAchievements.none { it.id == "lvl_10" }) {
        repository.unlockAchievement(Achievement(id = "lvl_10", name = "Scholar", description = "Reach Level 10", iconName = "AutoAwesome"))
    }
    if (newStreak >= 3 && currentAchievements.none { it.id == "streak_3" }) {
        repository.unlockAchievement(Achievement(id = "streak_3", name = "On a Roll", description = "3 Day Streak", iconName = "LocalFireDepartment"))
    }
    if (newStreak >= 7 && currentAchievements.none { it.id == "streak_7" }) {
        repository.unlockAchievement(Achievement(id = "streak_7", name = "Habit Builder", description = "7 Day Streak", iconName = "Whatshot"))
    }
}

suspend fun incrementMasteredCards(repository: SessionRepository) {
    val currentStats = repository.userStats.firstOrNull() ?: UserStats()
    val newMasteredCount = currentStats.flashcardsMastered + 1
    
    val newStats = currentStats.copy(flashcardsMastered = newMasteredCount)
    repository.saveUserStats(newStats)
    com.example.network.FirebaseManager.syncUserStats(newStats.level, newStats.totalXp, newStats.currentStreak, newStats.longestStreak, newStats.flashcardsMastered, newStats.sessionsCompleted)
    
    val currentAchievements = repository.achievements.firstOrNull() ?: emptyList()
    if (newMasteredCount >= 10 && currentAchievements.none { it.id == "cards_10" }) {
        repository.unlockAchievement(Achievement(id = "cards_10", name = "Memory Master", description = "Master 10 flashcards", iconName = "AutoAwesome"))
    }
}

suspend fun incrementSessionsCompleted(repository: SessionRepository) {
    val currentStats = repository.userStats.firstOrNull() ?: UserStats()
    val newCount = currentStats.sessionsCompleted + 1
    
    val newStats = currentStats.copy(sessionsCompleted = newCount)
    repository.saveUserStats(newStats)
    com.example.network.FirebaseManager.syncUserStats(newStats.level, newStats.totalXp, newStats.currentStreak, newStats.longestStreak, newStats.flashcardsMastered, newStats.sessionsCompleted)
    
    val currentAchievements = repository.achievements.firstOrNull() ?: emptyList()
    if (newCount >= 1 && currentAchievements.none { it.id == "first_session" }) {
        repository.unlockAchievement(Achievement(id = "first_session", name = "First Steps", description = "Complete your first reading session", iconName = "MenuBook"))
    }
}
