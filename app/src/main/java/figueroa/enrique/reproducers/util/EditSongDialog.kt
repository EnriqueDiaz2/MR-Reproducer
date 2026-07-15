package figueroa.enrique.reproducers.util

import android.content.Context
import androidx.appcompat.app.AlertDialog
import com.google.android.material.textfield.TextInputEditText
import figueroa.enrique.reproducers.R
import figueroa.enrique.reproducers.data.model.Song
import figueroa.enrique.reproducers.data.repository.MusicRepository
import kotlinx.coroutines.*

object EditSongDialog {
    fun show(context: Context, song: Song, repo: MusicRepository, onSaved: () -> Unit = {}) {
        val view = android.view.LayoutInflater.from(context)
            .inflate(R.layout.dialog_edit_song, null)

        val editTitle = view.findViewById<TextInputEditText>(R.id.editTitle)
        val editArtist = view.findViewById<TextInputEditText>(R.id.editArtist)
        val editAlbum = view.findViewById<TextInputEditText>(R.id.editAlbum)

        editTitle.setText(song.title)
        editArtist.setText(song.artist)
        editAlbum.setText(song.album)

        AlertDialog.Builder(context)
            .setTitle(R.string.edit_song)
            .setView(view)
            .setPositiveButton(android.R.string.ok) { _, _ ->
                val updated = song.copy(
                    title = editTitle.text?.toString()?.trim().takeIf { !it.isNullOrEmpty() } ?: song.title,
                    artist = editArtist.text?.toString()?.trim().takeIf { !it.isNullOrEmpty() } ?: song.artist,
                    album = editAlbum.text?.toString()?.trim().takeIf { !it.isNullOrEmpty() } ?: song.album
                )
                CoroutineScope(Dispatchers.IO).launch {
                    repo.updateSong(updated)
                    withContext(Dispatchers.Main) { onSaved() }
                }
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }
}
