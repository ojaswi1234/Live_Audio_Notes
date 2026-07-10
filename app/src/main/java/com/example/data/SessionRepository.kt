package com.example.data

import kotlinx.coroutines.flow.Flow

class SessionRepository(private val dao: BookSessionDao) {

    val allSessions: Flow<List<BookSession>> = dao.getAllSessions()

    suspend fun getSession(id: Int): BookSession? {
        return dao.getSessionById(id)
    }

    suspend fun saveSession(session: BookSession): Int {
        return dao.insertSession(session).toInt()
    }

    suspend fun updateSession(session: BookSession) {
        dao.updateSession(session)
    }

    suspend fun deleteSession(sessionId: Int) {
        dao.deleteSessionById(sessionId)
        dao.deleteChunksBySessionId(sessionId)
        dao.deleteCardsBySessionId(sessionId)
    }

    fun getChunks(sessionId: Int): Flow<List<TextChunk>> {
        return dao.getChunksForSession(sessionId)
    }

    suspend fun addChunk(chunk: TextChunk): Int {
        return dao.insertChunk(chunk).toInt()
    }

    fun getCards(sessionId: Int): Flow<List<StudyCard>> {
        return dao.getCardsForSession(sessionId)
    }

    suspend fun addCard(card: StudyCard): Int {
        return dao.insertCard(card).toInt()
    }

    suspend fun addCards(cards: List<StudyCard>) {
        dao.insertCards(cards)
    }

    suspend fun updateCard(card: StudyCard) {
        dao.updateCard(card)
    }

    suspend fun deleteCard(cardId: Int) {
        dao.deleteCardById(cardId)
    }
}
