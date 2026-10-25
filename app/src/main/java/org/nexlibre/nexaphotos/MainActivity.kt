package org.nexlibre.nexaphotos

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import org.nexlibre.nexaphotos.ui.AppViewModel
import org.nexlibre.nexaphotos.ui.NexaPhotosRoot
import org.nexlibre.nexaphotos.ui.theme.NexaPhotosTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val appViewModel: AppViewModel = viewModel(factory = AppViewModel.Factory)
            val settings = appViewModel.settings.collectAsStateWithLifecycle().value
            NexaPhotosTheme(settings = settings) {
                NexaPhotosRoot(
                    settings = settings,
                    onUiStyleChange = appViewModel::setUiStyle,
                    onAppearanceChange = appViewModel::setAppearanceMode
                )
            }
        }
    }
}
