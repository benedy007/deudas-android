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
    val createdAt: Long = System.currentTimeMillis()
)
