package com.moneymanager.app.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        UserSettingsEntity::class,
        AccountEntity::class,
        CategoryEntity::class,
        TransactionEntity::class,
        BudgetEntity::class,
        DetectedDraftEntity::class
    ],
    version = 23
)
abstract class FinanceDatabase : RoomDatabase() {
    abstract fun dao(): FinanceDao

    companion object {
        @Volatile private var instance: FinanceDatabase? = null

        fun get(context: Context): FinanceDatabase {
            return instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    FinanceDatabase::class.java,
                    "money_manager.db"
                )
                    .addMigrations(
                        Migration1To2,
                        Migration2To3,
                        Migration3To4,
                        Migration4To5,
                        Migration5To6,
                        Migration6To7,
                        Migration7To8,
                        Migration8To9,
                        Migration9To10,
                        Migration10To11,
                        Migration11To12,
                        Migration12To13,
                        Migration13To14,
                        Migration14To15,
                        Migration15To16,
                        Migration16To17,
                        Migration17To18,
                        Migration18To19,
                        Migration19To20,
                        Migration20To21,
                        Migration21To22,
                        Migration22To23
                    )
                    .build()
                    .also { instance = it }
            }
        }

        private val Migration1To2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE user_settings ADD COLUMN themeMode TEXT NOT NULL DEFAULT 'Dark'")
            }
        }

        private val Migration2To3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE detected_drafts ADD COLUMN transactionTimestampMillis INTEGER NOT NULL DEFAULT 0")
                db.execSQL("UPDATE detected_drafts SET transactionTimestampMillis = detectedAtMillis WHERE transactionTimestampMillis = 0")
            }
        }

        private val Migration3To4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE categories ADD COLUMN colorHex TEXT NOT NULL DEFAULT '#8F95A3'")
                db.execSQL("UPDATE categories SET colorHex = '#38E68B' WHERE name = 'Grocery'")
                db.execSQL("UPDATE categories SET colorHex = '#FFC857' WHERE name = 'Food'")
                db.execSQL("UPDATE categories SET colorHex = '#FF4FB8' WHERE name = 'Shopping'")
                db.execSQL("UPDATE categories SET colorHex = '#FF8A3D' WHERE name = 'Fuel'")
                db.execSQL("UPDATE categories SET colorHex = '#FF6B7A' WHERE name = 'Rent'")
            }
        }

        private val Migration4To5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("UPDATE transactions SET categoryId = 0 WHERE isAutoDetected = 1")
            }
        }

        private val Migration5To6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE user_settings ADD COLUMN salaryShiftIncomeEnabled INTEGER NOT NULL DEFAULT 0"
                )
                db.execSQL(
                    "ALTER TABLE user_settings ADD COLUMN salaryShiftWindowDays INTEGER NOT NULL DEFAULT 5"
                )
            }
        }

        private val Migration6To7 = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE transactions ADD COLUMN smsBankLabel TEXT")
                db.execSQL(
                    "ALTER TABLE transactions ADD COLUMN excludeFromSummary INTEGER NOT NULL DEFAULT 0"
                )
                db.execSQL("ALTER TABLE accounts ADD COLUMN smsMatchKey TEXT")
                db.execSQL("ALTER TABLE user_settings ADD COLUMN salaryCategoryId INTEGER")
                db.execSQL(
                    "ALTER TABLE user_settings ADD COLUMN salaryKeywordsForUncategorized INTEGER NOT NULL DEFAULT 1"
                )
                db.execSQL(
                    "ALTER TABLE user_settings ADD COLUMN bankSmsSetupCompleted INTEGER NOT NULL DEFAULT 0"
                )
                db.execSQL(
                    "ALTER TABLE user_settings ADD COLUMN summaryAccountFilterIdsCsv TEXT NOT NULL DEFAULT ''"
                )
            }
        }

        private val Migration7To8 = object : Migration(7, 8) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE user_settings ADD COLUMN uiAccent TEXT NOT NULL DEFAULT 'Sky'")
            }
        }

        private val Migration8To9 = object : Migration(8, 9) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE user_settings ADD COLUMN uiSurface TEXT NOT NULL DEFAULT 'Midnight'")
            }
        }

        private val Migration9To10 = object : Migration(9, 10) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE user_settings ADD COLUMN defaultAccountId INTEGER")
            }
        }

        private val Migration10To11 = object : Migration(10, 11) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE transactions ADD COLUMN isCreditCardTransaction INTEGER NOT NULL DEFAULT 0"
                )
                db.execSQL(
                    """
                    UPDATE transactions
                    SET isCreditCardTransaction = 1,
                        excludeFromSummary = 1
                    WHERE lower(COALESCE(rawMessage, '')) LIKE '%spent%card%'
                       OR lower(COALESCE(rawMessage, '')) LIKE '%bank card%'
                       OR lower(COALESCE(rawMessage, '')) LIKE '%credit card%debited%for upi%'
                    """.trimIndent()
                )
            }
        }

        private val Migration11To12 = object : Migration(11, 12) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE transactions ADD COLUMN description TEXT")
            }
        }

        private val Migration12To13 = object : Migration(12, 13) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE user_settings ADD COLUMN offlineLlmParsingEnabled INTEGER NOT NULL DEFAULT 0"
                )
                db.execSQL(
                    "ALTER TABLE user_settings ADD COLUMN offlineLlmModelDownloaded INTEGER NOT NULL DEFAULT 0"
                )
            }
        }

        private val Migration13To14 = object : Migration(13, 14) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE accounts ADD COLUMN accountType TEXT NOT NULL DEFAULT 'Bank'")
                db.execSQL("ALTER TABLE transactions ADD COLUMN fromAccountId INTEGER")
                db.execSQL("ALTER TABLE transactions ADD COLUMN toAccountId INTEGER")
                db.execSQL(
                    """
                    UPDATE transactions
                    SET excludeFromSummary = 0
                    WHERE isCreditCardTransaction = 1
                    """.trimIndent()
                )
            }
        }

        private val Migration14To15 = object : Migration(14, 15) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.addColumnIfMissing("detected_drafts", "fromAccountId", "INTEGER")
                db.addColumnIfMissing("detected_drafts", "toAccountId", "INTEGER")
            }
        }

        private val Migration15To16 = object : Migration(15, 16) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.addColumnIfMissing("user_settings", "salaryCounterpartyKey", "TEXT")
                db.addColumnIfMissing("user_settings", "dismissedSalaryKeysCsv", "TEXT NOT NULL DEFAULT ''")
            }
        }

        private val Migration16To17 = object : Migration(16, 17) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.addColumnIfMissing("user_settings", "lastSuccessfulScanMillis", "INTEGER NOT NULL DEFAULT 0")
            }
        }

        // Catch-up after merging the LLM-Integration and Major-Overhaul branches: installs
        // from either branch may be missing columns the other branch added, so add them all
        // idempotently here.
        private val Migration17To18 = object : Migration(17, 18) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.addColumnIfMissing("user_settings", "offlineLlmParsingEnabled", "INTEGER NOT NULL DEFAULT 0")
                db.addColumnIfMissing("user_settings", "offlineLlmModelDownloaded", "INTEGER NOT NULL DEFAULT 0")
                db.addColumnIfMissing("user_settings", "onboardedAtMillis", "INTEGER NOT NULL DEFAULT 0")
                db.addColumnIfMissing("user_settings", "lastSuccessfulScanMillis", "INTEGER NOT NULL DEFAULT 0")
                db.addColumnIfMissing("user_settings", "salaryCounterpartyKey", "TEXT")
                db.addColumnIfMissing("user_settings", "dismissedSalaryKeysCsv", "TEXT NOT NULL DEFAULT ''")
                db.addColumnIfMissing("accounts", "accountType", "TEXT NOT NULL DEFAULT 'Bank'")
                db.addColumnIfMissing("transactions", "fromAccountId", "INTEGER")
                db.addColumnIfMissing("transactions", "toAccountId", "INTEGER")
                db.addColumnIfMissing("detected_drafts", "fromAccountId", "INTEGER")
                db.addColumnIfMissing("detected_drafts", "toAccountId", "INTEGER")
            }
        }

        private val Migration18To19 = object : Migration(18, 19) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // 0 = unanchored; the balance guard then falls back to the global onboarding
                // stamp. The anchor arms itself on the next manual balance edit.
                db.addColumnIfMissing("accounts", "balanceAnchorAtMillis", "INTEGER NOT NULL DEFAULT 0")
            }
        }

        private val Migration19To20 = object : Migration(19, 20) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.addColumnIfMissing("user_settings", "dismissedSmsKeys", "TEXT NOT NULL DEFAULT ''")
            }
        }

        // The shared color library: one saved palette used for both the app accent and
        // category colors. Empty means "not seeded yet"; the repository seeds the defaults.
        private val Migration20To21 = object : Migration(20, 21) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.addColumnIfMissing("user_settings", "paletteHexCsv", "TEXT NOT NULL DEFAULT ''")
            }
        }

        private val Migration21To22 = object : Migration(21, 22) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.addColumnIfMissing("accounts", "linkedCardsCsv", "TEXT NOT NULL DEFAULT ''")
            }
        }

        // The optional second tag on credit-card rows. NULL = untagged, which is what every
        // existing row starts as.
        private val Migration22To23 = object : Migration(22, 23) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.addColumnIfMissing("transactions", "secondaryCategoryId", "INTEGER")
            }
        }

        private fun SupportSQLiteDatabase.addColumnIfMissing(
            tableName: String,
            columnName: String,
            columnDefinition: String
        ) {
            if (!hasColumn(tableName, columnName)) {
                execSQL("ALTER TABLE `$tableName` ADD COLUMN `$columnName` $columnDefinition")
            }
        }

        private fun SupportSQLiteDatabase.hasColumn(tableName: String, columnName: String): Boolean {
            query("PRAGMA table_info(`$tableName`)").use { cursor ->
                val nameIndex = cursor.getColumnIndex("name")
                while (cursor.moveToNext()) {
                    if (nameIndex >= 0 && cursor.getString(nameIndex) == columnName) {
                        return true
                    }
                }
            }
            return false
        }
    }
}
