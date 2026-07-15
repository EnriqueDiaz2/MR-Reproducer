package figueroa.enrique.reproducers.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "songs")
data class Song(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val artist: String,
    val album: String,
    val filePath: String,       // ruta del archivo de audio/video
    val duration: Long,          // en milisegundos
    val albumId: Long = 0,
    val isVideo: Boolean = false,
    val isFavorite: Boolean = false,
    val lyricsPath: String? = null,  // si el usuario importó letra .lrc
    val dateAdded: Long = System.currentTimeMillis()
)

