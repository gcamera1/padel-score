package com.gonzalocamera.padelcounter.mobile.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface MatchDao {
    @Query("SELECT * FROM matches ORDER BY finishedAt DESC")
    fun observeAll(): Flow<List<MatchEntity>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertIfAbsent(match: MatchEntity): Long

    @Query("DELETE FROM matches WHERE id = :matchId")
    suspend fun deleteById(matchId: String)

    @Query("SELECT * FROM matches WHERE id = :matchId")
    suspend fun getById(matchId: String): MatchEntity?
}
