# TimeLens - Android App

**TimeLens** es una aplicación nativa de Android moderna, construida con Kotlin y Jetpack Compose, que ayuda a los usuarios a ser más conscientes del tiempo que pasan en sus teléfonos, promoviendo hábitos digitales saludables.

## Características

- 📊 **Dashboard diario en tiempo real con Pull-to-Refresh:** Resumen de tiempo de pantalla sincronizado al milisegundo mediante `PullToRefreshBox` de Material 3 con feedback háptico.
- 📱 **Top apps más usadas:** Barras de progreso animadas, iconos de apps renderizados de forma nativa en memoria con `ImageBitmap`, conteo de aperturas y categorización automática.
- 🎯 **Límites de tiempo diarios por aplicación (App Limits):** Define límites de uso personalizados en apps críticas (Instagram, TikTok, etc.) con alertas automáticas al 80% y 100%.
- 🔍 **Detalle por aplicación enriquecido (`AppDetailScreen`):**
  - Acciones rápidas del sistema operativo ("Abrir app" y "Ajustes de app").
  - 6 KPIs enriquecidos: Tiempo hoy, aperturas con frecuencia estimada, sesión continua récord, promedio por sesión, horario pico y tendencia semanal comparativa.
  - Tarjeta de Bienestar Contextual por aplicación con consejos según patrones de consumo.
  - Gráfico de barras de actividad por hora (00:00 a 23:00) con resaltado de la hora pico.
  - Historial de uso real de los últimos 7 días.
- 💡 **Diagnóstico de Bienestar y Cierre del Día:** Diagnóstico reflexivo en tiempo real con score general (0 a 100), hábitos analizados y apertura inteligente atemporal o modo revisión nocturna a las 22:00 hs.
- 📈 **Tendencias e historial semanal 100% real:** Cero datos simulados ni inventados. Gráficos nativos en Canvas con detección de mejor/peor día y purga atómica de inconsistencias.
- 🌓 **Soporte de Temas:**
  - **Modo Oscuro Neón:** Estilo Cyberpunk moderno de alto contraste con acentos neón azul, púrpura, cian y verde.
  - **Modo Claro:** Interfaz luminosa y elegante adaptada a Material Design 3.
  - **Automático:** Siguiendo la preferencia del sistema operativo.
- 🛡️ **100% Local, Privado & Offline:** Todos los datos se procesan y almacenan exclusivamente en tu teléfono. Sin servidores externos, sin rastreadores y sin publicidad.
- 🧪 **Suite de Pruebas Unitarias Rigurosa:** 66 tests unitarios pasando al 100% en JVM local cubriendo lógica de cálculo, casos de uso, repositorios, preferencias y viewmodels.

---

## Primeros Pasos

Si es tu primer proyecto de Android:

1. Lee primero [SETUP.md](SETUP.md): instalación de Android Studio y configuración del proyecto.
2. Luego lee [BEGINNER_GUIDE.md](BEGINNER_GUIDE.md): introducción a Kotlin, Jetpack Compose y la arquitectura.
3. Revisa [ARCHITECTURE.md](ARCHITECTURE.md): explicación detallada de Clean Architecture y mapa del proyecto.
4. Consulta el [ROADMAP.md](ROADMAP.md) para ver el estado de cada fase y el [CHANGELOG.md](CHANGELOG.md) para el historial de versiones.
5. Explora [TESTING.md](TESTING.md) para aprender a ejecutar y entender la suite de pruebas unitarias.

---

## Documentación

| Documento | Descripción | Audiencia |
| :--- | :--- | :--- |
| **[SETUP.md](SETUP.md)** | Guía de instalación y ejecución paso a paso | Principiantes |
| **[BEGINNER_GUIDE.md](BEGINNER_GUIDE.md)** | Curso de Kotlin, Compose y flujo de la app | Principiantes |
| **[ARCHITECTURE.md](ARCHITECTURE.md)** | Clean Architecture, MVVM y mapa de archivos | Todos |
| **[TESTING.md](TESTING.md)** | Documentación y catálogo de tests unitarios | Desarrolladores |
| **[ROADMAP.md](ROADMAP.md)** | Plan de desarrollo por fases y cronograma | Todos |
| **[CHANGELOG.md](CHANGELOG.md)** | Historial detallado de versiones y cambios | Todos |
| **[CONTRIBUTING.md](CONTRIBUTING.md)** | Guía de contribución, ramas y commits | Desarrolladores |

---

## Tech Stack

- **Lenguaje:** [Kotlin](https://kotlinlang.org/) (100%)
- **UI:** [Jetpack Compose](https://developer.android.com/jetpack/compose) + Material Design 3
- **Iconos:** Material Icons Extended
- **Carga de imágenes / Iconos de apps:** Coil Compose
- **Arquitectura:** Clean Architecture + MVVM + Unidirectional Data Flow (UDF)
- **Inyección de Dependencias:** [Hilt](https://dagger.dev/hilt/)
- **Persistencia Local:** [Room](https://developer.android.com/training/data-storage/room) & DataStore Preferences
- **Tareas en Segundo Plano:** [WorkManager](https://developer.android.com/topic/libraries/architecture/workmanager)
- **Navegación:** Navigation Compose con rutas seguras y paso de argumentos
- **Gráficos:** Componentes nativos optimizados con Compose Canvas
- **Concurrencia:** Kotlin Coroutines & StateFlow / Flow reactivos
