package figueroa.enrique.reproducers.data.db

import androidx.lifecycle.LiveData
import androidx.room.*
import figueroa.enrique.reproducers.data.model.*

@Dao
interface PlaylistDao {
    @Query("SELECT * FROM playlists ORDER BY dateCreated DESC")
    fun getAllPlaylists(): LiveData<List<Playlist>>

    @Insert
    suspend fun insertPlaylist(playlist: Playlist): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun addSongToPlaylist(crossRef: PlaylistSongCrossRef)

    @Query("""
    SELECT songs.* FROM songs 
    INNER JOIN playlist_song_cross_ref ON songs.id = playlist_song_cross_ref.songId 
    WHERE playlist_song_cross_ref.playlistId = :playlistId 
    ORDER BY playlist_song_cross_ref.sortOrder ASC
""")
    fun getSongsInPlaylist(playlistId: Long): LiveData<List<Song>>

    @Query("DELETE FROM playlist_song_cross_ref WHERE playlistId = :playlistId AND songId = :songId")
    suspend fun removeSongFromPlaylist(playlistId: Long, songId: Long)

    @Delete
    suspend fun deletePlaylist(playlist: Playlist)
}
