package figueroa.enrique.reproducers.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import figueroa.enrique.reproducers.data.model.Artist
import figueroa.enrique.reproducers.ui.components.ArtistItemUi

@Composable
fun ArtistsScreen(
    artists: List<Artist>,
    onArtistClick: (Artist) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(vertical = 8.dp)
    ) {
        items(artists, key = { it.id }) { artist ->
            ArtistItemUi(
                artist = artist,
                onClick = { onArtistClick(artist) }
            )
        }
    }
}
