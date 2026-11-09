package com.kotlin.mvvm.contact.data

import android.provider.ContactsContract.CommonDataKinds.Email
import android.provider.ContactsContract.CommonDataKinds.Phone
import android.provider.ContactsContract.CommonDataKinds.StructuredName
import com.kotlin.mvvm.contact.model.Contact
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class PhoneContactsMapperTest {

    private fun row(
        key: String,
        mimeType: String,
        data1: String? = null,
        data2: String? = null,
        data3: String? = null,
        displayName: String? = null,
        primary: Boolean = false
    ) = PhoneContactRow(key, mimeType, displayName, data1, data2, data3, primary)

    @Test
    fun usesTheStructuredNameAndThePrimaryPhoneAndEmail() {
        val contacts = PhoneContactsMapper.toContacts(
            listOf(
                row("k1", StructuredName.CONTENT_ITEM_TYPE, data1 = "Grace Hopper", data2 = "Grace", data3 = "Hopper"),
                row("k1", Phone.CONTENT_ITEM_TYPE, data1 = "555-0100"),
                row("k1", Phone.CONTENT_ITEM_TYPE, data1 = "555-0199", primary = true),
                row("k1", Email.CONTENT_ITEM_TYPE, data1 = " grace@example.com ")
            )
        )

        assertEquals(
            listOf(Contact("phone:k1", "Grace", "Hopper", "grace@example.com", "555-0199")),
            contacts
        )
    }

    @Test
    fun fallsBackToTheDisplayNameAndSkipsNamelessContacts() {
        val contacts = PhoneContactsMapper.toContacts(
            listOf(
                row("k1", Phone.CONTENT_ITEM_TYPE, data1 = "555-0100", displayName = "陈伟明"),
                row("k2", Phone.CONTENT_ITEM_TYPE, data1 = "555-0101", displayName = "  ")
            )
        )

        assertEquals(listOf(Contact("phone:k1", "陈伟明", "", null, "555-0100")), contacts)
    }

    @Test
    fun splitsDisplayNamesAtTheLastSpace() {
        assertEquals("Grace Brewster" to "Hopper", PhoneContactsMapper.splitDisplayName("Grace  Brewster Hopper "))
        assertEquals("陈伟明" to "", PhoneContactsMapper.splitDisplayName("陈伟明"))
        assertEquals("" to "", PhoneContactsMapper.splitDisplayName("   "))
    }

    @Test
    fun duplicateKeyIgnoresCaseAndPhoneFormatting() {
        val typed = Contact("uuid", "grace", "HOPPER", null, "(555) 0100")
        val imported = Contact("phone:k1", "Grace", "Hopper", "grace@example.com", "555-0100")
        val other = Contact("phone:k2", "Grace", "Hopper", null, "555-0199")

        assertEquals(PhoneContactsMapper.duplicateKey(typed), PhoneContactsMapper.duplicateKey(imported))
        assertNotEquals(PhoneContactsMapper.duplicateKey(typed), PhoneContactsMapper.duplicateKey(other))
    }
}
