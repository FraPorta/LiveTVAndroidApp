package com.example.livetv.data.network

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AcestreamLinksTest {
    @Test fun buildsTheProxyUrlWithTheStandardPort() {
        assertEquals("http://192.168.1.5:6878/ace/getstream?id=abc123", AcestreamLinks.proxyUrl("192.168.1.5", "abc123"))
    }

    @Test fun keepsAPortThatIsPartOfTheAddress() {
        assertEquals("http://nas.local:6879/ace/getstream?id=abc", AcestreamLinks.proxyUrl(" nas.local:6879 ", "abc"))
    }

    @Test fun changingTheIpRewritesAnEarlierLink() {
        val old = "http://127.0.0.1:6878/ace/getstream?id=deadbeef"
        assertEquals("http://192.168.1.183:6878/ace/getstream?id=deadbeef", AcestreamLinks.withIp(old, "192.168.1.183"))
    }

    @Test fun otherLinksAreLeftAlone() {
        assertEquals("https://example.com/watch", AcestreamLinks.withIp("https://example.com/watch", "1.2.3.4"))
        assertEquals("acestream://abc", AcestreamLinks.withIp("acestream://abc", "1.2.3.4"))
        assertFalse(AcestreamLinks.isProxyLink("https://example.com/watch"))
        assertTrue(AcestreamLinks.isProxyLink("http://x:6878/ace/getstream?id=1"))
    }

    @Test fun aBlankIpKeepsTheStoredLink() {
        val link = "http://127.0.0.1:6878/ace/getstream?id=1"
        assertEquals(link, AcestreamLinks.withIp(link, " "))
    }
}
