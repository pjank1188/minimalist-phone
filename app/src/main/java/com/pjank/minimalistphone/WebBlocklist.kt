package com.pjank.minimalistphone

/**
 * Domains bounced out of Chrome by [WorkHoursService]. Chrome is the one door the
 * launcher can't filter: the YouTube app is pm-disabled but youtube.com works fine,
 * and Private DNS only covers the domains it was pointed at. Like the allow-list,
 * the blocklist is hard-coded — changing it means editing source and rebuilding.
 */
object WebBlocklist {

    /** Matched with subdomains included: "youtube.com" also blocks m.youtube.com. */
    private val BLOCKED = setOf(
        "youtube.com",     // the app is pm-disabled; this closes the web version
        "reddit.com",      // belt-and-braces on top of Private DNS
        "x.com",
        "twitter.com",
        "tiktok.com",
        "facebook.com",
        "news.google.com",
    )

    /** The toast to show if [host] is blocked, or null if it's allowed. */
    fun restrictionFor(host: String): String? =
        BLOCKED.firstOrNull { host == it || host.endsWith(".$it") }
            ?.let { "$it is blocked" }

    /**
     * Chrome's omnibox shows URLs without a scheme ("m.youtube.com/watch?v=…"), so the
     * host is everything before the first slash, minus any port and a leading "www.".
     * Returns null for text that isn't a host at all (search queries, error pages).
     */
    fun hostOf(displayed: String): String? =
        displayed.substringBefore('/').substringBefore(':')
            .lowercase().removePrefix("www.")
            .takeIf { '.' in it && ' ' !in it }
}
