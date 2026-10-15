package com.kotlin.mvvm.contact.data

import android.content.Context
import androidx.room.Room
import kotlin.coroutines.CoroutineContext

/**
 * In-memory database whose queries run on [queryContext], so a test dispatcher drives them and
 * advanceUntilIdle() really means "every write has landed". Robolectric runs tests on the main
 * thread, which is also where that dispatcher runs Room's work.
 */
fun inMemoryDatabase(context: Context, queryContext: CoroutineContext): ContactDatabase =
    Room.inMemoryDatabaseBuilder(context, ContactDatabase::class.java)
        .setQueryCoroutineContext(queryContext)
        .allowMainThreadQueries()
        .build()

fun contactEntity(id: String, firstName: String = "First $id") =
    ContactEntity(id = id, firstName = firstName, lastName = "Last", email = null, phone = null)
