# 📱 Plan de Acción — Screen Time App (Android)

> **Objetivo:** Crear una app Android que lea los datos de uso de pantalla del sistema y los presente en una interfaz visualmente atractiva, con métricas como tiempo por app, sesión continua más larga, horarios pico, cantidad de aperturas, etc.

---

## 🧭 Visión General del Proyecto

```
Usuario instala la app
        ↓
Concede permiso PACKAGE_USAGE_STATS
        ↓
La app lee UsageStatsManager / UsageEvents
        ↓
Procesa y calcula métricas (sesiones, rachas, promedios)
        ↓
Almacena histórico en base de datos local (Room)
        ↓
Muestra dashboards bonitos con gráficos y animaciones
```

---

## 📋 Fase 0 — Preparación y Decisiones Técnicas

### Stack Recomendado

| Componente | Tecnología | Por qué |
|:---|:---|:---|
| Lenguaje | **Kotlin** | Estándar oficial de Android, coroutines nativas |
| UI | **Jetpack Compose** | Declarativo, animaciones fluidas, moderno |
| Base de datos local | **Room** | ORM oficial de Android, integra con Flow/coroutines |
| Gráficos | **Vico** o **MPAndroidChart** | Librerías maduras para charts en Android |
| Inyección de dependencias | **Hilt** | Estándar de Google, simplifica mucho |
| Arquitectura | **MVVM + Clean Architecture** | Separación clara, testeable, escalable |
| Navegación | **Navigation Compose** | Oficial de Jetpack |

### Tareas de esta fase

- [x] Instalar **Android Studio** (última versión estable)
- [x] Crear el proyecto con template "Empty Compose Activity"
- [x] Configurar `minSdk = 26` (Android 8.0 — donde se introdujo `UsageStatsManager` completo)
- [x] Agregar dependencias iniciales: Compose, Room, Hilt, Navigation, librería de gráficos
- [x] Crear la estructura de carpetas del proyecto (ver sección de arquitectura abajo)
- [x] Inicializar repositorio Git

### Estructura de carpetas sugerida

```
com.tuapp.screentime/
├── data/
│   ├── local/
│   │   ├── db/                  # Room Database, DAOs, Entities
│   │   └── usage/               # Wrapper de UsageStatsManager
│   └── repository/              # Implementación de repositorios
├── domain/
│   ├── model/                   # Modelos de dominio (AppUsage, Session, DaySummary)
│   ├── repository/              # Interfaces de repositorios
│   └── usecase/                 # Casos de uso (GetDailyStats, GetLongestSession, etc.)
├── presentation/
│   ├── theme/                   # Colores, tipografía, shapes
│   ├── components/              # Componentes reutilizables (cards, charts, barras)
│   ├── screens/
│   │   ├── home/                # Dashboard principal
│   │   ├── detail/              # Detalle por app
│   │   ├── history/             # Historial semanal/mensual
│   │   └── settings/            # Configuración
│   └── navigation/              # NavHost y rutas
├── di/                          # Módulos de Hilt
├── util/                        # Extensiones, formateadores de tiempo, helpers
└── ScreenTimeApp.kt             # Application class
```

> **💡 Consejo:** No te saltes la estructura. Aunque al principio parezca "mucho para poco", cuando la app crezca vas a agradecer tener todo organizado.

---

## 📋 Fase 1 — Permisos y Lectura de Datos del Sistema

Esta es la fase más crítica. Sin esto, no hay app.

### 1.1 Solicitar el permiso `PACKAGE_USAGE_STATS`

```xml
<!-- AndroidManifest.xml -->
<uses-permission
    android:name="android.permission.PACKAGE_USAGE_STATS"
    tools:ignore="ProtectedPermissions" />
```

> ⚠️ **Importante:** Este permiso NO se pide con el diálogo normal de Android. Tenés que redirigir al usuario a la pantalla de Ajustes del sistema:

```kotlin
val intent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)
startActivity(intent)
```

- [x] Crear una pantalla de onboarding que explique POR QUÉ necesitás el permiso
- [x] Implementar la redirección a Ajustes
- [x] Verificar si el permiso ya fue concedido al volver a la app
- [x] Manejar el caso donde el usuario NO concede el permiso (pantalla amigable, no crash)

### 1.2 Leer datos con `UsageStatsManager`

```kotlin
// Obtener el servicio
val usageStatsManager = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager

// Consultar estadísticas del día
val calendar = Calendar.getInstance()
calendar.set(Calendar.HOUR_OF_DAY, 0)
calendar.set(Calendar.MINUTE, 0)

val stats: List<UsageStats> = usageStatsManager.queryUsageStats(
    UsageStatsManager.INTERVAL_DAILY,
    calendar.timeInMillis,
    System.currentTimeMillis()
)
```

Cada `UsageStats` tiene:
- `packageName` → nombre del paquete (ej: `com.instagram.android`)
- `totalTimeInForeground` → milisegundos totales en primer plano
- `firstTimeStamp` / `lastTimeStamp` → rango de tiempo
- `lastTimeUsed` → última vez que se usó

### 1.3 Leer eventos granulares con `UsageEvents`

Esto es lo que te permite calcular **sesiones individuales**:

```kotlin
val events: UsageEvents = usageStatsManager.queryEvents(
    startTime,   // inicio del día
    endTime      // ahora
)

val eventList = mutableListOf<UsageEvents.Event>()
while (events.hasNextEvent()) {
    val event = UsageEvents.Event()
    events.getNextEvent(event)
    eventList.add(event)
}
```

Tipos de evento clave:
- `UsageEvents.Event.ACTIVITY_RESUMED` (1) → App pasó al frente
- `UsageEvents.Event.ACTIVITY_PAUSED` (2) → App dejó de estar visible

- [x] Crear clase `UsageDataSource` que encapsule toda la lectura del sistema
- [x] Crear función para obtener lista de apps usadas hoy con su tiempo total
- [x] Crear función para obtener la lista de eventos crudos del día
- [x] Resolver nombres e íconos de apps a partir del `packageName` usando `PackageManager`

> **💡 Consejo:** `PackageManager.getApplicationLabel()` te da el nombre legible y `getApplicationIcon()` el ícono. Cacheá los íconos para no repetir la consulta.

---

## 📋 Fase 2 — Motor de Métricas (La Lógica Inteligente)

Acá es donde tu app se diferencia de "Tiempo de Uso" genérico del sistema.

### 2.1 Métricas a implementar (de menor a mayor complejidad)

#### Nivel Básico
- [x] **Tiempo total de pantalla del día** — Suma de `totalTimeInForeground` de todas las apps
- [x] **Top 5 apps más usadas** — Ordenar por tiempo descendente
- [x] **Cantidad de desbloqueos** — Contar eventos `KEYGUARD_HIDDEN` o usar `UsageStats` con `INTERVAL_DAILY`

#### Nivel Intermedio
- [x] **Sesión continua más larga por app** — Emparejar `ACTIVITY_RESUMED` → `ACTIVITY_PAUSED` del mismo paquete. Guardar la de mayor duración
- [x] **Cantidad de aperturas por app** — Contar cuántas veces aparece `ACTIVITY_RESUMED` para cada paquete
- [x] **Tiempo promedio por sesión** — `totalTimeInForeground / cantidadDeAperturas`
- [x] **Horario pico** — Agrupar eventos por franja horaria (mañana, tarde, noche, madrugada) y ver cuál tiene más actividad

#### Nivel Avanzado (diferenciadores)
- [x] **"Racha tóxica"** — La sesión continua más larga del día sin importar la app (tiempo total sin soltar el teléfono)
- [x] **Comparativa día a día** — "Hoy usaste 23% más que ayer"
- [x] **Tendencia semanal** — Gráfico de línea con los últimos 7 días
- [x] **"Momento más productivo"** — La franja horaria donde MENOS usaste el teléfono
- [x] **Categorización automática** — Agrupar apps por categoría (Redes Sociales, Productividad, Entretenimiento, etc.) usando la categoría de Google Play

### 2.2 Algoritmo para calcular sesiones

```
Pseudocódigo:

1. Filtrar eventos por packageName
2. Ordenar por timestamp ascendente
3. Para cada ACTIVITY_RESUMED:
   a. Buscar el siguiente ACTIVITY_PAUSED del mismo paquete
   b. duración = timestamp_paused - timestamp_resumed
   c. Guardar como Session(packageName, inicio, fin, duración)
4. La sesión con mayor duración = "Sesión más larga"
5. Suma de todas las duraciones = "Tiempo total"
6. Cantidad de sesiones = "Aperturas"
```

> **⚠️ Cuidado:** A veces Android no emite un `ACTIVITY_PAUSED` limpio (ej: si el sistema mata la app). Implementá un timeout máximo de sesión (ej: si no hay PAUSED en 4 horas, asumir que terminó cuando llegó otro RESUMED de otra app).

- [x] Crear `SessionCalculator` con tests unitarios
- [x] Crear `MetricsEngine` que consuma las sesiones y devuelva todas las métricas
- [x] Escribir tests para edge cases (sesiones superpuestas, falta de eventos PAUSED, cambio de día a medianoche)

---

## 📋 Fase 3 — Base de Datos Local (Room)

Para no recalcular todo cada vez y para tener historial.

### Entidades sugeridas

```kotlin
@Entity
data class DailyUsage(
    @PrimaryKey val date: String,           // "2026-09-25"
    val totalScreenTime: Long,              // milisegundos
    val totalUnlocks: Int,
    val longestSessionMs: Long,
    val longestSessionApp: String,
    val topAppPackage: String,
    val topAppTimeMs: Long
)

@Entity
data class AppDailyUsage(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val date: String,
    val packageName: String,
    val appName: String,
    val totalTimeMs: Long,
    val sessionCount: Int,
    val longestSessionMs: Long,
    val category: String?                   // "Social", "Entertainment", etc.
)
```

- [x] Crear las entidades Room
- [x] Crear los DAOs con queries útiles (últimos 7 días, top apps de la semana, etc.)
- [x] Crear la base de datos Room
- [x] Implementar un `Worker` (WorkManager) que al final del día guarde el resumen automáticamente
- [x] Implementar migración de esquema para futuras versiones

> **💡 Consejo:** Usá `WorkManager` con una tarea periódica de ~1 hora para ir guardando snapshots. Así si el usuario abre la app a las 11 PM, ya tiene datos parciales y no tiene que esperar que se procese todo de golpe.

---

## 📋 Fase 4 — Diseño de la UI (Jetpack Compose)

### 4.1 Pantallas principales

#### 🏠 Home / Dashboard
- Círculo animado con el tiempo total del día (estilo Apple Watch)
- Barra de progreso comparando con ayer ("20% menos que ayer 🎉")
- Top 3 apps con íconos, nombre y tiempo
- Tarjeta de "Sesión más larga" con emoji y texto motivacional/disuasivo
- Tarjeta de "Desbloqueos" del día

#### 📊 Detalle por App
- Al tocar una app del dashboard
- Gráfico de barras con uso por hora del día
- Historial de los últimos 7 días
- Métricas: tiempo total, sesiones, sesión más larga, promedio por sesión

#### 📅 Historial / Trends
- Gráfico de línea con los últimos 7/30 días
- Mejor día y peor día
- Promedio semanal

#### ⚙️ Settings
- Configurar objetivo diario de tiempo de pantalla
- Activar/desactivar notificaciones
- Tema oscuro/claro
- Exportar datos (CSV)

### 4.2 Diseño visual — Tips

- [x] Elegir una paleta de colores (recomendación: tonos oscuros con acentos neón/gradientes — tipo Spotify Wrapped)
- [x] Usar **Material 3** (`MaterialTheme` de Compose) como base
- [x] Implementar **tema oscuro desde el inicio** (los usuarios de este tipo de apps lo prefieren)
- [x] Usar animaciones de Compose (`animateFloatAsState`, `AnimatedVisibility`) para que las transiciones se sientan fluidas
- [x] Para los gráficos, evaluar **Vico** (Compose-native) antes que MPAndroidChart (basado en Views)

> **💡 Consejo:** Buscá inspiración en Dribbble con "screen time app UI" o "digital wellbeing dashboard". Hay diseños increíbles que podés adaptar.

---

## 📋 Fase 5 — Notificaciones y Funciones Extra

### Ideas de notificaciones útiles

- [x] "Llevás 2 horas seguidas en Instagram 👀" (alerta de sesión larga en tiempo real)
- [x] Resumen diario a las 22:00 ("Hoy usaste el teléfono 4h 32min")
- [x] "¡Nuevo récord! Tu sesión más larga fue de 3h en YouTube" (tono irónico/motivacional)
- [x] Alerta al superar el objetivo diario configurado

### Implementación

- [x] Usar `ForegroundService` liviano para monitoreo en tiempo real de sesiones
- [x] Usar `AlarmManager` o `WorkManager` para el resumen diario
- [x] Canal de notificación dedicado para que el usuario pueda silenciarlas sin perder otras

> **⚠️ Cuidado con la batería:** Un servicio en primer plano constante drena batería. Usá intervalos de verificación (cada 15-30 min) en vez de polling continuo. Documentá bien el consumo.

---

## 📋 Fase 6 — Testing y Pulido

- [x] **Tests unitarios** para `SessionCalculator`, `MetricsEngine` y los use cases
- [x] **Tests de integración** para los DAOs de Room
- [x] **Tests de UI** básicos con Compose Testing (`composeTestRule`)
- [x] Probar en dispositivos reales (los emuladores no siempre generan eventos de uso realistas)
- [x] Probar con distintas versiones de Android (API 26 a 35)
- [x] Revisar rendimiento con apps que generan muchos eventos (WhatsApp, Chrome)
- [x] Manejar edge cases: sin datos aún, primer día de uso, permisos revocados

---

## 📋 Fase 7 — Publicación en Google Play

### Requisitos previos

- [ ] Cuenta de desarrollador de Google Play ($25 USD, pago único)
- [ ] Generar signing key (keystore) y guardarla en lugar seguro
- [ ] Preparar assets gráficos:
  - Ícono de la app (512x512 PNG)
  - Feature graphic (1024x500)
  - Mínimo 2 screenshots por tipo de pantalla
- [ ] Escribir descripción optimizada para ASO (App Store Optimization)
- [ ] Crear política de privacidad (obligatoria si usás datos de uso)

### Sobre la política de datos

Google requiere que declares:
- **Qué datos recolectás:** Estadísticas de uso de apps (datos locales, no se envían a servidores)
- **Por qué:** Para mostrar al usuario su propio consumo de pantalla
- **Si se comparten:** No (todo local)

- [ ] Usar el formulario de Data Safety de Google Play Console
- [ ] Crear una página web sencilla con la política de privacidad (puede ser una GitHub Page gratis)

### Publicar

- [ ] Subir AAB (Android App Bundle) a Google Play Console
- [ ] Completar el cuestionario de clasificación de contenido
- [ ] Publicar primero en **testing interno** (para vos y amigos)
- [ ] Iterar con feedback real
- [ ] Publicar en **producción**

---

## 🗓️ Cronograma Estimado

| Fase | Duración estimada | Prioridad |
|:---|:---|:---|
| Fase 0 — Setup del proyecto | 1 día | 🔴 Crítica |
| Fase 1 — Permisos y lectura de datos | 2-3 días | 🔴 Crítica |
| Fase 2 — Motor de métricas | 3-4 días | 🔴 Crítica |
| Fase 3 — Base de datos Room | 2 días | 🟡 Alta |
| Fase 4 — UI con Compose | 5-7 días | 🟡 Alta |
| Fase 5 — Notificaciones y extras | 3-4 días | 🟢 Media |
| Fase 6 — Testing y pulido | 3-4 días | 🟡 Alta |
| Fase 7 — Publicación | 1-2 días | 🟢 Media |
| **Total estimado** | **~3-4 semanas** | |

> Esto asume dedicación parcial (~3-4 horas/día). Con dedicación full-time se reduce a ~2 semanas.

---

## 📚 Recursos de Aprendizaje

### Documentación oficial
- [UsageStatsManager](https://developer.android.com/reference/android/app/usage/UsageStatsManager)
- [UsageEvents](https://developer.android.com/reference/android/app/usage/UsageEvents)
- [Jetpack Compose](https://developer.android.com/jetpack/compose)
- [Room Database](https://developer.android.com/training/data-storage/room)
- [WorkManager](https://developer.android.com/topic/libraries/architecture/workmanager)

### Repos de referencia en GitHub
- Buscar: `"UsageStatsManager" android kotlin` en GitHub
- Buscar: `"screen time" android app` en GitHub
- Buscar: `"digital wellbeing" android open source`

### Apps para inspirarte
- **ActionDash** — La más completa en analytics de uso
- **StayFree** — Buena UI y funcionalidades sociales
- **YourHour** — Enfoque en reducción de uso con gamificación

---

## 💡 Consejos Finales

1. **Empezá por la Fase 1 y 2.** Si lográs leer datos y calcular métricas correctamente, el resto es "solo" UI y polish. La parte difícil es entender `UsageEvents`.

2. **No te obsesiones con el diseño al principio.** Primero hacé que funcione con textos planos. Después embellecé.

3. **Testeá en un dispositivo real desde el día 1.** El emulador no genera eventos de uso realistas.

4. **Guardá la keystore en un lugar seguro.** Si la perdés, no podés actualizar tu app en Google Play nunca más.

5. **La categorización de apps es un plus enorme.** Podés obtener la categoría de Google Play haciendo scraping o usando la API de `ApplicationInfo.category` (disponible desde API 26).

6. **Pensá en un nombre memorable.** Algo corto, que transmita la idea. Ejemplos: "ScreenPulse", "TimeLens", "PhoneGlass", "UseMeter".

7. **Monetización posible:**
   - Freemium: métricas básicas gratis, avanzadas con pago único
   - Sin ads intrusivos (tu audiencia odia eso)
   - Exportar a CSV/PDF como feature premium

---

> 📌 **Próximo paso concreto:** Abrir Android Studio, crear el proyecto, y lograr que la app lea y muestre en un `Text()` de Compose la lista de apps usadas hoy con su tiempo. Ese es tu "Hello World" para este proyecto.

---

*Plan creado el 25 de septiembre de 2026*
