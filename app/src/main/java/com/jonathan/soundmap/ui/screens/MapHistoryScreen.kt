package com.jonathan.soundmap.ui.screens

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.drawable.BitmapDrawable
import androidx.core.graphics.createBitmap
import androidx.core.graphics.drawable.toDrawable
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.background
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Map
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.jonathan.soundmap.domain.model.NoiseLevel
import com.jonathan.soundmap.domain.model.NoiseSpot
import com.jonathan.soundmap.ui.theme.color
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.BoundingBox
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.CustomZoomButtonsController
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.CopyrightOverlay
import org.osmdroid.views.overlay.Marker
import java.text.DateFormat
import java.util.Date
import kotlin.math.roundToInt

/** Centro inicial cuando aún no hay lugares guardados: Santiago de Chile. */
private val DEFAULT_CENTER = GeoPoint(-33.4489, -70.6693)

@Composable
fun MapHistoryScreen(viewModel: MapHistoryViewModel, modifier: Modifier = Modifier) {
    val spots by viewModel.spots.collectAsStateWithLifecycle()
    var selectedId by remember { mutableStateOf<Long?>(null) }
    var toDelete by remember { mutableStateOf<NoiseSpot?>(null) }

    Column(modifier.fillMaxSize()) {
        // clipToBounds: MapView (vista clásica) dibuja fuera de su marco si no se recorta.
        Box(Modifier.fillMaxWidth().weight(1.1f).clipToBounds()) {
            NoiseMap(spots = spots, selectedId = selectedId, onMarkerClick = { selectedId = it })
        }
        HorizontalDivider()
        if (spots.isEmpty()) {
            Column(
                Modifier.fillMaxWidth().weight(1f).padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Icon(Icons.Filled.Map, contentDescription = null, modifier = Modifier.size(48.dp))
                Text("Aún no hay lugares", style = MaterialTheme.typography.titleMedium)
                Text(
                    "Mide un lugar durante 10 segundos en la pestaña Medidor y guárdalo para verlo aquí.",
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        } else {
            Text(
                "${spots.size} lugares · el más silencioso: ${spots.minBy { it.averageDb }.name}",
                style = MaterialTheme.typography.labelLarge,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            )
            LazyColumn(Modifier.fillMaxWidth().weight(1f)) {
                items(spots, key = { it.id }) { spot ->
                    SpotRow(
                        spot = spot,
                        selected = spot.id == selectedId,
                        onClick = { selectedId = spot.id },
                        onDelete = { toDelete = spot },
                    )
                }
            }
        }
    }

    toDelete?.let { spot ->
        AlertDialog(
            onDismissRequest = { toDelete = null },
            title = { Text("Eliminar lugar") },
            text = { Text("¿Eliminar «${spot.name}» del mapa?") },
            confirmButton = {
                TextButton(onClick = { viewModel.delete(spot); if (selectedId == spot.id) selectedId = null; toDelete = null }) {
                    Text("Eliminar")
                }
            },
            dismissButton = { TextButton(onClick = { toDelete = null }) { Text("Cancelar") } },
        )
    }
}

@Composable
private fun SpotRow(spot: NoiseSpot, selected: Boolean, onClick: () -> Unit, onDelete: () -> Unit) {
    val date = remember(spot.timestampMillis) {
        DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT).format(Date(spot.timestampMillis))
    }
    ListItem(
        modifier = Modifier.clickable(onClick = onClick).let {
            if (selected) it.background(MaterialTheme.colorScheme.secondaryContainer) else it
        },
        leadingContent = {
            Box(Modifier.size(44.dp).background(spot.level.color(), CircleShape), contentAlignment = Alignment.Center) {
                Text("${spot.averageDb.roundToInt()}", color = androidx.compose.ui.graphics.Color.White, fontWeight = FontWeight.Bold)
            }
        },
        headlineContent = { Text(spot.name, fontWeight = FontWeight.SemiBold) },
        supportingContent = { Text("${spot.level.label} · $date") },
        trailingContent = {
            IconButton(onClick = onDelete) { Icon(Icons.Filled.Delete, contentDescription = "Eliminar ${spot.name}") }
        },
    )
}

@Composable
private fun NoiseMap(spots: List<NoiseSpot>, selectedId: Long?, onMarkerClick: (Long) -> Unit) {
    val context = LocalContext.current
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val icons = remember { NoiseLevel.entries.associateWith { markerIcon(context, it) } }
    val mapView = remember {
        Configuration.getInstance().apply {
            load(context, context.getSharedPreferences("osmdroid", Context.MODE_PRIVATE))
            userAgentValue = context.packageName
        }
        MapView(context).apply {
            setTileSource(TileSourceFactory.MAPNIK)
            setMultiTouchControls(true)
            zoomController.setVisibility(CustomZoomButtonsController.Visibility.SHOW_AND_FADEOUT)
            controller.setZoom(12.0)
            controller.setCenter(DEFAULT_CENTER)
            // Atribución obligatoria según la política de uso de las teselas de OpenStreetMap.
            overlays += CopyrightOverlay(context)
        }
    }

    DisposableEffect(lifecycle) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> mapView.onResume()
                Lifecycle.Event.ON_PAUSE -> mapView.onPause()
                else -> Unit
            }
        }
        lifecycle.addObserver(observer)
        onDispose {
            lifecycle.removeObserver(observer)
            mapView.onDetach()
        }
    }

    // Encuadra todos los lugares cuando cambia la lista.
    LaunchedEffect(spots.map { it.id }) {
        when {
            spots.size == 1 -> {
                mapView.controller.setZoom(16.0)
                mapView.controller.setCenter(GeoPoint(spots[0].latitude, spots[0].longitude))
            }
            spots.size > 1 -> mapView.post {
                val box = BoundingBox.fromGeoPointsSafe(spots.map { GeoPoint(it.latitude, it.longitude) })
                mapView.zoomToBoundingBox(box.increaseByScale(1.4f), false, 64)
            }
        }
    }

    LaunchedEffect(selectedId) {
        spots.firstOrNull { it.id == selectedId }?.let {
            mapView.controller.animateTo(GeoPoint(it.latitude, it.longitude), 17.0, 600L)
        }
    }

    AndroidView(
        factory = { mapView },
        modifier = Modifier.fillMaxSize(),
        update = { map ->
            map.overlays.removeAll { it is Marker }
            spots.forEach { spot ->
                map.overlays += Marker(map).apply {
                    position = GeoPoint(spot.latitude, spot.longitude)
                    setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
                    icon = icons.getValue(spot.level)
                    title = spot.name
                    snippet = "${spot.averageDb.roundToInt()} dB · ${spot.level.label}"
                    setOnMarkerClickListener { m, _ ->
                        onMarkerClick(spot.id)
                        m.showInfoWindow()
                        true
                    }
                    if (spot.id == selectedId) showInfoWindow()
                }
            }
            map.invalidate()
        },
    )
}

/** Círculo de color con borde blanco para los marcadores del mapa. */
private fun markerIcon(context: Context, level: NoiseLevel): BitmapDrawable {
    val density = context.resources.displayMetrics.density
    val size = (30 * density).toInt()
    val bmp = createBitmap(size, size)
    val canvas = Canvas(bmp)
    val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    paint.color = android.graphics.Color.WHITE
    canvas.drawCircle(size / 2f, size / 2f, size / 2f, paint)
    paint.color = level.color().toArgb()
    canvas.drawCircle(size / 2f, size / 2f, size / 2f - 3 * density, paint)
    return bmp.toDrawable(context.resources)
}
