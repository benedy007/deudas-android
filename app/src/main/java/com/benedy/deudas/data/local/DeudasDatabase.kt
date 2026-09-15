package com.benedy.deudas.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.benedy.deudas.data.local.dao.ClientDao
import com.benedy.deudas.data.local.dao.DebtDao
import com.benedy.deudas.data.local.dao.PaymentDao
import com.benedy.deudas.data.local.dao.ProductDao
import com.benedy.deudas.data.local.entity.ClientEntity
import com.benedy.deudas.data.local.entity.DebtEntity
import com.benedy.deudas.data.local.entity.PaymentEntity
import com.benedy.deudas.data.local.entity.ProductEntity

@Database(
    entities = [
        ClientEntity::class,
        DebtEntity::class,
        PaymentEntity::class,
        ProductEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class DeudasDatabase : RoomDatabase() {
    abstract fun clientDao(): ClientDao
    abstract fun debtDao(): DebtDao
    abstract fun paymentDao(): PaymentDao
    abstract fun productDao(): ProductDao

    companion object {
        @Volatile
        private var INSTANCE: DeudasDatabase? = null

        fun getInstance(context: Context): DeudasDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    DeudasDatabase::class.java,
                    "deudas.db"
                ).fallbackToDestructiveMigration().build().also { INSTANCE = it }
            }
        }
    }
}
