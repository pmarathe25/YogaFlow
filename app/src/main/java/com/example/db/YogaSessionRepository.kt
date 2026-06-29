package com.example.db

import kotlinx.coroutines.flow.Flow

class YogaSessionRepository(private val dao: YogaSessionDao) {
    val allSessions: Flow<List<YogaSession>> = dao.getAllSessions()
    val favoriteFlowIds: Flow<List<String>> = dao.getFavoriteFlowIds()

    suspend fun insertSession(session: YogaSession) {
        dao.insertSession(session)
    }

    suspend fun clearSessions() {
        dao.clearAllSessions()
    }

    suspend fun toggleFavorite(flowId: String, isFavorite: Boolean) {
        if (isFavorite) {
            dao.insertFavorite(FavoriteFlow(flowId))
        } else {
            dao.deleteFavorite(flowId)
        }
    }
}

