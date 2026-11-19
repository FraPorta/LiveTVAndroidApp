package com.example.livetv.data.network

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SourceTimeZoneTest {
    @Test fun readsAPositiveWholeHourOffset() {
        assertEquals(60, SourceTimeZone.parseOffsetMinutes("Your current time zone: 19:54 (UTC +1)"))
    }

    @Test fun readsNegativeAndHalfHourOffsets() {
        assertEquals(-300, SourceTimeZone.parseOffsetMinutes("Your current time zone: 13:54 (UTC -5)"))
        assertEquals(330, SourceTimeZone.parseOffsetMinutes("Your current time zone: 23:24 (UTC +5:30)"))
    }

    @Test fun plainUtcMeansZero() {
        assertEquals(0, SourceTimeZone.parseOffsetMinutes("Your current time zone: 18:54 (UTC)"))
    }

    @Test fun returnsNullWhenThePageHasNoTimeZoneText() {
        assertNull(SourceTimeZone.parseOffsetMinutes("Upcoming matches"))
    }
}
