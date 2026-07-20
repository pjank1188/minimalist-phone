package com.pjank.minimalistphone

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class WebBlocklistTest {

    @Test
    fun `host is parsed from schemeless omnibox text`() {
        assertEquals("youtube.com", WebBlocklist.hostOf("youtube.com/watch?v=abc"))
        assertEquals("m.youtube.com", WebBlocklist.hostOf("m.youtube.com"))
        assertEquals("youtube.com", WebBlocklist.hostOf("www.youtube.com/feed"))
        assertEquals("example.com", WebBlocklist.hostOf("Example.COM:8080/path"))
    }

    @Test
    fun `non-host omnibox text is rejected`() {
        assertNull(WebBlocklist.hostOf("how to fix a bike chain"))
        assertNull(WebBlocklist.hostOf("localhost"))
        assertNull(WebBlocklist.hostOf(""))
    }

    @Test
    fun `blocked domains match with and without subdomains`() {
        assertNotNull(WebBlocklist.restrictionFor("youtube.com"))
        assertNotNull(WebBlocklist.restrictionFor("m.youtube.com"))
        assertNotNull(WebBlocklist.restrictionFor("old.reddit.com"))
        assertNotNull(WebBlocklist.restrictionFor("news.google.com"))
    }

    @Test
    fun `allowed domains pass, including lookalike suffixes`() {
        assertNull(WebBlocklist.restrictionFor("google.com"))
        assertNull(WebBlocklist.restrictionFor("maps.google.com"))
        assertNull(WebBlocklist.restrictionFor("notyoutube.com"))
        assertNull(WebBlocklist.restrictionFor("claude.ai"))
    }
}
