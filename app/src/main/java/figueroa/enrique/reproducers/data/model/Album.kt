package figueroa.enrique.reproducers.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "albums")
data class Album(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val artist: String,
    val coverImagePath: String? = null   // el usuario puede poner/cambiar esta imagen
)
