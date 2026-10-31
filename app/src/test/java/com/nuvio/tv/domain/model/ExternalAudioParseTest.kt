package com.nuvio.tv.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ExternalAudioParseTest {

    @Test
    fun `missing or wrong type yields an empty list`() {
        assertTrue(parseExternalAudios(null).isEmpty())
        assertTrue(parseExternalAudios("nope").isEmpty())
        assertTrue(parseExternalAudios(mapOf("url" to "https://a")).isEmpty())
    }

    @Test
    fun `valid entry is read with all fields`() {
        val audios = parseExternalAudios(
            listOf(
                mapOf(
                    "url" to " https://cdn.example/audio.m3u8 ",
                    "lang" to "tr",
                    "label" to "Turkish",
                    "headers" to mapOf("Referer" to "https://example/", "Empty" to ""),
                    "autoSelect" to true
                )
            )
        )
        assertEquals(1, audios.size)
        val a = audios.single()
        assertEquals("https://cdn.example/audio.m3u8", a.url)
        assertEquals("tr", a.lang)
        assertEquals("Turkish", a.label)
        assertEquals(mapOf("Referer" to "https://example/"), a.headers)
        assertTrue(a.autoSelect)
    }

    @Test
    fun `language and name are accepted as aliases and defaults apply`() {
        val a = parseExternalAudios(
            listOf(mapOf("url" to "https://a/x", "language" to "de", "name" to "Deutsch"))
        ).single()
        assertEquals("de", a.lang)
        assertEquals("Deutsch", a.label)
        assertFalse(a.autoSelect)

        val b = parseExternalAudios(listOf(mapOf("url" to "https://a/y"))).single()
        assertEquals("und", b.lang)
        assertNull(b.label)
        assertNull(b.headers)
    }

    @Test
    fun `entries without an http url are dropped and duplicates collapse`() {
        val audios = parseExternalAudios(
            listOf(
                mapOf("url" to "ftp://a/x"),
                mapOf("url" to ""),
                mapOf("lang" to "tr"),
                "not a map",
                mapOf("url" to "https://a/x", "lang" to "tr"),
                mapOf("url" to "https://a/x", "lang" to "en"),
                mapOf("url" to "https://[object Object]")
            )
        )
        assertEquals(listOf("https://a/x"), audios.map { it.url })
        assertEquals("tr", audios.first().lang)
    }

    @Test
    fun `list is capped`() {
        val many = (1..20).map { mapOf("url" to "https://a/$it") }
        assertEquals(MAX_EXTERNAL_AUDIOS, parseExternalAudios(many).size)
    }

    @Test
    fun `autoSelect only counts when it is the boolean true`() {
        val a = parseExternalAudios(listOf(mapOf("url" to "https://a/x", "autoSelect" to "true"))).single()
        assertFalse(a.autoSelect)
    }
}
