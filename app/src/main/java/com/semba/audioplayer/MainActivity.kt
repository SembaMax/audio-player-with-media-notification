package com.semba.audioplayer

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import coil.compose.AsyncImage
import com.semba.audioplayer.data.TrackItem
import com.semba.audioplayer.ui.theme.AudioPlayerTheme
import com.semba.audioplayer.ui.theme.*
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject


@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var viewModel: MediaViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            AudioPlayerTheme {
                // A surface container using the 'background' color from the theme
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colors.background) {
                    Column(modifier = Modifier.fillMaxSize()) {
                        AudioPlayerView()
                        PlaylistView()
                    }
                }
            }
        }
    }
}

@Composable
fun AudioPlayerView(state :PlayerUIState, player: ExoPlayer) {
        // Fetching the Local Context
        val mContext = LocalContext.current

        // Declaring ExoPlayer
        val mExoPlayer = remember(player) {
            ExoPlayer.Builder(mContext).build().apply {
                prepare()
            }
        }

        // Implementing ExoPlayer
        AndroidView(modifier = Modifier.fillMaxSize(), factory = { context ->
            PlayerView(context).apply {
                this.player = mExoPlayer
                useController = false
                controllerHideOnTouch = false
            }
        })
}

@Composable
fun PlaylistView(tracks : PlayerUIState.Tracks) {
    LazyColumn() {
        items(tracks.items.size) {
            PlaylistItemView(tracks.items[it])
        }
    }
}

@Composable
fun PlaylistItemView(trackItem: TrackItem) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 10.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceAround,
        verticalAlignment = Alignment.CenterVertically
    ) {


        TrackImageView(imageUrl = trackItem.teaserUrl)

        Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.SpaceAround) {
            Text(text = trackItem.title, style = MaterialTheme.typography.subtitle1)
            Text(text = trackItem.artistName, style = MaterialTheme.typography.subtitle2)
        }

        Text(text = trackItem.duration, style = MaterialTheme.typography.subtitle2)
    }
}


@Composable
fun TrackImageView(size: Dp = 30.dp, imageUrl: String) {
    AsyncImage(
        modifier = Modifier.size(size),
        model = imageUrl,
        contentDescription = null
    )
}
