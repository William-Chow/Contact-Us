package com.kotlin.mvvm.contact.viewmodel

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import com.kotlin.mvvm.contact.R
import com.kotlin.mvvm.contact.data.ContactDatabase
import com.kotlin.mvvm.contact.data.ContactRepository
import com.kotlin.mvvm.contact.data.inMemoryDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
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
    private lateinit var repository: ContactRepository
    private lateinit var viewModel: ContactViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        db = inMemoryDatabase(app, dispatcher)
        repository = ContactRepository(app, db.contactDao(), dispatcher)
        viewModel = ContactViewModel(app, repository)
    }

    @After
    fun tearDown() {
        db.close()
        Dispatchers.resetMain()
    }

    @Test
    fun secondImportTapWhileOneIsRunningIsIgnored() = runTest(dispatcher) {
        val events = collectEvents()

        viewModel.importSampleData()
        viewModel.importSampleData()
        advanceUntilIdle()

        assertEquals(listOf(addedMessage(SAMPLE_SIZE)), events)
        assertEquals(SAMPLE_SIZE, db.contactDao().count())
        assertFalse(viewModel.isLoading.value)
    }

    @Test
    fun importLeavesDeletedSampleContactsDeleted() = runTest(dispatcher) {
        val events = collectEvents()
        viewModel.importSampleData()
        advanceUntilIdle()

        repository.delete(FIRST_SAMPLE_ID)
        viewModel.importSampleData()
        advanceUntilIdle()

        assertNull(db.contactDao().getById(FIRST_SAMPLE_ID))
        assertEquals(SAMPLE_SIZE - 1, db.contactDao().count())
        assertEquals(app.getString(R.string.import_up_to_date), events.last())
    }

    // Unconfined so each message is recorded as it is sent: advanceUntilIdle() stops once only
    // background work is left, so a collector on the standard dispatcher could miss the last one.
    private fun TestScope.collectEvents(): List<String> {
        val events = mutableListOf<String>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.events.toList(events)
        }
        return events
    }

    private fun addedMessage(count: Int) =
        app.resources.getQuantityString(R.plurals.import_added, count, count)

    private companion object {
        // Matches app/src/main/assets/data.json.
        const val SAMPLE_SIZE = 20
        const val FIRST_SAMPLE_ID = "5c8a80f52dfee238898d64cf"
    }
}
