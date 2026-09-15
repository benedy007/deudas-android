package com.benedy.deudas.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.benedy.deudas.data.local.entity.PaymentEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PaymentDao {
    @Query("SELECT * FROM payments WHERE clientId = :clientId ORDER BY createdAt DESC")
    fun observeByClient(clientId: Long): Flow<List<PaymentEntity>>

    @Query("SELECT * FROM payments WHERE debtId = :debtId ORDER BY createdAt DESC")
    fun observeByDebt(debtId: Long): Flow<List<PaymentEntity>>

    @Query("SELECT * FROM payments ORDER BY id ASC")
    fun observeAll(): Flow<List<PaymentEntity>>

    @Query("SELECT * FROM payments ORDER BY id ASC")
    suspend fun getAll(): List<PaymentEntity>

    @Query("SELECT * FROM payments WHERE id = :id")
    suspend fun getById(id: Long): PaymentEntity?

    @Query("SELECT * FROM payments WHERE groupId = :groupId ORDER BY id ASC")
    suspend fun getByGroupId(groupId: Long): List<PaymentEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(payment: PaymentEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(payments: List<PaymentEntity>)

    @Query("DELETE FROM payments")
    suspend fun deleteAll()
}
