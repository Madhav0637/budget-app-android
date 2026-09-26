package com.madhav0637.budgetapp.data

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.madhav0637.budgetapp.domain.DefaultCategories
import java.time.Instant
import java.util.UUID

class Converters {
    @TypeConverter
    fun fromInstant(value: Instant): Long = value.toEpochMilli()

    @TypeConverter
    fun toInstant(value: Long): Instant = Instant.ofEpochMilli(value)
}

/**
 * The single on-device store, shared by the app's screens and the quick-entry pop-up.
 *
 * Every change to the tables needs a new version number and a migration in [MIGRATIONS]. Room then upgrades the
 * database on phones that already have data. It must never fall back to wiping the database.
 */
@Database(entities = [Category::class, Expense::class], version = 2)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun categoryDao(): CategoryDao
    abstract fun expenseDao(): ExpenseDao

    companion object {
        /** Version 2 (Koku 2.0) adds an optional note to each expense. Existing expenses get no note. */
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE expenses ADD COLUMN note TEXT")
            }
        }

        /** Every migration, oldest first. The on-device migration test uses the same list. */
        val MIGRATIONS: Array<Migration> = arrayOf(MIGRATION_1_2)

        const val FILE_NAME = "budget.db"

        fun create(context: Context): AppDatabase =
            Room.databaseBuilder(context, AppDatabase::class.java, FILE_NAME)
                .addMigrations(*MIGRATIONS)
                .addCallback(SeedDefaults)
                .build()

        /** A throwaway database in memory, for tests. Seeding the defaults is optional. */
        fun inMemory(context: Context, seedDefaults: Boolean = false): AppDatabase =
            Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
                .apply { if (seedDefaults) addCallback(SeedDefaults) }
                .build()
    }

    /**
     * Inserts the default categories when the database file is first created, before any screen can read it,
     * so quick entry always has categories even if the main app was never opened.
     */
    private object SeedDefaults : Callback() {
        override fun onCreate(db: SupportSQLiteDatabase) {
            for (item in DefaultCategories.all) {
                val row = ContentValues().apply {
                    put("id", UUID.randomUUID().toString())
                    put("name", item.name)
                    put("emoji", item.emoji)
                }
                db.insert("categories", SQLiteDatabase.CONFLICT_ABORT, row)
            }
        }
    }
}
