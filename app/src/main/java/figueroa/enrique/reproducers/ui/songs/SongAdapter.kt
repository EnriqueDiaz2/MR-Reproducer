package figueroa.enrique.reproducers.ui.songs

import android.content.Context
import android.graphics.drawable.Drawable
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import figueroa.enrique.reproducers.R
import figueroa.enrique.reproducers.data.model.Song
import figueroa.enrique.reproducers.databinding.ItemSongBinding
import figueroa.enrique.reproducers.service.MusicService
import figueroa.enrique.reproducers.util.AppPreferences
import figueroa.enrique.reproducers.util.ArtworkResolver
import figueroa.enrique.reproducers.util.CoverCarousel
import android.util.Log
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
        val context = holder.itemView.context
        holder.binding.songTitle.text = song.title
        holder.binding.songArtist.text = song.artist

        val service = musicService()
        val isCurrentlyPlaying = service?.currentSong()?.id == song.id

        // Resaltado de la fila que está sonando
        holder.binding.root.background = if (isCurrentlyPlaying) {
            ContextCompat.getDrawable(context, R.drawable.bg_playing_row)
        } else {
            defaultRowBackground(context)
        }

        if (isCurrentlyPlaying) {
            CoroutineScope(Dispatchers.Main).launch {
                val sources = withContext(Dispatchers.IO) { ArtworkResolver.sourcesFor(context, song, service) }
                if (AppPreferences.isAppCarouselEnabled(context) && sources.size > 1) {
                    CoverCarousel.start(holder.binding.songCover, sources)
                } else {
                    CoverCarousel.applyStatic(holder.binding.songCover, sources)
                }

                Log.d("CarouselDebug", "Canción=${song.title} fuentes=${sources.size} carouselOn=${AppPreferences.isAppCarouselEnabled(context)}")
            }
        } else {
            CoverCarousel.stop(holder.binding.songCover)
            holder.binding.songCover.tag = null
            holder.binding.songCover.imageTintList =
                ContextCompat.getColorStateList(context, R.color.iconTint)
            holder.binding.songCover.setImageResource(R.drawable.ic_music_note)
        }

        holder.binding.btnFavorite.setImageResource(
            if (song.isFavorite) R.drawable.ic_favorite else R.drawable.ic_favorite_border
        )

        holder.binding.root.setOnClickListener { onClick(song, position) }
        holder.binding.btnFavorite.setOnClickListener { onFavoriteClick(song) }
        holder.binding.btnMore.setOnClickListener { onMoreClick(song) }
    }

    override fun onViewRecycled(holder: SongViewHolder) {
        super.onViewRecycled(holder)
        CoverCarousel.stop(holder.binding.songCover)
    }

    private fun defaultRowBackground(context: Context): Drawable? {
        val typedValue = TypedValue()
        context.theme.resolveAttribute(android.R.attr.selectableItemBackground, typedValue, true)
        return ContextCompat.getDrawable(context, typedValue.resourceId)
    }

    companion object {
        val DIFF = object : DiffUtil.ItemCallback<Song>() {
            override fun areItemsTheSame(oldItem: Song, newItem: Song) = oldItem.id == newItem.id
            override fun areContentsTheSame(oldItem: Song, newItem: Song) = oldItem == newItem
        }
    }
}
