package com.kotlin.mvvm.contact.data

import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ContactDaoTest {

    private val dispatcher = StandardTestDispatcher()
    private lateinit var db: ContactDatabase
    private lateinit var dao: ContactDao

    @Before
    fun setUp() {
        db = inMemoryDatabase(ApplicationProvider.getApplicationContext(), dispatcher)
        dao = db.contactDao()
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun insertMissingKeepsLocalEditsAndCountsOnlyNewRows() = runTest(dispatcher) {
        dao.upsert(contactEntity("1", firstName = "Edited"))

        val added = dao.insertMissing(
            listOf(contactEntity("1", firstName = "Remote"), contactEntity("2"))
        )

        assertEquals(1, added)
        assertEquals("Edited", dao.getById("1")?.firstName)
        assertEquals(2, dao.count())
    }

    @Test
    fun deletedContactIsNotAddedBackByInsertMissing() = runTest(dispatcher) {
        dao.upsert(contactEntity("1"))
        dao.deleteAndRemember("1")

        val added = dao.insertMissing(listOf(contactEntity("1"), contactEntity("2")))

        assertEquals(1, added)
        assertNull(dao.getById("1"))
        assertEquals(listOf("1"), dao.deletedIds())
    }

    @Test
    fun upsertUpdatesTheExistingRow() = runTest(dispatcher) {
        dao.upsert(contactEntity("1", firstName = "Ada"))
        dao.upsert(contactEntity("1", firstName = "Grace"))

        assertEquals(1, dao.count())
        assertEquals("Grace", dao.getById("1")?.firstName)
    }
}
