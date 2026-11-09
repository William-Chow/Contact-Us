package com.kotlin.mvvm.contact.data

import com.kotlin.mvvm.contact.model.Contact
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

// Robolectric for the real org.json; the plain JVM unit-test android.jar only has stubs.
@RunWith(RobolectricTestRunner::class)
class BackupFormatTest {

    @Test
    fun roundTripsContacts() {
        val contacts = listOf(
            Contact("1", "Grace", "Hopper", "grace@example.com", "555-0100"),
            Contact("2", "陈伟明", "", null, null)
        )

        assertEquals(contacts, BackupFormat.decode(BackupFormat.encode(contacts, exportedAt = 0L)))
    }

    @Test(expected = InvalidBackupException::class)
    fun rejectsTextThatIsNotJson() {
        BackupFormat.decode("not a backup")
    }

    @Test(expected = InvalidBackupException::class)
    fun rejectsJsonFromSomethingElse() {
        BackupFormat.decode("""{"format":"another-app","contacts":[]}""")
    }

    @Test
    fun treatsExplicitNullsAsMissingAndSkipsUnusableEntries() {
        val text = """
            {"format":"contact-us-backup","version":1,"contacts":[
              {"id":"1","firstName":"Ada","lastName":"Lovelace","email":null,"phone":null},
              {"id":"","firstName":"No","lastName":"Id"},
              {"id":"3","firstName":" ","lastName":""},
              "not an object"
            ]}
        """.trimIndent()

        assertEquals(listOf(Contact("1", "Ada", "Lovelace", null, null)), BackupFormat.decode(text))
    }
}
