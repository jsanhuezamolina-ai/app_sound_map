# Changelog

## [1.0.0] - 2026-11-16

### Añadido
- Medidor de ruido en tiempo real (AudioRecord, PCM 16 bits, RMS → dB) con indicador circular animado en Compose.
- Clasificación por color: silencioso (< 45 dB), moderado (45–65 dB) y ruidoso (> 65 dB).
- Medición estable de 10 segundos con barra de progreso.
- Guardado de lugares con nombre, promedio, fecha y coordenadas (Fused Location Provider + Room).
- Mapa de calma con OpenStreetMap (OSMDroid), marcadores por color y lista con borrado.
- Permisos en tiempo de ejecución para micrófono y ubicación, con explicación previa.
- Arquitectura MVVM con Hilt, Coroutines y StateFlow.
- 15 tests unitarios del cálculo acústico y del muestreo; CI en GitHub Actions.

### Decisiones
- El promedio de 10 s es la media de los dB (no Leq), para que un golpe breve no convierta un lugar tranquilo en ruidoso.
- El micrófono solo está abierto mientras la pantalla del medidor está visible.
