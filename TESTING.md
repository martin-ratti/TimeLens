# 🧪 Guía y Documentación de Tests — TimeLens

Este documento detalla la infraestructura, herramientas, arquitectura y suites de pruebas unitarias implementadas en **TimeLens**, orientadas a una cobertura cercana al 100% de la lógica de negocio, arquitectura y componentes de presentación.

---

## 🛠️ Herramientas y Librerías Utilizadas

1. **JUnit 4 (`junit:junit:4.13.2`)**:
   - Framework estándar para la ejecución y aserciones de pruebas unitarias locales en la máquina virtual de Java (JVM).
2. **Kotlinx Coroutines Test (`kotlinx-coroutines-test:1.9.0`)**:
   - Soporte para pruebas de código asíncrono con corrutinas y StateFlows mediante `runTest`.
   - Control determinista del tiempo virtual y ejecución inmediata de tareas suspendidas.
   - Implementación de `MainDispatcherRule` con `TestDispatcher` para sustituir `Dispatchers.Main` en pruebas de ViewModels.
3. **MockK (`io.mockk:mockk:1.13.12`)**:
   - Biblioteca de mocking moderna e idiomática para Kotlin.
   - Permite mockear APIs concretas, métodos estáticos (`mockkStatic(Intent::class)`), objetos singleton de Kotlin (`mockkObject(NotificationWorkScheduler)`), y soporte para funciones suspendidas (`coEvery`, `coVerify`).
4. **Fakes de Arquitectura Limpia**:
   - `FakeUsageRepository`: Implementación en memoria de `UsageRepository` para probar ViewModels y UseCases de forma predecible sin tocar el hardware ni la base de datos.
   - `FakeSharedPreferences` y `FakePreferencesHelper`: Implementación en memoria de `SharedPreferences` para probar `UserPreferencesManager` sin depender del contexto de Android ni de llamadas al sistema.
5. **Configuración Gradle (`testOptions`)**:
   - `unitTests.isReturnDefaultValues = true`: Permite que llamadas al SDK de Android devuelvan valores por defecto seguros en lugar de lanzar excepciones `RuntimeException: Method ... not mocked`.

---

## 🚀 Cómo Ejecutar los Tests

### Ejecutar toda la suite de pruebas:
```bash
./gradlew testDebugUnitTest
```

### Ejecutar una suite específica:
```bash
./gradlew testDebugUnitTest --tests "com.timelens.app.domain.util.SessionCalculatorTest"
```

### Ver el reporte HTML detallado:
Abrir en el navegador:
```
app/build/reports/tests/testDebugUnitTest/index.html
```

---

## 📊 Resumen de Suites y Cobertura (66 Tests, 100% Aprobados)

| Suite de Test | Archivo | Tests | Descripción y Casos Cubiertos |
|:---|:---|:---:|:---|
| **SessionCalculatorTest** | `domain/util/SessionCalculatorTest.kt` | 8 | Algoritmo central de cálculo de sesiones de uso: sesiones que cruzan la medianoche (clamp a 00:00), apagado de dispositivo (`DEVICE_SHUTDOWN`), pausas en la misma app, descarte de anomalías (> 12h), cálculo de horario pico, cálculo de momento más productivo y matriz de uso por hora de una app. |
| **GetWellnessReportUseCaseTest** | `domain/usecase/GetWellnessReportUseCaseTest.kt` | 6 | Motor de diagnóstico y bienestar: 7 reglas de hábitos digitales (control de meta, desbloqueos compulsivos, sesiones maratónicas, predominio de redes sociales, uso nocturno tardío, comparación día previo y franja más productiva), con textos adaptables según apertura libre vs notificación nocturna de las 22hs. |
| **TimeFormatterTest** | `util/TimeFormatterTest.kt` | 10 | Formateo estricto de duraciones (horas, minutos, segundos), horas pico ("19:00 - 20:00"), formato 12h AM/PM, fechas relativas ("Hoy", "Ayer", días de semana) y protección ante valores cero o negativos. |
| **DomainUseCasesTest** | `domain/usecase/DomainUseCasesTest.kt` | 4 | Casos de uso de Clean Architecture: `GetDailySummaryUseCase`, `GetTopAppsUseCase`, `GetWeeklyTrendUseCase` y `CheckUsagePermissionUseCase` con repositorio simulado. |
| **DomainModelsTest** | `domain/model/DomainModelsTest.kt` | 5 | Consistencia y métodos de modelos de dominio: `AppCategory`, `Session`, `DaySummary`, `WellnessInsight` y `AppDetailInfo`. |
| **MappersTest** | `data/local/db/entity/MappersTest.kt` | 6 | Mapeo bidireccional entre entidades Room y modelos de dominio: `DailyUsageEntity` <-> `DaySummary`, `AppDailyUsageEntity` <-> `AppUsageInfo`, manejo de categorías inválidas y preservación de `durationMs`. |
| **UserPreferencesManagerTest** | `data/local/prefs/UserPreferencesManagerTest.kt` | 4 | Persistencia reactiva de preferencias de usuario: meta diaria en horas, switch de notificaciones, switch de tema oscuro y valores iniciales. |
| **HomeViewModelTest** | `presentation/screens/home/HomeViewModelTest.kt` | 4 | Estados de la pantalla principal: manejo de falta de permisos (`MissingPermission`), carga exitosa con cálculo de bienestar (`Success`), reactividad ante cambios de meta y manejo de errores (`Error`). |
| **HistoryViewModelTest** | `presentation/screens/history/HistoryViewModelTest.kt` | 2 | Pantalla de historial semanal: carga exitosa de la tendencia de 7 días y propagación de estado de error ante fallas. |
| **AppDetailViewModelTest** | `presentation/screens/detail/AppDetailViewModelTest.kt` | 2 | Pantalla de detalle de app: obtención de métricas mediante `SavedStateHandle` y fallback seguro ante argumentos faltantes. |
| **SettingsViewModelTest** | `presentation/screens/settings/SettingsViewModelTest.kt` | 9 | Pantalla de configuración: cambio de metas, tema oscuro, gestión de notificaciones (programación y cancelación con `WorkManager` y `UsageMonitorService`), pruebas manuales de notificación, resumen y chequeo de alertas, y generación de `Intent` para compartir. |
| **UsageRepositoryImplTest** | `data/repository/UsageRepositoryImplTest.kt` | 6 | Repositorio principal: delegación de permisos a `UsageDataSource`, consulta de resúmenes en Room, guardado de resúmenes diarios, purga automática de registros artificiales (`deleteArtificialRecords` / `deleteOrphanedRecords`) y garantía de no inventar días ficticios cuando Android no tiene eventos. |

---

## 🛡️ Principios y Patrones Aplicados

1. **Aislamiento Total**: No se requiere emulador ni dispositivo físico para ejecutar esta suite de 66 tests; corren en segundos directamente sobre la JVM local.
2. **Determinismo con Corrutinas**: Se evitan retrasos arbitrarios (`delay` / `sleep`); se utiliza `runTest` y `StandardTestDispatcher` para un avance de reloj controlado y seguro.
3. **Desacoplamiento de Framework**: Se desacopló la lógica de cálculo en `SessionCalculator` utilizando modelos intermedios (`UsageEventModel`), facilitando pruebas sin depender de la clase nativa `UsageEvents.Event`.
4. **Pruebas de Límites y Casos Extremos**: Se cubren expresamente anomalías reales observadas en Android (apagar el teléfono a mitad de sesión, eventos duplicados de actividad, sesiones maratónicas de más de medio día y cambios de fecha).
