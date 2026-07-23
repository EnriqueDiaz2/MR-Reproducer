package figueroa.enrique.reproducers.service

import android.app.*
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Binder
import android.os.Build
import android.os.IBinder
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import figueroa.enrique.reproducers.R
import figueroa.enrique.reproducers.data.db.AppDatabase
import androidx.media3.common.Player.Listener
import figueroa.enrique.reproducers.data.model.Song
import figueroa.enrique.reproducers.data.repository.MusicRepository
import figueroa.enrique.reproducers.ui.main.MainActivity
import figueroa.enrique.reproducers.util.AppPreferences
import android.app.PendingIntent
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import kotlinx.coroutines.*

class MusicService : Service() {

    private lateinit var player: ExoPlayer
    private lateinit var mediaSession: MediaSessionCompat
    private val binder = MusicBinder()
    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val repo by lazy { MusicRepository(AppDatabase.getDatabase(this)) }

    private var cachedArtworkPath: String? = null
    private var cachedArtwork: Bitmap? = null
    private var onCurrentSongChanged: ((Song) -> Unit)? = null

    var currentPlaylist: MutableList<Song> = mutableListOf()
    var currentIndex: Int = 0
    var isShuffle = false
    var repeatMode = REPEAT_OFF

    companion object {
        const val CHANNEL_ID = "music_channel_v3"
        const val NOTIF_ID = 1
        const val REPEAT_OFF = 0
        const val REPEAT_ONE = 1
        const val REPEAT_ALL = 2
        const val ACTION_PLAY_PAUSE = "action_play_pause"
        const val ACTION_NEXT = "action_next"
        const val ACTION_PREVIOUS = "action_previous"
    }

    inner class MusicBinder : Binder() {
        fun getService(): MusicService = this@MusicService
    }

    @OptIn(UnstableApi::class)
    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate() {
        super.onCreate()
        player = ExoPlayer.Builder(this).build()
        createNotificationChannel()
        setupMediaSession()
        startForeground(NOTIF_ID, buildIdleNotification())

        player.addListener(object : Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                refreshSessionAndNotification()
            }
            override fun onPlaybackStateChanged(state: Int) {
                if (state == androidx.media3.common.Player.STATE_ENDED) onSongFinished()
                refreshSessionAndNotification()
            }
        })
    }

    private fun buildIdleNotification(): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.app_name))
            .setContentText(getString(R.string.no_song_playing))
            .setSmallIcon(R.drawable.ic_music_note)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(false)
            .build()
    }

    private fun setupMediaSession() {
        mediaSession = MediaSessionCompat(this, "MusicService").apply {
            setCallback(object : MediaSessionCompat.Callback() {
                override fun onPlay() { player.play() }
                override fun onPause() { player.pause() }
                override fun onSkipToNext() { next() }
                override fun onSkipToPrevious() { previous() }
                override fun onSeekTo(pos: Long) { player.seekTo(pos); refreshSessionAndNotification() }
            })
            val openAppIntent = PendingIntent.getActivity(
                this@MusicService, 5,
                Intent(this@MusicService, MainActivity::class.java)
                    .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            setSessionActivity(openAppIntent)
            isActive = true
        }
    }

    fun isReady(): Boolean = player.playbackState == androidx.media3.common.Player.STATE_READY

    override fun onBind(intent: Intent?): IBinder = binder

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_PLAY_PAUSE -> togglePlayPause()
            ACTION_NEXT -> next()
            ACTION_PREVIOUS -> previous()
        }
        return START_STICKY
    }

    fun playSong(song: Song, playlist: List<Song> = listOf(song), index: Int = 0) {
        try {
            currentPlaylist = playlist.toMutableList()
            currentIndex = index
            val mediaItem = MediaItem.fromUri(Uri.parse(song.filePath))
            player.setMediaItem(mediaItem)
            player.prepare()
            player.play()
            refreshSessionAndNotification()
            notifyCurrentSongChanged()
            serviceScope.launch { AppDatabase.getDatabase(this@MusicService).songDao().incrementPlayCount(song.id) }
        } catch (e: Exception) {
            android.util.Log.e("MusicService", "Error reproduciendo: ${e.message}", e)
        }
    }

    fun togglePlayPause() {
        if (player.isPlaying) player.pause() else player.play()
    }

    fun next() {
        if (currentPlaylist.isEmpty()) return
        currentIndex = if (isShuffle) pickShuffleIndex() else (currentIndex + 1) % currentPlaylist.size
        playSong(currentPlaylist[currentIndex], currentPlaylist, currentIndex)
    }

    fun previous() {
        if (currentPlaylist.isEmpty()) return
        currentIndex = if (isShuffle) pickShuffleIndex()
        else if (currentIndex - 1 < 0) currentPlaylist.size - 1 else currentIndex - 1
        playSong(currentPlaylist[currentIndex], currentPlaylist, currentIndex)
    }

    private fun pickShuffleIndex(): Int {
        if (currentPlaylist.size <= 1) return 0
        if (!AppPreferences.isAdaptiveShuffleEnabled(this)) {
            return currentPlaylist.indices.random()
        }
        val weights = currentPlaylist.mapIndexed { i, song ->
            if (i == currentIndex) 0.0 else (song.playCount + 1).toDouble()
        }
        val total = weights.sum()
        if (total <= 0.0) return currentPlaylist.indices.random()
        var r = Math.random() * total
        for (i in weights.indices) {
            r -= weights[i]
            if (r <= 0.0) return i
        }
        return currentPlaylist.indices.random()
    }

    fun updateCurrentSong(song: Song) {
        if (currentIndex !in currentPlaylist.indices) return
        currentPlaylist[currentIndex] = song
        refreshSessionAndNotification()
        notifyCurrentSongChanged()
    }

    fun setOnCurrentSongChangedListener(listener: ((Song) -> Unit)?) {
        onCurrentSongChanged = listener
        currentSong()?.let { listener?.invoke(it) }
    }

    private fun notifyCurrentSongChanged() {
        currentSong()?.let { onCurrentSongChanged?.invoke(it) }
    }

    private fun onSongFinished() {
        when (repeatMode) {
            REPEAT_ONE -> playSong(currentPlaylist[currentIndex], currentPlaylist, currentIndex)
            REPEAT_ALL -> next()
            REPEAT_OFF -> if (currentIndex < currentPlaylist.size - 1) next()
        }
    }

    fun seekTo(position: Long) { player.seekTo(position); refreshSessionAndNotification() }
    fun getCurrentPosition(): Long = player.currentPosition
    fun getDuration(): Long = player.duration
    fun isPlaying(): Boolean = player.isPlaying
    fun getExoPlayer(): ExoPlayer = player
    fun currentSong(): Song? = currentPlaylist.getOrNull(currentIndex)

    // Embebida en el archivo (fallback si no hay portada de álbum/artista)
    fun loadArtwork(song: Song): Bitmap? {
        if (cachedArtworkPath == song.filePath) return cachedArtwork
        val retriever = MediaMetadataRetriever()
        val artwork = try {
            val uri = Uri.parse(song.filePath)
            if (uri.scheme == "content" || uri.scheme == "file") retriever.setDataSource(this, uri)
            else retriever.setDataSource(song.filePath)
            retriever.embeddedPicture?.let { BitmapFactory.decodeByteArray(it, 0, it.size) }
        } catch (_: Exception) { null } finally {
            try { retriever.release() } catch (_: Exception) {}
        }
        cachedArtworkPath = song.filePath
        cachedArtwork = artwork
        return artwork
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID, "Reproducción de música", NotificationManager.IMPORTANCE_HIGH
        ).apply {
            setShowBadge(false)
            enableVibration(false)
            lockscreenVisibility = Notification.VISIBILITY_PUBLIC
        }
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    private fun refreshSessionAndNotification() {
        val song = currentSong() ?: return

        val state = if (player.isPlaying) PlaybackStateCompat.STATE_PLAYING else PlaybackStateCompat.STATE_PAUSED
        val actions = PlaybackStateCompat.ACTION_PLAY or PlaybackStateCompat.ACTION_PAUSE or
                PlaybackStateCompat.ACTION_PLAY_PAUSE or PlaybackStateCompat.ACTION_SKIP_TO_NEXT or
                PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS or PlaybackStateCompat.ACTION_SEEK_TO

        mediaSession.setPlaybackState(
            PlaybackStateCompat.Builder()
                .setActions(actions)
                .setState(state, player.currentPosition, player.playbackParameters.speed)
                .build()
        )

        val duration = player.duration
        mediaSession.setMetadata(
            MediaMetadataCompat.Builder()
                .putString(MediaMetadataCompat.METADATA_KEY_TITLE, song.title)
                .putString(MediaMetadataCompat.METADATA_KEY_ARTIST, song.artist)
                .apply { if (duration > 0) putLong(MediaMetadataCompat.METADATA_KEY_DURATION, duration) }
                .build()
        )

        // Prioridad de imagen: portada de álbum > embebida en el archivo > ninguna (ícono por defecto)
        serviceScope.launch {
            val albumCover = repo.albumById(song.albumId)?.coverImagePath
            val bmp = albumCover ?: loadArtwork(song)
            withContext(Dispatchers.Main) {
                val bitmap: Bitmap? = when (bmp) {
                    is Bitmap -> bmp
                    is String -> try {
                        contentResolver.openInputStream(Uri.parse(bmp))?.use { BitmapFactory.decodeStream(it) }
                    } catch (_: Exception) { null }
                    else -> null
                }
                postNotification(song, bitmap)
            }
        }
    }

    @OptIn(UnstableApi::class)
    private fun postNotification(song: Song, largeIcon: Bitmap?) {
        val prevIntent = PendingIntent.getService(
            this, 0, Intent(this, MusicService::class.java).setAction(ACTION_PREVIOUS),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val playPauseIntent = PendingIntent.getService(
            this, 1, Intent(this, MusicService::class.java).setAction(ACTION_PLAY_PAUSE),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val nextIntent = PendingIntent.getService(
            this, 2, Intent(this, MusicService::class.java).setAction(ACTION_NEXT),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val openAppIntent = PendingIntent.getActivity(
            this, 5,
            Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val playPauseIcon = if (player.isPlaying) R.drawable.ic_pause else R.drawable.ic_play

        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(song.title)
            .setContentText(song.artist)
            .setSmallIcon(R.drawable.ic_music_note)
            .setContentIntent(openAppIntent)
            .setOngoing(player.isPlaying)
            .setOnlyAlertOnce(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setCategory(NotificationCompat.CATEGORY_TRANSPORT)
            .addAction(R.drawable.ic_previous, getString(R.string.previous_track), prevIntent)
            .addAction(playPauseIcon, getString(R.string.play_pause), playPauseIntent)
            .addAction(R.drawable.ic_next, getString(R.string.next_track), nextIntent)
            .setStyle(
                androidx.media.app.NotificationCompat.MediaStyle()
                    .setMediaSession(mediaSession.sessionToken)
                    .setShowActionsInCompactView(0, 1, 2)
            )

        if (largeIcon != null) builder.setLargeIcon(largeIcon)

        startForeground(NOTIF_ID, builder.build())
    }

    override fun onDestroy() {
        mediaSession.release()
        player.release()
        serviceScope.cancel()
        super.onDestroy()
    }
}
