package figueroa.enrique.reproducers.data.db

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import figueroa.enrique.reproducers.data.model.Album

@Dao
interface AlbumDao {
    @Query("SELECT * FROM albums ORDER BY name ASC")
    fun getAllAlbums(): LiveData<List<Album>>

    @Query("SELECT * FROM albums WHERE id = :albumId LIMIT 1")
    suspend fun findById(albumId: Long): Album?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(album: Album): Long

    @Query("UPDATE albums SET coverImagePath = :path WHERE id = :albumId")
    suspend fun updateCover(albumId: Long, path: String)

    @Query("SELECT * FROM albums WHERE name = :name AND artist = :artist LIMIT 1")
    suspend fun findByNameAndArtist(name: String, artist: String): Album?
}
