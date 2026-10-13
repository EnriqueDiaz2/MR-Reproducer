@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package figueroa.enrique.reproducers.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import figueroa.enrique.reproducers.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    isAdaptiveShuffleEnabled: Boolean,
    onAdaptiveShuffleChanged: (Boolean) -> Unit,
    isAppCarouselEnabled: Boolean,
    onAppCarouselChanged: (Boolean) -> Unit,
    isNotificationCarouselEnabled: Boolean,
    onNotificationCarouselChanged: (Boolean) -> Unit,
    isDarkMode: Boolean,
    onDarkModeChanged: (Boolean) -> Unit,
    onSelectFolderClick: () -> Unit,
    onImportFilesClick: () -> Unit,
    onRescanFolderClick: () -> Unit,
    equalizerBandsCount: Int,
    getBandLevel: (Short) -> Short,
    getBandRange: () -> ShortArray?,
    setBandLevel: (Short, Short) -> Unit,
    modifier: Modifier = Modifier
) {
    var mostrarDialogoAbout by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Biblioteca de música
        Text(
            text = stringResource(R.string.library),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary
        )

        Button(
            onClick = onSelectFolderClick,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(stringResource(R.string.select_music_folder))
        }

        OutlinedButton(
            onClick = onImportFilesClick,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(stringResource(R.string.import_files))
        }

        OutlinedButton(
            onClick = onRescanFolderClick,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(stringResource(R.string.rescan_folder))
        }

        HorizontalDivider()

        // Apariencia
        Text(
            text = stringResource(R.string.appearance),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(stringResource(R.string.appearance_dark))
            Switch(
                checked = isDarkMode,
                onCheckedChange = onDarkModeChanged
            )
        }

        HorizontalDivider()

        // Opciones Experimentales
        Text(
            text = stringResource(R.string.experimental),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(R.string.experimentaltext),
                modifier = Modifier.weight(1f)
            )
            Switch(
                checked = isAdaptiveShuffleEnabled,
                onCheckedChange = onAdaptiveShuffleChanged
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(R.string.carousel_in_app),
                modifier = Modifier.weight(1f)
            )
            Switch(
                checked = isAppCarouselEnabled,
                onCheckedChange = onAppCarouselChanged
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(R.string.carousel_in_notification),
                modifier = Modifier.weight(1f)
            )
            Switch(
                checked = isNotificationCarouselEnabled,
                onCheckedChange = onNotificationCarouselChanged
            )
        }

        HorizontalDivider()

        Column(
            modifier = Modifier.padding(start = 12.dp, bottom = 24.dp, end = 5.dp)
        ) {
            OutlinedButton(
                onClick = {
                    mostrarDialogoAbout = true
                },
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                ),
                border = BorderStroke(
                    width = 2.dp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = stringResource(R.string.about_title),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }

        // Ecualizador
        Text(
            text = stringResource(R.string.equalizer),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary
        )

        if (mostrarDialogoAbout) {
            AlertDialog(
                onDismissRequest = {
                    mostrarDialogoAbout = false
                },
                confirmButton = {
                    ElevatedButton(
                        onClick = {
                            mostrarDialogoAbout = false
                        },
                        colors = ButtonDefaults.elevatedButtonColors(
                            contentColor = MaterialTheme.colorScheme.onPrimary,
                            containerColor = MaterialTheme.colorScheme.primary
                        ),
                        elevation = ButtonDefaults.elevatedButtonElevation(
                            defaultElevation = 8.dp,
                            pressedElevation = 10.dp,
                            disabledElevation = 2.dp,
                            hoveredElevation = 8.dp,
                            focusedElevation = 8.dp
                        )
                    ) {
                        Text(
                            text = stringResource(R.string.close),
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                },
                title = {
                    Text(
                        text = stringResource(R.string.about_title),
                        color = MaterialTheme.colorScheme.primary
                    )
                },
                text = {
                    Text(
                        text = stringResource(R.string.about_message1) +"\n" +
                                stringResource(R.string.about_message2) +"\n" +
                                stringResource(R.string.about_message3) +"\n\n" +
                                stringResource(R.string.about_message4) +"\n\n" +
                                stringResource(R.string.about_message5) +"\n" +
                                stringResource(R.string.about_message6) +"\n\n" +
                                stringResource(R.string.about_message7),
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                },
                icon = {
                    Image(
                        painter = painterResource(id = R.drawable.app_icon),
                        contentDescription = "Logo de la aplicacion",
                        modifier = Modifier
                            .size(80.dp)
                            .clip(RoundedCornerShape(16.dp))
                    )
                }
            )
        }

        if (equalizerBandsCount <= 0) {
            Text(
                text = stringResource(R.string.play_song_to_use_equalizer),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            val bandRange = getBandRange()
            if (bandRange != null && bandRange.size >= 2) {
                val minLevel = bandRange[0].toFloat()
                val maxLevel = bandRange[1].toFloat()

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    for (band in 0 until equalizerBandsCount) {
                        val bandShort = band.toShort()
                        var currentLevel by remember(band) { mutableFloatStateOf(getBandLevel(bandShort).toFloat()) }

                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = "B${band + 1}",
                                style = MaterialTheme.typography.labelSmall
                            )
                            Slider(
                                value = currentLevel,
                                onValueChange = {
                                    currentLevel = it
                                    setBandLevel(bandShort, it.toInt().toShort())
                                },
                                valueRange = minLevel..maxLevel,
                                modifier = Modifier.height(120.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
