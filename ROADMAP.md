# 🗺️ Roadmap — TimeLens

> Plan de desarrollo por fases, con tareas específicas y cronograma estimado.

---

## 📊 Progreso General

| Fase | Estado | Progreso |
|:---|:---|:---|
| Fase 0 — Preparación | 🟢 Completada | ▓▓▓▓▓▓▓▓▓▓ 100% |
| Fase 1 — Permisos y Datos | 🟢 Completada | ▓▓▓▓▓▓▓▓▓▓ 100% |
| Fase 2 — Motor de Métricas | 🟢 Completada | ▓▓▓▓▓▓▓▓▓▓ 100% |
| Fase 3 — Base de Datos Room | 🟢 Completada | ▓▓▓▓▓▓▓▓▓▓ 100% |
| Fase 4 — UI con Compose | 🟢 Completada | ▓▓▓▓▓▓▓▓▓▓ 100% |
| Fase 5 — Notificaciones | 🟢 Completada | ▓▓▓▓▓▓▓▓▓▓ 100% |
| Fase 6 — Testing y Pulido | 🟢 Avanzada | ▓▓▓▓▓▓▓░░░ 70% |
| Fase 7 — Publicación | ⬜ Pendiente | ░░░░░░░░░░ 0% |

---

## 📋 Fase 0 — Preparación y Setup (~1 día)

🔴 **Prioridad: Crítica**

- [x] Crear repositorio en GitHub
- [x] Crear documentación inicial (README, SETUP, ARCHITECTURE, etc.)
- [x] Instalar Android Studio
- [x] Crear proyecto con Empty Compose Activity
- [x] Configurar `minSdk = 26`, `targetSdk = 35`
- [x] Agregar dependencias iniciales (Compose, Room, Hilt, Navigation, Vico)
- [x] Crear estructura de carpetas del proyecto
- [x] Configurar Hilt en la Application class
- [x] Primer commit con el proyecto base

---

## 📋 Fase 1 — Permisos y Lectura de Datos (~2-3 días)

🔴 **Prioridad: Crítica**

### 1.1 Permisos
- [x] Agregar `PACKAGE_USAGE_STATS` al AndroidManifest.xml
- [x] Agregar `QUERY_ALL_PACKAGES` para compatibilidad completa en Android 11+
- [x] Crear pantalla de onboarding que explique el permiso
- [x] Implementar redirección a Ajustes del sistema
- [x] Verificar si el permiso fue concedido al volver a la app
- [x] Manejar caso sin permiso (pantalla amigable)

### 1.2 Lectura de datos
- [x] Crear `UsageDataSource` — wrapper de `UsageStatsManager`
- [x] Implementar `queryUsageStats()` para estadísticas diarias
- [x] Implementar `queryEvents()` para eventos granulares
- [x] Resolver nombres de apps con `PackageManager.getApplicationLabel()` y nombres limpios
- [x] Resolver íconos de apps con `PackageManager.getApplicationIcon()`
- [x] Cachear nombres e íconos para no repetir consultas
- [x] Filtro de aplicaciones elegibles (exclusión de Launcher y System UI)

### 🎯 Entregable
> La app muestra en un `Text()` la lista de apps usadas hoy con su tiempo.

---

## 📋 Fase 2 — Motor de Métricas (~3-4 días)

🔴 **Prioridad: Crítica**

### Nivel Básico
- [x] Tiempo total de pantalla del día
- [x] Top 5 apps más usadas
- [x] Cantidad de desbloqueos

### Nivel Intermedio
- [x] Sesión continua más larga por app
- [x] Cantidad de aperturas por app (máquina de estados de línea de tiempo)
- [x] Tiempo promedio por sesión
- [x] Horario pico (franja horaria con más actividad)

### Nivel Avanzado
- [x] Sesión continua más larga real del día
- [x] Comparativa día a día ("Hoy -15% vs ayer")
- [x] Tendencia semanal (últimos 7 días con gráfico dinámico)
- [ ] "Momento más productivo" — franja con menos uso
- [x] Categorización automática de apps (Social, Entretenimiento, Productividad, Juegos, etc.)

### Testing del motor
- [x] Crear `SessionCalculator` con máquina de estados precisa
- [x] Tests unitarios iniciales para `SessionCalculator`
- [ ] Tests adicionales para edge cases (sesiones cruzando medianoche, múltiples reinicios)

### 🎯 Entregable
> Todas las métricas calculándose correctamente con tests que lo demuestren.

---

## 📋 Fase 3 — Base de Datos Room (~2 días)

🟡 **Prioridad: Alta**

- [x] Crear entidades Room (`DailyUsage`, `AppDailyUsage`)
- [x] Crear DAOs con queries útiles (`DailyUsageDao`, `AppDailyUsageDao`)
- [x] Crear `TimeLensDatabase`
- [x] Implementar `DailySyncWorker` (WorkManager) para sincronizar en segundo plano
- [x] Tarea periódica de resumen persistida cada 12h
- [ ] Implementar migración de esquema para futuras versiones

### 🎯 Entregable
> Datos históricos persistidos, consultables por rango de fechas.

---

## 📋 Fase 4 — UI con Jetpack Compose (~5-7 días)

🟡 **Prioridad: Alta**

### Pantallas
- [x] **Home / Dashboard**
  - Círculo animado con tiempo total y objetivo sincronizado
  - Barra comparativa dinámica con ayer
  - Desglose horizontal por Categorías de uso
  - Apps más usadas con íconos, aperturas, categoría y barra de progreso proporcional
  - Tarjeta sesión más larga con nombre limpio
  - Tarjeta desbloqueos precisa
  - Tarjeta horario pico con formato limpio
- [x] **Detalle por App (`AppDetailScreen`)**
  - Header con ícono grande, nombre limpio y badge de categoría
  - 4 KPI cards: Tiempo hoy, aperturas, sesión máx. y promedio por sesión
  - Gráfico de barras por hora para la app seleccionada (24 horas)
  - Historial de uso de esa app (últimos 7 días)
- [x] **Historial / Trends**
  - Gráfico de barras semanal con Compose Canvas nativo e interactividad táctil
  - Mejor y peor día calculados con badges
  - Promedio semanal, total acumulado y desbloqueos
  - Desglose día por día con aplicación líder y barra proporcional
  - Lista scrolleable completa con `LazyColumn`
- [x] **Settings**
  - Objetivo diario con selector interactivo
  - Notificaciones on/off
  - Tema dinámico con cambio reactivo (Modo Oscuro Neón y Modo Claro)
  - Compartir TimeLens con selector nativo de Android
  - Diálogo Acerca de interactivo con misión, privacidad, autoría y repositorio

### Diseño visual
- [x] Definir paleta de colores (Modo Oscuro Neón y Modo Claro)
- [x] Configurar Material 3 con colorScheme dinámico
- [x] Implementar temas reactivos con DataStore Preferences
- [x] Animaciones fluidas con `animateFloatAsState`
- [x] Componentes nativos de gráficos en Canvas (`WeeklyBarChart` y `HourlyBarChart`)

### Navegación
- [x] Configurar Navigation Compose
- [x] Bottom navigation entre pantallas principales (Inicio, Tendencias, Ajustes)
- [x] Navegación con argumentos a pantalla de detalle (`AppDetailScreen`)

### 🎯 Entregable
> Todas las pantallas funcionales con datos reales, gráficos nativos y soporte para temas claro/oscuro.

---

## 📋 Fase 5 — Notificaciones y Extras (~3-4 días)

🟢 **Prioridad: Media**

- [x] Canal de notificación dedicado (`NotificationChannel`: Alertas de bienestar, Resumen nocturno y Monitoreo)
- [x] Alerta de sesión larga en tiempo real ("Llevás 1h en Instagram")
- [x] Resumen diario a las 22:00 (`DailySummaryWorker`)
- [x] Notificación de récord ("Nueva sesión más larga")
- [x] Alerta al superar objetivo diario
- [x] ForegroundService liviano para monitoreo (`UsageMonitorService`)
- [x] Gestión de permisos en tiempo de ejecución (`POST_NOTIFICATIONS` en Android 13+)
- [x] Panel de prueba y control interactivo en pantalla de Ajustes
- [x] Tarjeta interactiva de "Resumen de Bienestar" (`WellnessSummaryCard`) en pantalla de inicio con diagnóstico en tiempo real y consejos prácticos
- [x] ModalBottomSheet interactivo de Cierre del Día (`DailyDebriefBottomSheet`) con score (0-100), hábitos destacados y tips de desconexión nocturna
- [x] Navegación profunda automática al Cierre del Día al tocar la notificación nocturna de bienestar

### 🎯 Entregable
> Notificaciones inteligentes funcionando sin drenar batería, tarjeta de diagnóstico en inicio y cierre reflexivo diario en ModalBottomSheet con navegación profunda.

---

## 📋 Fase 6 — Testing y Pulido (~3-4 días)

🟡 **Prioridad: Alta**

- [x] Tests unitarios completos (SessionCalculator, DailySummaryCalculator, TimeFormatter)
- [x] Corrección de discrepancias en cálculo de tiempo diario y eventos superpuestos
- [x] Optimización de rendimiento con caché en memoria en `UsageDataSource` y renderizado de ImageBitmap
- [x] Pruebas y validación en dispositivo físico real con datos reales
- [x] Unificación estética y responsiva de tarjetas KPI en todas las pantallas
- [x] Diagnóstico y benchmark de tasa de refresco (60/120 Hz) con `dumpsys gfxinfo`
- [ ] Tests de integración para DAOs con base de datos en memoria
- [ ] Tests de UI básicos con Compose Testing
- [ ] Probar en múltiples versiones de Android (API 26 a API 35)
- [ ] Pulir transiciones avanzadas y accesibilidad adicional

### 🎯 Entregable
> App estable, probada y pulida, lista para publicar.

---

## 📋 Fase 7 — Publicación (~1-2 días)

🟢 **Prioridad: Media**

- [ ] Crear cuenta de Google Play Developer ($25 USD)
- [ ] Generar signing key (keystore) — ¡GUARDARLA EN LUGAR SEGURO!
- [ ] Preparar ícono de la app (512x512 PNG)
- [ ] Preparar feature graphic (1024x500)
- [ ] Tomar mínimo 4 screenshots
- [ ] Escribir descripción optimizada (ASO)
- [ ] Crear política de privacidad (GitHub Pages)
- [ ] Completar formulario Data Safety
- [ ] Subir AAB a Google Play Console
- [ ] Publicar en testing interno
- [ ] Recopilar feedback → iterar
- [ ] Publicar en producción

### 🎯 Entregable
> App publicada en Google Play Store.

---

## 🗓️ Cronograma Estimado

| Fase | Duración | Acumulado |
|:---|:---|:---|
| Fase 0 | 1 día | Día 1 |
| Fase 1 | 2-3 días | Día 3-4 |
| Fase 2 | 3-4 días | Día 7-8 |
| Fase 3 | 2 días | Día 9-10 |
| Fase 4 | 5-7 días | Día 15-17 |
| Fase 5 | 3-4 días | Día 19-21 |
| Fase 6 | 3-4 días | Día 23-25 |
| Fase 7 | 1-2 días | Día 25-27 |

> 📌 **Total: ~3-4 semanas** (con dedicación de 3-4 horas/día). Con full-time se reduce a ~2 semanas.

---

*Documento actualizado: 30 de septiembre de 2026*
