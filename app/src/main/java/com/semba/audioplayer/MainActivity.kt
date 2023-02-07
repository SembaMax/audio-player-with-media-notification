package com.semba.audioplayer

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import coil.compose.AsyncImage
import com.semba.audioplayer.data.ControlButtons
import com.semba.audioplayer.data.TrackItem
import com.semba.audioplayer.ui.theme.AudioPlayerTheme
import com.semba.audioplayer.ui.theme.*
import com.semba.audioplayer.util.formatMinSec
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.seconds


@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            AudioPlayerTheme {
                // A surface container using the 'background' color from the theme
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {

                    val viewModel = hiltViewModel<MediaViewModel>()

                    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
                    val currentTrackState by viewModel.currentPlayingIndex.collectAsStateWithLifecycle()
                    val isPlayingState by viewModel.isPlaying.collectAsStateWithLifecycle()
                    val totalDurationState by viewModel.totalDurationInMS.collectAsStateWithLifecycle()
                    var currentPositionState by remember { mutableStateOf(0L) }

                    LaunchedEffect(isPlayingState) {
                        while(isPlayingState) {
                            currentPositionState = viewModel.player.currentPosition
                            delay(1.seconds)
                        }
                    }

                    when (uiState) {
                        PlayerUIState.Loading -> {

                        }
                        is PlayerUIState.Tracks -> {
                            Column(modifier = Modifier.fillMaxSize()) {
                                AudioPlayerView(viewModel)
                                PlayerControlsView(currentTrackImage = (uiState as PlayerUIState.Tracks).items[currentTrackState].teaserUrl,
                                    totalDuration = totalDurationState,
                                    currentPosition = currentPositionState,
                                    isPlaying = isPlayingState,
                                    navigateTrack =  {action -> viewModel.updatePlaylist(action) },
                                    seekPosition = {position -> viewModel.updatePlayerPosition((position * 1000).toLong())}
                                )
                                PlaylistView((uiState as PlayerUIState.Tracks).items, currentTrackState)
                            }
                        }
                    }
                }
            }
        }
    }
}

@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
@Composable
fun AudioPlayerView(viewModel: MediaViewModel) {
        // Fetching the Local Context
        val mContext = LocalContext.current

        // Declaring ExoPlayer
        val mExoPlayer = remember(viewModel.player) {
            ExoPlayer.Builder(mContext).build().apply {
                viewModel.preparePlayer(context = mContext)
            }
        }

        // Implementing ExoPlayer
    DisposableEffect(
        AndroidView(modifier = Modifier.size(0.dp), factory = { context ->
            PlayerView(context).apply {
                this.player = mExoPlayer
                hideController()
                useController = false
                controllerHideOnTouch = false
            }
        })
    ) {
        onDispose { viewModel.onDestroy() }
    }
}

@Composable
fun PlayerControlsView(currentTrackImage: String, totalDuration: Long, currentPosition: Long, isPlaying: Boolean, navigateTrack: (ControlButtons) -> Unit, seekPosition: (Float) -> Unit) {

    Column(
        Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {

        Spacer(modifier = Modifier.height(40.dp))
        AsyncImage(
            modifier = Modifier
                .size(200.dp)
                .clip(CircleShape),
            model = currentTrackImage,
            contentDescription = "player_image"
        )

        Slider(modifier = Modifier
            .fillMaxWidth()
            .padding(top = 10.dp, start = 30.dp, end = 30.dp),
            value = (currentPosition / 1000).toFloat(),
            valueRange = 0f..(totalDuration / 1000).toFloat(),
            onValueChange = { seekPosition(it) },
            colors =
            SliderDefaults.colors(
                thumbColor = MaterialTheme.colorScheme.tertiary,
                activeTickColor = MaterialTheme.colorScheme.onBackground,
                activeTrackColor = MaterialTheme.colorScheme.tertiary
            ))

        Row(modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 30.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically) {
            Text(text = currentPosition.formatMinSec(), fontSize = 12.sp, color = MaterialTheme.colorScheme.onBackground)
            Text(text = totalDuration.formatMinSec(), fontSize = 12.sp, color = MaterialTheme.colorScheme.onBackground)
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 30.dp, end = 30.dp, top = 5.dp, bottom = 30.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(modifier = Modifier
                .size(45.dp),
            onClick = {navigateTrack(ControlButtons.Rewind)}) {
                Icon(imageVector = ImageVector.vectorResource(id = R.drawable.ic_rewind), contentDescription = "player_rewind"
                    ,tint = MaterialTheme.colorScheme.primary)
            }
            Spacer(modifier = Modifier.width(30.dp))
            IconButton(modifier = Modifier
                .size(70.dp),
            onClick = {navigateTrack(ControlButtons.Play)}) {
                Icon(imageVector = ImageVector.vectorResource(id = if (isPlaying) R.drawable.ic_pause else R.drawable.ic_play),
                    contentDescription = "player_play",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .size(70.dp)
                )
            }
            Spacer(modifier = Modifier.width(30.dp))
            IconButton(modifier = Modifier
                .size(45.dp),
            onClick = {navigateTrack(ControlButtons.Next)}) {
                Icon(imageVector = ImageVector.vectorResource(id = R.drawable.ic_next),
                    contentDescription = "player_next",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
fun PlaylistView(tracks : List<TrackItem>, currentTrack: Int) {
    LazyColumn {
        items(tracks.size) {
            PlaylistItemView(tracks[it], currentTrack == it)
        }
    }
}

@Composable
fun PlaylistItemView(trackItem: TrackItem, isPlaying: Boolean) {
    Row(
        Modifier
            .fillMaxWidth()
            .background(if (isPlaying) MaterialTheme.colorScheme.tertiary else Color.Transparent),
        horizontalArrangement = Arrangement.SpaceAround,
        verticalAlignment = Alignment.CenterVertically
    ) {


        TrackImageView(imageUrl = trackItem.teaserUrl)

        Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.Start, verticalArrangement = Arrangement.SpaceAround) {
            Text(text = trackItem.title, style = MaterialTheme.typography.titleMedium, color = if (isPlaying) MaterialTheme.colorScheme.onTertiary else MaterialTheme.colorScheme.onBackground)
            Text(text = trackItem.artistName, style = MaterialTheme.typography.titleSmall, color = if (isPlaying) MaterialTheme.colorScheme.onTertiary else MaterialTheme.colorScheme.onBackground)
        }

        Text(text = trackItem.duration, Modifier.padding(horizontal = 10.dp), style = MaterialTheme.typography.titleSmall, color = if (isPlaying) MaterialTheme.colorScheme.onTertiary else MaterialTheme.colorScheme.onBackground)
    }
}


@Composable
fun TrackImageView(size: Dp = 75.dp, imageUrl: String) {
    AsyncImage(
        modifier = Modifier
            .size(size)
            .padding(horizontal = 10.dp),
        model = imageUrl,
        contentDescription = null
    )
}
