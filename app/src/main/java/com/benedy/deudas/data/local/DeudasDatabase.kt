package com.benedy.deudas.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.benedy.deudas.data.local.dao.ClientDao
import com.benedy.deudas.data.local.dao.CobranzaNoteDao
import com.benedy.deudas.data.local.dao.DebtDao
import com.benedy.deudas.data.local.dao.PaymentDao
import com.benedy.deudas.data.local.dao.ProductDao
import com.benedy.deudas.data.local.entity.ClientEntity
import com.benedy.deudas.data.local.entity.CobranzaNoteEntity
import com.benedy.deudas.data.local.entity.DebtEntity
import com.benedy.deudas.data.local.entity.PaymentEntity
import com.benedy.deudas.data.local.entity.ProductEntity

@Database(
    entities = [
        ClientEntity::class,
        DebtEntity::class,
        PaymentEntity::class,
        ProductEntity::class,
        CobranzaNoteEntity::class
    ],
    version = 6,
    exportSchema = false
)
abstract class DeudasDatabase : RoomDatabase() {
    abstract fun clientDao(): ClientDao
    abstract fun debtDao(): DebtDao
    abstract fun paymentDao(): PaymentDao
    abstract fun productDao(): ProductDao
    abstract fun cobranzaNoteDao(): CobranzaNoteDao

    companion object {
        @Volatile
        private var INSTANCE: DeudasDatabase? = null

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE clients ADD COLUMN direccionCasa TEXT")
                db.execSQL("ALTER TABLE clients ADD COLUMN lugarTrabajo TEXT")
                db.execSQL("ALTER TABLE clients ADD COLUMN direccionTrabajo TEXT")
            }
        }

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE debts ADD COLUMN fechaEntrega INTEGER")
                db.execSQL("ALTER TABLE payments ADD COLUMN groupId INTEGER")
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_payments_groupId` ON `payments` (`groupId`)"
                )
            }
        }

        val MIGRATION_1_3 = object : Migration(1, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                MIGRATION_1_2.migrate(db)
                MIGRATION_2_3.migrate(db)
            }
        }

        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_payments_groupId` ON `payments` (`groupId`)"
                )
            }
        }

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

        /**
         * v5 → v6 (v1.4.0): client photoPath + creditLimit + cobranza_notes table.
         * No destructive migration.
         */
        val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE clients ADD COLUMN photoPath TEXT")
                db.execSQL("ALTER TABLE clients ADD COLUMN creditLimit REAL")
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `cobranza_notes` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `clientId` INTEGER NOT NULL,
                        `text` TEXT NOT NULL,
                        `promisedDate` INTEGER,
                        `createdAt` INTEGER NOT NULL,
                        FOREIGN KEY(`clientId`) REFERENCES `clients`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_cobranza_notes_clientId` ON `cobranza_notes` (`clientId`)"
                )
            }
        }

        val MIGRATION_4_6 = object : Migration(4, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                MIGRATION_4_5.migrate(db)
                MIGRATION_5_6.migrate(db)
            }
        }

        val MIGRATION_3_6 = object : Migration(3, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                MIGRATION_3_4.migrate(db)
                MIGRATION_4_5.migrate(db)
                MIGRATION_5_6.migrate(db)
            }
        }

        val MIGRATION_2_6 = object : Migration(2, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                MIGRATION_2_3.migrate(db)
                MIGRATION_3_4.migrate(db)
                MIGRATION_4_5.migrate(db)
                MIGRATION_5_6.migrate(db)
            }
        }

        val MIGRATION_1_6 = object : Migration(1, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                MIGRATION_1_2.migrate(db)
                MIGRATION_2_3.migrate(db)
                MIGRATION_3_4.migrate(db)
                MIGRATION_4_5.migrate(db)
                MIGRATION_5_6.migrate(db)
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
                        MIGRATION_1_5,
                        MIGRATION_5_6,
                        MIGRATION_4_6,
                        MIGRATION_3_6,
                        MIGRATION_2_6,
                        MIGRATION_1_6
                    )
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}
