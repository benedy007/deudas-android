package com.benedy.deudas.data.backup

import com.benedy.deudas.data.local.entity.ClientEntity
import com.benedy.deudas.data.local.entity.CobranzaNoteEntity
import com.benedy.deudas.data.local.entity.DebtEntity
import com.benedy.deudas.data.local.entity.PaymentEntity
import com.benedy.deudas.data.local.entity.ProductEntity
import org.json.JSONArray
import org.json.JSONObject

/**
 * Snapshot of all Room CRM tables for Drive backup/restore.
 * Format version 2 — photoPath, creditLimit, cobranza notes.
 * Photo binaries are device-local (path only in JSON; OK if restore breaks path).
 */
data class BackupPayload(
    val formatVersion: Int = FORMAT_VERSION,
    val exportedAt: Long = System.currentTimeMillis(),
    val clients: List<ClientEntity>,
    val debts: List<DebtEntity>,
    val payments: List<PaymentEntity>,
    val products: List<ProductEntity>,
    val cobranzaNotes: List<CobranzaNoteEntity> = emptyList()
) {
    fun toJson(): String {
        val root = JSONObject()
        root.put("formatVersion", formatVersion)
        root.put("exportedAt", exportedAt)
        root.put("clients", JSONArray().apply {
            clients.forEach { c ->
                put(
                    JSONObject()
                        .put("id", c.id)
                        .put("name", c.name)
                        .put("phone", c.phone)
                        .put("notes", c.notes)
                        .put("direccionCasa", c.direccionCasa)
                        .put("lugarTrabajo", c.lugarTrabajo)
                        .put("direccionTrabajo", c.direccionTrabajo)
                        .put("photoPath", c.photoPath)
                        .put("creditLimit", c.creditLimit ?: JSONObject.NULL)
                        .put("createdAt", c.createdAt)
                )
            }
        })
        root.put("debts", JSONArray().apply {
            debts.forEach { d ->
                put(
                    JSONObject()
                        .put("id", d.id)
                        .put("clientId", d.clientId)
                        .put("description", d.description)
                        .put("originalAmount", d.originalAmount)
                        .put("remainingBalance", d.remainingBalance)
                        .put("createdAt", d.createdAt)
                        .put("fechaEntrega", d.fechaEntrega ?: JSONObject.NULL)
                        .put("planFrequency", d.planFrequency ?: JSONObject.NULL)
                        .put("planPercent", d.planPercent ?: JSONObject.NULL)
                )
            }
        })
        root.put("payments", JSONArray().apply {
            payments.forEach { p ->
                put(
                    JSONObject()
                        .put("id", p.id)
                        .put("debtId", p.debtId)
                        .put("clientId", p.clientId)
                        .put("amount", p.amount)
                        .put("note", p.note)
                        .put("createdAt", p.createdAt)
                        .put("groupId", p.groupId ?: JSONObject.NULL)
                )
            }
        })
        root.put("products", JSONArray().apply {
            products.forEach { pr ->
                put(
                    JSONObject()
                        .put("id", pr.id)
                        .put("name", pr.name)
                        .put("price", pr.price)
                        .put("notes", pr.notes)
                        .put("createdAt", pr.createdAt)
                )
            }
        })
        root.put("cobranzaNotes", JSONArray().apply {
            cobranzaNotes.forEach { n ->
                put(
                    JSONObject()
                        .put("id", n.id)
                        .put("clientId", n.clientId)
                        .put("text", n.text)
                        .put("promisedDate", n.promisedDate ?: JSONObject.NULL)
                        .put("createdAt", n.createdAt)
                )
            }
        })
        return root.toString()
    }

    companion object {
        const val FORMAT_VERSION = 2
        const val BACKUP_FILE_NAME = "deudas-backup.json"

        fun fromJson(json: String): BackupPayload {
            val root = JSONObject(json)
            val clientsArr = root.optJSONArray("clients") ?: JSONArray()
            val debtsArr = root.optJSONArray("debts") ?: JSONArray()
            val paymentsArr = root.optJSONArray("payments") ?: JSONArray()
            val productsArr = root.optJSONArray("products") ?: JSONArray()
            val notesArr = root.optJSONArray("cobranzaNotes") ?: JSONArray()

            val clients = buildList {
                for (i in 0 until clientsArr.length()) {
                    val o = clientsArr.getJSONObject(i)
                    add(
                        ClientEntity(
                            id = o.getLong("id"),
                            name = o.getString("name"),
                            phone = o.getString("phone"),
                            notes = o.nullableString("notes"),
                            direccionCasa = o.nullableString("direccionCasa"),
                            lugarTrabajo = o.nullableString("lugarTrabajo"),
                            direccionTrabajo = o.nullableString("direccionTrabajo"),
                            photoPath = o.nullableString("photoPath"),
                            creditLimit = o.nullableDouble("creditLimit"),
                            createdAt = o.optLong("createdAt", System.currentTimeMillis())
                        )
                    )
                }
            }
            val debts = buildList {
                for (i in 0 until debtsArr.length()) {
                    val o = debtsArr.getJSONObject(i)
                    add(
                        DebtEntity(
                            id = o.getLong("id"),
                            clientId = o.getLong("clientId"),
                            description = o.getString("description"),
                            originalAmount = o.getDouble("originalAmount"),
                            remainingBalance = o.getDouble("remainingBalance"),
                            createdAt = o.optLong("createdAt", System.currentTimeMillis()),
                            fechaEntrega = o.nullableLong("fechaEntrega"),
                            planFrequency = o.nullableString("planFrequency"),
                            planPercent = o.nullableDouble("planPercent")
                        )
                    )
                }
            }
            val payments = buildList {
                for (i in 0 until paymentsArr.length()) {
                    val o = paymentsArr.getJSONObject(i)
                    add(
                        PaymentEntity(
                            id = o.getLong("id"),
                            debtId = o.getLong("debtId"),
                            clientId = o.getLong("clientId"),
                            amount = o.getDouble("amount"),
                            note = o.nullableString("note"),
                            createdAt = o.optLong("createdAt", System.currentTimeMillis()),
                            groupId = o.nullableLong("groupId")
                        )
                    )
                }
            }
            val products = buildList {
                for (i in 0 until productsArr.length()) {
                    val o = productsArr.getJSONObject(i)
                    add(
                        ProductEntity(
                            id = o.getLong("id"),
                            name = o.getString("name"),
                            price = o.getDouble("price"),
                            notes = o.nullableString("notes"),
                            createdAt = o.optLong("createdAt", System.currentTimeMillis())
                        )
                    )
                }
            }
            val notes = buildList {
                for (i in 0 until notesArr.length()) {
                    val o = notesArr.getJSONObject(i)
                    add(
                        CobranzaNoteEntity(
                            id = o.getLong("id"),
                            clientId = o.getLong("clientId"),
                            text = o.getString("text"),
                            promisedDate = o.nullableLong("promisedDate"),
                            createdAt = o.optLong("createdAt", System.currentTimeMillis())
                        )
                    )
                }
            }

            return BackupPayload(
                formatVersion = root.optInt("formatVersion", FORMAT_VERSION),
                exportedAt = root.optLong("exportedAt", 0L),
                clients = clients,
                debts = debts,
                payments = payments,
                products = products,
                cobranzaNotes = notes
            )
        }

        private fun JSONObject.nullableString(key: String): String? {
            if (!has(key) || isNull(key)) return null
            val v = getString(key)
            return v.ifBlank { null }
        }

        private fun JSONObject.nullableLong(key: String): Long? {
            if (!has(key) || isNull(key)) return null
            return getLong(key)
        }

        private fun JSONObject.nullableDouble(key: String): Double? {
            if (!has(key) || isNull(key)) return null
            return getDouble(key)
        }
    }
}
