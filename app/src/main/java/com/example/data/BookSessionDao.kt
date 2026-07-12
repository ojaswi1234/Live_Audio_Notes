package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface BookSessionDao {

    @Query("SELECT * FROM book_sessions ORDER BY createdAt DESC")
    fun getAllSessions(): Flow<List<BookSession>>

    @Query("SELECT * FROM book_sessions WHERE id = :sessionId LIMIT 1")
    suspend fun getSessionById(sessionId: Int): BookSession?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: BookSession): Long

    @Update
    suspend fun updateSession(session: BookSession)

    @Query("DELETE FROM book_sessions WHERE id = :sessionId")
    suspend fun deleteSessionById(sessionId: Int)

    // Chunks
    @Query("SELECT * FROM text_chunks WHERE sessionId = :sessionId ORDER BY chunkNumber ASC")
    fun getChunksForSession(sessionId: Int): Flow<List<TextChunk>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChunk(chunk: TextChunk): Long

    @Query("DELETE FROM text_chunks WHERE sessionId = :sessionId")
    suspend fun deleteChunksBySessionId(sessionId: Int)

    // Flashcards / Study Cards
    @Query("SELECT * FROM study_cards WHERE sessionId = :sessionId ORDER BY createdAt DESC")
    fun getCardsForSession(sessionId: Int): Flow<List<StudyCard>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCard(card: StudyCard): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCards(cards: List<StudyCard>)

    @Update
    suspend fun updateCard(card: StudyCard)

    @Query("DELETE FROM study_cards WHERE id = :cardId")
    suspend fun deleteCardById(cardId: Int)

    @Query("DELETE FROM study_cards WHERE sessionId = :sessionId")
    suspend fun deleteCardsBySessionId(sessionId: Int)

    // User Stats & Gamification
    @Query("SELECT * FROM user_stats WHERE id = 1")
    fun getUserStats(): Flow<UserStats?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUserStats(stats: UserStats)

    @Query("SELECT * FROM achievements ORDER BY unlockedAt DESC")
    fun getAchievements(): Flow<List<Achievement>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAchievement(achievement: Achievement)
}
