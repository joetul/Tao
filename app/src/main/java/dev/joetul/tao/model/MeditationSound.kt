package dev.joetul.tao.model

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import dev.joetul.tao.R

/**
 * Represents a meditation sound that can be selected in settings
 */
data class MeditationSound(
    val id: String,
    val displayNameResId: Int,
    val resourceId: Int
) {
    // This property will be used in Composable functions
    @Composable
    fun getDisplayName(): String {
        return stringResource(id = displayNameResId)
    }
}

/**
 * List of all available meditation sounds
 */
object MeditationSounds {
    private val BELL_BURMA = MeditationSound(
        id = "bell_burma",
        displayNameResId = R.string.sound_bell_burma,
        resourceId = R.raw.bell_burma
    )

    private val BELL_BURMA_THREE = MeditationSound(
        id = "bell_burma_three",
        displayNameResId = R.string.sound_bell_burma_triple,
        resourceId = R.raw.bell_burma_three
    )

    private val BELL_INDIAN = MeditationSound(
        id = "bell_indian",
        displayNameResId = R.string.sound_bell_indian,
        resourceId = R.raw.bell_indian
    )

    private val BELL_MEDITATION = MeditationSound(
        id = "bell_meditation",
        displayNameResId = R.string.sound_bell_meditation,
        resourceId = R.raw.bell_meditation
    )

    private val BELL_SINGING = MeditationSound(
        id = "bell_singing",
        displayNameResId = R.string.sound_bell_singing,
        resourceId = R.raw.bell_singing
    )

    private val BOWL_SINGING = MeditationSound(
        id = "bowl_singing",
        displayNameResId = R.string.sound_bowl_singing,
        resourceId = R.raw.bowl_singing
    )

    private val BOWL_SINGING_BIG = MeditationSound(
        id = "bowl_singing_big",
        displayNameResId = R.string.sound_bowl_singing_deep,
        resourceId = R.raw.bowl_singing_big
    )

    private val GONG_BODHI = MeditationSound(
        id = "gong_bodhi",
        displayNameResId = R.string.sound_gong_bodhi,
        resourceId = R.raw.gong_bodhi
    )

    private val GONG_GENERATED = MeditationSound(
        id = "gong_generated",
        displayNameResId = R.string.sound_gong_synthesized,
        resourceId = R.raw.gong_generated
    )

    private val GONG_WATTS = MeditationSound(
        id = "gong_watts",
        displayNameResId = R.string.sound_gong_watts,
        resourceId = R.raw.gong_watts
    )

    val DEFAULT = BOWL_SINGING

    val allSounds = listOf(
        BELL_BURMA,
        BELL_BURMA_THREE,
        BELL_INDIAN,
        BELL_MEDITATION,
        BELL_SINGING,
        BOWL_SINGING,
        BOWL_SINGING_BIG,
        GONG_BODHI,
        GONG_GENERATED,
        GONG_WATTS
    )

    fun getSoundById(id: String): MeditationSound {
        return allSounds.find { it.id == id } ?: DEFAULT
    }
}