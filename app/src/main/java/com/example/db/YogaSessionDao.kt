package com.example.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface YogaSessionDao {
    @Query("SELECT * FROM yoga_sessions ORDER BY timestamp DESC")
    fun getAllSessions(): Flow<List<YogaSession>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: YogaSession)

    @Query("DELETE FROM yoga_sessions")
    suspend fun clearAllSessions()

    @Query("SELECT flowId FROM favorite_flows")
    fun getFavoriteFlowIds(): Flow<List<String>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFavorite(favorite: FavoriteFlow)

    @Query("DELETE FROM favorite_flows WHERE flowId = :flowId")
    suspend fun deleteFavorite(flowId: String)
}

