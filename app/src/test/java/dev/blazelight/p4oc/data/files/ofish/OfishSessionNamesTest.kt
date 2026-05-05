package dev.blazelight.p4oc.data.files.ofish

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class OfishSessionNamesTest {
    @Test
    fun `build creates sanitized OFISH title`() {
        val title = OfishSessionNames.build("Write File!!", 1234L, "ab_cd!!")

        assertEquals("__ofish_write-file_1234_abcd", title)
        assertTrue(OfishSessionNames.isOfishTitle(title))
        assertEquals(1234L, OfishSessionNames.parseTimestamp(title))
    }

    @Test
    fun `predicate handles null and normal titles`() {
        assertFalse(OfishSessionNames.isOfishTitle(null))
        assertFalse(OfishSessionNames.isOfishTitle("normal session"))
        assertTrue(OfishSessionNames.isOfishTitle("__ofish_probe_1_x"))
    }

    @Test
    fun `parseTimestamp returns null for malformed OFISH title`() {
        assertNull(OfishSessionNames.parseTimestamp("__ofish_probe_not-time_x"))
    }
}
