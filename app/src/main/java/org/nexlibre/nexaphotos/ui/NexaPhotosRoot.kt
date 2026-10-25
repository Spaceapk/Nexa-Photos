package org.nexlibre.nexaphotos.ui

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import org.nexlibre.nexaphotos.domain.model.AppearanceMode
import org.nexlibre.nexaphotos.domain.model.UiStyle
import org.nexlibre.nexaphotos.domain.model.UserSettings
import org.nexlibre.nexaphotos.ui.media.gallery.GalleryScreen
import org.nexlibre.nexaphotos.ui.media.gallery.GalleryViewModel
import org.nexlibre.nexaphotos.ui.media.gallery.MediaShare
import org.nexlibre.nexaphotos.ui.media.gallery.rememberGalleryImageLoader
import org.nexlibre.nexaphotos.ui.media.viewer.MediaViewerScreen

/**
 * Raíz de la app: conecta la galería con el visor, el flujo de permisos y el selector de
 * apariencia. La lógica de datos vive en [GalleryViewModel]; aquí solo se cablea la UI.
 */
@Composable
fun NexaPhotosRoot(
    settings: UserSettings,
    onUiStyleChange: (UiStyle) -> Unit,
    onAppearanceChange: (AppearanceMode) -> Unit,
    onDynamicColorsChange: (Boolean) -> Unit = {},
    modifier: Modifier = Modifier,
    galleryViewModel: GalleryViewModel = viewModel(factory = GalleryViewModel.Factory)
) {
    val state by galleryViewModel.state.collectAsStateWithLifecycle()
    val imageLoader = rememberGalleryImageLoader()
    val context = LocalContext.current
    var showAppearance by rememberSaveable { mutableStateOf(false) }
    var autoRequested by rememberSaveable { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { galleryViewModel.refreshPermissions() }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner, galleryViewModel) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) galleryViewModel.refreshPermissions()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LaunchedEffect(state.needsPermission) {
        if (state.needsPermission && !autoRequested) {
            autoRequested = true
            permissionLauncher.launch(galleryViewModel.requiredPermissions())
        }
    }

    BackHandler(enabled = state.viewer != null || state.album != null) {
        if (state.viewer != null) galleryViewModel.closeViewer() else galleryViewModel.closeAlbum()
    }

    Box(modifier = modifier.fillMaxSize()) {
        GalleryScreen(
            state = state,
            imageLoader = imageLoader,
            onRequestPermission = { permissionLauncher.launch(galleryViewModel.requiredPermissions()) },
            onSectionChange = galleryViewModel::setSection,
            onOpenAlbum = galleryViewModel::openAlbum,
            onCloseAlbum = galleryViewModel::closeAlbum,
            onQueryChange = galleryViewModel::setQuery,
            onOpenItem = galleryViewModel::openViewer,
            onToggleSelection = galleryViewModel::toggleSelection,
            onSelectAll = galleryViewModel::selectAllVisible,
            onClearSelection = galleryViewModel::clearSelection,
            onToggleFavorite = galleryViewModel::toggleFavorite,
            onLoadMore = galleryViewModel::loadMore,
            onShareSelection = {
                MediaShare.share(
                    context = context,
                    items = galleryViewModel.shareItems(),
                    chooserTitle = "Compartir con"
                )
            },
            onOpenAppearance = { showAppearance = true }
        )

        state.viewer?.let { viewer ->
            MediaViewerScreen(
                items = viewer.items,
                initialIndex = viewer.index,
                imageLoader = imageLoader,
                onClose = galleryViewModel::closeViewer,
                onToggleFavorite = galleryViewModel::toggleFavorite,
                onShare = { item -> MediaShare.share(context, listOf(item), chooserTitle = "Compartir con") }
            )
        }
    }

    if (showAppearance) {
        AppearanceDialog(
            settings = settings,
            onUiStyleChange = onUiStyleChange,
            onAppearanceChange = onAppearanceChange,
            onDynamicColorsChange = onDynamicColorsChange,
            onDismiss = { showAppearance = false }
        )
    }
}

/**
 * Selector de tema compartido por ambos estilos visuales.
 *
 * Los controles se apilan y la columna es desplazable para que sigan siendo utilizables con el
 * texto ampliado hasta el 200 %. Los colores dinámicos solo se ofrecen en Google Expressive y
 * desde Android 12 (API 31); en iOS Classic la paleta es siempre estática.
 */
@Composable
private fun AppearanceDialog(
    settings: UserSettings,
    onUiStyleChange: (UiStyle) -> Unit,
    onAppearanceChange: (AppearanceMode) -> Unit,
    onDynamicColorsChange: (Boolean) -> Unit,
    onDismiss: () -> Unit
) {
    val supportsDynamic = android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Apariencia") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                DialogSectionTitle("Estilo")
                DialogChipRow {
                    FilterChip(
                        selected = settings.uiStyle == UiStyle.GOOGLE,
                        onClick = { onUiStyleChange(UiStyle.GOOGLE) },
                        label = { Text("Google Expressive") }
                    )
                    FilterChip(
                        selected = settings.uiStyle == UiStyle.IOS,
                        onClick = { onUiStyleChange(UiStyle.IOS) },
                        label = { Text("iOS Classic") }
                    )
                }

                DialogSectionTitle("Modo")
                DialogChipRow {
                    AppearanceMode.entries.forEach { mode ->
                        FilterChip(
                            selected = settings.appearanceMode == mode,
                            onClick = { onAppearanceChange(mode) },
                            label = {
                                Text(
                                    when (mode) {
                                        AppearanceMode.SYSTEM -> "Sistema"
                                        AppearanceMode.LIGHT -> "Claro"
                                        AppearanceMode.DARK -> "Oscuro"
                                    }
                                )
                            }
                        )
                    }
                }

                if (supportsDynamic && settings.uiStyle == UiStyle.GOOGLE) {
                    DialogSectionTitle("Color")
                    DialogChipRow {
                        FilterChip(
                            selected = settings.useDynamicColors,
                            onClick = { onDynamicColorsChange(!settings.useDynamicColors) },
                            label = { Text("Colores dinámicos") }
                        )
                    }
                    Text(
                        text = "Colores del fondo de pantalla. En modo oscuro los fondos siguen siendo negros.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Listo") }
        }
    )
}

@Composable
private fun DialogSectionTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleSmall,
        modifier = Modifier
            .padding(top = 4.dp)
            .semantics { heading() }
    )
}

/**
 * Fila de controles que se apila cuando no cabe en una sola línea (texto ampliado).
 * Usa dos filas como máximo: los grupos son de 2-3 elementos cortos.
 */
@Composable
private fun DialogChipRow(content: @Composable ColumnScope.() -> Unit) {
    Column(
        verticalArrangement = Arrangement.spacedBy(4.dp),
        content = content
    )
}
