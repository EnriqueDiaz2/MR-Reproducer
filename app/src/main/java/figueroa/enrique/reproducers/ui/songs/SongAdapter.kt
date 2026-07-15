package figueroa.enrique.reproducers.ui.songs

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import figueroa.enrique.reproducers.R
import figueroa.enrique.reproducers.data.model.Song
import figueroa.enrique.reproducers.databinding.ItemSongBinding

class SongAdapter(
    private val onClick: (Song, Int) -> Unit,
    private val onFavoriteClick: (Song) -> Unit,
    private val onMoreClick: (Song) -> Unit
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

        /*Glide.with(holder.itemView)
            .load(song.filePath) // idealmente carga la portada del álbum, no el audio
            .placeholder(R.drawable.ic_music_note)
            .into(holder.binding.songCover)*/

        holder.binding.songCover.setImageResource(R.drawable.ic_music_note)

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
