package com.benedy.deudas.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
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
    version = 3,
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

        /**
         * v1 → v2: optional address fields on clients.
         * Never wipe user data on schema bumps — add Migrations instead of
         * fallbackToDestructiveMigration().
         */
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE clients ADD COLUMN direccionCasa TEXT")
                db.execSQL("ALTER TABLE clients ADD COLUMN lugarTrabajo TEXT")
                db.execSQL("ALTER TABLE clients ADD COLUMN direccionTrabajo TEXT")
            }
        }

        /**
         * v2 → v3: optional delivery/due date on debts + payment groupId
         * for waterfall / multi-debt receipts.
         */
        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE debts ADD COLUMN fechaEntrega INTEGER")
                db.execSQL("ALTER TABLE payments ADD COLUMN groupId INTEGER")
            }
        }

        fun getInstance(context: Context): DeudasDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    DeudasDatabase::class.java,
                    "deudas.db"
                )
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}
