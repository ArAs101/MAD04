package com.example.movieappmad24.screens

import android.util.Log
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import androidx.navigation.NavController
import com.example.movieappmad24.viewmodels.MoviesViewModel
import com.example.movieappmad24.widgets.HorizontalScrollableImageView
import com.example.movieappmad24.widgets.MovieRow
import com.example.movieappmad24.widgets.SimpleTopAppBar

@Composable
fun DetailScreen(
    movieId: String?,
    navController: NavController,
    moviesViewModel: MoviesViewModel
) {

    movieId?.let {
        val movie = moviesViewModel.movies.filter { movie -> movie.id == movieId }[0]

        Scaffold(
            topBar = {
                SimpleTopAppBar(title = movie.title) {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            imageVector = Icons.Filled.ArrowBack,
                            contentDescription = "Go back"
                        )
                    }
                }
            }
        ) { innerPadding ->
            Column {
                MovieRow(
                    modifier = Modifier.padding(innerPadding),
                    movie = movie,
                    onFavoriteClick = { movieId ->
                        moviesViewModel.toggleFavoriteMovie(movieId)
                    },
                    onItemClick = {}
                )

                Text(text = "Movie Trailer")

                var lifecycle by remember {
                    mutableStateOf(Lifecycle.Event.ON_CREATE)
                }
                val context = LocalContext.current
                val lifecycleOwner = LocalLifecycleOwner.current
                var playbackPosition by remember { mutableLongStateOf(0L) }
                var playWhenReady by remember { mutableStateOf(true) }

                val exoPlayer = remember {
                    ExoPlayer.Builder(context).build()
                }

                DisposableEffect(exoPlayer) {
                    val mediaItem =
                        MediaItem.fromUri("android.resource://${context.packageName}/${movie.trailer}")
                    exoPlayer.setMediaItem(mediaItem)
                    exoPlayer.prepare()
                    exoPlayer.playWhenReady = true
                    onDispose {
                        playbackPosition = exoPlayer.currentPosition
                        playWhenReady = exoPlayer.playWhenReady
                        exoPlayer.release()
                    }
                }

                DisposableEffect(lifecycleOwner) {
                    val observer = LifecycleEventObserver { _, event ->
                        lifecycle = event
                        when (lifecycle) {
                            Lifecycle.Event.ON_RESUME -> {
                                Log.e("RESUMEExoPlayer", "Resume executed")
                                exoPlayer.playWhenReady = playWhenReady
                                exoPlayer.seekTo(playbackPosition)
                            }

                            Lifecycle.Event.ON_PAUSE -> {
                                Log.e("PAUSEExoPlayer", "Pause executed")
                                playbackPosition = exoPlayer.currentPosition
                                exoPlayer.pause()
                            }

                            Lifecycle.Event.ON_STOP -> {
                                Log.e("STOPExoPlayer", "Stop/Pause executed")
                                exoPlayer.pause()
                            }

                            else -> Unit
                        }
                    }
                    lifecycleOwner.lifecycle.addObserver(observer)

                    onDispose {
                        exoPlayer.release()
                        lifecycleOwner.lifecycle.removeObserver(observer)
                    }
                }
                AndroidView(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(16f / 9f),
                    factory = {
                        PlayerView(context).also { playerView ->
                            playerView.player = exoPlayer
                        }
                    }
                )

                HorizontalScrollableImageView(movie = movie)
            }
        }
    }
}