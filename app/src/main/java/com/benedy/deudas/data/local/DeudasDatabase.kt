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
    version = 5,
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
         * for waterfall / multi-debt receipts. Must also create the
         * index declared on PaymentEntity (Index("groupId")).
         */
        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE debts ADD COLUMN fechaEntrega INTEGER")
                db.execSQL("ALTER TABLE payments ADD COLUMN groupId INTEGER")
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_payments_groupId` ON `payments` (`groupId`)"
                )
            }
        }

        /**
         * v1 → v3: chained safety for installs that skip intermediate bumps.
         */
        val MIGRATION_1_3 = object : Migration(1, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                MIGRATION_1_2.migrate(db)
                MIGRATION_2_3.migrate(db)
            }
        }

        /**
         * v3 → v4: recover devices that already reached schema v3 without
         * the groupId index (old MIGRATION_2_3 only ADDed columns).
         * CREATE INDEX IF NOT EXISTS is idempotent — no data wipe.
         */
        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_payments_groupId` ON `payments` (`groupId`)"
                )
            }
        }

        /**
         * v1 → v4 / v2 → v4: full upgrade paths without destructive fallback.
         */
        val MIGRATION_1_4 = object : Migration(1, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                MIGRATION_1_2.migrate(db)
                MIGRATION_2_3.migrate(db)
                MIGRATION_3_4.migrate(db)
            }
        }

        val MIGRATION_2_4 = object : Migration(2, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                MIGRATION_2_3.migrate(db)
                MIGRATION_3_4.migrate(db)
            }
        }

        /**
         * v4 → v5: optional installment plan on debts
         * (planFrequency TEXT, planPercent REAL). Nullable — existing rows stay null.
         */
        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE debts ADD COLUMN planFrequency TEXT")
                db.execSQL("ALTER TABLE debts ADD COLUMN planPercent REAL")
            }
        }

        val MIGRATION_3_5 = object : Migration(3, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                MIGRATION_3_4.migrate(db)
                MIGRATION_4_5.migrate(db)
            }
        }

        val MIGRATION_2_5 = object : Migration(2, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                MIGRATION_2_3.migrate(db)
                MIGRATION_3_4.migrate(db)
                MIGRATION_4_5.migrate(db)
            }
        }

        val MIGRATION_1_5 = object : Migration(1, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                MIGRATION_1_2.migrate(db)
                MIGRATION_2_3.migrate(db)
                MIGRATION_3_4.migrate(db)
                MIGRATION_4_5.migrate(db)
            }
        }

        fun getInstance(context: Context): DeudasDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    DeudasDatabase::class.java,
                    "deudas.db"
                )
                    .addMigrations(
                        MIGRATION_1_2,
                        MIGRATION_2_3,
                        MIGRATION_1_3,
                        MIGRATION_3_4,
                        MIGRATION_1_4,
                        MIGRATION_2_4,
                        MIGRATION_4_5,
                        MIGRATION_3_5,
                        MIGRATION_2_5,
                        MIGRATION_1_5
                    )
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}
