package com.semba.audioplayer

import android.app.Notification
import android.app.PendingIntent
import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.*
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.MediaSource
import androidx.media3.exoplayer.source.ProgressiveMediaSource
import androidx.media3.session.MediaSession
import androidx.media3.ui.PlayerNotificationManager
import com.semba.audioplayer.data.TrackItem
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.*
import javax.inject.Inject

@HiltViewModel
class MediaViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    val player: ExoPlayer
): ViewModel() {

    companion object {
        const val SESSION_INTENT_REQUEST_CODE = 0
    }

    private val playlist = arrayListOf(
        TrackItem("1", "", "", "", "", ""),
        TrackItem("2", "", "", "", "", ""),
        TrackItem("3", "", "", "", "", ""),
        TrackItem("4", "", "", "", "", ""),
        TrackItem("5", "", "", "", "", ""),
    )

    val uiState: StateFlow<PlayerUIState> = MutableStateFlow(PlayerUIState.Tracks(playlist)).stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        initialValue = PlayerUIState.Loading
    )

    private lateinit var notificationManager: MediaNotificationManager

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

        currentPlayer = ExoPlayer.Builder(context).build()
        player.setAudioAttributes(audioAttributes, true)
        player.repeatMode = Player.REPEAT_MODE_OFF

        player.addListener(playerListener)

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


sealed interface PlayerUIState {
    data class Tracks(val items: List<TrackItem>): PlayerUIState
    object Loading: PlayerUIState
}