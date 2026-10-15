package com.kotlin.mvvm.contact.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Tombstone for a contact the user deleted. Sync and sample import skip these ids, so a deleted
 * contact is not treated as "missing" and added back. Only the id is kept, not the details.
 */
@Entity(tableName = "deleted_contacts")
data class DeletedContactEntity(
    @PrimaryKey
    val id: String
)
