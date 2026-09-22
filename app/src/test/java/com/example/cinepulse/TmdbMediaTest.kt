package com.example.cinepulse

import com.cinepulse.app.data.remote.tmdb.TmdbMedia
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TmdbMediaTest {

    @Test
    fun testDisplayTitle_withTitle_returnsTitle() {
        val media = TmdbMedia(id = 1, title = "Inception", name = null)
        assertEquals("Inception", media.displayTitle)
    }

    @Test
    fun testDisplayTitle_withNameOnly_returnsName() {
        val media = TmdbMedia(id = 2, title = null, name = "Breaking Bad")
        assertEquals("Breaking Bad", media.displayTitle)
    }

    @Test
    fun testDisplayTitle_withNoTitleOrName_returnsUnknown() {
        val media = TmdbMedia(id = 3, title = null, name = null)
        assertEquals("Unknown", media.displayTitle)
    }

    @Test
    fun testIsTv_withMediaTypeTv_returnsTrue() {
        val media = TmdbMedia(id = 1, mediaType = "tv")
        assertTrue(media.isTv)
    }

    @Test
    fun testIsTv_withNameAndNoTitle_returnsTrue() {
        val media = TmdbMedia(id = 2, name = "Breaking Bad", title = null, mediaType = "movie")
        // Note: mediaType is movie, but name != null and title == null satisfies the second condition
        assertTrue(media.isTv)
    }

    @Test
    fun testIsTv_withTitleAndNoMediaTypeTv_returnsFalse() {
        val media = TmdbMedia(id = 3, title = "Inception", name = null, mediaType = "movie")
        assertFalse(media.isTv)
    }
}
