package figueroa.enrique.reproducers.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import figueroa.enrique.reproducers.data.model.Playlist
import figueroa.enrique.reproducers.ui.components.CreatePlaylistDialogUi
import figueroa.enrique.reproducers.ui.components.PlaylistItemUi

@Composable
fun PlaylistsScreen(
    playlists: List<Playlist>,
    onPlaylistClick: (Playlist) -> Unit,
    onCreatePlaylistConfirm: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var showCreateDialog by remember { mutableStateOf(false) }

    Box(modifier = modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(vertical = 8.dp)
        ) {
            items(playlists, key = { it.id }) { playlist ->
                PlaylistItemUi(
                    playlist = playlist,
                    onClick = { onPlaylistClick(playlist) }
                )
            }
        }

        FloatingActionButton(
            onClick = { showCreateDialog = true },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
        ) {
            Icon(imageVector = Icons.Default.Add, contentDescription = "Nueva Lista")
        }

        if (showCreateDialog) {
            CreatePlaylistDialogUi(
                onDismiss = { showCreateDialog = false },
                onConfirm = { name ->
                    onCreatePlaylistConfirm(name)
                    showCreateDialog = false
                }
            )
        }
    }
}
