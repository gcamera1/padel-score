# Plan de migración de toolchain — AGP / Kotlin 2.x / Compose BOM / Paparazzi 2.x

Escrito el **30/08/2026**, con las matrices de compatibilidad vigentes a esa fecha.
**Antes de empezar, ejecutar el Paso 0.1 (re-verificación de versiones):** si pasaron meses,
las versiones destino de este plan pueden haber quedado viejas — el *método* sigue valiendo.

## Qué es y qué no es

- **No trae nada al usuario.** Ninguna feature, ningún fix. Es correctitud de configuración
  (`compileSdk 36`), sacarse de encima un BOM de junio 2024, y llegar en frío al ciclo de
  requisitos de Play de agosto 2027.
- **No desbloquea nada hoy.** Play evalúa el `targetSdk` del manifest y ya se cumple
  (mobile 36 / wear 35).
- **El riesgo está concentrado en un punto conocido:** lo que se rompe con estos bumps es la
  UI y los screenshot tests *al mismo tiempo* — la red de seguridad se cae junto con lo que
  hay que verificar. Todo el diseño del plan sale de ahí: **nunca mover dos cosas que puedan
  romper el render en la misma etapa**, así cada diff de snapshot tiene una sola causa posible.

## Estado actual → destino

| Componente | Hoy | Destino (verificar en Paso 0.1) | Notas |
|---|---|---|---|
| AGP | 8.5.2 | **8.13.x** (última 8.x) | **NO ir a 9.x**: AGP 9 integra Kotlin al plugin, elimina `org.jetbrains.kotlin.android` y cambia comportamiento de build. Es otra migración, para otro día. |
| Kotlin | 1.9.24 | **2.3.0** | Es la versión con la que se compiló Paparazzi 2.0.0-alpha04 — combo probado. No perseguir la última (2.4.x) si Paparazzi no se compiló contra ella. |
| Compose Compiler | 1.5.14 (plugin viejo) | plugin `org.jetbrains.kotlin.plugin.compose`, **misma versión que Kotlin** | Desaparece `composeOptions { kotlinCompilerExtensionVersion }` en `:mobile` y `:wear`. |
| KSP | 1.9.24-1.0.20 | la que corresponda a Kotlin 2.3.0 | Solo lo usa Room en `:mobile`. |
| Compose BOM | 2024.06.00 | **2026.08.00** (o última estable) | Acá viven los cambios visuales de Material3. |
| material3 (pin) | 1.2.1 pinneado en el toml | **quitar el pin**, que lo maneje el BOM | ⚠️ Hoy el pin coincide con el BOM; con el BOM nuevo un pin viejo mezcla versiones de Compose → crashes tipo `NoSuchMethodError` en runtime. |
| material3Adaptive | 1.0.0 pinneado | BOM-managed si el BOM 2026 lo incluye; si no, última estable | Verificar en el [BOM mapping](https://developer.android.com/develop/ui/compose/bom/bom-mapping). |
| Wear Compose | 1.4.1 | **1.6.x** (seguir en `compose-material` clásico) | **NO migrar a Wear Material3**: es un redesign, fuera de alcance. |
| Paparazzi | 1.3.4 | **2.0.0-alpha05** (o estable si ya salió 🤞) | Único camino a `compileSdk 36` (soporte desde alpha02, LayoutLib 15.2.3). Sigue en alpha a ago/2026: pinnear versión exacta. |
| Gradle | 9.2.1 | quedarse | Ya al día. |
| JDK daemon | 21 (`gradle-daemon-jvm.properties`) | quedarse | Ya cumple el requisito de Java 21+ de Paparazzi 2.0.0-alpha04+. Nada que hacer. |
| compileSdk | 35 / 35 | **36 / 36** | Recién al final, cuando Paparazzi 2.x ya esté adentro. `targetSdk` NO se toca (36 mobile / 35 wear). |
| Room | 2.6.1 | última estable (2.8.x) | Con KSP nuevo conviene Room al día (soporte KSP2 maduro). Hay tests de migración (`room-testing`) que cubren. |
| Kotest | 5.8.0 | **no tocar** salvo que rompa | Es librería consumidora, corre bien sobre Kotlin 2.x. Kotest 6 tiene breaking changes propios — migración aparte si algún día hace falta. |

### La matriz que dicta el orden (de los releases de Paparazzi)

| Paparazzi | Compilado contra | Requiere |
|---|---|---|
| 1.3.4 (actual) | AGP 8.3.2 · Kotlin 1.9.24 · LayoutLib Iguana | — |
| **1.3.5** | AGP 8.4.2 · **Kotlin 2.0.21** · LayoutLib Jellyfish | — |
| 2.0.0-alpha02 | AGP 8.10.1 · Kotlin 2.1.21 · LayoutLib 15.2.3 | primero con **API 36** |
| **2.0.0-alpha04** | **AGP 8.13.2 · Kotlin 2.3.0** · LayoutLib 16.1.1 | **Java 21+** |
| **2.0.0-alpha05** | LayoutLib 16.2.1 | soporta consumidores pre-AGP 9 |

De acá sale la estrategia: cada etapa aterriza en una fila de esta tabla, nunca en el medio.

## Reglas del juego

1. **Branch propio**: `chore/toolchain-migration` (mismo patrón que `chore/target-sdk-bump`).
2. **Cada etapa termina verde y commiteada.** Verde = `./gradlew build` pasa (incluye las 3
   suites) + snapshots verificados o regrabados y revisados. Si una etapa se complica, se
   revierte SOLO esa etapa (`git revert`) — por eso no se mezclan.
3. **Un solo motivo de diff visual por etapa.** Las regrabaciones de snapshots están ubicadas
   para que cada una tenga una única explicación posible (LayoutLib / Material3 / Wear Compose).
4. **No aprovechar el viaje.** Nada de refactors, nada de features, nada de "ya que estoy".
   Deprecation warnings se anotan en `docs/pendientes.md`, no se arreglan acá (salvo que
   bloqueen la compilación).
5. **Los snapshots regrabados se commitean junto con el bump que los movió**, nunca en un
   commit aparte — así `git log` de cada PNG explica por qué cambió.

## Verificación por etapa (el "harness")

```bash
# La verificación completa de cada etapa:
./gradlew build                        # compila todo + :shared/:mobile/:wear test
./gradlew verifyPaparazziDebug         # snapshots contra los goldens actuales

# Si verify falla y el diff es esperado (ver la etapa), regrabar y revisar:
./gradlew recordPaparazziDebug
git status --short   # qué PNGs cambiaron
```

Para revisar los PNGs cambiados de a pares (antes/después) sin abrirlos uno por uno:

```bash
# genera /tmp/snapshot-review.html con cada golden cambiado lado a lado (HEAD vs working tree)
mkdir -p /tmp/snap-old && rm -f /tmp/snap-old/*.png /tmp/snapshot-review.html
i=0; for f in $(git diff --name-only -- '*snapshots*images*png' '*snapshots*png'); do
  git show "HEAD:$f" > "/tmp/snap-old/$i.png" 2>/dev/null || continue
  printf '<div style="margin:24px 0"><p style="font-family:monospace">%s</p><img src="/tmp/snap-old/%s.png" style="max-width:45%%;border:1px solid #c00"><img src="%s" style="max-width:45%%;border:1px solid #0a0"></div>\n' "$f" "$i" "$(pwd)/$f" >> /tmp/snapshot-review.html
  i=$((i+1))
done; open /tmp/snapshot-review.html
```

Criterio de revisión: **antialiasing/kerning/sub-pixel = OK; cualquier cambio de layout,
tamaño, wrap de texto o color = investigar antes de aceptar.** Especial atención a los
snapshots de wear: el wrap de texto con fuente grande es literalmente lo que causó dos
rechazos de Play (WO-V1).

---

## Paso 0 — Preparación (sin tocar nada)

**0.1 Re-verificar versiones del día.** Este plan se escribió en agosto 2026; confirmar:

- Última AGP 8.x: <https://developer.android.com/build/releases/gradle-plugin> — quedarse en
  la serie 8.x aunque 9.x sea la default de la página.
- Paparazzi: <https://github.com/cashapp/paparazzi/releases> — **si ya hay 2.x estable, usar
  esa** y verificar contra qué AGP/Kotlin se compiló (está siempre en las release notes);
  esas dos versiones pasan a ser el destino de la Etapa 2, reemplazando a las de este plan.
- Compose BOM estable: <https://developer.android.com/develop/ui/compose/bom/bom-mapping>
- Wear Compose estable: <https://developer.android.com/jetpack/androidx/releases/wear-compose>
- KSP para el Kotlin elegido: <https://github.com/google/ksp/releases>

**0.2 Baseline.** Sobre `main` limpio:

```bash
./gradlew build verifyPaparazziDebug    # debe estar todo verde ANTES de empezar
git checkout -b chore/toolchain-migration
```

Si el baseline no está verde, arreglar eso primero, en `main`, como trabajo aparte.

**0.3 Anotar el estado del reloj y teléfono de prueba** (qué versión tienen instalada), para
el smoke test de la Etapa 5.

---

## Etapa 1 — Kotlin 2.0.21 + plugin de Compose + Paparazzi 1.3.5

El salto conceptual grande (el plugin de Compose nuevo) con el salto de versiones más chico
posible. AGP **se queda** en 8.5.2. Combo destino = la fila de Paparazzi 1.3.5.

En `gradle/libs.versions.toml`:

```toml
kotlin = "2.0.21"
ksp = "2.0.21-1.0.28"        # verificar el sufijo exacto en los releases de KSP
paparazzi = "1.3.5"
# composeCompiler = "1.5.14"  ← ELIMINAR la entrada
[plugins]
kotlin-compose = { id = "org.jetbrains.kotlin.plugin.compose", version.ref = "kotlin" }
```

En el `build.gradle.kts` raíz: agregar `alias(libs.plugins.kotlin.compose) apply false`.

En `:mobile` y `:wear`:

- agregar `alias(libs.plugins.kotlin.compose)` a `plugins {}`
- **eliminar el bloque `composeOptions { kotlinCompilerExtensionVersion = ... }`** entero
- `buildFeatures { compose = true }` se queda
- opcional pero recomendado (el `kotlinOptions` viejo queda deprecado en Kotlin 2.x):

```kotlin
kotlin { compilerOptions { jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17) } }
```

`:shared` no necesita nada (el plugin `kotlin-jvm` y `kotlin-serialization` toman la versión
nueva solos vía el catálogo).

**Verificar** con el harness. Snapshots: Paparazzi 1.3.5 cambia LayoutLib (Iguana→Jellyfish),
así que es probable que `verify` falle con diffs de render mínimos → **regrabación #1**,
diffs esperados: solo antialiasing/texto sub-pixel. Si un layout se movió acá, algo anda mal
(Kotlin no debería mover UI): investigar antes de seguir.

Dónde puede doler: Kotest 5.8 y Turbine compilados contra Kotlin viejo — deberían correr
igual (compat hacia adelante de librerías); si Kotest rompe, probar 5.9.1 antes de considerar
la 6.x.

**Commit:** `chore: Kotlin 2.0.21 + plugin de Compose + Paparazzi 1.3.5`

---

## Etapa 2 — AGP 8.13.x + Kotlin 2.3.0 + Paparazzi 2.0.0-alpha05

Los tres juntos **a propósito**: es la matriz contra la que se compiló Paparazzi
2.0.0-alpha04/05 (AGP 8.13.2 · Kotlin 2.3.0 · Java 21, que el daemon ya tiene). Un
intermedio "AGP nuevo con Paparazzi 1.3.5" sería una combinación que nadie probó — más
riesgosa que el lockstep.

```toml
agp = "8.13.2"          # o la última 8.13.x del día
kotlin = "2.3.0"
ksp = "2.3.0-..."       # la correspondiente
paparazzi = "2.0.0-alpha05"
```

Y en `gradle.properties`: **eliminar `android.suppressUnsupportedCompileSdk=35,36`** — AGP
8.13 soporta API 36 nativamente y el warning ya no existe.

**Verificar** con el harness. LayoutLib salta a 16.x → **regrabación #2**, diffs esperados:
de render fino otra vez (fuentes, antialiasing). Mismo criterio: layouts que se mueven acá
son señal de alarma, la UI todavía no cambió de versión.

Dónde puede doler:

- **R8 nuevo** (viene con AGP): el build `release` minificado puede romper distinto. No
  esperar a la Etapa 5: correr `./gradlew :mobile:bundleRelease :wear:bundleRelease` ya en
  esta etapa y ver que compile. El smoke en dispositivo del release va en la Etapa 5.
- Paparazzi alpha: si alpha05 tiene algún bug bloqueante con este proyecto, probar alpha04
  (pide AGP 8.13.2 exacto). Si ninguna alpha funciona → **plan B** al final del documento.
- Deprecations de AGP entre 8.5 y 8.13: suelen ser warnings, no errores. Anotarlas, no
  arreglarlas acá.

**Commit:** `chore: AGP 8.13 + Kotlin 2.3 + Paparazzi 2.0.0-alpha05`

---

## Etapa 3a — Compose BOM + AndroidX de `:mobile`

Recién ahora se mueve la UI de verdad. Solo `:mobile` (+ `:shared` indirecto); Wear va aparte.

```toml
composeBom = "2026.08.00"          # o la última estable
# material3 = "1.2.1"              ← ELIMINAR: pasa a manejarlo el BOM (⚠ crítico)
# material3Adaptive: BOM-managed si el BOM lo incluye; si no, última estable
activityCompose = "última"
lifecycleRuntimeKtx = "última"
navigationCompose = "última 2.x"   # NO saltar a Navigation 3: otra migración
room = "última" ; roomTesting = idem
datastorePreferences = "última"
coreKtx = "última"
kotlinxSerialization = "última"    # los tests de MatchCodec cubren la compat del formato
kotlinxCoroutines = "última" ; coroutinesTest = idem
```

En `mobile/build.gradle.kts`: quitar `version.ref` de `compose-material3` en el toml para que
la resuelva el BOM (revisar también `material-icons-extended`, que ya es BOM-managed).

**Verificar** con el harness, más:

- `:mobile:test` completo — Room bumpeado: los tests de migración 3→4 tienen que seguir verdes.
- **Regrabación #3** — acá SÍ se esperan diffs visibles: dos años de Material3 (métricas de
  componentes, ripples, tipografía). Revisar los ~20 snapshots de `:mobile` uno por uno con
  el HTML. Aceptar solo lo que sea "Material3 se ve así ahora"; cualquier layout roto
  (overflow, clipping, wrap) se arregla en el código de la app en esta misma etapa.
- App instalada en el teléfono (`:mobile:installDebug`) y pasada rápida por las 4 tabs +
  detalle de partido + settings + el modal de calificación (`Settings > ...` no tiene
  trigger manual; con verlo en el snapshot alcanza).

Dónde puede doler: deprecations/removals de M3 después de 2 años (APIs experimentales que
cambiaron de firma — `ListDetailPaneScaffold`/adaptive es candidato número uno). El advisory
de `androidx.fragment` de la consola debería morir acá solo.

**Commit:** `chore(mobile): Compose BOM 2026.08 + AndroidX al día`

---

## Etapa 3b — Wear Compose 1.6.x

Separado de 3a porque es la superficie con antecedentes de rechazo de Play, y conviene que
sus diffs de snapshot no compitan con los de Material3.

```toml
wearCompose = "1.6.x"     # seguir en compose-material clásico; NADA de wear material3
wearRemoteInteractions = "última"
playServicesWearable = "última"
```

**Verificar** con el harness, más:

- **Regrabación #4** — los 6 snapshots de wear. Mirar con lupa `WideTextButton`
  (`Chip`/`OutlinedChip`: que el alto siga creciendo con la fuente y el padding se respete).
- ⚠️ **Los snapshots NO alcanzan para wear**: las pantallas con `ScalingLazyColumn` renderizan
  vacías en Paparazzi (limitación conocida, `docs/pendientes.md` §4). Repetir el
  procedimiento que destrabó los rechazos: **emulador de 192dp, `font_scale 1.30`**, recorrer
  walkthrough completo, ajustes, partido nuevo, fin de partido y companion prompt, buscando
  texto cortado. El procedimiento exacto está en `docs/pendientes.md` (sección del segundo
  rechazo) y `docs/publishing-guide.md` §8.
- Partido corto en el emulador de wear para el flujo de sync (la cola se inspecciona con
  `run-as ... cat files/datastore/wear_sync_queue.preferences_pb`, ver pendientes.md).

Dónde puede doler: cambios de comportamiento/espaciado de `ScalingLazyColumn` y `Chip` entre
1.4 y 1.6 — exactamente lo que no se ve en snapshots. Por eso el paso de emulador no es
opcional.

**Commit:** `chore(wear): Wear Compose 1.6`

---

## Etapa 4 — `compileSdk 36`

Lo que todo esto vino a habilitar. En `mobile/build.gradle.kts` y `wear/build.gradle.kts`:

```kotlin
compileSdk = 36
```

- Borrar el comentario largo sobre Paparazzi/compileSdk 35 en `mobile/build.gradle.kts`
  (quedó obsoleto) y actualizar la mención en `CLAUDE.md` y `docs/pendientes.md` §3.
- `targetSdk` NO se toca: 36 mobile / 35 wear (el requisito de Wear OS es 35 y no hay razón
  para adelantarse).
- `minSdk` NO se toca (26/30).

**Verificar** con el harness completo. `verifyPaparazzi` debería pasar **sin regrabar**:
compilar contra 36 no cambia el render. Si pide regrabar, entender por qué antes de aceptar.
Compilar también ambos `bundleRelease`.

**Commit:** `chore: compileSdk 36 en :mobile y :wear`

---

## Etapa 5 — Verificación final y cierre

1. `./gradlew clean build verifyPaparazziDebug` — desde cero, sin caches.
2. `./gradlew :mobile:bundleRelease :wear:bundleRelease` y sobre los AABs:
   - `bundletool dump manifest` → `targetSdk` 36/35, versionCode/Name correctos.
   - Firma = la de siempre (`5A:39:B2:…:5E:BB`).
3. **Smoke del build minificado en hardware** (el riesgo R8 no se ve en debug): instalar el
   release en el teléfono (universal APK vía bundletool con la firma release) y jugar un
   partido sincronizado reloj→teléfono de punta a punta. Room, DataStore, serialization y
   DataClient pasan todos por reglas de ProGuard — esta pasada los cubre de una.
4. Merge a `main`.
5. **Publicar la migración sola**, sin mezclarla con features (misma lógica que las releases
   anteriores): si algo aparece en producción, se sabe que es del toolchain. Versionado según
   el esquema de `gradle.properties` (prefijo del versionCode = targetSdk, que no cambió).
   Checklist de `docs/publishing-guide.md` §9. No hace falta apuro: puede esperar en `main`
   hasta la próxima ventana tranquila.
6. Actualizar `docs/pendientes.md` §3 (marcar hecha) y el estado del toolchain.

---

## Riesgos y planes B

| Riesgo | Señal | Plan B |
|---|---|---|
| Paparazzi 2.x alpha rota con este proyecto | falla `record`/`verify` en Etapa 2 con las alphas probadas (05 y 04) | Parar la Etapa 2: quedarse en el estado post-Etapa 1 (Kotlin 2 + 1.3.5, que ya es medio camino), hacer 3a/3b igual (no dependen de Paparazzi 2), y dejar `compileSdk 36` para cuando salga una versión que funcione. `compileSdk 35` no incumple nada. |
| R8 nuevo rompe el release | crash del build minificado en el smoke de Etapa 5 | Los stacktraces minificados se leen con el mapping de `mobile/build/outputs/mapping/release/`. Lo típico: falta una regla keep para serialization/Room. Se arregla en `proguard-rules.pro`, no bajando versiones. |
| M3 nuevo rompe un layout de mobile | regrabación #3 con overflow/clipping | Arreglar el composable en la misma etapa. Si es masivo, evaluar quedarse un BOM intermedio (p.ej. 2025.xx) y subir en dos pasos. |
| Wear 1.6 cambia métricas de Chip/ScalingLazyColumn | regrabación #4 o el pase de emulador 192dp | Ajustar `WideTextButton`/paddings. Es la zona WO-V1: no aceptar "casi bien". |
| Kotest/Turbine incompatibles con Kotlin 2.3 | fallo de las suites en Etapa 1/2 | Kotest 5.9.1 primero; 6.x solo como último recurso (migración propia). |
| El plan quedó viejo (meses después) | Paso 0.1 | Recalcular destinos con el mismo criterio: **elegir las versiones contra las que se compiló el Paparazzi elegido**, no las últimas de cada cosa. |

## Estimación

| Etapa | Tamaño |
|---|---|
| 0 | 15 min |
| 1 | 1–2 h (la mecánica es corta; el margen es por Kotest/KSP) |
| 2 | 1–2 h |
| 3a | 2–4 h (la revisión visual de ~20 snapshots + arreglos de M3 es lo elástico) |
| 3b | 2–3 h (incluye el pase de emulador 192dp obligatorio) |
| 4 | 30 min |
| 5 | 1–2 h (incluye smoke en hardware) |

Total realista: **una tarde larga o dos sesiones cortas** (cortar entre 3a y 3b es el punto
natural: todo queda verde y commiteado). No hacerlo con apuro de publicar nada.
