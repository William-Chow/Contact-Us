package com.kotlin.mvvm.contact.data

import android.app.Application
import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import com.kotlin.mvvm.contact.R
import com.kotlin.mvvm.contact.model.Contact
import com.kotlin.mvvm.contact.network.ResponseState
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
class ContactRepositoryImportTest {

    private val dispatcher = StandardTestDispatcher()
    private val app = ApplicationProvider.getApplicationContext<Application>()
    private lateinit var db: ContactDatabase

    private class FakePhone(private val contacts: List<Contact>, private val allowed: Boolean = true) :
        PhoneContactsSource {
        override fun readAll(): List<Contact> =
            if (allowed) contacts else throw SecurityException("no READ_CONTACTS")

        override fun readPicked(phoneUri: Uri): Contact? = null
    }

    @Before
    fun setUp() {
        db = inMemoryDatabase(app, dispatcher)
    }

    @After
    fun tearDown() {
        db.close()
    }

    private fun repository(phone: PhoneContactsSource) = ContactRepository(app, db.contactDao(), phone)

    @Test
    fun importSkipsTypedInDuplicatesDeletedContactsAndEarlierImports() = runTest(dispatcher) {
        val dao = db.contactDao()
        dao.upsert(ContactEntity("typed", "Grace", "Hopper", null, "(555) 0100"))
        dao.upsert(contactEntity("phone:deleted"))
        dao.deleteAndRemember("phone:deleted")
        val phone = FakePhone(
            listOf(
                Contact("phone:grace", "Grace", "Hopper", "grace@example.com", "555-0100"),
                Contact("phone:deleted", "First phone:deleted", "Last", null, null),
                Contact("phone:alan", "Alan", "Turing", null, "555-0199")
            )
        )
        val repository = repository(phone)

        assertEquals(ResponseState.Success(1), repository.importFromPhone())
        assertEquals("Turing", dao.getById("phone:alan")?.lastName)
        assertNull(dao.getById("phone:grace"))
        assertNull(dao.getById("phone:deleted"))

        // Importing again adds nothing.
        assertEquals(ResponseState.Success(0), repository.importFromPhone())
    }

    @Test
    fun importWithoutPermissionSaysSo() = runTest(dispatcher) {
        val result = repository(FakePhone(emptyList(), allowed = false)).importFromPhone()

        assertEquals(ResponseState.Error(null, app.getString(R.string.import_permission_denied)), result)
    }
}
