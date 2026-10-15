package com.kotlin.mvvm.contact.viewmodel

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import com.kotlin.mvvm.contact.R
import com.kotlin.mvvm.contact.data.ContactDatabase
import com.kotlin.mvvm.contact.data.ContactRepository
import com.kotlin.mvvm.contact.data.contactEntity
import com.kotlin.mvvm.contact.data.inMemoryDatabase
import com.kotlin.mvvm.contact.viewmodel.MyContactViewModel.UiEvent
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
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class MyContactViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private val app = ApplicationProvider.getApplicationContext<Application>()
    private lateinit var db: ContactDatabase
    private lateinit var viewModel: MyContactViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        db = inMemoryDatabase(app, dispatcher)
        viewModel = MyContactViewModel(app, ContactRepository(app, db.contactDao(), dispatcher))
    }

    @After
    fun tearDown() {
        db.close()
        Dispatchers.resetMain()
    }

    @Test
    fun doubleTappedSaveInsertsOneContact() = runTest(dispatcher) {
        viewModel.start(null)
        viewModel.onFirstNameChange("Ada")
        viewModel.onLastNameChange("Lovelace")

        viewModel.save()
        viewModel.save()
        advanceUntilIdle()

        assertEquals(1, db.contactDao().count())
    }

    @Test
    fun saveRightAfterDeleteDoesNotRestoreTheContact() = runTest(dispatcher) {
        db.contactDao().upsert(contactEntity("1"))
        viewModel.start("1")
        advanceUntilIdle()

        viewModel.delete()
        viewModel.save()
        advanceUntilIdle()

        assertNull(db.contactDao().getById("1"))
    }

    @Test
    fun failedSaveReportsAnErrorAndKeepsTheScreenOpen() = runTest(dispatcher) {
        val events = mutableListOf<UiEvent>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.events.toList(events)
        }
        // Break the table under Room so the write throws, as a full disk would.
        db.openHelper.writableDatabase.execSQL("DROP TABLE contacts")
        viewModel.start(null)
        viewModel.onFirstNameChange("Ada")
        viewModel.onLastNameChange("Lovelace")

        viewModel.save()
        advanceUntilIdle()

        // An error message, and no Close: the user can still retry.
        assertEquals(listOf(UiEvent.Message(app.getString(R.string.storage_error))), events)
        assertFalse(viewModel.isLoading.value)
    }
}
