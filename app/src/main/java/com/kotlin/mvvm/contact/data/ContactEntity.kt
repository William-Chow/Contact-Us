package com.kotlin.mvvm.contact.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.kotlin.mvvm.contact.model.Contact

@Entity(tableName = "contacts")
data class ContactEntity(
    @PrimaryKey
    val id: String,
    val firstName: String,
    val lastName: String,
    val email: String?,
    val phone: String?
) {
    fun toContact(): Contact = Contact(id, firstName, lastName, email, phone)

    companion object {
        fun from(contact: Contact): ContactEntity = ContactEntity(
            id = contact.id,
            firstName = contact.firstName,
            lastName = contact.lastName,
            email = contact.email,
            phone = contact.phone
        )
    }
}
