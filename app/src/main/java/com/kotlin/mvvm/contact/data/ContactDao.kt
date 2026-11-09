package com.kotlin.mvvm.contact.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
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

    @Query("SELECT * FROM contacts ORDER BY firstName COLLATE NOCASE, lastName COLLATE NOCASE")
    suspend fun getAll(): List<ContactEntity>

    @Upsert
    suspend fun upsert(contact: ContactEntity)

    /**
     * For importing phone contacts. Rows already on the device are kept, so local edits are never
     * clobbered, and ids the user deleted are skipped, so a re-import doesn't bring them back.
     * Returns how many rows were added.
     */
    @Transaction
    suspend fun insertMissing(contacts: List<ContactEntity>): Int {
        val deleted = deletedIds().toSet()
        return insertIgnoringExisting(contacts.filterNot { it.id in deleted }).count { it != -1L }
    }

    /**
     * For restoring a backup the user chose. Unlike [insertMissing], contacts they deleted come
     * back (that is the point of restoring); rows already on the device are still left untouched.
     * Returns how many rows were added.
     */
    @Transaction
    suspend fun restore(contacts: List<ContactEntity>): Int {
        // Chunked to stay under SQLite's bound-parameter limit on older Android versions.
        contacts.map { it.id }.chunked(500).forEach { forgetDeleted(it) }
        return insertIgnoringExisting(contacts).count { it != -1L }
    }

    /** Removes the contact and remembers its id, so a later sync or import cannot bring it back. */
    @Transaction
    suspend fun deleteAndRemember(id: String) {
        deleteById(id)
        insertDeleted(DeletedContactEntity(id))
    }

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertIgnoringExisting(contacts: List<ContactEntity>): List<Long>

    @Query("DELETE FROM contacts WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("SELECT id FROM deleted_contacts")
    suspend fun deletedIds(): List<String>

    @Query("DELETE FROM deleted_contacts WHERE id IN (:ids)")
    suspend fun forgetDeleted(ids: List<String>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDeleted(deleted: DeletedContactEntity)
}
