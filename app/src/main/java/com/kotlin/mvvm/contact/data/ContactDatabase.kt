package com.kotlin.mvvm.contact.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

// The schema is exported to app/schemas (see the room block in app/build.gradle). v1 predates the
// export, which is why MIGRATION_1_2 is hand-written.
@Database(
    entities = [ContactEntity::class, DeletedContactEntity::class],
    version = 2
)
abstract class ContactDatabase : RoomDatabase() {

    abstract fun contactDao(): ContactDao

    companion object {
        @Volatile
        private var instance: ContactDatabase? = null

        // v2 adds the deleted-contact tombstones; existing contacts are kept as they are.
        // Internal so ContactDatabaseMigrationTest can open a v1 file with it.
        internal val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `deleted_contacts` " +
                        "(`id` TEXT NOT NULL, PRIMARY KEY(`id`))"
                )
            }
        }

        fun get(context: Context): ContactDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    ContactDatabase::class.java,
                    "contacts.db"
                ).addMigrations(MIGRATION_1_2).build().also { instance = it }
            }
    }
}
