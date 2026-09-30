package com.example.data

import android.net.Uri

/**
 * LocationPrivacyManager — implements GPS privacy as specified.
 *
 * PRIVACY RULES:
 * - Default mode is OFF.
 * - Exact coordinates are NEVER stored in the database.
 * - GPS permission is requested ONLY when user taps "Find Nearby".
 * - Background location is NEVER requested.
 * - Temp location is cleared after search ends.
 * - Location is NEVER shared with the AI assistant unless user explicitly consents.
 * - Location is NEVER shown in admin dashboard.
 */
object LocationPrivacyManager {

    /**
     * Builds a Google Maps search URL.
     * If lat/lng are provided (temporary exact mode), includes coordinates.
     * Never stores coordinates — they are used only to build this URL.
     */
    fun buildGoogleMapsSearchUrl(
        query: String,
        lat: Double? = null,
        lng: Double? = null
    ): String {
        val encodedQuery = Uri.encode(query)
        return if (lat != null && lng != null) {
            "https://www.google.com/maps/search/$encodedQuery/@$lat,$lng,14z"
        } else {
            "https://www.google.com/maps/search/$encodedQuery"
        }
    }

    /**
     * Nearby search categories supported by the Care tab.
     */
    val nearbyCategories = listOf(
        NearbyCategory("pharmacy", "Pharmacy / Dispensary", "💊"),
        NearbyCategory("gynecologist clinic near me", "Clinic / Gynecologist", "🏥"),
        NearbyCategory("sanitary pads store near me", "Sanitary Pads", "🛒"),
        NearbyCategory("period panties near me", "Period Panties", "🌸"),
        NearbyCategory("menstrual cup store near me", "Menstrual Cup", "🌙"),
        NearbyCategory("hot water bag pharmacy near me", "Hot Water Bag", "🔥"),
        NearbyCategory("heating pad store near me", "Heating Pad", "♨️"),
        NearbyCategory("cotton underwear near me", "Cotton Underwear", "🧺"),
        NearbyCategory("dark chocolate store near me", "Dark Chocolate", "🍫"),
        NearbyCategory("grocery store near me", "Grocery / Comfort Food", "🥬"),
        NearbyCategory("cafe warm tea near me", "Warm Tea / Café", "☕")
    )

    data class NearbyCategory(
        val query: String,
        val label: String,
        val emoji: String
    )

    /**
     * Determines if GPS permission should be requested for a given mode.
     * NEVER request GPS on first launch or outside of "Find Nearby" action.
     */
    fun shouldRequestGpsForMode(mode: LocationPrivacyMode): Boolean {
        return mode == LocationPrivacyMode.TEMPORARY_EXACT || mode == LocationPrivacyMode.APPROXIMATE_REGION
    }

    /**
     * Privacy mode descriptions shown to users in the Permission Center.
     */
    fun getModeDescription(mode: LocationPrivacyMode): String {
        return when (mode) {
            LocationPrivacyMode.OFF ->
                "No GPS access. Search by city name manually. Nothing is stored."
            LocationPrivacyMode.ON_DEVICE_ONLY ->
                "GPS used only to open Google Maps. Nothing is sent to LunaCare servers."
            LocationPrivacyMode.APPROXIMATE_REGION ->
                "Only your country/city is saved — never exact coordinates."
            LocationPrivacyMode.TEMPORARY_EXACT ->
                "Exact location used only during search, then discarded immediately."
        }
    }
}
