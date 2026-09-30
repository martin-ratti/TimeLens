# 📝 Changelog — TimeLens

Todos los cambios notables del proyecto se documentan en este archivo.

El formato está basado en [Keep a Changelog](https://keepachangelog.com/es-ES/1.1.0/).

## [0.4.0] — 2026-09-30

### Optimizado
- 🚀 **Rendimiento Extremo en Home y Listas**:
  - Eliminación de la sobrecarga de Coil durante el scroll: los íconos de aplicaciones ahora se convierten una única vez a `ImageBitmap` en memoria y se renderizan de forma nativa directamente en el canvas de Compose con `Image(bitmap = ...)`.
  - Supresión de corrutinas y recomposiciones continuas en cada tarjeta: `LinearProgressIndicator` ahora consume `{ progress }` directamente sin disparar ticks de 60/120 Hz por ítem.
  - Reducción del uso de memoria y recolección de basura en `CircularProgressCard`: memorización de `Brush.sweepGradient` con `remember`, evitando la creación de shaders nativos en cada frame de dibujo.
  - Verificación en dispositivo físico Motorola edge 50 fusion (`ZY22KK29KP`) mediante `dumpsys gfxinfo`: 0 janks por subida de bitmaps, mediana de tiempo por frame de 11 ms y GPU en 5 ms, garantizando una tasa de refresco fluida a 60/120 Hz.
- 🔁 **Animaciones Inteligentes y No Repetitivas**:
  - `CircularProgressCard` utiliza `rememberSaveable`, garantizando que la animación de entrada solo se ejecute al inicio y no se reinicie a cero al scrollear o volver a la parte superior.
- 💳 **Rediseño Unificado de Tarjetas KPI (`StatCard`)**:
  - Contenedor con borde tenue con tinte de acento de neón (`BorderStroke(1.dp, accentColor.copy(alpha = 0.18f))`).
  - Badge para íconos en contenedor squircle redondeado (`RoundedCornerShape(10.dp)`) con fondo suave translúcido (`accentColor.copy(alpha = 0.14f)`).
  - Jerarquía tipográfica pulida y adaptativa para evitar desbordes y cortes de texto en pantallas compactas (360dp a 390dp).
  - Unificación visual transversal aplicada en `HomeScreen`, `AppDetailScreen` y `HistoryScreen`.

### Corregido
- 🧭 **Navegación de Ajustes desde Home**:
  - El botón de configuración en la barra superior de Home ahora sincroniza de manera limpia con la barra de navegación inferior (`popUpTo` al destino inicial con `launchSingleTop = true`), evitando pantallas duplicadas o estados rotos en la pila de navegación.
- 🛡️ **Protección contra divisiones por cero**:
  - Validación defensiva para evitar valores `NaN` en cálculos de progreso cuando el tiempo total o meta diaria sea 0.

### Agregado
- 🏷️ **Filtro interactivo por Categorías en Inicio**:
  - Fila interactiva con chip *"Todas"* y chips por categoría que filtran instantáneamente la lista de aplicaciones más usadas.
- 📈 **Rediseño Completo de la Pantalla de Tendencias (`HistoryScreen`)**:
  - Estructura 100% scrolleable con `LazyColumn`.
  - Tarjetas KPI semanales: Total semanal acumulado, promedio diario y total de desbloqueos.
  - Tarjetas de destaque para Mejor día (menor uso) y Mayor uso con badges informativos.
  - Desglose día por día de la semana con barra proporcional, cantidad de desbloqueos y la aplicación más usada de cada jornada.
- 📊 **Interactividad en Gráficos Nativos**:
  - `HourlyBarChart`: Toque en cualquier barra para ver la franja horaria y duración exacta.
  - `WeeklyBarChart`: Toque en cualquier día para ver la fecha completa y tiempo exacto formateado.
- 🔔 **Sistema Integral de Notificaciones Inteligentes (Fase 5 Completa)**:
  - 3 canales dedicados (`NotificationChannel`): Alertas de bienestar, Resumen nocturno y Monitoreo en segundo plano.
  - Alerta automática al superar el objetivo diario de horas de pantalla.
  - Detección y alerta de sesiones continuas prolongadas (>= 60 min en apps distractoras).
  - Alerta de nueva sesión récord comparando con el histórico semanal.
  - `DailySummaryWorker` con `WorkManager` programado para las 22:00 hs con comparativa y app más usada.
  - `UsageMonitorService` para monitoreo en primer plano (`dataSync`).
  - Gestión del permiso en tiempo de ejecución `POST_NOTIFICATIONS` en Android 13+.
  - Panel de pruebas de notificaciones en la pantalla de Ajustes con disparador de alertas y simulación de resumen en tiempo real.

---

## [0.3.0] — 2026-09-26
 
### Agregado
- 🔍 **Pantalla de Detalle de Aplicación (`AppDetailScreen`)**:
  - Encabezado interactivo con ícono oficial, nombre limpio de app y badge con categoría de uso.
  - 4 tarjetas KPI: Tiempo acumulado hoy, cantidad de aperturas, duración de la sesión más larga y duración promedio por sesión.
  - Gráfico interactivo por hora (`HourlyBarChart`) que desglosa el uso en las 24 horas del día actual (00:00 a 23:00).
  - Gráfico de tendencia histórica (`WeeklyBarChart`) mostrando el uso de los últimos 7 días específicos para esa app.
- 🌓 **Modo Claro y Oscuro 100% Dinámico**:
  - Definición completa de `LightColorScheme` en Material Design 3 con fondo slate y tarjetas blancas de alto contraste.
  - Conexión reactiva en `MainActivity` mediante `UserPreferencesManager` (DataStore) para aplicar el cambio inmediatamente al tocar el switch en Ajustes sin requerir reinicio de la app.
  - Migración de todas las pantallas y tarjetas para consumir `MaterialTheme.colorScheme` de manera adaptativa.
- 🤝 **Compartir TimeLens (Share Intent)**:
  - Reemplazo de la función de exportación de CSV por un diálogo amigable de compartir usando el selector nativo de Android (`Intent.ACTION_SEND` con `Intent.createChooser`).
  - Mensaje cálido invitando a amigos a probar TimeLens junto con el enlace al repositorio oficial.
- ℹ️ **Diálogo "Acerca de" rediseñado**:
  - Visualización por tarjetas con la Misión de bienestar digital, garantía de Privacidad 100% Local (sin telemetría ni servidores externos), autoría (Martín Ratti) y botón directo para abrir el repositorio de GitHub en el navegador.
- 🏷️ **Categorización inteligente de aplicaciones**:
  - Detección y clasificación automática de apps (`Social`, `Entretenimiento`, `Productividad`, `Juegos`, `Educación`, `Salud y Bienestar`, `Herramientas`, `Otros`) con badges de colores personalizados.

### Corregido
- ⏱️ **Cálculo de Tiempo de Pantalla Sincronizado**:
  - Solución al error donde el tiempo reportado excedía las 24 horas diarias por sumas directas de paquetes en segundo plano o eventos superpuestos.
  - Implementación de un algoritmo basado en intervalos temporales combinados en `SessionCalculator`, igualando con exactitud los minutos reportados por Bienestar Digital (Digital Wellbeing) de Android.
- 🚫 **Filtrado de Aplicaciones del Sistema**:
  - Exclusión de Launchers del sistema (ej. Nova Launcher, Pixel Launcher, OneUI Home), System UI y la propia app TimeLens de las estadísticas para reflejar únicamente las apps utilizadas por el usuario.
- ⚡ **Optimización de Rendimiento y Carga de UI**:
  - Implementación de caché en memoria de nombres de apps e íconos en `UsageDataSource`, eliminando el lag y jitter en las listas de Compose.

---

## [0.2.0] — 2026-09-25

### Agregado
- 🧭 Navegación principal con `AppNavHost` y barra inferior (`NavigationBar` de Material 3).
- 📱 Pantalla de bienvenida y solicitud de permisos: `OnboardingScreen`.
- 📊 Pantalla de historial y tendencias semanales: `HistoryScreen`.
- ⚙️ Pantalla de configuración completa con toggles y objetivos: `SettingsScreen`.
- 🎨 Iconos outlined modernos de Material Icons Extended en sustitución de todos los emojis.

### Modificado
- 🏷️ Renombrado integral del proyecto a **TimeLens**:
  - Package migrado a `com.timelens.app`.
  - Base de datos Room renombrada a `TimeLensDatabase` (`timelens_db`).
  - Application class renombrada a `TimeLensApp`.
  - Tema renombrado a `TimeLensTheme`.
  - Gradle `namespace` y `applicationId` actualizados a `com.timelens.app`.
  - Repositorio remoto actualizado a `TimeLens.git`.

---

## [0.1.0] — 2026-09-25

### Agregado
- 🏗️ Estructura inicial con Clean Architecture (data, domain, presentation).
- 📱 Dashboard visual con datos demostrativos en Compose.
- 📄 Documentación inicial (`README.md`, `SETUP.md`, `ARCHITECTURE.md`, `ROADMAP.md`, `CONTRIBUTING.md`).

---

## Versionado

Este proyecto usa [Semantic Versioning](https://semver.org/):
- **MAJOR** — Cambios incompatibles (ej: rediseño completo)
- **MINOR** — Nueva funcionalidad retrocompatible
- **PATCH** — Corrección de bugs

---

*Documento actualizado: 25 de septiembre de 2026*
