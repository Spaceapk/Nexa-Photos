package org.nexlibre.nexaphotos.ui.media.gallery

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.ImageLoader
import coil.compose.AsyncImage
import java.time.LocalDate
import java.time.ZoneId
import org.nexlibre.nexaphotos.domain.model.MediaAlbum
import org.nexlibre.nexaphotos.domain.model.MediaItem
import org.nexlibre.nexaphotos.ui.theme.LocalNexaPhotosTokens

/** Entrada de la cuadrícula: cabecera de día o elemento multimedia. */
private sealed interface GridEntry {
    val key: String

    data class Header(val dayStartMillis: Long) : GridEntry {
        override val key: String get() = "day-$dayStartMillis"
    }

    data class Cell(val item: MediaItem, val position: Int) : GridEntry {
        override val key: String get() = item.uri
    }
}

private fun buildEntries(items: List<MediaItem>, zoneId: ZoneId): List<GridEntry> {
    val positions = HashMap<String, Int>(items.size)
    items.forEachIndexed { index, item -> positions.putIfAbsent(item.uri, index) }
    val entries = ArrayList<GridEntry>(items.size + 8)
    groupByDay(items, zoneId).forEach { group ->
        entries += GridEntry.Header(group.dayStartMillis)
        group.items.forEach { item ->
            entries += GridEntry.Cell(item = item, position = positions[item.uri] ?: 0)
        }
    }
    return entries
}

/**
 * Cuadrícula cronológica con cabeceras por día, carga incremental al acercarse al final y
 * selección múltiple (toque largo o "seleccionar todo").
 */
@Composable
fun MediaTimelineGrid(
    items: List<MediaItem>,
    selection: SelectionState,
    imageLoader: ImageLoader,
    onItemClick: (position: Int) -> Unit,
    onItemLongClick: (uri: String) -> Unit,
    onLoadMore: () -> Unit,
    modifier: Modifier = Modifier,
    gridState: LazyGridState = rememberLazyGridState()
) {
    val tokens = LocalNexaPhotosTokens.current
    val zoneId = remember { ZoneId.systemDefault() }
    val entries = remember(items, zoneId) { buildEntries(items, zoneId) }
    val today = remember { LocalDate.now() }

    LaunchedEffect(entries.size, gridState) {
        if (entries.isEmpty()) return@LaunchedEffect
        snapshotFlow { gridState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: -1 }
            .collect { lastVisible ->
                if (lastVisible >= entries.size - PREFETCH_DISTANCE) onLoadMore()
            }
    }

    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 112.dp),
        state = gridState,
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(tokens.gridSpacing),
        verticalArrangement = Arrangement.spacedBy(tokens.gridSpacing),
        horizontalArrangement = Arrangement.spacedBy(tokens.gridSpacing)
    ) {
        items(
            count = entries.size,
            span = { index ->
                if (entries[index] is GridEntry.Header) GridItemSpan(maxLineSpan) else GridItemSpan(1)
            },
            key = { index -> entries[index].key },
            contentType = { index -> if (entries[index] is GridEntry.Header) "header" else "cell" }
        ) { index ->
            when (val entry = entries[index]) {
                is GridEntry.Header -> DayHeader(
                    label = formatDayLabel(entry.dayStartMillis, today, zoneId)
                )
                is GridEntry.Cell -> MediaCell(
                    item = entry.item,
                    imageLoader = imageLoader,
                    selectionActive = selection.isActive,
                    selected = selection.contains(entry.item.uri),
                    onClick = {
                        if (selection.isActive) onItemLongClick(entry.item.uri) else onItemClick(entry.position)
                    },
                    onLongClick = { onItemLongClick(entry.item.uri) }
                )
            }
        }
    }
}

@Composable
private fun DayHeader(label: String) {
    Text(
        text = label,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.onBackground,
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 8.dp, top = 12.dp, bottom = 4.dp)
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun MediaCell(
    item: MediaItem,
    imageLoader: ImageLoader,
    selectionActive: Boolean,
    selected: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    val tokens = LocalNexaPhotosTokens.current
    val zoneId = remember { ZoneId.systemDefault() }
    // Descripción completa para TalkBack: la selección y el favorito se anuncian como estado,
    // no solo con color.
    val description = remember(item, selected, zoneId) {
        describeMediaItem(item = item, isSelected = selected, zoneId = zoneId)
    }

    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(tokens.thumbnailCornerRadius))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .then(
                if (selected) {
                    Modifier.border(
                        border = BorderStroke(3.dp, tokens.selectionBorderColor),
                        shape = RoundedCornerShape(tokens.thumbnailCornerRadius)
                    )
                } else {
                    Modifier
                }
            )
            .semantics(mergeDescendants = true) {
                contentDescription = description
                if (selectionActive) {
                    this.selected = selected
                    stateDescription = if (selected) "Seleccionado" else "No seleccionado"
                }
            }
            .combinedClickable(role = Role.Button, onClick = onClick, onLongClick = onLongClick)
    ) {
        AsyncImage(
            model = item.uri,
            imageLoader = imageLoader,
            // La descripción la aporta el contenedor para no duplicarla en TalkBack.
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )

        if (selectionActive) {
            Box(
                Modifier
                    .fillMaxSize()
                    .background(
                        if (selected) tokens.selectionOverlayColor else Color.Black.copy(alpha = 0.18f)
                    )
            )
            Icon(
                imageVector = if (selected) Icons.Filled.CheckCircle else Icons.Outlined.CheckCircle,
                contentDescription = null,
                tint = if (selected) MaterialTheme.colorScheme.primary else Color.White,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(6.dp)
            )
        } else if (item.isFavorite) {
            Icon(
                imageVector = Icons.Filled.Favorite,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(6.dp)
            )
        }

        if (item.isVideo) {
            VideoBadge(
                durationLabel = formatDuration(item.durationMillis),
                modifier = Modifier.align(Alignment.BottomEnd)
            )
        }
    }
}

@Composable
private fun VideoBadge(durationLabel: String, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .padding(6.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(Color.Black.copy(alpha = 0.55f))
            .padding(horizontal = 4.dp, vertical = 2.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Filled.PlayArrow,
                contentDescription = null,
                tint = Color.White
            )
            Text(
                text = durationLabel,
                style = MaterialTheme.typography.labelSmall,
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

/** Cuadrícula de álbumes/carpetas con portada y conteo. */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AlbumGrid(
    albums: List<MediaAlbum>,
    imageLoader: ImageLoader,
    onAlbumClick: (MediaAlbum) -> Unit,
    modifier: Modifier = Modifier
) {
    val tokens = LocalNexaPhotosTokens.current
    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 148.dp),
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(albums, key = { album -> album.id }) { album ->
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(tokens.cardCornerRadius))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .semantics(mergeDescendants = true) {
                        contentDescription = "${album.name}, ${album.itemCount} elementos"
                    }
                    .combinedClickable(
                        role = Role.Button,
                        onClick = { onAlbumClick(album) },
                        onLongClick = {}
                    )
            ) {
                Column {
                    AsyncImage(
                        model = album.coverUri,
                        imageLoader = imageLoader,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(1.2f)
                    )
                    Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)) {
                        Text(
                            text = album.name,
                            style = MaterialTheme.typography.titleSmall,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "${album.itemCount} elementos",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

private const val PREFETCH_DISTANCE = 12
