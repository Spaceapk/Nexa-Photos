package org.nexlibre.nexaphotos.ui.media.gallery

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.ImageLoader
import org.nexlibre.nexaphotos.data.permissions.MediaAccess
import org.nexlibre.nexaphotos.domain.model.MediaAlbum
import org.nexlibre.nexaphotos.domain.model.MediaItem
import org.nexlibre.nexaphotos.ui.theme.LocalNexaPhotosTokens

/**
 * Pantalla principal de la galería. Ambos estilos (Google Expressive e iOS Classic) comparten
 * esta implementación; solo cambian los tokens del tema.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GalleryScreen(
    state: GalleryUiState,
    imageLoader: ImageLoader,
    onRequestPermission: () -> Unit,
    onSectionChange: (GallerySection) -> Unit,
    onOpenAlbum: (MediaAlbum) -> Unit,
    onCloseAlbum: () -> Unit,
    onQueryChange: (String) -> Unit,
    onOpenItem: (Int) -> Unit,
    onToggleSelection: (String) -> Unit,
    onSelectAll: () -> Unit,
    onClearSelection: () -> Unit,
    onToggleFavorite: (MediaItem) -> Unit,
    onLoadMore: () -> Unit,
    onShareSelection: () -> Unit,
    onOpenAppearance: () -> Unit,
    modifier: Modifier = Modifier
) {
    val tokens = LocalNexaPhotosTokens.current
    val snackbarHostState = remember { SnackbarHostState() }
    val selectedItems = remember(state.selection, state.displayItems) {
        state.displayItems.filter { state.selection.contains(it.uri) }
    }

    LaunchedEffect(state.error) {
        val error = state.error
        if (error != null) snackbarHostState.showSnackbar(error)
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = state.album?.name ?: when (state.section) {
                            GallerySection.TIMELINE -> "Fotos y videos"
                            GallerySection.ALBUMS -> "Álbumes"
                            GallerySection.FAVORITES -> "Favoritos"
                        }
                    )
                },
                navigationIcon = {
                    if (state.album != null) {
                        IconButton(onClick = onCloseAlbum) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver a álbumes")
                        }
                    }
                },
                actions = {
                    if (state.selection.isActive) {
                        IconButton(onClick = onSelectAll) {
                            Icon(Icons.Filled.SelectAll, contentDescription = "Seleccionar todo")
                        }
                        IconButton(onClick = onClearSelection) {
                            Icon(Icons.Filled.Close, contentDescription = "Cancelar selección")
                        }
                    } else {
                        IconButton(onClick = onOpenAppearance) {
                            Icon(Icons.Filled.Palette, contentDescription = "Apariencia")
                        }
                    }
                }
            )
        },
        bottomBar = {
            if (state.selection.isActive) {
                Surface(tonalElevation = 3.dp) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 56.dp)
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (state.selection.count == 1) "1 seleccionado" else "${state.selection.count} seleccionados",
                            style = MaterialTheme.typography.titleSmall,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(Modifier.width(8.dp))
                        if (selectedItems.size == 1) {
                            val item = selectedItems.first()
                            IconButton(onClick = { onToggleFavorite(item) }) {
                                Icon(
                                    imageVector = if (item.isFavorite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                                    contentDescription = if (item.isFavorite) "Quitar de favoritos" else "Añadir a favoritos"
                                )
                            }
                        }
                        FilledTonalButton(onClick = onShareSelection) {
                            Icon(Icons.Filled.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Compartir")
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
        ) {
            when {
                state.needsPermission -> PermissionContent(
                    access = state.access,
                    onRequestPermission = onRequestPermission
                )

                state.showAlbums -> AlbumGrid(
                    albums = state.albums,
                    imageLoader = imageLoader,
                    onAlbumClick = onOpenAlbum
                )

                else -> {
                    SectionChips(
                        section = state.section,
                        onSectionChange = onSectionChange
                    )
                    OutlinedTextField(
                        value = state.query,
                        onValueChange = onQueryChange,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        singleLine = true,
                        shape = RoundedCornerShape(tokens.cardCornerRadius / 2),
                        placeholder = { Text("Buscar por nombre o carpeta") },
                        leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                        trailingIcon = {
                            if (state.query.isNotEmpty()) {
                                IconButton(onClick = { onQueryChange("") }) {
                                    Icon(Icons.Filled.Close, contentDescription = "Limpiar búsqueda")
                                }
                            }
                        }
                    )

                    if (state.hasPartialAccess) {
                        PartialAccessBanner(onRequestPermission = onRequestPermission)
                    }

                    Box(modifier = Modifier.fillMaxSize()) {
                        when {
                            state.isLoading && state.displayItems.isEmpty() -> {
                                Column(
                                    modifier = Modifier
                                        .align(Alignment.Center)
                                        .semantics { contentDescription = "Cargando biblioteca" },
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    CircularProgressIndicator()
                                    Text("Cargando…", style = MaterialTheme.typography.bodyMedium)
                                }
                            }

                            state.displayItems.isEmpty() -> EmptyState(
                                isSearch = state.isSearchActive,
                                section = state.section,
                                modifier = Modifier.align(Alignment.Center)
                            )

                            else -> MediaTimelineGrid(
                                items = state.displayItems,
                                selection = state.selection,
                                imageLoader = imageLoader,
                                onItemClick = onOpenItem,
                                onItemLongClick = onToggleSelection,
                                onLoadMore = onLoadMore
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionChips(
    section: GallerySection,
    onSectionChange: (GallerySection) -> Unit
) {
    // Desplazable en horizontal: con texto al 200 % las etiquetas no se recortan ni se apilan.
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 12.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        GallerySection.entries.forEach { entry ->
            FilterChip(
                selected = section == entry,
                onClick = { onSectionChange(entry) },
                label = {
                    Text(
                        when (entry) {
                            GallerySection.TIMELINE -> "Fotos y videos"
                            GallerySection.ALBUMS -> "Álbumes"
                            GallerySection.FAVORITES -> "Favoritos"
                        }
                    )
                }
            )
        }
    }
}

@Composable
private fun PermissionContent(
    access: MediaAccess,
    onRequestPermission: () -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Icon(
                imageVector = Icons.Filled.Info,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
        }
        item {
            Text(
                text = when (access) {
                    MediaAccess.NONE -> "Nexa Photos necesita acceso a tus fotos y videos"
                    MediaAccess.PARTIAL, MediaAccess.FULL -> "Sin acceso a la biblioteca"
                },
                style = MaterialTheme.typography.titleLarge
            )
        }
        item {
            Text(
                text = "La app solo lee la biblioteca local para mostrarla. Nunca borra, mueve ni " +
                    "modifica tus archivos, y no tiene acceso a internet.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        item {
            Button(onClick = onRequestPermission) {
                Text(if (access == MediaAccess.PARTIAL) "Elegir más fotos y videos" else "Permitir acceso")
            }
        }
        item {
            Text(
                text = "En Android 14 o superior puedes conceder acceso solo a elementos " +
                    "seleccionados; puedes ampliar la selección cuando quieras.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun PartialAccessBanner(onRequestPermission: () -> Unit) {
    Surface(
        color = MaterialTheme.colorScheme.secondaryContainer,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 4.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Acceso parcial: solo ves los elementos que elegiste.",
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.weight(1f)
            )
            TextButton(onClick = onRequestPermission) {
                Text("Ampliar")
            }
        }
    }
}

@Composable
private fun EmptyState(
    isSearch: Boolean,
    section: GallerySection,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = when {
                    isSearch -> "Sin resultados"
                    section == GallerySection.FAVORITES -> "Todavía no hay favoritos"
                    else -> "No hay fotos ni videos"
                },
                style = MaterialTheme.typography.titleMedium,
                textAlign = TextAlign.Center
            )
            Text(
                text = when {
                    isSearch -> "Prueba con otro nombre o carpeta."
                    section == GallerySection.FAVORITES -> "Mantén pulsado un elemento y usa el corazón para guardarlo aquí."
                    else -> "Cuando haya fotos o videos en el dispositivo aparecerán aquí."
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}
