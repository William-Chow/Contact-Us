package com.kotlin.mvvm.contact.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ContactDatabaseMigrationTest {

    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val name = "migration-test.db"

    @After
    fun tearDown() {
        context.deleteDatabase(name)
    }

    @Test
    fun migration1To2KeepsContactsAndAddsTombstones() = runTest {
        // A database as v1 of the app left it: only the contacts table, at user_version 1.
        context.openOrCreateDatabase(name, Context.MODE_PRIVATE, null).use { v1 ->
            v1.execSQL(
                "CREATE TABLE IF NOT EXISTS `contacts` (`id` TEXT NOT NULL, " +
                    "`firstName` TEXT NOT NULL, `lastName` TEXT NOT NULL, `email` TEXT, " +
                    "`phone` TEXT, PRIMARY KEY(`id`))"
            )
            v1.execSQL("INSERT INTO contacts (id, firstName, lastName) VALUES ('1', 'Ada', 'Lovelace')")
            v1.version = 1
        }

        val db = Room.databaseBuilder(context, ContactDatabase::class.java, name)
            .addMigrations(ContactDatabase.MIGRATION_1_2)
            .build()
        try {
            // The first query opens the database, which validates the migrated schema against v2
            // and throws if MIGRATION_1_2 does not produce exactly what Room expects.
            val dao = db.contactDao()
            assertEquals("Ada", dao.getById("1")?.firstName)

            dao.deleteAndRemember("1")
            assertEquals(listOf("1"), dao.deletedIds())
        } finally {
            db.close()
        }
    }
}
