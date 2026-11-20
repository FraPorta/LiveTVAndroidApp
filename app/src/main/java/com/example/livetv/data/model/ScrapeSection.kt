package com.example.livetv.data.model

/**
 * A tab on the home screen, in the order the tabs are shown.
 *
 * [selector] picks the part of the football page to read. The sport tabs instead read the site's
 * own page for that sport, `/allupcomingsports/<sportPage>/`, and label every match with [sportName].
 */
enum class ScrapingSection(
    val displayName: String,
    val selector: String,
    val sportPage: Int? = null,
    val sportName: String? = null,
) {
    FOOTBALL("Football", ":not(#upcoming)"),
    TENNIS("Tennis", "", 4, "Tennis"),
    TOP_EVENTS_LIVE("Top live", "#upcoming"),
    ALL("All", ""),
    BASKETBALL("Basketball", "", 3, "Basketball"),
    ICE_HOCKEY("Ice hockey", "", 2, "Ice Hockey"),
    VOLLEYBALL("Volleyball", "", 5, "Volleyball"),
    HANDBALL("Handball", "", 13, "Handball"),
    CRICKET("Cricket", "", 41, "Cricket"),
    RUGBY("Rugby", "", 33, "Rugby Union"),
    AMERICAN_FOOTBALL("American football", "", 27, "American Football"),
    RACING("Racing", "", 7, "Racing"),
}
