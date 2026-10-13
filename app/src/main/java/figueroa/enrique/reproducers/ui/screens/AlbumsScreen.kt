package figueroa.enrique.reproducers.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import figueroa.enrique.reproducers.data.model.Album
import figueroa.enrique.reproducers.ui.components.AlbumItemUi

@Composable
fun AlbumsScreen(
    albums: List<Album>,
    onAlbumClick: (Album) -> Unit,
    onEditCoverClick: (Album) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(vertical = 8.dp)
    ) {
        items(albums, key = { it.id }) { album ->
            AlbumItemUi(
                album = album,
                onClick = { onAlbumClick(album) },
                onEditCover = { onEditCoverClick(album) }
            )
        }
    }
}
