package figueroa.enrique.reproducers.ui.songs

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import figueroa.enrique.reproducers.data.model.Song
import figueroa.enrique.reproducers.data.repository.MusicRepository
import kotlinx.coroutines.launch

class SongsViewModel(private val repo: MusicRepository) : ViewModel() {
    val songs = repo.allSongs

    fun toggleFavorite(song: Song) = viewModelScope.launch {
        repo.toggleFavorite(song)
    }
}

class SongsViewModelFactory(private val repo: MusicRepository) :
    ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        @Suppress("UNCHECKED_CAST")
        return SongsViewModel(repo) as T
    }
}
