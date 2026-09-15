package com.benedy.deudas.ui.history

import com.benedy.deudas.data.local.entity.ClientEntity
import com.benedy.deudas.data.local.entity.PaymentEntity
import com.benedy.deudas.data.payment.PaymentLifo

data class PaymentHistoryItem(
    /** Payment id used to open the receipt (first allocation in the group). */
    val receiptPaymentId: Long,
    val clientId: Long,
    val clientName: String,
    val totalAmount: Double,
    val dateMs: Long,
    val allocationCount: Int,
    /** Only the latest payment unit for this client may be deleted (LIFO). */
    val canDelete: Boolean = false
)

/**
 * Groups waterfall allocations (same groupId) into one history row.
 * Legacy payments without groupId stay as individual rows.
 * Input should be newest-first; output preserves that order.
 * Marks [PaymentHistoryItem.canDelete] per client LIFO rule.
 */
fun groupPaymentsForHistory(
    payments: List<PaymentEntity>,
    clientsById: Map<Long, ClientEntity>
): List<PaymentHistoryItem> {
    val deletableIds = PaymentLifo.deletableReceiptIds(payments)
    val seen = LinkedHashMap<String, MutableList<PaymentEntity>>()
    for (p in payments) {
        val key = p.groupId?.let { "g_$it" } ?: "p_${p.id}"
        seen.getOrPut(key) { mutableListOf() }.add(p)
    }
    return seen.values.map { group ->
        val ordered = group.sortedBy { it.id }
        val first = ordered.first()
        val client = clientsById[first.clientId]
        PaymentHistoryItem(
            receiptPaymentId = first.id,
            clientId = first.clientId,
            clientName = client?.name ?: "Cliente #${first.clientId}",
            totalAmount = group.sumOf { it.amount },
            dateMs = group.maxOf { it.createdAt },
            allocationCount = group.size,
            canDelete = first.id in deletableIds
        )
    }
}
