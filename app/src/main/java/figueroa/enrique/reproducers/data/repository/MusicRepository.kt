package figueroa.enrique.reproducers.data.repository

import androidx.lifecycle.LiveData
import figueroa.enrique.reproducers.data.db.AppDatabase
import figueroa.enrique.reproducers.data.model.*

class MusicRepository(db: AppDatabase) {
    private val songDao = db.songDao()
    private val albumDao = db.albumDao()
    private val artistDao = db.artistDao()
    private val playlistDao = db.playlistDao()

    val allSongs: LiveData<List<Song>> = songDao.getAllSongs()
    val favorites: LiveData<List<Song>> = songDao.getFavorites()
    val allAlbums: LiveData<List<Album>> = albumDao.getAllAlbums()
    val allArtists: LiveData<List<Artist>> = artistDao.getAllArtists()
    val allPlaylists: LiveData<List<Playlist>> = playlistDao.getAllPlaylists()

    fun songsByAlbum(albumId: Long) = songDao.getSongsByAlbum(albumId)
    fun songsByAlbum(albumName: String, artistName: String) = songDao.getSongsByAlbum(albumName, artistName)
    fun songsByArtist(name: String) = songDao.getSongsByArtist(name)
    fun songsInPlaylist(playlistId: Long) = playlistDao.getSongsInPlaylist(playlistId)
    suspend fun albumById(albumId: Long) = albumDao.findById(albumId)

    suspend fun toggleFavorite(song: Song) {
        songDao.setFavorite(song.id, !song.isFavorite)
    }

    suspend fun insertSong(song: Song) = songDao.insert(song)

    suspend fun updateSong(song: Song) {
        val normalizedArtist = ensureArtist(song.artist)
        val normalizedAlbum = ensureAlbum(song.album, normalizedArtist.name)
        songDao.update(
            song.copy(
                artist = normalizedArtist.name,
                album = normalizedAlbum.name,
                albumId = normalizedAlbum.id
            )
        )
    }

    suspend fun updateAlbumCover(albumId: Long, path: String) =
        albumDao.updateCover(albumId, path)

    suspend fun createPlaylist(name: String) = playlistDao.insertPlaylist(Playlist(name = name))

    suspend fun addSongToPlaylist(playlistId: Long, songId: Long) =
        playlistDao.addSongToPlaylist(PlaylistSongCrossRef(playlistId, songId))

    // Escanea una carpeta e inserta canciones + crea álbum/artista si no existen
    suspend fun importSongsFromFolder(songs: List<Song>) {
        val preparedSongs = songs.map { song ->
            val artist = ensureArtist(song.artist)
            val album = ensureAlbum(song.album, artist.name)
            song.copy(
                artist = artist.name,
                album = album.name,
                albumId = album.id
            )
        }
        songDao.insertAll(preparedSongs)
    }

    private suspend fun ensureArtist(name: String): Artist {
        return artistDao.findByName(name) ?: run {
            artistDao.insert(Artist(name = name))
            artistDao.findByName(name) ?: Artist(name = name)
        }
    }

    private suspend fun ensureAlbum(name: String, artistName: String): Album {
        return albumDao.findByNameAndArtist(name, artistName) ?: run {
            albumDao.insert(Album(name = name, artist = artistName))
            albumDao.findByNameAndArtist(name, artistName) ?: Album(name = name, artist = artistName)
        }
    }
}
