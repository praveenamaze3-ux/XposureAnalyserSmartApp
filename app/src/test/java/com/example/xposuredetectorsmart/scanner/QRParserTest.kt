package com.example.xposuredetectorsmart.scanner

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test
import java.time.LocalDate

class QRParserTest {

    private val parser = QRParser()

    @Test
    fun `parses a well-formed payload`() {
        val data = parser.parse("h2s-dose:WRK_4838|2026-08-25|LocationA|morning")

        assertEquals("WRK_4838", data.workerId)
        assertEquals(LocalDate.of(2026, 8, 25), data.date)
        assertEquals("LocationA", data.location)
        assertEquals("morning", data.shift)
    }

    @Test
    fun `trims surrounding whitespace`() {
        val data = parser.parse("  h2s-dose:WRK_1|2026-01-01|Loc|shift1  ")
        assertEquals("WRK_1", data.workerId)
    }

    @Test
    fun `rejects payload missing the prefix`() {
        assertThrows(QRParseException::class.java) {
            parser.parse("WRK_4838|2026-08-25|LocationA|morning")
        }
    }

    @Test
    fun `rejects payload with wrong field count`() {
        assertThrows(QRParseException::class.java) {
            parser.parse("h2s-dose:WRK_4838|2026-08-25|LocationA")
        }
    }

    @Test
    fun `rejects worker id missing WRK_ prefix`() {
        assertThrows(QRParseException::class.java) {
            parser.parse("h2s-dose:4838|2026-08-25|LocationA|morning")
        }
    }

    @Test
    fun `rejects an invalid date`() {
        assertThrows(QRParseException::class.java) {
            parser.parse("h2s-dose:WRK_4838|not-a-date|LocationA|morning")
        }
    }

    @Test
    fun `rejects blank location`() {
        assertThrows(QRParseException::class.java) {
            parser.parse("h2s-dose:WRK_4838|2026-08-25||morning")
        }
    }
}
