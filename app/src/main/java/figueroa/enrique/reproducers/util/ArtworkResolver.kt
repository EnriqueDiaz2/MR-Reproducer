package figueroa.enrique.reproducers.util

import android.content.Context
import figueroa.enrique.reproducers.data.db.AppDatabase
import figueroa.enrique.reproducers.data.model.Song
import figueroa.enrique.reproducers.data.repository.MusicRepository
import figueroa.enrique.reproducers.service.MusicService

object ArtworkResolver {
    // Prioridad: portada de álbum (elegida por el usuario) > embebida en el archivo
    suspend fun sourcesFor(context: Context, song: Song, service: MusicService?): List<Any> {
        val repo = MusicRepository(AppDatabase.getDatabase(context))
        val album = repo.albumById(song.albumId)
        val embedded = service?.loadArtwork(song)

        val list = mutableListOf<Any>()
        album?.coverImagePath?.takeIf { it.isNotBlank() }?.let { list.add(it) }
        embedded?.let { list.add(it) }
        return list.distinct()
    }
}