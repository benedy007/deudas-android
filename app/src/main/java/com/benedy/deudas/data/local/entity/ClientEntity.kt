package com.benedy.deudas.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "clients")
data class ClientEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val phone: String,
    val notes: String? = null,
    val direccionCasa: String? = null,
    val lugarTrabajo: String? = null,
    val direccionTrabajo: String? = null,
    /** Relative path under filesDir (e.g. client_photos/uuid.jpg), or null. */
    val photoPath: String? = null,
    /** Optional credit limit; null = no limit. Warn when new debt would exceed. */
    val creditLimit: Double? = null,
    val createdAt: Long = System.currentTimeMillis()
)
