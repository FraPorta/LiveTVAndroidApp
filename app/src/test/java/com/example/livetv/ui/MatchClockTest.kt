package com.example.livetv.ui

import com.example.livetv.data.model.Match
import java.util.Calendar
import java.util.TimeZone
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class MatchClockTest {
    private lateinit var originalZone: TimeZone

    @Before fun setUp() {
        originalZone = TimeZone.getDefault()
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"))
    }

    @After fun tearDown() = TimeZone.setDefault(originalZone)

    private fun utc(y: Int, mo: Int, d: Int, h: Int, mi: Int): Long =
        Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
            clear(); set(y, mo - 1, d, h, mi, 0)
        }.timeInMillis

    private fun match(time: String, date: String = "Today", offset: Int? = null) = Match(
        time = time, date = date, teams = "A vs B", competition = "Cup", sport = "Football",
        league = "Cup", detailPageUrl = "u/$time/$date", sourceUtcOffsetMin = offset,
    )

    // 19 Nov 2026, 18:00 UTC
    private val now = utc(2026, 11, 19, 18, 0)

    @Test fun kickoffIsConvertedFromTheSourceZoneToTheDeviceZone() {
        // Page says 18:45 in UTC+1, which is 17:45 UTC on the device.
        assertEquals("17:45", MatchClock.displayTime(match("18:45", offset = 60), now))
    }

    @Test fun timeIsLeftAloneWhenTheSourceZoneIsUnknown() {
        assertEquals("18:45", MatchClock.displayTime(match("18:45"), now))
    }

    @Test fun todayIsRelativeToTheSourceZone() {
        // 23:30 UTC on the 19th is already the 20th at UTC+2, so the page's "Today" is the 20th.
        val lateNow = utc(2026, 11, 19, 23, 30)
        val start = MatchClock.startMillis(match("21:00", "Today", offset = 120), lateNow)
        assertEquals(utc(2026, 11, 20, 19, 0), start)
    }

    @Test fun dayLabelUsesTheDeviceZone() {
        // 00:30 on the 20th at UTC+2 is 22:30 on the 19th (today) on the device.
        assertEquals("Today", MatchClock.dayLabel(match("00:30", "20 Nov", offset = 120), now))
        assertEquals("Tomorrow", MatchClock.dayLabel(match("12:00", "20 Nov", offset = 0), now))
        assertEquals("25 Nov", MatchClock.dayLabel(match("12:00", "25 Nov", offset = 0), now))
    }

    @Test fun phaseIsLiveFromKickoffUntilTheWindowEnds() {
        val m = match("17:00", "19 Nov", offset = 0)
        assertEquals(MatchPhase.UPCOMING, MatchClock.phase(m, utc(2026, 11, 19, 16, 59)))
        assertEquals(MatchPhase.LIVE, MatchClock.phase(m, utc(2026, 11, 19, 17, 0)))
        assertEquals(MatchPhase.LIVE, MatchClock.phase(m, utc(2026, 11, 19, 19, 14)))
        assertEquals(MatchPhase.FINISHED, MatchClock.phase(m, utc(2026, 11, 19, 19, 15)))
    }

    @Test fun unreadableTimesAreTreatedAsUpcoming() {
        assertEquals(MatchPhase.UPCOMING, MatchClock.phase(match("TBD"), now))
        assertEquals(MatchPhase.UPCOMING, MatchClock.phase(match("18:00", "someday"), now))
    }

    @Test fun datesNearTheYearEndBelongToTheNeighbouringYear() {
        val newYear = utc(2026, 12, 30, 12, 0)
        assertEquals(utc(2027, 1, 2, 20, 0), MatchClock.startMillis(match("20:00", "2 Jan", offset = 0), newYear))
    }

    @Test fun feedGroupsLiveThenDaysThenFinished() {
        val live = match("17:30", "19 Nov", 0)
        val later = match("21:00", "19 Nov", 0)
        val tomorrow = match("15:00", "20 Nov", 0)
        val done = match("12:00", "19 Nov", 0)
        val feed = buildFeed(listOf(later, done, tomorrow, live), now)
        val titles = feed.filterIsInstance<FeedItem.Header>().map { it.title }
        assertEquals(listOf("Live now", "Today", "Tomorrow", "Finished"), titles)
        val entries = feed.filterIsInstance<FeedItem.Entry>()
        assertEquals(listOf(live, later, tomorrow, done), entries.map { it.match })
    }

    @Test fun splitTeamsHandlesTheSeparatorsTheSiteUses() {
        assertEquals("Arsenal" to "Lyon", splitTeams("Arsenal - Lyon"))
        assertEquals("Arsenal" to "Lyon", splitTeams("Arsenal vs Lyon"))
        assertEquals("Paris" to "HB Koege", splitTeams("Paris – HB Koege"))
        assertTrue(splitTeams("Singapore Grand Prix") == null)
    }
}
