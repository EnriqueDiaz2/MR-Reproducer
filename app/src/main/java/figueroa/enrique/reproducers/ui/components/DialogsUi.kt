package figueroa.enrique.reproducers.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import figueroa.enrique.reproducers.R
import figueroa.enrique.reproducers.data.model.Playlist
import figueroa.enrique.reproducers.data.model.Song

@Composable
fun EditSongDialogUi(
    song: Song,
    onDismiss: () -> Unit,
    onConfirm: (title: String, artist: String, album: String) -> Unit
) {
    var title by remember { mutableStateOf(song.title) }
    var artist by remember { mutableStateOf(song.artist) }
    var album by remember { mutableStateOf(song.album) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = stringResource(R.string.edit_song)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text(stringResource(R.string.title)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = artist,
                    onValueChange = { artist = it },
                    label = { Text(stringResource(R.string.artist)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = album,
                    onValueChange = { album = it },
                    label = { Text(stringResource(R.string.album)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onConfirm(title.ifBlank { song.title }, artist.ifBlank { song.artist }, album.ifBlank { song.album })
                }
            ) {
                Text(stringResource(R.string.ok))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
}

@Composable
fun CreatePlaylistDialogUi(
    onDismiss: () -> Unit,
    onConfirm: (name: String) -> Unit
) {
    var name by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.new_playlist_title)) },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text(stringResource(R.string.playlist_name_hint)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (name.isNotBlank()) onConfirm(name.trim())
                }
            ) {
                Text(stringResource(R.string.create))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
}

@Composable
fun AddToPlaylistDialogUi(
    playlists: List<Playlist>,
    playlistIdsWithSong: Set<Long>,
    onDismiss: () -> Unit,
    onPlaylistSelected: (Playlist) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.add_to_playlist)) },
        text = {
            if (playlists.isEmpty()) {
                Text(stringResource(R.string.first_create_playlist))
            } else {
                LazyColumn(modifier = Modifier.fillMaxWidth()) {
                    items(playlists) { playlist ->
                        val alreadyAdded = playlistIdsWithSong.contains(playlist.id)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .then(
                                    if (!alreadyAdded) {
                                        Modifier.clickable { onPlaylistSelected(playlist) }
                                    } else {
                                        Modifier
                                    }
                                )
                                .padding(vertical = 12.dp, horizontal = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = playlist.name,
                                style = MaterialTheme.typography.bodyLarge,
                                color = if (alreadyAdded) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f) else MaterialTheme.colorScheme.onSurface
                            )
                            if (alreadyAdded) {
                                Text(
                                    text = stringResource(R.string.already_in_playlist),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
}

@Composable
fun SongOptionsDialogUi(
    song: Song,
    onDismiss: () -> Unit,
    onEditSong: () -> Unit,
    onAddToPlaylist: () -> Unit,
    onRemoveFromPlaylist: (() -> Unit)? = null
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = song.title) },
        text = {
            Column {
                Text(
                    text = stringResource(R.string.edit_song),
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onEditSong() }
                        .padding(vertical = 12.dp)
                )
                Text(
                    text = stringResource(R.string.add_to_playlist_option),
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onAddToPlaylist() }
                        .padding(vertical = 12.dp)
                )
                onRemoveFromPlaylist?.let { remove ->
                    Text(
                        text = stringResource(R.string.remove_from_playlist),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { remove() }
                            .padding(vertical = 12.dp)
                    )
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
}
