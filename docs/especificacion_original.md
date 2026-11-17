# 🎧 SoundMap — Mapa de Calma y Medidor de Ruido Ambiental

**SoundMap** es una aplicación nativa para Android desarrollada en **Kotlin** y **Jetpack Compose** que mide el nivel de ruido ambiental en tiempo real (en decibelios) y permite registrar espacios silenciosos geolocalizados en un mapa interactivo. Está diseñada para ayudar a estudiantes y trabajadores remotos a encontrar lugares ideales para concentrarse.

> 💡 **Privacidad por diseño:** El audio capturado por el micrófono se procesa exclusivamente en memoria RAM para calcular la amplitud sonora (RMS). Nunca se graba, almacena ni transmite ningún archivo de voz o sonido.

---

## 📱 Capturas de Pantalla

*(Espacio reservado: agrega aquí 2 o 3 capturas de pantalla de la app o un GIF corto desde la carpeta `/docs`)*

| Medidor en Tiempo Real | Guardado de Punto | Mapa de Calma |
| :---: | :---: | :---: |
| ![Medidor](docs/screen_meter.png) | ![Guardar](docs/screen_save.png) | ![Mapa](docs/screen_map.png) |

---

## ✨ Características Principales

* **Medición Acústica en Tiempo Real:** Visualizador circular animado en Jetpack Compose que refleja los decibelios (dB) actuales y cambia de color según el nivel de ruido:
  * 🟢 **< 45 dB:** Silencioso (Ideal para estudiar o leer).
  * 🟡 **45 – 65 dB:** Moderado (Cafetería tranquila o conversación suave).
  * 🔴 **> 65 dB:** Ruidoso (Tráfico o ambiente agitado).
* **Muestreo Inteligente (10 segundos):** Calcula el promedio estable de ruido durante 10 segundos para evitar falsos positivos causados por ruidos momentáneos.
* **Geolocalización y Mapa Sin Costos de API:** Utiliza **OSMDroid (OpenStreetMap)** para visualizar los puntos guardados sin depender de llaves de pago de Google Maps.
* **Persistencia Local Offline:** Todos los registros (nombre del lugar, decibelios promedio, fecha y coordenadas) se almacenan localmente mediante **Room**.
* **Gestión Moderna de Permisos:** Implementación limpia de solicitud de permisos en tiempo de ejecución para micrófono (`RECORD_AUDIO`) y ubicación (`ACCESS_FINE_LOCATION`).

---

## 🛠️ Stack Tecnológico y Arquitectura

Este proyecto sigue los estándares recomendados por Google para el desarrollo moderno en Android bajo el patrón **MVVM (Model-View-ViewModel)** con separación clara de responsabilidades.

* **Lenguaje:** 100 % [Kotlin](https://kotlinlang.org/)
* **Interfaz de Usuario:** [Jetpack Compose](https://developer.android.com/jetpack/compose) + Material Design 3
* **Asincronía:** Kotlin Coroutines & `StateFlow`
* **Inyección de Dependencias:** Dagger Hilt
* **Base de Datos Local:** Room Database
* **Hardware y Sensores:**
  * `android.media.AudioRecord` (Cálculo de presión sonora RMS a dB)
  * `FusedLocationProviderClient` (Google Play Services Location)
* **Mapas:** [OSMDroid](https://github.com/osmdroid/osmdroid)

---

## 📂 Estructura del Proyecto

```text
app/src/main/java/com/tuusuario/soundmap/
├── data/
│   ├── local/
│   │   ├── NoiseSpotEntity.kt      # Entidad de base de datos Room
│   │   ├── NoiseSpotDao.kt         # Consultas SQL reactivas (Flow)
│   │   └── AppDatabase.kt          # Configuración de Room
│   └── sensor/
│       ├── AudioMeterManager.kt    # Captura de buffer PCM y cálculo de dB
│       └── LocationTracker.kt      # Obtención de coordenadas GPS
├── domain/
│   └── model/
│       ├── NoiseLevel.kt           # Clasificación (Silencioso, Moderado, Alto)
│       └── NoiseSpot.kt            # Modelo limpio de dominio
├── di/
│   └── AppModule.kt                # Módulos de inyección con Hilt
├── ui/
│   ├── components/
│   │   └── DecibelGauge.kt         # Componente Canvas animado en Compose
│   ├── screens/
│   │   ├── MeterScreen.kt          # Pantalla principal de medición
│   │   └── MapHistoryScreen.kt     # Mapa interactivo y lista de lugares
│   └── theme/                      # Tipografía y paleta Material 3
└── MainActivity.kt
```

---

## ⚙️ Fórmula de Cálculo Acústico

Para convertir la señal digital del micrófono (`AudioRecord` en formato `ENCODING_PCM_16BIT`) a una escala aproximada de decibelios, se calcula el valor cuadrático medio (**RMS**) de cada bloque de lectura:

$$RMS = \sqrt{\frac{1}{N} \sum_{i=1}^{N} x_i^2}$$

$$dB = 20 \cdot \log_{10}\left(\frac{RMS}{P_{ref}}\right)$$

---

## 🚀 Cómo Ejecutar el Proyecto

1. **Clona el repositorio:**
   ```bash
   git clone https://github.com/tuusuario/SoundMap-Android.git
   ```
2. **Abre el proyecto:** Inicia **Android Studio (Ladybug o superior)** y selecciona la carpeta clonada.
3. **Sincroniza Gradle:** Espera a que se descarguen las dependencias (no se requieren API Keys adicionales).
4. **Ejecuta la app:** Conecta un dispositivo físico Android (recomendado para probar el micrófono real) o usa un emulador con API 26+.

---

## 🗺️ Mejoras Futuras (Roadmap)

* [ ] Calibración manual de sensibilidad de micrófono según el modelo de dispositivo.
* [ ] Filtros en el mapa por franja horaria (mañana, tarde, noche).
* [ ] Exportación e importación de puntos de calma en formato JSON/GeoJSON.
* [ ] Tests unitarios para `AudioMeterManager` y pruebas de UI en Compose.

---

## 📄 Licencia

Este proyecto está bajo la Licencia MIT. Consulta el archivo `LICENSE` para más detalles.
