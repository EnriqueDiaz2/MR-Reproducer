package figueroa.enrique.reproducers.ui.player

import okhttp3.OkHttpClient
import okhttp3.Request
import java.net.URLEncoder

object LyricsFetcher {
    private val client = OkHttpClient()

    fun searchOnline(rawTitle: String, rawArtist: String): String? {
        return try {
            val title = cleanTitle(rawTitle)
            val artist = cleanTitle(rawArtist)

            val encodedArtist = URLEncoder.encode(artist, "UTF-8")
            val encodedTitle = URLEncoder.encode(title, "UTF-8")
            val url = "https://api.lyrics.ovh/v1/$encodedArtist/$encodedTitle"

            val request = Request.Builder().url(url).build()
            val response = client.newCall(request).execute()
            val body = response.body?.string() ?: return null

            if (!response.isSuccessful) return null

            val json = org.json.JSONObject(body)
            val lyrics = json.optString("lyrics", "")
            lyrics.ifBlank { null }
        } catch (e: Exception) {
            android.util.Log.e("LyricsFetcher", "Error: ${e.message}", e)
            null
        }
    }

    // Limpia sufijos comunes de nombres de archivo descargados
    private fun cleanTitle(text: String): String {
        return text
            .replace(Regex("\\(.*?\\)"), "")           // quita (Official Video), (Lyrics), etc.
            .replace(Regex("\\[.*?\\]"), "")            // quita [Official Audio], etc.
            .replace(Regex("(?i)official\\s*(music\\s*)?video"), "")
            .replace(Regex("(?i)lyrics?"), "")
            .replace(Regex("(?i)audio"), "")
            .replace(Regex("(?i)ft\\.?|feat\\.?"), "")
            .replace(Regex("[_]"), " ")
            .replace(Regex("\\s+"), " ")
            .trim()
    }
}
