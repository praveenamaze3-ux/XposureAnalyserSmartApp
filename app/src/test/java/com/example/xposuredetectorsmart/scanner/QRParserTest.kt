package com.example.xposuredetectorsmart.scanner

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class QRParserTest {

    private val parser = QRParser()

    @Test
    fun `parses a well-formed payload`() {
        val data = parser.parse("h2s-worker:acme_chemicals:WRK_4838")

        assertEquals("acme_chemicals", data.industryId)
        assertEquals("WRK_4838", data.workerId)
    }

    @Test
    fun `trims surrounding whitespace`() {
        val data = parser.parse("  h2s-worker:acme_chemicals:WRK_1  ")
        assertEquals("acme_chemicals", data.industryId)
        assertEquals("WRK_1", data.workerId)
    }

    @Test
    fun `rejects payload missing the prefix`() {
        assertThrows(QRParseException::class.java) {
            parser.parse("acme_chemicals:WRK_4838")
        }
    }

    @Test
    fun `rejects payload with wrong field count`() {
        assertThrows(QRParseException::class.java) {
            parser.parse("h2s-worker:acme_chemicals:WRK_4838:extra")
        }
    }

    @Test
    fun `rejects payload with too few fields`() {
        assertThrows(QRParseException::class.java) {
            parser.parse("h2s-worker:acme_chemicals")
        }
    }

    @Test
    fun `rejects blank industry id`() {
        assertThrows(QRParseException::class.java) {
            parser.parse("h2s-worker::WRK_4838")
        }
    }

    @Test
    fun `rejects blank worker id`() {
        assertThrows(QRParseException::class.java) {
            parser.parse("h2s-worker:acme_chemicals:")
        }
    }
}
