package com.semba.audioplayer

import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.datasource.DefaultDataSourceFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.ProgressiveMediaSource
import androidx.media3.ui.PlayerView
import com.semba.audioplayer.ui.theme.AudioPlayerTheme

class MainActivity : ComponentActivity() {

    val viewModel: MediaViewModel = MediaViewModel()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            AudioPlayerTheme {
                // A surface container using the 'background' color from the theme
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colors.background) {
                    AudioPlayer(viewModel)
                }
            }
        }
    }
}

@Composable
fun AudioPlayer(mediaViewModel: MediaViewModel) {
    Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {

        // Fetching the Local Context
        val mContext = LocalContext.current

        // Declaring ExoPlayer
        mediaViewModel.preparePlayer(mContext)
        val mExoPlayer = remember(mediaViewModel.currentPlayer) {
            ExoPlayer.Builder(mContext).build().apply {
                prepare()
            }
        }

        // Implementing ExoPlayer
        AndroidView(modifier = Modifier.fillMaxSize(), factory = { context ->
            PlayerView(context).apply {
                player = mExoPlayer
                useController = true
                controllerHideOnTouch = false
            }
        })
    }
}
