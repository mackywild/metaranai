package jp.metaranai.app

import org.junit.Assert.assertEquals
import org.junit.Test

class ClientIdDisplayTest {
    @Test fun hidesEverythingAfterPrefixWithoutChangingOffsets() {
        val source = "abcd12345678"
        val masked = maskClientId(source)
        assertEquals("abcd••••••••", masked)
        assertEquals(source.length, masked.length)
        assertEquals("abcd12345678", source)
    }
    @Test fun handlesShortAndEmptyIds() {
        listOf("", "a", "abc", "abcd").forEach { assertEquals(it, maskClientId(it)) }
    }
}
