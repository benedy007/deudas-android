package com.benedy.deudas.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.benedy.deudas.data.local.entity.DebtEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface DebtDao {
    @Query("SELECT * FROM debts WHERE clientId = :clientId ORDER BY createdAt DESC")
    fun observeByClient(clientId: Long): Flow<List<DebtEntity>>

    @Query("SELECT * FROM debts WHERE clientId = :clientId AND remainingBalance > 0 ORDER BY createdAt DESC")
    fun observeOpenByClient(clientId: Long): Flow<List<DebtEntity>>

    @Query("SELECT * FROM debts ORDER BY id ASC")
    suspend fun getAll(): List<DebtEntity>

    @Query("SELECT * FROM debts WHERE id = :id")
    suspend fun getById(id: Long): DebtEntity?

    @Query("SELECT COALESCE(SUM(remainingBalance), 0) FROM debts WHERE clientId = :clientId")
    fun observeTotalRemaining(clientId: Long): Flow<Double>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(debt: DebtEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(debts: List<DebtEntity>)

    @Update
    suspend fun update(debt: DebtEntity)

    @Query("DELETE FROM debts")
    suspend fun deleteAll()
}
