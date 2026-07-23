package figueroa.enrique.reproducers.data.db

import androidx.lifecycle.LiveData
import androidx.room.*
import figueroa.enrique.reproducers.data.model.Song

@Dao
interface SongDao {
    @Query("SELECT * FROM songs ORDER BY title ASC")
    fun getAllSongs(): LiveData<List<Song>>

    @Query("SELECT * FROM songs WHERE isFavorite = 1")
    fun getFavorites(): LiveData<List<Song>>

    @Query("SELECT * FROM songs WHERE albumId = :albumId")
    fun getSongsByAlbum(albumId: Long): LiveData<List<Song>>

    @Query("SELECT * FROM songs WHERE album = :albumName AND artist = :artistName ORDER BY title ASC")
    fun getSongsByAlbum(albumName: String, artistName: String): LiveData<List<Song>>

    @Query("SELECT * FROM songs WHERE artist = :artistName")
    fun getSongsByArtist(artistName: String): LiveData<List<Song>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(song: Song): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(songs: List<Song>)

    @Update
    suspend fun update(song: Song)

    @Query("UPDATE songs SET isFavorite = :isFav WHERE id = :songId")
    suspend fun setFavorite(songId: Long, isFav: Boolean)

    @Query("UPDATE songs SET playCount = playCount + 1 WHERE id = :songId")
    suspend fun incrementPlayCount(songId: Long)

    @Delete
    suspend fun delete(song: Song)
}
