package com.kotlin.mvvm.contact.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [ContactEntity::class], version = 1, exportSchema = false)
abstract class ContactDatabase : RoomDatabase() {

    abstract fun contactDao(): ContactDao

    companion object {
        @Volatile
        private var instance: ContactDatabase? = null

        fun get(context: Context): ContactDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    ContactDatabase::class.java,
                    "contacts.db"
                ).build().also { instance = it }
            }
    }
}
