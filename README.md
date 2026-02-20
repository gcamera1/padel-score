# Padel Score – Wear OS App

Aplicación para Wear OS que registra puntos, games y sets en partidos de pádel con interfaz optimizada para pantallas de 40mm.

## Stack

- **Kotlin** + **Jetpack Compose for Wear OS**
- **DataStore** para persistencia de estado
- **JUnit4** + **Truth** + **Coroutines Test** para testing
- Arquitectura simple: `presentation` (UI) → `logic` (PadelLogic) → `repository` (PadelRepository)

---

## Arquitectura

### Capas

1. **Presentation** (`MainActivity.kt`)
   - Composables UI (CounterScreen, SettingsScreen, NewMatchScreen)
   - Manejo de navegación entre pantallas
   - Gestión de haptic feedback y responsive design

2. **Logic** (`PadelLogic.kt`)
   - Funciones puras: `addPointToMy()`, `addPointToOpp()`, `subtractPointFrom*()`
   - Cálculos de scoring (games, sets, tie-break, golden point)
   - Sin side effects; input → output

3. **Repository** (`PadelDataStore.kt`)
   - `PadelRepository`: capa de persistencia con DataStore
   - `PadelState`: data class inmutable del estado del partido
   - `PadelRepository.stateFlow`: Flow reactivo para UI

### Módulos

```
app/src/main/java/com/gonzalocamera/padelcounter/
├── presentation/
│   ├── MainActivity.kt          # Activity y punto de entrada
│   ├── PadelDataStore.kt        # Repository y State
│   └── PadelLogic.kt            # Lógica de scoring
└── resources/
    └── values/
        └── strings.xml          # Recursos (español)

app/src/test/java/com/gonzalocamera/padelcounter/
├── presentation/
│   ├── PadelLogicTest.kt        # Tests de lógica de scoring
│   └── PadelRepositoryTest.kt   # Tests de persistencia
```

---

## Reglas de UX

### Scoring – Pantalla Principal

**Tap**
- Tap en zona **superior** → suma punto al rival
- Tap en zona **inferior** → suma punto a vos
- Feedback háptico: `TextHandleMove`

**Double Tap**
- Double tap en zona **superior** → resta punto al rival
- Double tap en zona **inferior** → resta punto a vos
- Feedback háptico: `LongPress`

**Highlight**
- Al presionar, la zona se ilumina con color translúcido (verde para vos, rojo para rival)

### Scoring – Reglas de Juego

**Game Normal (0/15/30/40)**
- Índices: 0 → "0" | 1 → "15" | 2 → "30" | 3 → "40" | 4 → "AD"

**Golden Point** (sin AD)
- En 40-40, el próximo punto gana el game inmediatamente
- Se configura al iniciar nuevo partido (default: activado)

**Deuce con AD** (cuando golden point desactivado)
- 40-40 → el siguiente punto pasa a 40-AD
- Si estás en AD y marcas → ganas game
- Si estabas en AD y el otro marca → vuelve a Deuce (40-40)

**Tie-Break** (desempate)
- Se activa **solo** cuando games llegan a 6-6
- Puntuación continua (0, 1, 2, 3…)
- Target: **7 puntos** (TB7, default) o **10 puntos** (SUPER10, configurable)
- Diferencia mínima: 2 puntos para ganar

**Set**
- Gana quien primero llegue a 6 games con diferencia de 2
- Excepción: 7-6 en tie-break

**Swipe**
- Swipe horizontal en contador → abre Ajustes

### Ajustes

**Pantalla siempre encendida**
- Toggle simple: Activado/Desactivado
- Aplica `FLAG_KEEP_SCREEN_ON` en tiempo real

**Color de cancha**
- Opciones: Verde, Naranja, Violeta, Azul
- Swipe o tap para cambiar color
- Indicador visual (puntos blancos) mostrando opción activa
- Color seleccionado persiste entre partidos

**Nuevo Partido**
- Elige desempate: TB7 (a 7) o SUPER10 (a 10)
- Elige golden point: Activado/Desactivado
- Reinicia puntos, games, sets
- Mantiene preferencias de pantalla y color

### Responsive Design

**Pantallas 40mm** (prioridad)
- Cero padding adicional para maximizar área visible
- Cancha ocupa ~88% ancho × 82% alto
- Texto ajustado para legibilidad

**Pantallas mayores** (40–46mm)
- Padding y espaciado aumentan levemente
- Tamaños de fuente escalados

---

## Convenciones de Código

- **Nombres UI**: español (ej: "Padel Score", "Nuevo partido")
- **Código**: inglés (funciones, variables)
- **Funciones puras**: preferidas en lógica; evitar side effects
- **Commits**: [Conventional Commits](https://www.conventionalcommits.org/)
  - Ejemplo: `feat(wear): add golden point toggle`

---

## Testing

### Suite de Tests

#### `PadelLogicTest.kt`
- **Normal game scoring**: progresión 0 → 15 → 30 → 40 → win
- **Golden Point**: ganar inmediatamente en 40-40
- **Deuce con AD**: cambios entre Deuce ↔ AD, win desde AD
- **Set win**: lógica de 6-0, 6-4, 7-5, etc.
- **Tie-break**: entrada en 6-6, scoring, win con TB7 y SUPER10
- **Subtract points**: decrementos y edge cases
- **Complex scenarios**: secuencias largas de juego

#### `PadelRepositoryTest.kt`
- **Persistencia**: guardar y cargar estado
- **Preferencias**: keep screen on, court color, golden point, decider
- **Reset match**: reinicia scores pero preserva preferences

### Ejecutar Tests

```bash
# Ejecutar TODOS los tests
./gradlew test

# Ejecutar tests de lógica únicamente
./gradlew test --tests "*PadelLogicTest"

# Ejecutar tests de repositorio únicamente
./gradlew test --tests "*PadelRepositoryTest"

# Ver reporte detallado
./gradlew test --info
```

### Resultado esperado

Todos los tests deben pasar (54 tests en total):
- 45 en `PadelLogicTest`
- 9 en `PadelRepositoryTest`

---

## Build & Run

```bash
# Build debug APK
./gradlew assembleDebug

# Instalar en dispositivo/emulador
adb install -r app/build/outputs/apk/debug/app-debug.apk

# Build & install + ejecutar app
./gradlew installDebug
adb shell am start -n com.gonzalocamera.padelcounter/com.gonzalocamera.padelcounter.presentation.MainActivity
```

---

## Licencia

MIT
