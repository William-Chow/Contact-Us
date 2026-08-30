package com.kotlin.mvvm.contact.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface ContactDao {

    @Query("SELECT * FROM contacts ORDER BY firstName COLLATE NOCASE, lastName COLLATE NOCASE")
    fun observeAll(): Flow<List<ContactEntity>>

    @Query("SELECT * FROM contacts WHERE id = :id")
    suspend fun getById(id: String): ContactEntity?

    @Query("SELECT COUNT(*) FROM contacts")
    suspend fun count(): Int

    @Upsert
    suspend fun upsert(contact: ContactEntity)

    /** Used by sync/import: rows already on the device are kept, so local edits are never clobbered. */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertMissing(contacts: List<ContactEntity>): List<Long>

    @Query("DELETE FROM contacts WHERE id = :id")
    suspend fun deleteById(id: String)
}
