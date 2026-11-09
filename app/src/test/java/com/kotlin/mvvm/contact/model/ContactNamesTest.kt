package com.kotlin.mvvm.contact.model

import org.junit.Assert.assertEquals
import org.junit.Test

class ContactNamesTest {

    @Test
    fun westernNamesAreFirstThenLast() {
        assertEquals("Grace Hopper", ContactNames.displayName(" Grace ", "Hopper"))
        assertEquals("GH", ContactNames.initials("grace", "hopper"))
    }

    @Test
    fun chineseNamesAreFamilyNameFirstWithoutASpace() {
        assertEquals("陈伟明", ContactNames.displayName("伟明", "陈"))
        assertEquals("陈", ContactNames.initials("伟明", "陈"))
    }

    @Test
    fun aSingleNameIsShownAsIs() {
        assertEquals("陈伟明", ContactNames.displayName("陈伟明", ""))
        assertEquals("陈", ContactNames.initials("陈伟明", ""))
        assertEquals("Hopper", ContactNames.displayName("", "Hopper"))
        assertEquals("H", ContactNames.initials("", "hopper"))
    }
}
