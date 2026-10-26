package com.kotlin.mvvm.contact.viewmodel

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import com.kotlin.mvvm.contact.data.ContactDatabase
import com.kotlin.mvvm.contact.data.ContactRepository
import com.kotlin.mvvm.contact.data.contactEntity
import com.kotlin.mvvm.contact.data.inMemoryDatabase
import com.kotlin.mvvm.contact.model.Contact
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class ContactViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private val app = ApplicationProvider.getApplicationContext<Application>()
    private lateinit var db: ContactDatabase
    private lateinit var viewModel: ContactViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        db = inMemoryDatabase(app, dispatcher)
        viewModel = ContactViewModel(app, ContactRepository(app, db.contactDao()))
    }

    @After
    fun tearDown() {
        db.close()
        Dispatchers.resetMain()
    }

    @Test
    fun contactsStartUnloadedThenFollowTheDatabase() = runTest(dispatcher) {
        // Unconfined so every emission is recorded as it happens.
        val seen = mutableListOf<List<Contact>?>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.contacts.toList(seen)
        }

        // Not loaded yet: the screen shows neither the list nor the "No contacts" state.
        assertEquals(listOf<List<Contact>?>(null), seen)

        // A fresh install starts empty. Nothing is imported on the user's behalf.
        advanceUntilIdle()
        assertEquals(emptyList<Contact>(), seen.last())

        db.contactDao().upsert(contactEntity("1"))
        advanceUntilIdle()
        assertEquals(listOf("1"), seen.last()?.map { it.id })
    }
}
