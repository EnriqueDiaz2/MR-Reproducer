package figueroa.enrique.reproducers.ui.songs

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import figueroa.enrique.reproducers.R
import figueroa.enrique.reproducers.data.db.AppDatabase
import figueroa.enrique.reproducers.data.model.Song
import figueroa.enrique.reproducers.data.repository.MusicRepository
import figueroa.enrique.reproducers.databinding.ItemSongBinding
import figueroa.enrique.reproducers.service.MusicService
import figueroa.enrique.reproducers.util.CoverCarousel
import kotlinx.coroutines.*

class SongAdapter(
    private val onClick: (Song, Int) -> Unit,
    private val onFavoriteClick: (Song) -> Unit,
    private val onMoreClick: (Song) -> Unit,
    private val musicService: () -> MusicService?
) : ListAdapter<Song, SongAdapter.SongViewHolder>(DIFF) {

    inner class SongViewHolder(val binding: ItemSongBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SongViewHolder {
        val binding = ItemSongBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return SongViewHolder(binding)
    }

    override fun onBindViewHolder(holder: SongViewHolder, position: Int) {
        val song = getItem(position)
        holder.binding.songTitle.text = song.title
        holder.binding.songArtist.text = song.artist

        val service = musicService()
        val isCurrentlyPlaying = service?.currentSong()?.id == song.id

        if (isCurrentlyPlaying) {
            val repo = MusicRepository(AppDatabase.getDatabase(holder.itemView.context))
            CoroutineScope(Dispatchers.Main).launch {
                val album = withContext(Dispatchers.IO) { repo.albumById(song.albumId) }
                val artist = withContext(Dispatchers.IO) { repo.artistByName(song.artist) }
                val embedded = withContext(Dispatchers.IO) { service?.loadArtwork(song) }

                val sources = mutableListOf<Any>()
                embedded?.let { sources.add(it) }
                album?.coverImagePath?.let { sources.add(it) }
                artist?.imagePath?.let { sources.add(it) }

                CoverCarousel.start(holder.binding.songCover, sources.distinct())
            }
        } else {
            CoverCarousel.stop(holder.binding.songCover)
            holder.binding.songCover.imageTintList =
                androidx.core.content.ContextCompat.getColorStateList(holder.itemView.context, R.color.iconTint)
            holder.binding.songCover.setImageResource(R.drawable.ic_music_note)
        }

        holder.binding.btnFavorite.setImageResource(
            if (song.isFavorite) R.drawable.ic_favorite else R.drawable.ic_favorite_border
        )

        holder.binding.root.setOnClickListener { onClick(song, position) }
        holder.binding.btnFavorite.setOnClickListener { onFavoriteClick(song) }
        holder.binding.btnMore.setOnClickListener { onMoreClick(song) }
    }

    companion object {
        val DIFF = object : DiffUtil.ItemCallback<Song>() {
            override fun areItemsTheSame(oldItem: Song, newItem: Song) = oldItem.id == newItem.id
            override fun areContentsTheSame(oldItem: Song, newItem: Song) = oldItem == newItem
        }
    }
}
