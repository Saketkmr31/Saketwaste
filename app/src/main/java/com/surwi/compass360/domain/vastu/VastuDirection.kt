package com.surwi.compass360.domain.vastu

/**
 * 16-sector compass classification. Bearing is degrees clockwise from magnetic
 * north unless the caller has explicitly corrected it to true north.
 *
 * Directional associations are presented as commonly encountered Vastu traditions;
 * schools and source texts differ. This is guidance, not a building-code assessment.
 */
data class VastuDirection(
    val code: String,
    val english: String,
    val hindi: String,
    val startBearing: Double,
    val endBearing: Double,
    val traditionalAssociation: String,
    val generalNote: String
)

object VastuDirectionCatalog {
    val directions = listOf(
        VastuDirection("N", "North", "उत्तर", 348.75, 11.25, "Kubera in common modern Vastu", "Often associated with prosperity in modern Vastu guidance."),
        VastuDirection("NNE", "North-North-East", "उत्तर-उत्तर-पूर्व", 11.25, 33.75, "Intermediate zone", "Interpretation varies by Vastu school."),
        VastuDirection("NE", "North-East", "ईशान", 33.75, 56.25, "Ishanya", "Commonly preferred for prayer or meditation spaces."),
        VastuDirection("ENE", "East-North-East", "पूर्व-उत्तर-पूर्व", 56.25, 78.75, "Intermediate zone", "Interpretation varies by Vastu school."),
        VastuDirection("E", "East", "पूर्व", 78.75, 101.25, "Indra in common Vastu mappings", "Often associated with morning light and beginnings."),
        VastuDirection("ESE", "East-South-East", "पूर्व-दक्षिण-पूर्व", 101.25, 123.75, "Intermediate zone", "Interpretation varies by Vastu school."),
        VastuDirection("SE", "South-East", "आग्नेय", 123.75, 146.25, "Agni", "Commonly associated with fire and kitchen placement."),
        VastuDirection("SSE", "South-South-East", "दक्षिण-दक्षिण-पूर्व", 146.25, 168.75, "Intermediate zone", "Interpretation varies by Vastu school."),
        VastuDirection("S", "South", "दक्षिण", 168.75, 191.25, "Yama in common Vastu mappings", "Traditional interpretations vary; avoid fear-based remedies."),
        VastuDirection("SSW", "South-South-West", "दक्षिण-दक्षिण-पश्चिम", 191.25, 213.75, "Intermediate zone", "Interpretation varies by Vastu school."),
        VastuDirection("SW", "South-West", "नैऋत्य", 213.75, 236.25, "Nairitya", "Often associated with stability and master-bedroom placement in modern Vastu."),
        VastuDirection("WSW", "West-South-West", "पश्चिम-दक्षिण-पश्चिम", 236.25, 258.75, "Intermediate zone", "Interpretation varies by Vastu school."),
        VastuDirection("W", "West", "पश्चिम", 258.75, 281.25, "Varuna in common Vastu mappings", "Room recommendations differ between traditions."),
        VastuDirection("WNW", "West-North-West", "पश्चिम-उत्तर-पश्चिम", 281.25, 303.75, "Intermediate zone", "Interpretation varies by Vastu school."),
        VastuDirection("NW", "North-West", "वायव्य", 303.75, 326.25, "Vayu", "Often associated with movement and guest spaces in modern Vastu."),
        VastuDirection("NNW", "North-North-West", "उत्तर-उत्तर-पश्चिम", 326.25, 348.75, "Intermediate zone", "Interpretation varies by Vastu school.")
    )

    fun atBearing(degrees: Double): VastuDirection {
        val normalized = ((degrees % 360.0) + 360.0) % 360.0
        return directions.first { direction ->
            if (direction.startBearing > direction.endBearing) {
                normalized >= direction.startBearing || normalized < direction.endBearing
            } else {
                normalized >= direction.startBearing && normalized < direction.endBearing
            }
        }
    }
}
