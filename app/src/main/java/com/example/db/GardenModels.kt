package com.example.db

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Delete
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "garden_items")
data class GardenItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val itemType: String, // e.g., "bonsai", "lantern", "pond"
    val x: Float, // Position in the garden
    val y: Float
)

@Dao
interface GardenItemDao {
    @Query("SELECT * FROM garden_items")
    fun getAllItems(): Flow<List<GardenItemEntity>>

    @Insert
    suspend fun insert(item: GardenItemEntity)

    @Delete
    suspend fun delete(item: GardenItemEntity)
    
    @Query("DELETE FROM garden_items")
    suspend fun clearAll()
}
