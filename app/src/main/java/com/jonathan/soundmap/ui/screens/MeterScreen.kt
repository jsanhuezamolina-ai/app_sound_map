package com.jonathan.soundmap.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.jonathan.soundmap.domain.model.NoiseLevel
import com.jonathan.soundmap.ui.components.DecibelGauge
import com.jonathan.soundmap.ui.theme.color
import kotlin.math.ceil
import kotlin.math.roundToInt

@Composable
fun MeterScreen(viewModel: MeterViewModel, onMessage: (String) -> Unit, modifier: Modifier = Modifier) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    fun granted(p: String) = ContextCompat.checkSelfPermission(context, p) == PackageManager.PERMISSION_GRANTED

    var micGranted by remember { mutableStateOf(granted(Manifest.permission.RECORD_AUDIO)) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { result ->
        micGranted = result[Manifest.permission.RECORD_AUDIO] ?: granted(Manifest.permission.RECORD_AUDIO)
        viewModel.setLocationPermission(granted(Manifest.permission.ACCESS_FINE_LOCATION) || granted(Manifest.permission.ACCESS_COARSE_LOCATION))
    }
    val requestAll = {
        launcher.launch(arrayOf(Manifest.permission.RECORD_AUDIO, Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
    }

    LaunchedEffect(Unit) {
        viewModel.setLocationPermission(granted(Manifest.permission.ACCESS_FINE_LOCATION) || granted(Manifest.permission.ACCESS_COARSE_LOCATION))
        if (!micGranted) requestAll()
    }

    // El micrófono solo está abierto mientras la pantalla está visible.
    LifecycleResumeEffect(micGranted) {
        if (micGranted) viewModel.startListening()
        onPauseOrDispose { viewModel.stopListening() }
    }

    LaunchedEffect(state.message) {
        state.message?.let { onMessage(it); viewModel.messageShown() }
    }

    Column(
        modifier = modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 24.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text("Medidor de ruido", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold)

        if (!micGranted) {
            PermissionCard(onRequest = requestAll)
            return@Column
        }

        DecibelGauge(db = state.currentDb, active = state.isListening, modifier = Modifier.widthIn(max = 340.dp).fillMaxWidth())

        LevelLegend(current = NoiseLevel.from(state.currentDb))

        state.error?.let { Text(it, color = MaterialTheme.colorScheme.error, textAlign = TextAlign.Center) }

        if (state.isSampling) {
            val remaining = ceil((1f - state.samplingProgress) * 10f).toInt().coerceAtLeast(0)
            Text("Midiendo… quedan $remaining s. Mantén el teléfono quieto.", style = MaterialTheme.typography.bodyLarge)
            LinearProgressIndicator(progress = { state.samplingProgress }, modifier = Modifier.fillMaxWidth().height(8.dp))
        } else {
            Button(onClick = viewModel::startSampling, enabled = state.isListening, modifier = Modifier.fillMaxWidth().height(56.dp)) {
                Icon(Icons.Filled.GraphicEq, contentDescription = null)
                Spacer(Modifier.size(8.dp))
                Text("Medir 10 segundos y guardar", style = MaterialTheme.typography.titleMedium)
            }
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.Lock, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.size(6.dp))
            Text(
                "El audio se procesa solo en memoria: nunca se graba ni se envía.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }

    state.pendingAverageDb?.let { avg ->
        SaveSpotDialog(
            averageDb = avg,
            state = state,
            onRetryLocation = viewModel::requestLocation,
            onSave = viewModel::saveSpot,
            onDismiss = viewModel::discardMeasurement,
        )
    }
}

@Composable
private fun PermissionCard(onRequest: () -> Unit) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Icon(Icons.Filled.Mic, contentDescription = null, modifier = Modifier.size(40.dp))
            Text("Se necesita el micrófono", style = MaterialTheme.typography.titleLarge)
            Text(
                "SoundMap usa el micrófono solo para calcular el nivel de ruido en decibelios. " +
                    "El sonido no se graba ni sale del teléfono. La ubicación se usa para marcar el lugar en el mapa.",
            )
            Button(onClick = onRequest) { Text("Dar permisos") }
            Text(
                "Si los rechazaste antes, actívalos en Ajustes → Apps → SoundMap → Permisos.",
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}

@Composable
private fun LevelLegend(current: NoiseLevel) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
        listOf(
            NoiseLevel.QUIET to "< 45",
            NoiseLevel.MODERATE to "45–65",
            NoiseLevel.LOUD to "> 65",
        ).forEach { (level, range) ->
            val selected = level == current
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    "● ${level.label}",
                    color = level.color(),
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                    style = MaterialTheme.typography.bodyMedium,
                )
                Text("$range dB", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun SaveSpotDialog(
    averageDb: Double,
    state: MeterUiState,
    onRetryLocation: () -> Unit,
    onSave: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var name by rememberSaveable { mutableStateOf("") }
    val level = NoiseLevel.from(averageDb)
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Guardar lugar") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    "Promedio de 10 s: ${averageDb.roundToInt()} dB",
                    style = MaterialTheme.typography.titleMedium,
                )
                Text("${level.label} · ${level.description}", color = level.color())
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it.take(60) },
                    label = { Text("Nombre del lugar") },
                    placeholder = { Text("Ej.: Biblioteca, sala 2") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.LocationOn, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.size(6.dp))
                    when {
                        state.isLocating -> {
                            CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp)
                            Spacer(Modifier.size(8.dp))
                            Text("Obteniendo ubicación…")
                        }
                        state.location != null -> Text(
                            "%.5f, %.5f".format(state.location.latitude, state.location.longitude),
                            style = MaterialTheme.typography.bodyMedium,
                        )
                        else -> Column {
                            Text("Sin ubicación. Activa el GPS y el permiso de ubicación.", color = MaterialTheme.colorScheme.error)
                            TextButton(onClick = onRetryLocation) { Text("Reintentar") }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = { onSave(name) }, enabled = state.location != null && name.isNotBlank()) { Text("Guardar") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Descartar") } },
    )
}
