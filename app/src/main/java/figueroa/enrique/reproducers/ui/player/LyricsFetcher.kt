package figueroa.enrique.reproducers.ui.player

import android.util.Log
import okhttp3.OkHttpClient
import okhttp3.Request
import java.net.URLEncoder
import java.util.concurrent.TimeUnit

object LyricsFetcher {
    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .writeTimeout(15, TimeUnit.SECONDS)
        .build()

    fun searchOnline(rawTitle: String, rawArtist: String): String? {
        return try {
            val title = cleanTitle(rawTitle)
            val artist = cleanTitle(rawArtist)

            if (artist.isBlank() || artist.equals("desconocido", ignoreCase = true) ||
                artist.equals("unknown", ignoreCase = true)) {
                Log.w("LyricsFetcher", "Artista desconocido, no se puede buscar letra")
                return null
            }

            val encodedArtist = URLEncoder.encode(artist, "UTF-8")
            val encodedTitle = URLEncoder.encode(title, "UTF-8")
            val url = "https://api.lyrics.ovh/v1/$encodedArtist/$encodedTitle"

            val request = Request.Builder().url(url).build()
            val response = client.newCall(request).execute()
            val body = response.body?.string()

            if (!response.isSuccessful || body.isNullOrBlank()) {
                Log.w("LyricsFetcher", "Respuesta no exitosa: ${response.code}")
                return null
            }

            val json = org.json.JSONObject(body)
            val lyrics = json.optString("lyrics", "")
            lyrics.ifBlank { null }
        } catch (e: java.net.SocketTimeoutException) {
            Log.e("LyricsFetcher", "Timeout buscando letra", e)
            null
        } catch (e: Exception) {
            Log.e("LyricsFetcher", "Error: ${e.message}", e)
            null
        }
    }

    private fun cleanTitle(text: String): String {
        return text
            .replace(Regex("\\(.*?\\)"), "")
            .replace(Regex("\\[.*?]"), "")
            .replace(Regex("(?i)official\\s*(music\\s*)?video"), "")
            .replace(Regex("(?i)lyrics?"), "")
            .replace(Regex("(?i)audio"), "")
            .replace(Regex("(?i)ft\\.?|feat\\.?"), "")
            .replace(Regex("_"), " ")
            .replace(Regex("\\s+"), " ")
            .trim()
    }
}