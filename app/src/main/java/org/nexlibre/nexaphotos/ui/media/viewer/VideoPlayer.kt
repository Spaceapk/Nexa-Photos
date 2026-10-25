package org.nexlibre.nexaphotos.ui.media.viewer

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import androidx.media3.common.MediaItem as ExoMediaItem
import org.nexlibre.nexaphotos.domain.model.MediaItem

/**
 * Reproducción local de un video con Media3/ExoPlayer. El reproductor se libera al salir de la
 * página y se pausa cuando el usuario pasa a otra. Sin red: solo se usa el URI de MediaStore.
 */
@androidx.annotation.OptIn(UnstableApi::class)
@Composable
fun VideoPlayerPage(
    item: MediaItem,
    isActive: Boolean,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val player = remember(item.uri) {
        ExoPlayer.Builder(context).build().apply {
            setMediaItem(ExoMediaItem.fromUri(item.uri))
            prepare()
            playWhenReady = false
        }
    }

    DisposableEffect(player) {
        onDispose { player.release() }
    }

    LaunchedEffect(isActive) {
        if (!isActive) player.pause()
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        AndroidView(
            factory = { ctx ->
                PlayerView(ctx).apply {
                    this.player = player
                    useController = true
                }
            },
            onRelease = { view -> view.player = null },
            modifier = Modifier.fillMaxSize()
        )
    }
}
