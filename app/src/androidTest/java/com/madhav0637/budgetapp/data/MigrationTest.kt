package com.madhav0637.budgetapp.data

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.time.Instant

/**
 * Proves that a phone with BudgetApp 1.0's database (version 1) keeps every expense after updating to Koku 2.0.
 *
 * It builds a real version-1 database file from the schema Room exported for 1.0 (app/schemas/.../1.json, which the
 * Room Gradle plugin packs into this test's assets), fills it the way 1.0 stored data, then opens it exactly as the
 * app does. While opening, Room runs [AppDatabase.MIGRATION_1_2] and then checks every table, column, index and
 * foreign key against what version 2 expects, so a wrong migration fails here. (Room's MigrationTestHelper does the
 * same, but it needs a newer kotlinx-serialization than the one the app ships with.) Uses its own file name, never
 * the app's real budget.db.
 */
@RunWith(AndroidJUnit4::class)
class MigrationTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()

    @Before
    fun setUp() {
        context.deleteDatabase(TEST_DB)
    }

    @After
    fun tearDown() {
        context.deleteDatabase(TEST_DB)
    }

    /** Creates the version-1 tables from the exported schema, then two categories and three expenses. */
    private fun createVersion1() {
        val json = InstrumentationRegistry.getInstrumentation().context.assets
            .open("${AppDatabase::class.java.name}/1.json")
            .bufferedReader()
            .use { it.readText() }
        val schema = JSONObject(json).getJSONObject("database")
        assertEquals(1, schema.getInt("version"))

        val file = context.getDatabasePath(TEST_DB).also { it.parentFile?.mkdirs() }
        SQLiteDatabase.openOrCreateDatabase(file, null).use { db ->
            val entities = schema.getJSONArray("entities")
            for (i in 0 until entities.length()) {
                val entity = entities.getJSONObject(i)
                val table = entity.getString("tableName")
                db.execSQL(entity.getString("createSql").replace("\${TABLE_NAME}", table))
                val indices = entity.optJSONArray("indices") ?: continue
                for (j in 0 until indices.length()) {
                    db.execSQL(indices.getJSONObject(j).getString("createSql").replace("\${TABLE_NAME}", table))
                }
            }
            val setup = schema.getJSONArray("setupQueries") // Room's own bookkeeping table, with 1.0's identity hash
            for (i in 0 until setup.length()) db.execSQL(setup.getString(i))
            db.version = 1

            fun category(id: String, name: String, emoji: String) = ContentValues().apply {
                put("id", id)
                put("name", name)
                put("emoji", emoji)
            }
            fun expense(id: String, merchant: String, amount: Long, date: Long, categoryId: String) = ContentValues().apply {
                put("id", id)
                put("merchant", merchant)
                put("amount", amount)
                put("date", date)
                put("categoryId", categoryId)
            }
            db.insertOrThrow("categories", null, category("food", "Food", "🍔"))
            db.insertOrThrow("categories", null, category("bills", "Bills", "🧾"))
            db.insertOrThrow("expenses", null, expense("e1", "Swiggy", 450, 1_758_000_000_000, "food"))
            db.insertOrThrow("expenses", null, expense("e2", "Electricity, Sept", 1_850, 1_758_100_000_000, "bills"))
            db.insertOrThrow("expenses", null, expense("e3", "Chai \"Point\"", 60, 1_758_200_000_000, "food"))
        }
    }

    /** Opens the file the same way the app does: AppDatabase.MIGRATIONS and no destructive fallback. */
    private fun openLikeTheApp(): AppDatabase =
        Room.databaseBuilder(context, AppDatabase::class.java, TEST_DB).addMigrations(*AppDatabase.MIGRATIONS).build()

    @Test
    fun migrating1To2KeepsEveryRowAndAddsAnEmptyNote() {
        createVersion1()
        val db = openLikeTheApp()
        try {
            val sql = db.openHelper.writableDatabase // opening runs the migration and Room's schema check
            assertEquals(2, sql.version)
            sql.query("SELECT id, merchant, amount, date, categoryId, note FROM expenses ORDER BY date").use { cursor ->
                assertEquals(3, cursor.count)
                cursor.moveToFirst()
                assertEquals("e1", cursor.getString(0))
                assertEquals("Swiggy", cursor.getString(1))
                assertEquals(450L, cursor.getLong(2))
                assertEquals(1_758_000_000_000L, cursor.getLong(3))
                assertEquals("food", cursor.getString(4))
                assertTrue(cursor.isNull(5))
            }
            sql.query("SELECT COUNT(*) FROM categories").use { cursor ->
                cursor.moveToFirst()
                assertEquals(2, cursor.getInt(0))
            }
        } finally {
            db.close()
        }
    }

    @Test
    fun theAppReadsAMigratedDatabaseWithItsDataIntact() = runBlocking {
        createVersion1()
        val db = openLikeTheApp()
        try {
            val expenses = db.expenseDao().observeAllWithCategory().first()
            assertEquals(listOf("Chai \"Point\"", "Electricity, Sept", "Swiggy"), expenses.map { it.expense.merchant })
            assertEquals(listOf(60L, 1_850L, 450L), expenses.map { it.expense.amount })
            assertEquals(Instant.ofEpochMilli(1_758_100_000_000L), expenses[1].expense.date)
            assertEquals("Bills", expenses[1].category.name)
            expenses.forEach { assertNull(it.expense.note) }
            assertEquals(2, db.categoryDao().count())

            // Notes work on the upgraded database.
            db.expenseDao().update(expenses.last().expense.copy(note = "team lunch"))
            assertEquals("team lunch", db.expenseDao().observeAllWithCategory().first().last().expense.note)
        } finally {
            db.close()
        }
    }

    @Test
    fun withoutTheMigrationRoomRefusesToOpenRatherThanWipeTheData() = runBlocking {
        createVersion1()
        val unmigrated = Room.databaseBuilder(context, AppDatabase::class.java, TEST_DB).build()
        try {
            unmigrated.openHelper.writableDatabase
            fail("Expected Room to refuse a version-1 database without a migration")
        } catch (expected: IllegalStateException) {
            // "A migration from 1 to 2 was required but not found."
        } finally {
            unmigrated.close()
        }

        // Nothing was lost: the real migration still finds all three expenses.
        val db = openLikeTheApp()
        try {
            assertEquals(3, db.expenseDao().observeAllWithCategory().first().size)
        } finally {
            db.close()
        }
    }

    private companion object {
        const val TEST_DB = "migration-test.db"
    }
}
