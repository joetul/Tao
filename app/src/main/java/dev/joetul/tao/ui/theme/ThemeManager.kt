package dev.joetul.tao.ui.theme

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.core.content.edit

enum class ThemeMode {
    SYSTEM,
    LIGHT,
    DARK;

    fun getDisplayName(): String {
        return when (this) {
            SYSTEM -> "System Default"
            LIGHT -> "Light"
            DARK -> "Dark"
        }
    }
}

class ThemeManager private constructor(context: Context) {

    private val sharedPreferences: SharedPreferences =
        context.getSharedPreferences("meditation_settings", Context.MODE_PRIVATE)

    private val _currentTheme = mutableStateOf(
        getThemeFromPreferences()
    )

    val currentTheme: State<ThemeMode> = _currentTheme

    // Add dynamic colors support
    private val _useDynamicColors = mutableStateOf(
        getDynamicColorsFromPreferences()
    )

    val useDynamicColors: State<Boolean> = _useDynamicColors

    private fun getThemeFromPreferences(): ThemeMode {
        val themeName = sharedPreferences.getString("theme_mode", ThemeMode.SYSTEM.name)
        return try {
            ThemeMode.valueOf(themeName ?: ThemeMode.SYSTEM.name)
        } catch (_: IllegalArgumentException) {
            ThemeMode.SYSTEM
        }
    }

    private fun getDynamicColorsFromPreferences(): Boolean {
        // Default to true to use dynamic colors when available
        return sharedPreferences.getBoolean("use_dynamic_colors", true)
    }

    fun setThemeMode(mode: ThemeMode) {
        sharedPreferences.edit { putString("theme_mode", mode.name) }
        _currentTheme.value = mode
    }

    companion object {
        @Volatile
        private var INSTANCE: ThemeManager? = null

        fun getInstance(context: Context): ThemeManager {
            return INSTANCE ?: synchronized(this) {
                val instance = ThemeManager(context)
                INSTANCE = instance
                instance
            }
        }
    }
}