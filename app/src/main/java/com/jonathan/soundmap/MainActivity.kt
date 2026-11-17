package com.jonathan.soundmap

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Map
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.jonathan.soundmap.ui.screens.MapHistoryScreen
import com.jonathan.soundmap.ui.screens.MapHistoryViewModel
import com.jonathan.soundmap.ui.screens.MeterScreen
import com.jonathan.soundmap.ui.screens.MeterViewModel
import com.jonathan.soundmap.ui.theme.SoundMapTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SoundMapTheme {
                var tab by rememberSaveable { mutableIntStateOf(intent.getIntExtra(EXTRA_TAB, 0)) }
                val snackbar = remember { SnackbarHostState() }
                val scope = rememberCoroutineScope()
                val meterViewModel: MeterViewModel = hiltViewModel()
                val mapViewModel: MapHistoryViewModel = hiltViewModel()
                Scaffold(
                    snackbarHost = { SnackbarHost(snackbar) },
                    bottomBar = {
                        NavigationBar {
                            NavigationBarItem(
                                selected = tab == 0, onClick = { tab = 0 },
                                icon = { Icon(Icons.Filled.GraphicEq, contentDescription = null) },
                                label = { Text("Medidor") },
                            )
                            NavigationBarItem(
                                selected = tab == 1, onClick = { tab = 1 },
                                icon = { Icon(Icons.Filled.Map, contentDescription = null) },
                                label = { Text("Mapa de calma") },
                            )
                        }
                    },
                ) { padding ->
                    val content = Modifier.padding(padding)
                    if (tab == 0) {
                        MeterScreen(meterViewModel, onMessage = { scope.launch { snackbar.showSnackbar(it) } }, modifier = content)
                    } else {
                        MapHistoryScreen(mapViewModel, modifier = content)
                    }
                }
            }
        }
    }

    companion object {
        /** Pestaña inicial (0 = medidor, 1 = mapa). Útil para abrir el mapa directo desde adb. */
        const val EXTRA_TAB = "tab"
    }
}
