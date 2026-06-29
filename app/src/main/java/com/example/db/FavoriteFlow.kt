package com.example.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "favorite_flows")
data class FavoriteFlow(
    @PrimaryKey val flowId: String
)
