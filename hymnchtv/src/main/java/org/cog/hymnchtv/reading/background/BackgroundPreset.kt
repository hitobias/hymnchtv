package org.cog.hymnchtv.reading.background

import org.cog.hymnchtv.reading.background.BackgroundCategory.CALM
import org.cog.hymnchtv.reading.background.BackgroundCategory.GRADIENT
import org.cog.hymnchtv.reading.background.BackgroundCategory.MOTIF
import org.cog.hymnchtv.reading.background.BackgroundCategory.NIGHT

enum class BackgroundCategory { CALM, GRADIENT, MOTIF, NIGHT }

/** A translucent layer drawn over the stops (texture grain, motif, light rays); [maxAlpha] is its strongest pixel. */
data class Overlay(val color: Int, val maxAlpha: Float)

private fun argb(hex: String): Int = (0xFF000000L or hex.removePrefix("#").toLong(16)).toInt()

/**
 * The 20 drawn backgrounds (plan A2). Declaration order is the picker order.
 * [stops] must appear verbatim in res/drawable/bg_<id>.xml (BackgroundResourcesTest), which
 * tools/gen_backgrounds.py writes from the same values.
 */
enum class BackgroundPreset(
    val id: String,
    val category: BackgroundCategory,
    stopHex: List<String>,
    overlayHex: List<Pair<String, Float>>,
    textHex: String,
    accentHex: String,
) {
    XUAN("xuan", CALM, listOf("#f8f6f0"), listOf("#594d40" to 0.14f), "#2b2a28", "#7a3b2e"),
    LINEN("linen", CALM, listOf("#ecebe6"), listOf("#000000" to 0.05f), "#2b2a28", "#3d5a73"),
    PARCHMENT("parchment", CALM, listOf("#f6ead0", "#e3cd9e"), listOf("#735226" to 0.2f), "#3a2f22", "#8a4b1f"),
    MIST("mist", CALM, listOf("#e7eef3"), emptyList(), "#2b2a28", "#2f5d84"),
    DAWN("dawn", GRADIENT, listOf("#fbd9bd", "#fff1e2", "#fffaf4"), emptyList(), "#2b2a28", "#b2542b"),
    SKY("sky", GRADIENT, listOf("#c9def2", "#eef5fb", "#f8fbfe"), emptyList(), "#2b2a28", "#2c5f93"),
    HARVEST("harvest", GRADIENT, listOf("#f3dd9b", "#faf0d2", "#fdf9ec"), emptyList(), "#33291a", "#8c6216"),
    DUSK("dusk", GRADIENT, listOf("#e3d6ef", "#f6e4ec", "#fcf3f6"), emptyList(), "#2b2a28", "#7a3f74"),
    MEADOW("meadow", GRADIENT, listOf("#f6fbf2", "#e5f2df", "#d5ead0"), emptyList(), "#2b2a28", "#3c6b35"),
    STAFF("staff", MOTIF, listOf("#faf9f5"), listOf("#465064" to 0.13f), "#2b2a28", "#34466b"),
    OLIVE("olive", MOTIF, listOf("#f7f8f1"), listOf("#6e8246" to 0.22f, "#5c6e3c" to 0.35f), "#2b2a28", "#4f6230"),
    WHEAT("wheat", MOTIF, listOf("#fffdf6", "#f8f0da"), listOf("#be963c" to 0.25f, "#967328" to 0.35f), "#2b2a28", "#80591a"),
    DOVE("dove", MOTIF, listOf("#eef3f9", "#fafcfe"), listOf("#5a78a0" to 0.16f), "#2b2a28", "#355c8c"),
    RAYS("rays", MOTIF, listOf("#fbe7b9", "#fdf6e6"), listOf("#ffffff" to 0.55f), "#33291a", "#9a5d12"),
    WATER("water", MOTIF, listOf("#f5fbfc", "#e1f0f3"), listOf("#286e8c" to 0.22f), "#2b2a28", "#1f6a80"),
    VINE("vine", MOTIF, listOf("#faf8fb"), listOf("#645078" to 0.3f, "#6e508c" to 0.2f, "#5a7846" to 0.2f), "#2b2a28", "#5e3f73"),
    NIGHTREAD("nightread", NIGHT, listOf("#24211d"), listOf("#e6d9bf" to 0.1f), "#ece4d6", "#e3b77a"),
    // Star dots are capped at 0.28 so text drawn over the brightest one still passes (StarsDrawable reads this)
    STARRY("starry", NIGHT, listOf("#0f1a33", "#1c2a4a"), listOf("#ffffff" to 0.28f), "#e6ebf5", "#f2d48a"),
    DEEPSEA("deepsea", NIGHT, listOf("#0f2a33", "#123d42"), emptyList(), "#e2efee", "#8fd3c7"),
    INK("ink", NIGHT, listOf("#1b1d22"), listOf("#ccccd9" to 0.12f), "#e6e6ea", "#b9c7e8");

    /** Gradient stops (or the single solid colour), top to bottom. */
    val stops: List<Int> = stopHex.map(::argb)
    val overlays: List<Overlay> = overlayHex.map { (hex, alpha) -> Overlay(argb(hex), alpha) }
    val textColor: Int = argb(textHex)
    val accentColor: Int = argb(accentHex)

    /** First stop; on dark backgrounds the score paper is recoloured to this. */
    val baseColor: Int get() = stops.first()
    val isDark: Boolean get() = category == NIGHT

    /**
     * Every colour a reader can see behind text: for each stop, every combination of overlays (each layer absent or at
     * its strongest alpha, painted in order). A gradient between two stops lies between them; the rasterised check in
     * androidTest covers the pixels in between.
     */
    fun swatches(): List<Int> = stops.flatMap { stop ->
        overlays.fold(listOf(stop)) { acc, o -> acc + acc.map { Wcag.blend(it, o.color, o.maxAlpha) } }
    }.distinct()

    companion object {
        @JvmStatic
        fun fromId(id: String?): BackgroundPreset? = entries.firstOrNull { it.id == id }
    }
}
