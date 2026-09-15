package com.benedy.deudas.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "payments",
    foreignKeys = [
        ForeignKey(
            entity = DebtEntity::class,
            parentColumns = ["id"],
            childColumns = ["debtId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("debtId"), Index("clientId"), Index("groupId")]
)
data class PaymentEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val debtId: Long,
    val clientId: Long,
    val amount: Double,
    val note: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    /**
     * Shared id for waterfall allocations that belong to one receipt.
     * Null / self for legacy single-debt payments.
     */
    val groupId: Long? = null
)
