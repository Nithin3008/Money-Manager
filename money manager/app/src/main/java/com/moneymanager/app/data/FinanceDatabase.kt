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
    version = 11
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
                        Migration10To11
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
    }
}
