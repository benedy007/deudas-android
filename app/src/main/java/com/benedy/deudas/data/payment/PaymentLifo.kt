package com.benedy.deudas.data.payment

import com.benedy.deudas.data.local.entity.PaymentEntity

/**
 * LIFO delete rule (per client): only the most recent payment unit may be deleted.
 * Waterfall allocations sharing [PaymentEntity.groupId] are one unit;
 * legacy payments without groupId are each their own unit.
 *
 * Latest unit = max(max(createdAt) in unit), then max(max(id) in unit).
 */
object PaymentLifo {
    const val NOT_LATEST_MESSAGE = "Solo puedes eliminar el último pago de este cliente"

    fun unitKey(payment: PaymentEntity): String =
        payment.groupId?.let { "g_$it" } ?: "p_${payment.id}"

    fun groupIntoUnits(payments: List<PaymentEntity>): Map<String, List<PaymentEntity>> {
        val map = LinkedHashMap<String, MutableList<PaymentEntity>>()
        for (p in payments) {
            map.getOrPut(unitKey(p)) { mutableListOf() }.add(p)
        }
        return map
    }

    fun unitSortKey(unit: List<PaymentEntity>): Pair<Long, Long> {
        require(unit.isNotEmpty())
        return unit.maxOf { it.createdAt } to unit.maxOf { it.id }
    }

    fun latestUnit(clientPayments: List<PaymentEntity>): List<PaymentEntity>? {
        val units = groupIntoUnits(clientPayments).values
        if (units.isEmpty()) return null
        return units.maxWith(
            compareBy<List<PaymentEntity>> { unitSortKey(it).first }
                .thenBy { unitSortKey(it).second }
        )
    }

    fun isDeletable(paymentId: Long, clientPayments: List<PaymentEntity>): Boolean {
        val payment = clientPayments.find { it.id == paymentId } ?: return false
        val latest = latestUnit(clientPayments) ?: return false
        return unitKey(payment) == unitKey(latest.first())
    }

    /** Receipt payment ids (min id in unit) that are currently deletable, across clients. */
    fun deletableReceiptIds(payments: List<PaymentEntity>): Set<Long> {
        return payments.groupBy { it.clientId }.mapNotNull { (_, cps) ->
            val latest = latestUnit(cps) ?: return@mapNotNull null
            latest.minBy { it.id }.id
        }.toSet()
    }
}
