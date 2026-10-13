package figueroa.enrique.reproducers.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import coil.compose.AsyncImage
import figueroa.enrique.reproducers.data.model.Song
import figueroa.enrique.reproducers.service.MusicService
import figueroa.enrique.reproducers.ui.components.CoverCarouselView
import figueroa.enrique.reproducers.ui.player.SpectrumVisualizerView
import figueroa.enrique.reproducers.ui.player.VuMeterView
import java.util.Locale

@Composable
fun PlayerScreen(
    currentSong: Song?,
    isPlaying: Boolean,
    currentPositionMs: Long,
    durationMs: Long,
    isShuffle: Boolean,
    repeatMode: Int,
    coverSources: List<Any>,
    visualizerMode: Int, // 0: Cover, 1: Spectrum, 2: VuMeter
    showingLyrics: Boolean,
    lyricsText: String?,
    onCollapseClick: () -> Unit,
    onPlayPauseClick: () -> Unit,
    onNextClick: () -> Unit,
    onPrevClick: () -> Unit,
    onShuffleClick: () -> Unit,
    onRepeatClick: () -> Unit,
    onFavoriteClick: () -> Unit,
    onAddToPlaylistClick: () -> Unit,
    onLyricsClick: () -> Unit,
    onImportLyricsClick: () -> Unit,
    onPrevVisualizerModeClick: () -> Unit,
    onNextVisualizerModeClick: () -> Unit,
    onSeekTo: (Long) -> Unit,
    spectrumViewInstance: SpectrumVisualizerView?,
    vuMeterViewInstance: VuMeterView?,
    modifier: Modifier = Modifier
) {
    if (currentSong == null) return

    var isUserSeeking by remember { mutableStateOf(false) }
    var seekProgressMs by remember { mutableLongStateOf(0L) }

    val effectivePositionMs = if (isUserSeeking) seekProgressMs else currentPositionMs

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // Top Row: Collapse Button
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Start
        ) {
            IconButton(onClick = onCollapseClick) {
                Icon(
                    imageVector = Icons.Default.KeyboardArrowDown,
                    contentDescription = "Colapsar",
                    tint = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.size(32.dp)
                )
            }
        }

        // Center Area: Cover / Visualizers / Lyrics
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(vertical = 16.dp),
            contentAlignment = Alignment.Center
        ) {
            if (showingLyrics) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(16.dp))
                        .background(MaterialTheme.colorScheme.surface)
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .verticalScroll(rememberScrollState()),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = lyricsText ?: "",
                            fontSize = 16.sp,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Button(onClick = onImportLyricsClick) {
                        Text("Importar archivo .txt")
                    }
                }
            } else {
                when (visualizerMode) {
                    1 -> {
                        spectrumViewInstance?.let { view ->
                            AndroidView(
                                factory = { view },
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(RoundedCornerShape(16.dp))
                            )
                        }
                    }
                    2 -> {
                        vuMeterViewInstance?.let { view ->
                            AndroidView(
                                factory = { view },
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(RoundedCornerShape(16.dp))
                            )
                        }
                    }
                    else -> {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(RoundedCornerShape(16.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant),
                            contentAlignment = Alignment.Center
                        ) {
                            if (coverSources.isNotEmpty()) {
                                CoverCarouselView(
                                    sources = coverSources,
                                    modifier = Modifier.fillMaxSize()
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.MusicNote,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(80.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        // Visualizer mode switcher arrows (if not showing lyrics)
        if (!showingLyrics) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onPrevVisualizerModeClick) {
                    Icon(
                        imageVector = Icons.Default.ChevronLeft,
                        contentDescription = "Anterior modo",
                        tint = MaterialTheme.colorScheme.onBackground
                    )
                }
                Text(
                    text = when (visualizerMode) {
                        1 -> "Espectro"
                        2 -> "Vúmetro"
                        else -> "Portada"
                    },
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                    modifier = Modifier.padding(horizontal = 12.dp)
                )
                IconButton(onClick = onNextVisualizerModeClick) {
                    Icon(
                        imageVector = Icons.Default.ChevronRight,
                        contentDescription = "Siguiente modo",
                        tint = MaterialTheme.colorScheme.onBackground
                    )
                }
            }
        }

        // Track Info & Actions
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = currentSong.title,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = currentSong.artist,
                    fontSize = 15.sp,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Row {
                IconButton(onClick = onFavoriteClick) {
                    Icon(
                        imageVector = if (currentSong.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = "Favorito",
                        tint = if (currentSong.isFavorite) Color.Red else MaterialTheme.colorScheme.onBackground
                    )
                }
                IconButton(onClick = onAddToPlaylistClick) {
                    Icon(
                        imageVector = Icons.Default.PlaylistAdd,
                        contentDescription = "Agregar a lista",
                        tint = MaterialTheme.colorScheme.onBackground
                    )
                }
                IconButton(onClick = onLyricsClick) {
                    Icon(
                        imageVector = Icons.Default.Subtitles,
                        contentDescription = "Letras",
                        tint = if (showingLyrics) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onBackground
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Progress Bar & Timers
        Column(modifier = Modifier.fillMaxWidth()) {
            Slider(
                value = if (durationMs > 0) effectivePositionMs.coerceIn(0L, durationMs).toFloat() else 0f,
                onValueChange = {
                    isUserSeeking = true
                    seekProgressMs = it.toLong()
                },
                onValueChangeFinished = {
                    onSeekTo(seekProgressMs)
                    isUserSeeking = false
                },
                valueRange = 0f..(if (durationMs > 0) durationMs.toFloat() else 1f),
                modifier = Modifier.fillMaxWidth()
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = formatTime(effectivePositionMs),
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                )
                Text(
                    text = formatTime(durationMs),
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Main Controls (Shuffle, Prev, Play/Pause, Next, Repeat)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onShuffleClick) {
                Icon(
                    imageVector = Icons.Default.Shuffle,
                    contentDescription = "Aleatorio",
                    tint = if (isShuffle) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f)
                )
            }

            IconButton(onClick = onPrevClick) {
                Icon(
                    imageVector = Icons.Default.SkipPrevious,
                    contentDescription = "Anterior",
                    tint = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.size(36.dp)
                )
            }

            FilledIconButton(
                onClick = onPlayPauseClick,
                modifier = Modifier.size(64.dp),
                shape = CircleShape
            ) {
                Icon(
                    imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = if (isPlaying) "Pausa" else "Reproducir",
                    modifier = Modifier.size(36.dp)
                )
            }

            IconButton(onClick = onNextClick) {
                Icon(
                    imageVector = Icons.Default.SkipNext,
                    contentDescription = "Siguiente",
                    tint = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.size(36.dp)
                )
            }

            IconButton(onClick = onRepeatClick) {
                Icon(
                    imageVector = if (repeatMode == MusicService.REPEAT_ONE) Icons.Default.RepeatOne else Icons.Default.Repeat,
                    contentDescription = "Repetir",
                    tint = if (repeatMode != MusicService.REPEAT_OFF) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f)
                )
            }
        }
    }
}

private fun formatTime(ms: Long): String {
    val totalSeconds = (ms / 1000).toInt()
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format(Locale.getDefault(), "%d:%02d", minutes, seconds)
}
