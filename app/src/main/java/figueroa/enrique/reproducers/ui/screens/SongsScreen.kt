package figueroa.enrique.reproducers.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import figueroa.enrique.reproducers.data.model.Playlist
import figueroa.enrique.reproducers.data.model.Song
import figueroa.enrique.reproducers.ui.components.*

@Composable
fun SongsScreen(
    songs: List<Song>,
    currentSong: Song?,
    playlists: List<Playlist>,
    getPlaylistIdsForSong: suspend (Long) -> List<Long>,
    onSongClick: (Song, Int) -> Unit,
    onFavoriteClick: (Song) -> Unit,
    onEditSongConfirm: (Song, String, String, String) -> Unit,
    onAddToPlaylistSelected: (Song, Playlist) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedSongForOptions by remember { mutableStateOf<Song?>(null) }
    var selectedSongForEdit by remember { mutableStateOf<Song?>(null) }
    var selectedSongForAddToPlaylist by remember { mutableStateOf<Song?>(null) }
    var playlistIdsWithSong by remember { mutableStateOf<Set<Long>>(emptySet()) }

    Box(modifier = modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(vertical = 8.dp)
        ) {
            itemsIndexed(songs, key = { _, song -> song.id }) { index, song ->
                SongItemUi(
                    song = song,
                    isPlayingCurrentSong = currentSong?.id == song.id,
                    onClick = { onSongClick(song, index) },
                    onFavoriteClick = { onFavoriteClick(song) },
                    onMoreClick = { selectedSongForOptions = song }
                )
            }
        }

        selectedSongForOptions?.let { song ->
            SongOptionsDialogUi(
                song = song,
                onDismiss = { selectedSongForOptions = null },
                onEditSong = {
                    selectedSongForOptions = null
                    selectedSongForEdit = song
                },
                onAddToPlaylist = {
                    selectedSongForOptions = null
                    selectedSongForAddToPlaylist = song
                }
            )
        }

        selectedSongForEdit?.let { song ->
            EditSongDialogUi(
                song = song,
                onDismiss = { selectedSongForEdit = null },
                onConfirm = { title, artist, album ->
                    onEditSongConfirm(song, title, artist, album)
                    selectedSongForEdit = null
                }
            )
        }

        selectedSongForAddToPlaylist?.let { song ->
            LaunchedEffect(song.id) {
                val ids = getPlaylistIdsForSong(song.id)
                playlistIdsWithSong = ids.toSet()
            }
            AddToPlaylistDialogUi(
                playlists = playlists,
                playlistIdsWithSong = playlistIdsWithSong,
                onDismiss = { selectedSongForAddToPlaylist = null },
                onPlaylistSelected = { playlist ->
                    onAddToPlaylistSelected(song, playlist)
                    selectedSongForAddToPlaylist = null
                }
            )
        }
    }
}
