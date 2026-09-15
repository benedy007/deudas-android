package com.benedy.deudas.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.benedy.deudas.data.local.entity.CobranzaNoteEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CobranzaNoteDao {
    @Query("SELECT * FROM cobranza_notes WHERE clientId = :clientId ORDER BY createdAt DESC")
    fun observeByClient(clientId: Long): Flow<List<CobranzaNoteEntity>>

    @Query("SELECT * FROM cobranza_notes ORDER BY id ASC")
    suspend fun getAll(): List<CobranzaNoteEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(note: CobranzaNoteEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(notes: List<CobranzaNoteEntity>)

    @Query("DELETE FROM cobranza_notes WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM cobranza_notes")
    suspend fun deleteAll()
}
