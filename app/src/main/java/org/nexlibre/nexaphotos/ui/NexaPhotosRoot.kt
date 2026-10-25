package org.nexlibre.nexaphotos.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.nexlibre.nexaphotos.BuildConfig
import org.nexlibre.nexaphotos.domain.model.UserSettings

/**
 * Raíz de la app. En esta base (0.1.0) solo se muestra el estado de la fundación y los controles
 * de apariencia; la biblioteca multimedia se conecta en la capa `ui/media`.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NexaPhotosRoot(
    settings: UserSettings,
    onUiStyleChange: (org.nexlibre.nexaphotos.domain.model.UiStyle) -> Unit,
    onAppearanceChange: (org.nexlibre.nexaphotos.domain.model.AppearanceMode) -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = { TopAppBar(title = { Text("Nexa Photos") }) }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .padding(horizontal = 24.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Fundación ${BuildConfig.VERSION_NAME}",
                style = MaterialTheme.typography.titleLarge
            )
            Text(
                text = "Estilo: ${settings.uiStyle.name} · Apariencia: ${settings.appearanceMode.name}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "La biblioteca multimedia (MediaStore, cuadrícula, visor y favoritos) se " +
                    "conecta en la siguiente capa de trabajo.",
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                text = "Los originales son de solo lectura: Nexa Photos no borra ni modifica fotos " +
                    "ni videos, y no declara permisos de red.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
