package org.nexlibre.nexaphotos.ui.media.viewer

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.ImageLoader
import org.nexlibre.nexaphotos.domain.model.MediaItem

/**
 * Visor a pantalla completa con paginación horizontal entre los elementos visibles de la
 * cuadrícula. Las imágenes se pueden ampliar con gestos; los videos se reproducen en su página.
 */
@Composable
fun MediaViewerScreen(
    items: List<MediaItem>,
    initialIndex: Int,
    imageLoader: ImageLoader,
    onClose: () -> Unit,
    onToggleFavorite: (MediaItem) -> Unit,
    onShare: (MediaItem) -> Unit,
    modifier: Modifier = Modifier
) {
    if (items.isEmpty()) return

    val pagerState = rememberPagerState(
        initialPage = initialIndex.coerceIn(0, items.lastIndex),
        pageCount = { items.size }
    )
    val current = items.getOrNull(pagerState.currentPage) ?: items.first()

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        HorizontalPager(
            state = pagerState,
            key = { page -> items[page].uri }
        ) { page ->
            val item = items[page]
            if (item.isVideo) {
                VideoPlayerPage(
                    item = item,
                    isActive = page == pagerState.currentPage
                )
            } else {
                ZoomableImage(item = item, imageLoader = imageLoader)
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        listOf(Color.Black.copy(alpha = 0.6f), Color.Transparent)
                    )
                )
                .statusBarsPadding()
                .align(Alignment.TopCenter)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onClose) {
                    Icon(Icons.Filled.Close, contentDescription = "Cerrar", tint = Color.White)
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = current.name,
                        style = MaterialTheme.typography.titleSmall,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "${pagerState.currentPage + 1} de ${items.size}",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White.copy(alpha = 0.75f)
                    )
                }
                IconButton(onClick = { onToggleFavorite(current) }) {
                    Icon(
                        imageVector = if (current.isFavorite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                        contentDescription = if (current.isFavorite) "Quitar de favoritos" else "Añadir a favoritos",
                        tint = Color.White
                    )
                }
                IconButton(onClick = { onShare(current) }) {
                    Icon(Icons.Filled.Share, contentDescription = "Compartir", tint = Color.White)
                }
            }
        }

        Text(
            text = if (current.isVideo) "Doble toque no aplica a video" else "Pellizca para ampliar · doble toque para 2.5x",
            style = MaterialTheme.typography.labelSmall,
            color = Color.White.copy(alpha = 0.6f),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = 16.dp)
        )
    }
}
