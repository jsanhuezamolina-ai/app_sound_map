package com.jonathan.soundmap.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.layout
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.jonathan.soundmap.domain.acoustics.NoiseMath
import com.jonathan.soundmap.domain.model.NoiseLevel
import com.jonathan.soundmap.ui.theme.color
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

private const val START_ANGLE = 150f
private const val SWEEP = 240f

/** Medidor circular animado: el arco crece con los dB y cambia de color según el nivel. */
@Composable
fun DecibelGauge(db: Double, modifier: Modifier = Modifier, active: Boolean = true) {
    val level = NoiseLevel.from(db)
    val fraction = (db / NoiseMath.MAX_DB).toFloat().coerceIn(0f, 1f)
    val animatedFraction by animateFloatAsState(fraction, tween(220), label = "fraction")
    val arcColor by animateColorAsState(level.color(), tween(400), label = "color")
    val track = MaterialTheme.colorScheme.surfaceVariant
    val tick = MaterialTheme.colorScheme.outline

    Box(
        modifier = modifier
            .aspectRatio(1f)
            // El arco de 240° deja vacía la parte baja del cuadrado: se informa una altura menor
            // al layout para que la leyenda quede pegada al medidor.
            .layout { measurable, constraints ->
                val p = measurable.measure(constraints)
                layout(p.width, (p.height * 0.84f).toInt()) { p.place(0, 0) }
            }
            .semantics { contentDescription = "Nivel de ruido ${db.roundToInt()} decibelios, ${level.label}" },
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.fillMaxSize().padding(12.dp)) {
            val stroke = size.minDimension * 0.09f
            val inset = stroke / 2f
            val arcSize = Size(size.width - stroke, size.height - stroke)
            val topLeft = Offset(inset, inset)
            drawArc(track, START_ANGLE, SWEEP, false, topLeft, arcSize, style = Stroke(stroke, cap = StrokeCap.Round))
            if (active) {
                drawArc(
                    arcColor, START_ANGLE, SWEEP * animatedFraction, false, topLeft, arcSize,
                    style = Stroke(stroke, cap = StrokeCap.Round),
                )
            }
            // Marcas cada 10 dB y marcas largas en los límites 45 y 65 dB.
            val radius = size.minDimension / 2f
            val c = Offset(size.width / 2f, size.height / 2f)
            for (value in 0..120 step 10) {
                drawTick(c, radius - stroke * 1.6f, radius - stroke * 2.1f, value / 120f, tick, 2.dp.toPx())
            }
            for (limit in listOf(NoiseLevel.QUIET_LIMIT, NoiseLevel.LOUD_LIMIT)) {
                drawTick(c, radius - stroke * 1.4f, radius - stroke * 2.6f, (limit / 120.0).toFloat(), tick, 3.dp.toPx())
            }
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = if (active) db.roundToInt().toString() else "--",
                style = MaterialTheme.typography.displayLarge,
                fontWeight = FontWeight.Bold,
            )
            Text("dB", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (active) {
                Text(level.label, style = MaterialTheme.typography.titleLarge, color = arcColor, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawTick(
    center: Offset, outer: Float, inner: Float, fraction: Float,
    color: androidx.compose.ui.graphics.Color, width: Float,
) {
    val angle = Math.toRadians((START_ANGLE + SWEEP * fraction).toDouble())
    val dx = cos(angle).toFloat()
    val dy = sin(angle).toFloat()
    drawLine(color, Offset(center.x + dx * inner, center.y + dy * inner), Offset(center.x + dx * outer, center.y + dy * outer), width)
}
