package figueroa.enrique.reproducers.service

import android.app.*
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Binder
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.IBinder
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import figueroa.enrique.reproducers.R
import figueroa.enrique.reproducers.data.model.Song
import android.app.PendingIntent
import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import android.widget.RemoteViews
import figueroa.enrique.reproducers.ui.main.MainActivity

class MusicService : Service() {

    private lateinit var player: ExoPlayer
    private val binder = MusicBinder()
    private val notificationHandler = Handler(Looper.getMainLooper())
    private var cachedArtworkPath: String? = null
    private var cachedArtwork: Bitmap? = null
    private var onCurrentSongChanged: ((Song) -> Unit)? = null
    private val notificationUpdater = object : Runnable {
        override fun run() {
            val song = currentSong() ?: return
            showNotification(song)
            if (player.isPlaying) {
                notificationHandler.postDelayed(this, 1000)
            }
        }
    }

    var currentPlaylist: MutableList<Song> = mutableListOf()
    var currentIndex: Int = 0
    var isShuffle = false
    var repeatMode = REPEAT_OFF // REPEAT_OFF, REPEAT_ONE, REPEAT_ALL

    companion object {
        const val CHANNEL_ID = "music_channel_v2"
        const val NOTIF_ID = 1
        const val REPEAT_OFF = 0
        const val REPEAT_ONE = 1
        const val REPEAT_ALL = 2
        const val ACTION_PLAY_PAUSE = "action_play_pause"
        const val ACTION_NEXT = "action_next"
        const val ACTION_PREVIOUS = "action_previous"
        const val ACTION_SEEK_FORWARD = "action_seek_forward"
        const val ACTION_SEEK_BACKWARD = "action_seek_backward"
    }

    /*companion object {
        const val CHANNEL_ID = "music_channel"
        const val NOTIF_ID = 1
        const val REPEAT_OFF = 0
        const val REPEAT_ONE = 1
        const val REPEAT_ALL = 2
    }*/

    inner class MusicBinder : Binder() {
        fun getService(): MusicService = this@MusicService
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate() {
        super.onCreate()
        player = ExoPlayer.Builder(this).build()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            createNotificationChannel()
        }

        player.addListener(object : androidx.media3.common.Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                currentSong()?.let { showNotification(it) }
                scheduleNotificationUpdates()
            }

            override fun onPlaybackStateChanged(state: Int) {
                if (state == androidx.media3.common.Player.STATE_ENDED) {
                    onSongFinished()
                }
            }
        })
    }

    fun isReady(): Boolean = player.playbackState == androidx.media3.common.Player.STATE_READY

    override fun onBind(intent: Intent?): IBinder = binder

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_PLAY_PAUSE -> {
                togglePlayPause()
                currentPlaylist.getOrNull(currentIndex)?.let { showNotification(it) }
            }
            ACTION_NEXT -> next()
            ACTION_PREVIOUS -> previous()
            ACTION_SEEK_FORWARD -> currentSong()?.let {
                seekBy(10_000L)
            }
            ACTION_SEEK_BACKWARD -> currentSong()?.let {
                seekBy(-10_000L)
            }
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
            showNotification(song)
            notifyCurrentSongChanged()
        } catch (e: Exception) {
            android.util.Log.e("MusicService", "Error reproduciendo: ${e.message}", e)
        }
    }

    /*fun playSong(song: Song, playlist: List<Song> = listOf(song), index: Int = 0) {
            currentSong()?.let { showNotification(it) }
            scheduleNotificationUpdates()
        currentPlaylist = playlist
        currentIndex = index
        val mediaItem = MediaItem.fromUri(song.filePath)
        player.setMediaItem(mediaItem)
        player.prepare()
        player.play()
        showNotification(song)
    }*/

    fun togglePlayPause() {
        if (player.isPlaying) player.pause() else player.play()
    }

    fun next() {
        if (currentPlaylist.isEmpty()) return
        currentIndex = if (isShuffle) {
            currentPlaylist.indices.random()
        } else {
            (currentIndex + 1) % currentPlaylist.size
        }
        playSong(currentPlaylist[currentIndex], currentPlaylist, currentIndex)
    }

    fun previous() {
        if (currentPlaylist.isEmpty()) return
        currentIndex = if (isShuffle) {
            currentPlaylist.indices.random()
        } else {
            if (currentIndex - 1 < 0) currentPlaylist.size - 1 else currentIndex - 1
        }
        playSong(currentPlaylist[currentIndex], currentPlaylist, currentIndex)
    }

    fun updateCurrentSong(song: Song) {
        if (currentIndex !in currentPlaylist.indices) return
        currentPlaylist[currentIndex] = song
        showNotification(song)
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

    private fun seekBy(deltaMs: Long) {
        val duration = player.duration.takeIf { it > 0 } ?: Long.MAX_VALUE
        val target = (player.currentPosition + deltaMs).coerceIn(0L, duration)
        player.seekTo(target)
        currentSong()?.let { showNotification(it) }
        scheduleNotificationUpdates()
    }

    fun seekTo(position: Long) = player.seekTo(position)
    fun getCurrentPosition(): Long = player.currentPosition
    fun getDuration(): Long = player.duration
    fun isPlaying(): Boolean = player.isPlaying
    fun getExoPlayer(): ExoPlayer = player

    fun currentSong(): Song? = currentPlaylist.getOrNull(currentIndex)

    fun loadArtwork(song: Song): Bitmap? {
        if (cachedArtworkPath == song.filePath) {
            return cachedArtwork
        }

        val retriever = MediaMetadataRetriever()
        val artwork = try {
            val uri = Uri.parse(song.filePath)
            if (uri.scheme == "content" || uri.scheme == "file") {
                retriever.setDataSource(this, uri)
            } else {
                retriever.setDataSource(song.filePath)
            }
            retriever.embeddedPicture?.let { BitmapFactory.decodeByteArray(it, 0, it.size) }
        } catch (_: Exception) {
            null
        } finally {
            try {
                retriever.release()
            } catch (_: Exception) {
            }
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
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(channel)
    }

    @OptIn(UnstableApi::class)
    private fun showNotification(song: Song) {
        val openAppIntent = PendingIntent.getActivity(
            this,
            5,
            Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val prevIntent = PendingIntent.getService(
            this, 0, Intent(this, MusicService::class.java).setAction(ACTION_PREVIOUS),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val rewindIntent = PendingIntent.getService(
            this, 3, Intent(this, MusicService::class.java).setAction(ACTION_SEEK_BACKWARD),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val playPauseIntent = PendingIntent.getService(
            this, 1, Intent(this, MusicService::class.java).setAction(ACTION_PLAY_PAUSE),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val forwardIntent = PendingIntent.getService(
            this, 4, Intent(this, MusicService::class.java).setAction(ACTION_SEEK_FORWARD),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val nextIntent = PendingIntent.getService(
            this, 2, Intent(this, MusicService::class.java).setAction(ACTION_NEXT),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val playPauseIcon = if (player.isPlaying) R.drawable.ic_pause else R.drawable.ic_play
        val duration = player.duration.takeIf { it > 0 } ?: 0L
        val position = player.currentPosition.coerceIn(0L, duration)
        val artwork = loadArtwork(song)
        val progressText = if (duration > 0) getString(R.string.song_progress, formatTime(position), formatTime(duration)) else null
        val notificationViews = RemoteViews(packageName, R.layout.notification_music).apply {
            setTextViewText(R.id.notificationTitle, song.title)
            setTextViewText(R.id.notificationArtist, song.artist)
            setTextViewText(R.id.notificationProgressText, progressText ?: formatTime(position))
            setProgressBar(R.id.notificationProgressBar, duration.toInt().coerceAtLeast(1), position.toInt(), duration <= 0)
            if (artwork != null) {
                setImageViewBitmap(R.id.notificationCover, artwork)
            } else {
                setImageViewResource(R.id.notificationCover, R.drawable.ic_music_note)
            }
            setOnClickPendingIntent(R.id.notificationRoot, openAppIntent)
            setOnClickPendingIntent(R.id.notificationPrev, prevIntent)
            setOnClickPendingIntent(R.id.notificationPlayPause, playPauseIntent)
            setOnClickPendingIntent(R.id.notificationNext, nextIntent)
        }

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(song.title)
            .setContentText(song.artist)
            .setSubText(progressText)
            .setSmallIcon(R.drawable.ic_music_note)
            .setLargeIcon(artwork)
            .setContentIntent(openAppIntent)
            .setDeleteIntent(null)
            .setOngoing(player.isPlaying)
            .setOnlyAlertOnce(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_TRANSPORT)
            .setShowWhen(true)
            .setUsesChronometer(player.isPlaying)
            .setWhen(System.currentTimeMillis() - position)
            .setProgress(duration.toInt().coerceAtLeast(1), position.toInt(), duration <= 0)
            .setCustomContentView(notificationViews)
            .setCustomBigContentView(notificationViews)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setAutoCancel(false)
            .addAction(android.R.drawable.ic_media_rew, getString(R.string.rewind_10_seconds), rewindIntent)
            .addAction(R.drawable.ic_previous, getString(R.string.previous_track), prevIntent)
            .addAction(playPauseIcon, getString(R.string.play_pause), playPauseIntent)
            .addAction(android.R.drawable.ic_media_ff, getString(R.string.forward_10_seconds), forwardIntent)
            .addAction(R.drawable.ic_next, getString(R.string.next_track), nextIntent)
            .build()

        startForeground(NOTIF_ID, notification)
        scheduleNotificationUpdates()
    }

    private fun scheduleNotificationUpdates() {
        notificationHandler.removeCallbacks(notificationUpdater)
        if (player.isPlaying && currentSong() != null) {
            notificationHandler.postDelayed(notificationUpdater, 1000)
        }
    }

    private fun formatTime(position: Long): String {
        val totalSeconds = position / 1000
        val minutes = totalSeconds / 60
        val seconds = totalSeconds % 60
        return String.format(java.util.Locale.getDefault(), "%d:%02d", minutes, seconds)
    }

    /*private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID, "Reproducción de música", NotificationManager.IMPORTANCE_LOW
        )
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(channel)
    }

    private fun showNotification(song: Song) {
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(song.title)
            .setContentText(song.artist)
            .setSmallIcon(R.drawable.ic_music_note)
            .setOngoing(true)
            .build()
        startForeground(NOTIF_ID, notification)
    }*/

    override fun onDestroy() {
        notificationHandler.removeCallbacksAndMessages(null)
        player.release()
        super.onDestroy()
    }
}
