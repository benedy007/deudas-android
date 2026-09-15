package com.benedy.deudas.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Installment frequency stored as nullable string on [DebtEntity.planFrequency].
 * Null / blank = no plan ("Ninguno").
 */
object PlanFrequency {
    const val WEEKLY = "WEEKLY"
    const val BIWEEKLY = "BIWEEKLY"
    const val MONTHLY = "MONTHLY"

    val ALL = listOf(WEEKLY, BIWEEKLY, MONTHLY)

    fun isValid(value: String?): Boolean =
        value == null || value in ALL
}

@Entity(
    tableName = "debts",
    foreignKeys = [
        ForeignKey(
            entity = ClientEntity::class,
            parentColumns = ["id"],
            childColumns = ["clientId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("clientId")]
)
data class DebtEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val clientId: Long,
    val description: String,
    val originalAmount: Double,
    val remainingBalance: Double,
    val createdAt: Long = System.currentTimeMillis(),
    /** Optional delivery / due date (epoch millis). */
    val fechaEntrega: Long? = null,
    /**
     * Optional installment plan frequency:
     * [PlanFrequency.WEEKLY], [PlanFrequency.BIWEEKLY], [PlanFrequency.MONTHLY], or null.
     */
    val planFrequency: String? = null,
    /** Percent of originalAmount that counts as one full cuota (1–100). */
    val planPercent: Double? = null
) {
    /** Cuota amount from originalAmount × percent/100, or null if no plan. */
    fun cuotaAmount(): Double? {
        val freq = planFrequency?.takeIf { it.isNotBlank() } ?: return null
        if (freq !in PlanFrequency.ALL) return null
        val pct = planPercent ?: return null
        if (pct <= 0) return null
        return originalAmount * (pct / 100.0)
    }

    fun hasInstallmentPlan(): Boolean = cuotaAmount() != null
}
