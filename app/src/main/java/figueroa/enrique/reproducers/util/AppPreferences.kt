package figueroa.enrique.reproducers.util

import android.content.Context

object AppPreferences {
    private const val PREFS = "app_prefs"
    private const val KEY_ADAPTIVE_SHUFFLE = "adaptive_shuffle"

    fun isAdaptiveShuffleEnabled(context: Context): Boolean =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getBoolean(KEY_ADAPTIVE_SHUFFLE, false)

    fun setAdaptiveShuffle(context: Context, enabled: Boolean) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putBoolean(KEY_ADAPTIVE_SHUFFLE, enabled).apply()
    }
}