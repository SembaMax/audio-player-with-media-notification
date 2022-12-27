package com.semba.audioplayer

import android.app.Notification
import android.app.PendingIntent
import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.media3.common.*
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.MediaSource
import androidx.media3.exoplayer.source.ProgressiveMediaSource
import androidx.media3.session.MediaSession
import androidx.media3.ui.PlayerNotificationManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

class MediaViewModel {

    companion object {
        const val SESSION_INTENT_REQUEST_CODE = 0
    }

    val playlist = arrayListOf(
        MediaTrack("1", "", "", ""),
        MediaTrack("2", "", "", ""),
        MediaTrack("3", "", "", "")
    )

    private lateinit var notificationManager: MediaNotificationManager

    // The current player will either be an ExoPlayer (for local playback)
    lateinit var currentPlayer: ExoPlayer
    protected lateinit var mediaSession: MediaSession
    private val serviceJob = SupervisorJob()
    private val serviceScope = CoroutineScope(Dispatchers.Main + serviceJob)

    private lateinit var storage: PersistentStorage

    private var isStarted = false

    fun preparePlayer(context: Context)
    {
        val audioAttributes = AudioAttributes.Builder()
            .setUsage(C.USAGE_MEDIA)
            .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
            .build()

        currentPlayer = ExoPlayer.Builder(context).build();
        currentPlayer.setAudioAttributes(audioAttributes, true)
        currentPlayer.repeatMode = Player.REPEAT_MODE_OFF

        currentPlayer.addListener(playerListener)

        setupPlaylist(context)
    }

    private fun setupPlaylist(context: Context) {

        val videoItems: ArrayList<MediaSource> = arrayListOf()
        playlist.forEach {

            val mediaMetaData = MediaMetadata.Builder()
                .setArtworkUri(Uri.parse(it.teaserUrl))
                .setTitle(it.title)
                .build()

            val trackUri = Uri.parse(it.audioUrl)
            val mediaItem = MediaItem.Builder()
                .setUri(trackUri)
                .setMediaId(it.id)
                .setMediaMetadata(mediaMetaData)
                .build()
            val dataSourceFactory = DefaultDataSource.Factory(context)

            val mediaSource = ProgressiveMediaSource.Factory(dataSourceFactory).createMediaSource(mediaItem)

            videoItems.add(
                mediaSource
            )
        }

        onStart(context)

        currentPlayer.playWhenReady = true
        currentPlayer.setMediaSources(videoItems)
        currentPlayer.prepare()
    }

    fun onStart(context: Context) {
        if (isStarted) return

        isStarted = true

        // Build a PendingIntent that can be used to launch the UI.
        val sessionActivityPendingIntent =
            context.packageManager?.getLaunchIntentForPackage(context.packageName)?.let { sessionIntent ->
                PendingIntent.getActivity(context, SESSION_INTENT_REQUEST_CODE, sessionIntent, PendingIntent.FLAG_UPDATE_CURRENT)
            }

        // Create a new MediaSession.
        mediaSession = MediaSession.Builder(context, currentPlayer)
            .setSessionActivity(sessionActivityPendingIntent!!).build()

        /**
         * The notification manager will use our player and media session to decide when to post
         * notifications. When notifications are posted or removed our listener will be called, this
         * allows us to promote the service to foreground (required so that we're not killed if
         * the main UI is not visible).
         */
        notificationManager =
            MediaNotificationManager(
                context,
                mediaSession.token,
                currentPlayer,
                PlayerNotificationListener()
            )


        notificationManager.showNotificationForPlayer(currentPlayer)

        storage =
            PersistentStorage.getInstance(
                context
            )
    }

    /**
     * Destroy audio notification
     */
    fun onDestroy() {
        onClose()
        if (this::currentPlayer.isInitialized)
            currentPlayer.release()
    }

    /**
     * Close audio notification
     */
    fun onClose()
    {
        if (!isStarted) return

        isStarted = false
        mediaSession.run {
            release()
        }

        // Hide notification
        notificationManager.hideNotification()

        // Free ExoPlayer resources.
        currentPlayer.removeListener(playerListener)
    }

    /**
     * Listen for notification events.
     */
    private inner class PlayerNotificationListener :
        PlayerNotificationManager.NotificationListener {
        override fun onNotificationPosted(
            notificationId: Int,
            notification: Notification,
            ongoing: Boolean
        ) {

        }

        override fun onNotificationCancelled(notificationId: Int, dismissedByUser: Boolean) {

        }
    }

    /**
     * Listen to events from ExoPlayer.
     */
    private val playerListener = object : Player.Listener {

        override fun onPlaybackStateChanged(playbackState: Int) {
            super.onPlaybackStateChanged(playbackState)
            when (playbackState) {
                Player.STATE_BUFFERING,
                Player.STATE_READY -> {
                    notificationManager.showNotificationForPlayer(currentPlayer)
                }
                else -> {
                    notificationManager.hideNotification()
                }
            }
        }
        override fun onPlayerError(error: PlaybackException) {
            super.onPlayerError(error)
            Log.e(TAG,"Error: ${error.message}")
        }
    }
}

private const val TAG = "MediaNotification"