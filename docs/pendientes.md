# Pendientes — Simple Padel Score

Estado al **29 de agosto de 2026**. Todo lo que hay en `main` está publicado en producción.

## 🎉 Wear OS 1.1.0 aprobada (11 ago 2026)

Google aprobó el **Envío 13** (`350110103`). Se publica sola al 100% en los 177 países. Con eso:

- **Se cierra el ciclo de los dos rechazos** por WO-V1. El fix que funcionó fue
  `Button`→`Chip` (`WideTextButton`) + tutorial centrado + textos acortados.
- **Se desbloquea la release del teléfono.** Ya no hace falta el plan de contingencia de
  desactivar el form factor de Wear OS alrededor del 20 de agosto: quedó sin efecto.

Pendiente al bajarla de Play: **desinstalar antes el APK de debug del reloj**, que está firmado
con la debug keystore. Play no puede actualizar sobre otra firma y el error que da no explica
la causa.

## Segundo rechazo de Wear OS (5 ago 2026) — ✅ RESUELTO

Google rechazó también el Envío 12, con **un solo motivo** y el mismo de antes:

> Directrices de calidad de la aplicación de Wear OS: la funcionalidad no se comporta según lo
> descrito — *"textos sin cortes cuando se selecciona un tamaño de fuente grande"*.

El link **"Ver app bundles"** del detalle del problema confirma el alcance: el único bundle
señalado es **`350110003` (1.1.0), targetSdk 35, Producción, primera publicación 3 ago 2026**. No
figura el `340100003` viejo. Dos consecuencias:

- El rechazo es sobre el bundle **con los fixes de la primera vuelta ya aplicados**, así que el
  defecto que quedaba era real y distinto.
- La remediación se limita a **Producción**. A diferencia de la vuelta anterior —donde el bundle
  señalado vivía en el canal de prueba interna y hubo que pausar los dos canales— acá los canales
  de prueba **no están implicados**, y además ya están pausados.

La primera vuelta arregló el walkthrough, que era la pantalla que Google había capturado. Pero
el defecto vivía en **otras dos pantallas que no se habían mirado con la fuente en grande**, y
por dos causas distintas de la del walkthrough.

### Causa 1 — `Button` de Wear Material NO es un botón de texto

Es un botón **circular para iconos**. Su implementación (`RoundButton`, en
`compose-material-core`) aplica `.size(52.dp)` — tamaño **fijo** — y no reserva padding
interno; está pensado para un icono de 26dp. Usado a lo ancho con una etiqueta de texto, con la
fuente del sistema en Largest la etiqueta envuelve y **pierde la primera y la última letra**
contra el borde redondeado de la píldora:

| Pantalla | Se veía |
|----------|---------|
| Tutorial → "Recorrido guiado" | `ecorrido` / `uiado` |
| Fin de partido → "Instalar en el teléfono" | `nstalar en el` / `eléfono` |
| Aviso companion → mismos botones | ídem |

`Chip` / `ToggleChip`, en cambio, **sí** están hechos para texto:
`defaultMinSize(minHeight = 52.dp).height(IntrinsicSize.Min)` los hace crecer en alto, y
`ChipDefaults.ContentPadding` deja 14dp a cada lado. Se comprobó leyendo las fuentes de
`androidx.wear.compose:compose-material:1.4.1` y se confirmó en el emulador: el chip "Nuevo
partido / Cambiar opciones" ya renderizaba 4 líneas perfectas mientras los botones de al lado
se cortaban.

**Arreglo:** helper `WideTextButton` (en `MainActivity.kt`) que envuelve `Chip`/`OutlinedChip`,
y se reemplazaron los 12 botones de texto de ancho completo de la app. Los dos `Button`
circulares de 36dp de `StrokeTestScreen` se quedan como están: ahí sí son botones de icono.

### Causa 2 — el Tutorial tenía texto alineado a la izquierda

Cada paso era un item de `ScalingLazyColumn` de 4 a 6 líneas con la fuente en grande: un item
**más alto que la pantalla**, así que se dibuja a escala 1.0 y sus líneas de arriba y de abajo
caen en la curva del bisel. Alineadas a la izquierda todas arrancan en el mismo `x`, justo
donde el círculo se angosta, y perdían la primera letra:

```
"saca."   ->  "aca."
"rival,"  ->  "ival,"
"un punto" -> "n punto"
```

El padding lateral del 17% que se le había puesto en la primera vuelta no alcanzaba: a `y=345`
de 384 el semi-ancho visible es `√(192² − 153²) = 116px`, o sea hacen falta 76px de margen y
había 65.

**Arreglo:** texto **centrado** (`TextAlign.Center`) — cada línea se angosta hacia el centro
justo donde el círculo se angosta, y es además lo que recomienda la guía de diseño de Wear OS
para pantallas redondas. Los pasos 2, 4, 5 y 7 se acortaron para que ninguno pase de 3 líneas
(constante `TUTORIAL_STEPS`). Vuelve al padding por defecto del 12%.

### Causa 3 — textos largos en el aviso companion y el fin de partido

- Fin de partido: el aviso de 75 caracteres ocupaba 5 líneas → "Guardá el historial en tu
  teléfono" (34).
- Aviso companion: título de 2 líneas + mensaje de 2 líneas + botón no entran en los ~122dp de
  presupuesto vertical de un reloj de 192dp con la fuente al máximo. Se eliminó el título
  separado y el mensaje corto hace de título.

### Verificación

Emulador `Wear_OS_Small_Round` (**384x384 @ 320dpi = 192dp**, el mínimo de WO-V16) y
`Wear_OS_Large_Round` (**454x454 = 227dp**), los dos con `font_scale 1.30` (Largest), con
máscara circular aplicada a cada captura para ver exactamente lo que recorta el bisel:

| Pantalla | 192dp @1.3 | 227dp @1.3 |
|----------|-----------|-----------|
| Walkthrough (8 pasos) | ✅ | ✅ |
| Aviso companion | ✅ botón visible | ✅ |
| Marcador (0/40/G3, saque) | ✅ | ✅ |
| Ajustes (recorrido completo) | ✅ | ✅ |
| Nuevo partido (recorrido completo) | ✅ | ✅ |
| Tutorial (recorrido completo) | ✅ | ✅ |
| Fin de partido (con aviso companion) | ✅ | ✅ |
| Probar contador | ✅ | — |

También se repasó a `font_scale 1.0` para confirmar que no hubo regresión visual.

### Cuál es el peor caso real, medido en el sistema

No hace falta adivinar hasta dónde puede subir la fuente un revisor. Se midió en
**Ajustes → Accesibilidad → Tamaño del texto** del propio Wear OS (API 36):

| Eje | Rango que ofrece el sistema | Peor caso |
|-----|-----------------------------|-----------|
| **Tamaño de fuente** | slider de 7 pasos: 0.94 · 1.0 · 1.06 · 1.12 · 1.18 · **1.24** | `font_scale = 1.24` |
| **Tamaño de pantalla** | slider de 4 pasos, **arranca en el máximo** (320dpi = 192dp). Solo se puede bajar, y bajarlo da *más* dp (301dpi = 204dp) | el default |
| **Texto en negrita** | on/off (`font_weight_adjustment = 300`) | on |

O sea: **1.24 es el tope**, y las capturas de arriba están tomadas a **1.30**, más duro que
cualquier cosa que un usuario pueda elegir. Se repitió además el recorrido completo a
**1.24 + negrita** (el peor caso alcanzable de verdad) y también quedó limpio.

Ojo con el atajo de `adb shell settings put system font_scale 1.30`: **acepta** valores por
encima del tope de la UI, así que sirve para tener margen, pero no confundirlo con "lo que ve
el revisor".

### Lo que se revisó y NO era problema

- **`maxLines = 1` sin `overflow = Ellipsis`** en el marcador (los sets, los puntos y los games,
  `MainActivity.kt` L567-581 y L791-805). Es un sospechoso habitual, pero acá los textos son de
  1 o 2 caracteres como máximo ("40", "AD", "G3", "10") y entran de sobra incluso a 1.30 en
  192dp. Además ponerles puntos suspensivos sería peor: mostrar "4…" en vez del puntaje.
- **El marcador** (la pantalla que más riesgo tiene a priori: números grandes y botones juntos).
  Verificado en 0-0, 40-0 y con el indicador de saque, a 1.30 y a 1.24+negrita.
- **`TimeText`**: lo dibuja el sistema, no la app.

**Test de regresión nuevo:** `WideTextButtonScreenshotTest` renderiza las tres etiquetas más
largas de la app en 198dp con `fontScale 1.3`. Si alguien vuelve a usar `Button` con texto, las
capturas cambian y el test falla. Se puede cubrir en Paparazzi porque los botones no viven
dentro de un `ScalingLazyColumn` (que es lo que renderiza vacío).

---

## Primer rechazo de Wear OS (3 ago 2026) — ✅ RESUELTO

Tres violaciones de las Wear OS app quality guidelines. Las tres están **corregidas y
verificadas en hardware** (Galaxy Watch 6 real) en el branch `chore/target-sdk-bump`.

| Violación | Causa real | Estado |
|-----------|-----------|--------|
| **Tamaño de fuente de Wear** (WO-V1) | `WalkthroughScreen` era un `Column` sin scroll, sin padding lateral y con `\n` hardcodeados. Con la fuente del sistema en Largest (1.3) el contenido no entraba y se cortaba contra el borde curvo. | ✅ verificado en reloj |
| **La funcionalidad no se comporta según lo descrito** | Misma causa que la anterior: "textos sin cortes cuando se selecciona un tamaño de fuente grande". | ✅ verificado en reloj |
| **Falta la barra de desplazamiento** (WO-V8) | `MatchFinishedScreen` y `CompanionPromptScreen` tenían `positionIndicator = { }` vacío. | ✅ verificado en reloj |

Las capturas de evidencia que mandó Google fueron el paso "Deshacer" del walkthrough y la
pantalla del companion prompt — las dos pantallas exactas que estaban anotadas en la sección
"Detalles menores" de este documento y se habían dejado sin arreglar.

### Qué se cambió

- **`WalkthroughScreen`** pasó de `Column` fijo a `ScalingLazyColumn`. No fue solo por el
  scroll: en pantalla redonda, el ancho disponible a una distancia `y` del centro es
  √(R²−y²), así que un padding lateral fijo no evita el corte — cualquier línea que scrollee
  hacia los extremos se recorta. `ScalingLazyColumn` escala y desvanece los items contra el
  borde, que es el patrón que espera Wear OS. Se sacaron los `\n` hardcodeados y se agregó
  padding lateral del 12%.
- La lista queda **fuera** del `AnimatedContent` a propósito: durante la transición habría
  dos listas montadas compartiendo el mismo `ScalingLazyListState`. Se anima el bloque de
  texto, no la lista.
- **`PositionIndicator` real** en `MatchFinishedScreen` y `CompanionPromptScreen`.
- **`Modifier.scrollAway(listState)`** en el `TimeText` de las 5 pantallas desplazables —
  resuelve además el título pasando por debajo del reloj del sistema.

### La causa de fondo no era el padding

El corte del título no venía del `contentPadding` sino de
**`rememberScalingLazyListState()`**, cuyo default es `initialCenterItemIndex = 1`: la lista
arranca centrando el **segundo** item, así que el primero —el título— queda arriba, debajo del
`TimeText` y recortado contra el borde superior. Se probó primero con más padding lateral (12%
y 15%) y con más padding vertical, y en las dos el título seguía cortado.

El fix es `rememberScalingLazyListState(initialCenterItemIndex = 0)`, aplicado a **las 6
listas** del módulo. Eso corrigió de una vez el walkthrough, el companion prompt y también
Ajustes, que tenía el mismo defecto sin haber sido señalado por Google.

### Verificado en emulador de Wear OS (API 36, 454x454 @ 320dpi = 227dp)

Con `font_scale = 1.3` (Largest) y aplicando por software la máscara circular del reloj — el
emulador captura el framebuffer cuadrado, sin el recorte que sí hace el hardware:

| Pantalla | Resultado |
|----------|-----------|
| Walkthrough paso "Deshacer" (el que Google capturó) | ✅ título, descripción y "Tocá para continuar" completos |
| Walkthrough paso "Contador de golpes" (texto más largo, 4 líneas) | ✅ completo |
| Companion prompt (el que Google capturó) | ✅ título completo debajo del reloj; el botón se alcanza scrolleando |
| Companion prompt durante el scroll | ✅ **barra de desplazamiento visible** (WO-V8) y `scrollAway` esconde el reloj |
| Ajustes | ✅ título completo; el botón largo usa elipsis, no se corta contra el borde |
| Toda la sesión | ✅ sin crashes ni excepciones en logcat |

También se verificó que **el tap sigue avanzando el walkthrough**: al pasar a
`ScalingLazyColumn` existía el riesgo de que la lista consumiera el gesto del `Box` padre.

Segunda pasada, con las pantallas restantes:

| Pantalla | Resultado |
|----------|-----------|
| Nuevo partido (arriba y scrolleado) | ✅ título y chips completos, barra de desplazamiento visible |
| Nuevo partido (final) | ✅ "Arrancar" y "Cancelar" completos |
| Fin de partido | ✅ "Ganaste!", sets y resultado completos, contenido centrado |
| Fin de partido (scrolleado) | ✅ "Jugar de nuevo" y "Nuevo partido" completos, con barra |

### Tercera pasada: emulador de 192dp (el mínimo de WO-V16)

Se repitió todo en un emulador de **384x384 @ 320dpi = 192dp**, más chico que el Galaxy
Watch 6 real (216dp) y que el primer emulador (227dp). Es el peor caso posible, y ahí
apareció un problema que en 227dp no se veía: con la fuente en Largest, las descripciones de
4 líneas del walkthrough empujaban el "Tocá para continuar" fuera de la vista inicial.

Se acortaron los textos más largos, sin perder información:

| Antes | Ahora |
|-------|-------|
| "Contamos tus golpes en el partido. Usá el reloj en la muñeca de la paleta" | "Usá el reloj en la muñeca de la paleta" (el conteo ya lo dice el título) |
| "Instalá la app en el celu para guardar tu historial y ver estadísticas" | "Instalá la app en el celu para ver tu historial" |
| Companion prompt: "Vinculá tu reloj a un teléfono con Simple Padel Score para guardar tu historial y estadísticas." | "Vinculá el reloj a un teléfono que tenga la app" |

Resultado en 192dp con `font_scale = 1.3`: walkthrough (pasos "Deshacer" y "Contador de
golpes") con todo el contenido visible sin scrollear, companion prompt con título completo y
el botón alcanzable, barra de desplazamiento presente, y Ajustes correcto en sus tres
posiciones de scroll.

### Bug encontrado de paso: versión hardcodeada

La pantalla de Ajustes mostraba **`"v1.0.0"` hardcodeado** (`MainActivity.kt:1052`), así que
con el bump a 1.1.0 iba a mostrar una versión equivocada. Ahora usa
`BuildConfig.VERSION_NAME`, lo que requirió habilitar `buildFeatures.buildConfig = true` en
`wear/build.gradle.kts` (en AGP 8 viene desactivado).

### `TutorialScreen`: verificado, y necesitaba más margen que el resto

Llegar ahí por adb es inviable —el swipe horizontal de la app lo intercepta el emulador como
gesto de "atrás" del sistema y la cierra—, así que se compiló un debug temporal con
`mutableStateOf(Screen.TUTORIAL)` como estado inicial, se capturó, y **se revirtió el cambio**.

Con el 12% de padding del resto de la app, el punto 1 perdía la "e" de "elegir" contra la
curva. La causa: los textos del tutorial están **alineados a la izquierda**, así que todas las
líneas arrancan en el mismo `x` y en la zona baja del círculo eso ya cae fuera; el texto
centrado del walkthrough y del companion prompt no tiene ese problema porque las líneas cortas
quedan lejos de los bordes. Por eso `roundSafeContentPadding` ahora acepta `sideFraction` y el
tutorial usa **0.17f**.

Verificado en 192dp con `font_scale = 1.3`: título, punto 1 y punto 2 completos sin letras
comidas, con barra de desplazamiento visible.

### Cobertura final de la verificación

Todo con `font_scale = 1.3` (Largest) y máscara circular aplicada por software, porque tanto
el emulador como el reloj capturan el framebuffer cuadrado sin el recorte del hardware.

| Pantalla | 192dp (emu) | 203dp (Galaxy Watch 6 **real**) | 227dp (emu) |
|----------|-------------|--------------------------------|-------------|
| Walkthrough — paso "Deshacer" (el que Google capturó) | ✅ | ✅ | ✅ |
| Walkthrough — paso "Contador de golpes" (texto más largo) | ✅ | ✅ | ✅ |
| Tutorial | ✅ | ✅ | — |
| Ajustes | ✅ | ✅ | ✅ |
| Nuevo partido | — | ✅ | ✅ |
| Fin de partido | — | ✅ | ✅ |
| Companion prompt | ✅ | — | ✅ |
| Barra de desplazamiento durante scroll | ✅ | — | ✅ |

**El Galaxy Watch 6 40mm real es 432x432 @ 340dpi = 203dp**, no 216dp como se había calculado
asumiendo 320dpi. Queda entre los dos emuladores, así que el rango 192–227dp lo cubre por
ambos lados.

Para capturar pantallas puntuales sin navegar (el swipe horizontal de la app lo intercepta
Wear OS como gesto de "atrás", tanto en emulador como en hardware) y **sin borrar los datos
del reloj**, se compiló un debug temporal que acepta la pantalla inicial por extra del intent
(`am start ... --es screen TUTORIAL`). Los cambios se revirtieron y el working tree quedó
limpio.

### El paso 0 del walkthrough: resuelto ocultando la pelota

Con la fuente en Largest, los 48dp fijos de la pelota empujaban el "Tocá para continuar"
**fuera de la vista inicial**. `walkthroughBallSize()` la escala en sentido inverso a la fuente:
48dp normal, 34dp desde `fontScale 1.1`, y **se oculta desde 1.25**.

El mecanismo es un **umbral**, no "ganar espacio", y esto importa para no repetir intentos
fallidos: el `Arrangement` está en `CenterVertically`, así que al dejar de ocupar esos ~32dp el
contenido pasa a caber en la pantalla, se centra, y el texto de acción sube a una franja más
ancha del círculo. Por eso:

- Bajar el `top` del contentPadding de 40 a 30dp y el spacing de 8 a 6dp **no cambió nada** (la
  captura salió idéntica): el contenido seguía sin caber, así que seguía anclado arriba.
- Achicar la pelota a 24dp hizo aparecer el texto, pero con la última letra al ras de la curva.
- Ocultarla cruzó el umbral y el texto entró **completo y con margen**.

Verificado en el Galaxy Watch 6 real con `font_scale = 1.3`. Con fuente normal la pelota sigue
en 48dp, así que el uso de todos los días no cambia.

### Verificación en hardware

Los screenshot tests **no pueden cubrir esto**: las tres pantallas involucradas usan
`ScalingLazyColumn`, que renderiza vacío en Paparazzi. Se agregó un test de `fontScale = 1.3`
sobre `CounterScreen` (que sí renderiza y pasa), pero el walkthrough y las dos pantallas del
rechazo hay que verificarlas en el reloj:

```bash
adb shell settings put system font_scale 1.3   # Largest
adb shell pm clear com.gonzalocamera.padelcounter   # fuerza el walkthrough
# recorrer los 8 pasos del walkthrough, ajustes, nuevo partido, fin de partido y
# el companion prompt, confirmando que no hay texto cortado y que se ve la barra
adb shell settings put system font_scale 1.0   # dejarlo como estaba
```

Google avisa además: *"Este problema también puede encontrarse en otras ubicaciones.
Comprueba todas las áreas de tu aplicación cuando lo corrijas"*. Hay que recorrer **todas**
las pantallas con la fuente en Largest, no solo las dos señaladas.

---

## Estado actual de la publicación

| Artefacto | versionCode | versionName | targetSdk | Estado |
|-----------|-------------|-------------|-----------|--------|
| mobile | 350100000 | 1.0.0 | 35 | Producción, 177 países (publicado 30/07/2026) |
| wear | 340100003 | 1.0.0 | 34 | Rechazada (Envío 11) |
| wear | 350110003 | 1.1.0 | 35 | Rechazada (Envío 12, 05/08/2026) — WO-V1 → "Sustituida por otra versión" |
| wear | 350110103 | 1.1.0 | 35 | ✅ Publicada (11/08/2026 04:00) — Envío 13, 100% |
| mobile | 360110100 | 1.1.0 | **36** | ✅ **Publicada** (12/08/2026) — Envío 14, 100% |
| wear | 350110203 | 1.1.0 | 35 | ✅ Publicada — Envío 15 |
| — (ficha) | — | — | — | ✅ Publicada (20/08/2026) — Envío 16: nombre, descripción y capturas |
| mobile | **360120000** | **1.2.0** | **36** | ✅ **Publicada** (27/08/2026 14:22) — Envío 17, 100% |

Con *Publicación gestionada desactivada*, el bundle aprobado se publica automáticamente al 100%
en los 177 países en cuanto Google lo apruebe (plazo estimado: 7 días).

El Envío 13 llevó dos cambios: Producción (Wear OS) → `350110103` → lanzamiento completo, y
Prueba cerrada (Wear OS) → pausar canal (venía pendiente de la vuelta anterior). El
`350110003` quedó en **"No incluido"**, que es lo que exige la remediación de Play, verificado
en la pantalla de revisión. 75 wearables compatibles. Única advertencia: la de símbolos de
depuración nativos, que es opcional (sección 4).

El **aviso de nivel de API de Wear OS no reapareció**: el `targetSdk 35` quedó aceptado. El del
teléfono seguía visible con fecha límite **31 de agosto** (la notificación de Play dice
textualmente *"Haz algo no más tarde del 31 ago"*; antes acá figuraba 30, un día de margen que
no existía) y **volvió a aparecer el 27/08**, ya con los dos artefactos en regla — ver la nota
de abajo.

El Envío 12 (rechazado) había llevado los mismos dos cambios con el bundle `350110003`.

**Los dos canales de prueba quedaron pausados** para sacar de circulación los bundles con
targetSdk 34, como pide la remediación de Play (*"actualiza también esos canales"*):

- **Prueba interna**: tenía el 340100003 rechazado. Pausarla se aplica **de inmediato**, no
  pasa por revisión — y eso conviene: si viajara en el envío, Google revisaría con el bundle
  rechazado todavía activo. Reversible con "Reanudar canal".
- **Prueba cerrada**: tenía el `vc5` del 31/03/2026. Acá el pausado **sí** queda pendiente y
  viajó en el envío. Se descartó "Detener lanzamiento" porque hacía caer a los testers al
  `vc4`, todavía más viejo, sin resolver nada.

**El aviso de nivel de API de Wear OS sigue visible** después del envío. Es lo esperado: Play
lo recalcula cuando la versión queda aprobada y publicada, no al enviarla. La página ahora
muestra "Actualización en revisión".

---

## 1. Fecha límite dura: 31 de agosto de 2026 — ✅ RESUELTO Y PUBLICADO

**Al 29/08/2026 los dos artefactos de producción cumplen y no hay nada que compilar.** El plazo
pide API 36 (Android 16) para el teléfono, pero
[Wear OS tiene requisito propio](https://support.google.com/googleplay/android-developer/answer/11926878)
y se queda en **API 35**: `:mobile` está en 36 desde el Envío 14 y `:wear` en 35 desde el 13.

El aviso que volvió a notificar Play el **27/08** no es el bundle de producción. *Ver app
bundles* (29/08) lista **uno solo**: `350100000` (1.0.0), SDK objetivo **35**, canal **Prueba
cerrada — Alpha**, primera publicación 6 jul 2026. Es el `:mobile` 1.0.0 viejo, que quedó activo
en ese canal al ser reemplazado en producción. Tampoco es el `vc5` de más abajo: ése está en 34
y el detalle dice que el nivel incumplidor **más alto** es 35.


Hecho en el branch `chore/target-sdk-bump` (2 de agosto de 2026), **sin publicar todavía**:

| Módulo | Antes | Ahora | compileSdk |
|--------|-------|-------|------------|
| `:mobile` | `targetSdk 35` | **36** (Android 16) | 35 (ver nota) |
| `:wear` | `targetSdk 34` | **35** (Android 15) | 35 |

Versionado nuevo: `PADEL_MOBILE_VERSION_CODE=360110000`,
`PADEL_WEAR_VERSION_CODE=350110003`, `PADEL_VERSION_NAME=1.1.0`.

**Por qué `:mobile` tiene `compileSdk 35` con `targetSdk 36`:** Paparazzi 1.3.4 no soporta
`compileSdk 36` — falla con `lateinit property sessionParamsBuilder has not been initialized`
y se cae toda la suite de screenshot tests. El soporte llegó en Paparazzi 2.0.0-alpha02 con
LayoutLib 15.2.3. Lo que Play evalúa es el targetSdk del manifest, así que el requisito se
cumple igual. Subirlo a 36 implica migrar el toolchain completo — ver la sección
**"Migración de toolchain"** más abajo.

Lo que se migró junto con el salto (Android 16 hace el edge-to-edge obligatorio y **elimina
el opt-out**):

- `MainActivity.kt` de mobile: `enableEdgeToEdge()` ahora pasa
  `SystemBarStyle.dark(TRANSPARENT)` explícito para status y navigation bar. La app es
  dark-only e ignora el tema del sistema; con `auto`, un sistema en claro pondría iconos
  oscuros sobre el fondo negro.
- `Theme.kt`: eliminado el `SideEffect` que seteaba `window.statusBarColor` y
  `window.navigationBarColor` — deprecados y **sin efecto** desde API 35. Eran el origen del
  advisory "tu aplicación usa APIs o parámetros obsoletos".
- `themes.xml`: eliminados `android:statusBarColor`, `android:navigationBarColor` y
  `android:windowDrawsSystemBarBackgrounds`, por lo mismo.

Verificado que no hacía falta tocar: el `Scaffold` de M3 ya aplica `innerPadding` al
contenido y `NavigationBar` absorbe su propio inset; `HistoryScreen` usa `BackHandler` de
androidx.activity (compatible con el predictive back que Android 16 activa por defecto); y el
manifest no fija orientación (Android 16 la ignora en pantallas grandes).

**Falta probar en hardware real** el edge-to-edge de mobile: los screenshot tests de Paparazzi
no renderizan la Activity, así que no cubren las barras del sistema.

### Sacar de circulación el `350100000` del canal Alpha — ✅ RESUELTO (Envío 18, publicado)

Era lo que disparaba el aviso. Sin código: se pausó el canal de **prueba cerrada (Alpha)** del
teléfono, desde *Gestionar canal → Pausar canal*. "Detener lanzamiento" no servía: hace caer a
los testers al bundle anterior, todavía más viejo.

**Enviado el 29/08/2026 12:02 como Envío 18 y publicado ~12:29**, en menos de media hora, con
un único ítem (Prueba cerrada - Alpha → Estado del canal → Pausar canal) — verificado antes de
mandarlo que no arrastraba Producción, ficha ni otros tracks.

El aviso no se va en el momento: Play lo recalcula un rato después de aplicar el cambio, como ya
había pasado en el Envío 13. Del lado del repo no queda nada pendiente por el plazo del 31/08.

### Limpiar el bundle vc5 del canal cerrado (Wear OS)

El aviso de API de Wear OS lo dispara un bundle viejo: **versionCode 5, targetSdk 34,
publicado el 31/03/2026 en el canal de prueba cerrada**, que quedó de un intento anterior de
publicar el reloj. Mientras siga activo ahí, el aviso **no desaparece aunque se suba un
artefacto nuevo con API 35**. Hay que sacarlo de circulación en el mismo movimiento.

---

## 2. Actualización de `:mobile` (deuda funcional)

Hay código mergeado en `main` que nunca se publicó. El AAB en producción es del 6 de julio;
los commits posteriores no están en la tienda.

### El cartel equivocado del companion

**Síntoma:** el reloj muestra hasta 3 veces al arrancar la pantalla *"También en tu teléfono →
Instalar en el teléfono"* a usuarios que **ya tienen** la app de teléfono instalada.

**Causa:** el reloj detecta al companion buscando la capability
`verify_remote_padel_phone_app` (`CompanionDetector.kt:35`). Esa capability se declara en
`mobile/src/main/res/values/wear.xml`, archivo agregado el 12 de julio (commit `e007f77`) —
posterior al AAB publicado. Verificado sobre el artefacto:

```bash
bundletool dump resources --bundle release-artifacts/padel-mobile-v1.0.0-vc350100000.aab | grep wear_capabilities
# no devuelve nada
```

**Qué NO se rompe:** el sync de partidos funciona igual. El AAB publicado tiene el
`SyncBridgeListener` registrado con su intent-filter de `/padel-score/match`, y
`syncSender.trySendPending()` (`MainActivity.kt:182`) se llama sin consultar el estado del
companion. Es un defecto cosmético, no una falla de conectividad.

**Se corrige solo** publicando cualquier versión de `:mobile` posterior al 12 de julio.

### Features sin publicar

El commit `afedd27` (31 de julio) agregó a `:mobile`: carga manual de partidos, compartir
resultado, backup/import del historial y contacto. Todo eso está en `main` y no en la tienda.

### Advisories de la consola sobre el AAB de mobile

De los tres que marcaba la consola, **los dos de edge-to-edge ya están resueltos** en
`chore/target-sdk-bump` (ver sección 1). Queda uno solo, y no es bloqueante:

- `androidx.fragment` desactualizado. Viene por transitividad de alguna dependencia, no se usa
  directo, así que se va a resolver solo cuando se suba el Compose BOM en la migración de
  toolchain (sección 3).

---

## 3. Migración de toolchain (AGP / Kotlin / Compose / Paparazzi)

**Plan paso a paso completo en [`toolchain-migration-plan.md`](toolchain-migration-plan.md)**
(30/08/2026): etapas con combos probados, dónde regrabar snapshots y por qué, verificación por
etapa, riesgos y planes B. Lo de abajo queda como contexto histórico; datos nuevos que lo
corrigen: existe **Paparazzi 1.3.5** (Kotlin 2.0.21) como escalón intermedio, Paparazzi 2.x
sigue en alpha (última: 2.0.0-alpha05, compilada contra AGP 8.13.2 / Kotlin 2.3.0 / Java 21 —
el daemon ya usa JDK 21), y el destino de AGP es la **serie 8.13.x, no 9.x** (AGP 9 integra
Kotlin y es otra migración).

**Decidido el 02/08/2026: se hace, pero como tanda propia y después de publicar 1.1.0.**
No desbloquea nada — el requisito de Play es el `targetSdk` del manifest y ya está cumplido.
El único beneficio concreto es poder poner `compileSdk 36` en `:mobile`, que es correctitud de
configuración, no funcionalidad.

### Estado del toolchain

| Componente | Versión actual |
|------------|----------------|
| AGP | 8.5.2 |
| Kotlin | 1.9.24 |
| Compose Compiler | 1.5.14 (plugin separado, pre-Kotlin 2.0) |
| Compose BOM | 2024.06.00 |
| Wear Compose | 1.4.1 |
| Paparazzi | 1.3.4 |
| Gradle | 9.2.1 (ya al día, no hace falta tocarlo) |

### Lo que está verificado

- **AGP 8.5.2 compila con `compileSdk 36`.** Probado: `:mobile:compileDebugKotlin` pasa, solo
  emite el warning de "conviene usar un AGP más nuevo". No hay que subir AGP para cumplir el
  requisito de Play.
- **Paparazzi 1.3.4 NO soporta `compileSdk 36`.** Probado: los 20 screenshot tests de
  `:mobile` fallan con `kotlin.UninitializedPropertyAccessException: lateinit property
  sessionParamsBuilder has not been initialized`. Con `compileSdk 35` pasan todos.
- **El soporte de API 36 en Paparazzi llegó en 2.0.0-alpha02**, con LayoutLib 15.2.3
  ([release](https://github.com/cashapp/paparazzi/releases/tag/2.0.0-alpha02),
  [issue #1877](https://github.com/cashapp/paparazzi/issues/1877)). Sigue en alpha.

### Lo que NO está verificado (hay que confirmarlo antes de empezar)

Esto es la cadena *esperada*, no comprobada. Conviene chequear la matriz de compatibilidad
real antes de tocar versiones:

- Que AGP nuevo obligue a **Kotlin 2.x** (el KGP 1.9.24 no está soportado con AGP tan nuevo).
- Qué **versión mínima de AGP** soporta API 36 nativamente.
- Con qué AGP es compatible **Paparazzi 2.x**.

### Trabajo que implica

1. Kotlin 2.x cambia cómo se configura Compose: desaparece
   `composeOptions.kotlinCompilerExtensionVersion` y entra el plugin
   `org.jetbrains.kotlin.plugin.compose`. Toca los `build.gradle.kts` de `:mobile` y `:wear`.
2. Subir el Compose BOM (el actual es de junio 2024). Ahí aparecen cambios de API y de
   apariencia en Material3.
3. Subir Paparazzi y **regrabar todos los snapshots**: ~20 de `:mobile` y 6 de `:wear`. Hay
   que revisarlos de a uno para distinguir "cambió el antialiasing" de "se rompió el layout".
4. Recién entonces, `compileSdk = 36` en `:mobile`.

### Por qué no se hizo junto con el bump de targetSdk

El riesgo se concentra justo donde hay menos cobertura: lo que se rompe es la UI y los
screenshot tests, o sea que la red de seguridad se cae al mismo tiempo que lo que hay que
verificar. Y meter Paparazzi 2.x alpha —la única verificación visual del proyecto— es un
downgrade de confiabilidad mientras siga en alpha.

Argumento a favor de no dejarlo eternamente: el Compose BOM ya tiene más de dos años, y el
próximo ciclo de requisitos de Play va a empujar igual. Mejor hacerlo en frío que apurado.

---

## 4. Bugs y detalles de código

El primero es un bug real que corrompe datos; el resto no bloquea nada.

### 🐛 El reloj re-sincroniza el partido terminado en cada arranque en frío — ✅ RESUELTO (11/08/2026)

**Encontrado el 5/8/2026** mientras se instalaba el build nuevo en el reloj real. Es
**preexistente**, no lo introdujo el fix de WO-V1.

`MainActivity.kt`, el `LaunchedEffect(state.mySets, state.oppSets)` que sincroniza el partido al
terminarlo:

```kotlin
LaunchedEffect(state.mySets, state.oppSets) {
    if (isMatchFinished(state) && screen == Screen.COUNTER && !matchSynced) {
        matchSynced = true
        val match = Match(id = UUID.randomUUID().toString(), ...)   // <-- id nuevo cada vez
        syncQueue.enqueue(match); syncSender.trySendPending()
```

En un arranque en frío `screen` vale `COUNTER` y `matchSynced` vale `false`. Si el estado
persistido en DataStore tiene un partido terminado, el efecto **se vuelve a disparar** y manda el
mismo partido otra vez con un id nuevo. Y el id nuevo rompe las dos defensas que existen:

- `WearSyncSender` publica en `"/padel-score/match/${match.id}"`. Con el mismo id, el
  `DataClient` deduplicaría solo (mismo path + mismo payload = no hay evento). Con id aleatorio,
  cada envío es un `DataItem` distinto.
- El teléfono inserta con `insertIfAbsent` (`OnConflictStrategy.IGNORE`), que protege contra
  reimportar **el mismo id**. Con id nuevo no aplica: entra una fila más.

**Resultado:** un partido duplicado en el historial del teléfono por cada vez que se abre la app
del reloj con un partido terminado sin resetear.

**Cuándo se manifiesta:** solo si terminás un partido y cerrás la app **sin** tocar "Jugar de
nuevo" ni "Nuevo partido" — cualquiera de los dos resetea el estado. Por eso no se había notado.

#### El mismo bug corrompe además la duración (encontrado el 11/8/2026)

Una captura del historial mostró un partido con **duración de 85h 30min**, origen Reloj, fecha
`08/08/2026 09:41`, marcador `4-6 6-4 7-5`. No es un error de medición: es el re-disparo, y la
duración funciona como reloj forense de **cuándo se reabrió la app**.

| Campo | De dónde sale | Qué pasa en el re-disparo |
|-------|---------------|---------------------------|
| `startedAt` | `MATCH_STARTED_AT` de DataStore, escrito **solo** en `resetMatchWithConfig()` y nunca limpiado | conserva el valor del partido original (4/8 20:11) |
| `finishedAt` | `System.currentTimeMillis()` | se recalcula: pasa a ser el momento de la reapertura (8/8 09:41) |

85h 30min es exactamente la distancia entre esas dos fechas, y el marcador es el mismo partido
del 4/8 ya anotado más abajo.

**Descarta una hipótesis natural:** *"quedó el partido sin cerrar en el reloj"*. No hay botón de
finalizar — el partido se cierra por marcador (`isMatchFinished`) y ahí mismo se sincroniza, así
que el teléfono se entera sin intervención. Una duración implausible **siempre** es este bug,
nunca un partido olvidado.

**Consecuencia para el fix:** el id determinístico **no alcanza**. Evita la fila duplicada, pero
no evita que `finishedAt` se recalcule. Si el primer envío no llegó (teléfono apagado, sin
batería) y el que entra es el del re-disparo, la única fila que queda tiene la duración basura.

**Arreglo aplicado (11/08/2026) — cuatro piezas.** Al implementarlo aparecieron dos más de las
dos que estaban planificadas:

1. **Persistir el momento de finalización** (`MATCH_FINISHED_AT`) y usar ese valor en vez de
   `System.currentTimeMillis()`. La duración deja de crecer en cada reapertura. Se limpia en
   `resetMatchWithConfig()` — si no, el partido siguiente heredaría el fin del anterior.
2. **No reenviar**: `markMatchFinished()` devuelve `firstTime`, y el partido se encola **una sola
   vez**. Es la defensa principal, y no estaba en el plan original.
3. **Id determinístico** (`matchId(startedAt, setsScore)` en `:shared`) en vez de
   `UUID.randomUUID()`. Con eso el path del `DataItem` se repite, el `DataClient` deduplica solo,
   y el `INSERT OR IGNORE` del teléfono funciona de verdad como segunda red.
4. **Sacar el `timestamp` del `dataMap`** en `WearSyncSender`. Era un
   `System.currentTimeMillis()` que hacía que cada envío fuese un `DataItem` distinto aunque el
   partido fuera idéntico, anulando la dedup del `DataClient`. El teléfono nunca lo leyó:
   `SyncBridgeListener` solo usa `"match_data"`.

**Por qué (2) es imprescindible y no bastaba con (1) + (3).** `StrokeCounter` es un singleton
**en memoria**, y el efecto lo resetea después de encolar. En un arranque en frío
`StrokeCounter.snapshot()` vuelve vacío, así que el reenvío llevaría `strokesPerSet = null`.
Compartiendo path con el original —que es justo lo que logra (3)— ese segundo envío le **pisaría
los golpes** al partido si el primero todavía no había llegado al teléfono. El id determinístico
sin (2) convertía un bug de duplicados en un bug de pérdida de datos.

**Nota de migración:** un partido terminado que ya se hubiera sincronizado con el id viejo (UUID)
y siga sin resetear entraría **una vez más** con el id nuevo, porque `MATCH_FINISHED_AT` no
existía. A partir de ahí queda estable. Se evita reseteando el partido antes de actualizar.

**Lo que NO hace falta tocar:** las estadísticas no agregan duración (solo usan `finishedAt` para
ordenar), así que un partido con duración corrupta no ensucia ningún promedio. Sí se ve en el
detalle y —desde el 11/8— en el **texto de compartir**, que ahora incluye la duración.

**Por qué no se arregló junto con el fix de WO-V1:** toca el camino de sincronización, que es lo
más riesgoso de cambiar justo antes de un reenvío, y verificarlo bien necesita el teléfono **y**
el reloj conectados a la vez. El bundle del reloj además está bloqueando la release del teléfono,
que sí tiene fecha límite. Va en la próxima release del reloj (ver el plan de releases).

**Ojo al depurar en el reloj real:** cada `adb install -r` mata el proceso, así que relanzar la
app con un partido terminado en el estado dispara el duplicado. El 5/8 pasó dos veces con el
partido del 4/8 (2-1, 4-6 6-4 7-5).

### 🐛 Crash del foreground service en Android 16 — ✅ RESUELTO en código (02/09/2026, sin publicar)

**19 crashes en producción**, visibles en Play Console → Android vitals → Fallos y errores ANR
solo con el filtro en **"Todos los fallos"** (el default "percibidos por los usuarios" lo
oculta). Todos con el mismo perfil: `ForegroundServiceStartNotAllowedException` en
`StrokeCounterService.startAsForeground` (llamado desde `onCreate`), **únicamente Android 16
(SDK 36)**, únicamente Galaxy Watch7 (fresh7bl/ul, projectx2ul/bl), siempre "en segundo plano".

**Causa:** `onStartCommand` devolvía `START_STICKY`. Cuando el sistema mata el proceso a mitad
de partido (presión de memoria, o el usuario desliza la app de recientes), Android recrea el
servicio **en background**, donde promoverse a foreground está prohibido desde Android 12 — y
en Wear OS con Android 16 esa excepción pasó de tolerada a **fatal**. El restart sticky no
podía funcionar nunca en 16: solo crashear.

**Fix (3 piezas en `StrokeCounterService`):** `START_NOT_STICKY` (elimina la recreación en
background; al reabrir la app el Activity re-arranca el servicio desde foreground y el conteo
retoma del respaldo por game); `try/catch IllegalStateException` alrededor de
`startAsForeground()` con apagado silencioso (`ForegroundServiceStartNotAllowedException` la
extiende, así el catch no referencia una clase inexistente en API 30); y guarda en `onDestroy`
para no pisar el respaldo bueno con el snapshot vacío del proceso recreado.

**Costo funcional:** si el sistema mata el proceso a mitad de partido, los golpes entre la
muerte y la próxima apertura de la app se pierden (antes también: el crash no contaba nada).
El marcador no se ve afectado — vive en DataStore.

Sale como **wear 350120003 (1.2.0)** — ver plan de releases.

#### ✅ Verificado en emulador (02/09/2026, Wear_OS_Small_Round · API 36 · 192dp)

Contrafactual con el mismo método del partido duplicado — matar el proceso con la app en
background (`run-as ... kill -9`, que simula al LMK) con un partido activo y el servicio en
foreground:

| Escenario | Resultado |
|-----------|-----------|
| **Código viejo** (STICKY), 3 kills | el proceso **resucita solo en background** en <15 s, las 3 veces — la precondición exacta del crash |
| **Código nuevo** (NOT_STICKY), kill + 45 s | **no resucita** (0 crashes) — el único camino al crash ya no existe |
| Código nuevo, reabrir la app | partido intacto y servicio de vuelta en foreground (`isForeground=true`, notif 4201) |

**Limitación honesta:** la imagen genérica de AOSP no deniega el `startForeground` del restart
sticky (conserva la excepción "venía siendo FGS") ni siquiera con `am make-uid-idle`, así que la
excepción en sí no se puede disparar en emulador — la deniega la política más dura de Samsung en
los Watch7 con Android 16. Lo que sí queda demostrado: el código viejo se recrea en background
(3/3) y el nuevo no se recrea nunca; sin recreación en background, la excepción no tiene dónde
ocurrir, en ningún OEM. El `try/catch` queda como segunda capa por si existe otro camino.

**UI de sensibilidad** verificada en el mismo pase, con `font_scale 1.30` en 192dp (el peor
caso WO-V1): las tres leyendas nuevas del selector ("Capta hasta los golpes suaves" /
"Equilibrado · recomendado" / "Solo golpes fuertes") completas, centradas y sin cortes — la más
larga wrappea a 2 líneas dentro del círculo. De paso: Ajustes muestra v1.2.0 del BuildConfig.

### Config de test del Galaxy Watch propio

`CounterScreenshotTest.kt` cubre 225dp (`PIXEL_WATCH`) y 198dp (`GALAXY_WATCH_4_40MM`, el
Galaxy Watch 4/5 40mm). El reloj real de prueba es un **Watch 6 40mm: 432x432 @ 340dpi =
203dp**, que cae entre los dos y por eso no tiene config propio. Si se quiere cubrir exacto,
agregar uno de 864x864 @ XXXHIGH con `density = 680`.

### Símbolos de depuración nativos (opcional)

Play advierte que el bundle del reloj no sube símbolos de depuración para su código nativo. Se
activa con `ndk.debugSymbolLevel = "SYMBOL_TABLE"` en el `buildTypes.release` del módulo. El
valor real es bajo: las dos `.so` del bundle (`libandroidx.graphics.path`,
`libdatastore_shared_counter`) son de dependencias de AndroidX, no código propio.

### Limitación conocida de Paparazzi

Las pantallas con `ScalingLazyColumn` renderizan **vacías** en Paparazzi: el componente
necesita una pasada de scroll que el render estático no hace. Por eso los screenshot tests
cubren `CounterScreen` (que no usa lista) y los botones sueltos (`WideTextButtonScreenshotTest`),
no las pantallas de lista completas. Forzarlo requeriría inyectar el `ScalingLazyListState`
desde afuera solo para el test. Ya está documentado en el header de `CounterScreenshotTest.kt`.

**Consecuencia práctica:** las pantallas de lista con la fuente en grande **solo** se pueden
verificar en emulador o hardware. Es exactamente el hueco por el que se colaron los dos
rechazos. El procedimiento que sí funciona está en la sección del segundo rechazo: emulador de
192dp con `font_scale 1.30` y máscara circular sobre cada captura.

---

## 5. Cosas a verificar en Play Console

- **Declaración de IA.** La ficha tiene 4 slots de capturas de tablet (7" y 10", posiciones 7
  y 8) que usan archivos llamados `Gemini_Generated_Image_*.png`, subidos el 11/03/2026 — las
  fotos de cancha con muñeca, reloj y paleta. Conviene confirmar que la *AI asset declaration*
  quedó marcada como "Label assets as created or edited using AI" con esos cuatro etiquetados.
  El ícono y la imagen destacada son propios y no corresponde etiquetarlos.
- **Capturas de teléfono con relojes viejos.** Las posiciones 6, 7 y 8 de "Capturas de
  pantalla de teléfonos" son screenshots de reloj del **diseño anterior al rediseño
  negro-oro** (marzo 2026). Están publicadas hoy. Se decidió no tocarlas en esta entrega.
- ~~**Descripción de la ficha.** Hoy describe solo la app de teléfono.~~ **Desactualizado**:
  revisado el 12/08/2026, la descripción **ya menciona el reloj** desde la primera línea
  ("anotá desde tu reloj Wear OS durante el juego") y tiene una sección `EN EL RELOJ`. WO-G2
  cumplido. Lo que queda es de posicionamiento, no de contenido → ver sección 7.
- **Canal de prueba interna de Wear OS.** Quedó sin testers asignados, así que no distribuye a
  nadie. Si se quiere usar más adelante, hay que crear la lista.

---

## 6. Housekeeping local

Cerrado el 03/08/2026:

- ✅ **Fuente del reloj** de vuelta en Normal (`font_scale = 1.0`).
- ✅ **APK limpio reinstalado** en el Galaxy Watch 6, sin el mecanismo temporal de
  verificación. Comprobado funcionalmente: `am start ... --es screen TUTORIAL` ya no tiene
  efecto y abre el marcador.
- ✅ **La pelota con fuente normal**: verificada en el reloj real, se muestra a 48dp y todo el
  paso 0 del walkthrough entra sin scrollear. El caso de todos los días no cambió.
- Nunca se usó `pm clear` en el reloj real, así que la configuración y el historial quedaron
  intactos en todo el proceso.

Pendiente:

- **Desinstalar del reloj el APK de debug antes de bajar la app de Play.** El instalado está
  firmado con la **debug keystore**, y Play **no puede actualizar sobre una firma distinta**:
  el error que aparece no es claro sobre la causa.
- ✅ **`release-artifacts/padel-wear-v1.0.0-vc11.aab`** (esquema de versionCode viejo): ya no
  está.
- **El video del foreground service** tiene que seguir accesible en YouTube como **No
  listado** (no Privado) mientras dure la revisión: si el revisor no lo puede abrir, rechaza.

---

## 7. ASO — que la app se encuentre más fácil

**Nada de esta sección requiere compilar ni publicar una versión nueva.** Todo se edita en
*Play Console → Crecimiento → Presencia en Play Store → Ficha principal*. Sí pasa por revisión
de Google, como envío propio: en Actividad de envíos figura como **"Ficha de Play Store"** (así
viajó el Envío 10 del 31/07).

**Estado medido el 12/08/2026:** `10+` descargas, **sin reseñas ni valoración**. Categoría
Deportes.

### Lo que ya está bien — verificado sobre la ficha real

No hace falta tocarlo, y conviene anotarlo para no "arreglar" lo que funciona:

- La **descripción larga es sólida** (leída completa el 12/08/2026). Menciona el reloj desde la
  primera línea, se divide en `EN EL RELOJ` / `EN EL TELÉFONO`, y ya indexa los términos que
  importan: *marcador*, *Punto de Oro*, *Deuce/Ventaja*, *Punto Estrella*, *tie-break*, *súper
  tie-break*, *golpes*, *PGG*, *desgaste*, *categoría*, *historial*, *estadísticas*, *Wear OS*.
  Cierra con el argumento de privacidad ("sin cuentas, sin anuncios y sin conexión a internet"),
  que es diferencial real y coincide con la sección Seguridad de datos.
- Las **5 primeras capturas son del diseño negro-oro actual**, con titulares por pantalla
  ("EN LA CANCHA", "ANÁLISIS", "TU PROGRESO", "HISTORIAL", "EN SEGUNDOS").
- **Seguridad de datos**: "No se recogen datos" / "No se comparten con terceros" — es cierto y
  es un argumento de venta en una app de deporte.
- Las **notas de la versión** están al día.

### Cómo rankea Play en 2026 (lo que condiciona todo lo demás)

- **No existe campo de keywords.** Se indexan **título**, **descripción corta** y **descripción
  larga**; el título es el factor con más peso.
- Entre 2024 y 2026 el algoritmo se corrió **de señales pre-install** (keywords, ratings,
  descargas brutas) **a señales post-install**: retención, tasa de conversión desde la búsqueda y
  relevancia semántica.
- **Crashes y ANR pesan en el ranking.** Los fixes de sincronización también son ASO.

Consecuencia práctica: la metadata te mete en la carrera, pero lo que sube posiciones es que
quien instala **se quede**.

### Acciones, en orden de impacto

1. **Reseñas.** Es lo que más falta y ninguna optimización lo compensa: con 0 valoraciones la app
   es invisible y no hay señal de calidad. Diez reseñas reales de gente que juega valen más que
   cualquier otro ítem de esta lista.
2. **Título** (máx **30** caracteres). Hoy `Simple Padel Score` (18) — 12 desperdiciados, y todo
   en inglés mientras el público busca "marcador de padel". Propuesta:
   **`Simple Padel Score: Marcador`** (28). Con 10 descargas el costo de cambiar el nombre es
   nulo; más adelante no.
3. **Descripción corta** (máx **80**). No se ve en la web, hay que abrirla en Console. Es campo
   **indexado** y es lo primero que se lee en el móvil.
4. **Descripción larga** (máx 4000). Está bien escrita y ya cubre casi todo el vocabulario del
   deporte. Lo que falta son cuatro huecos concretos de búsqueda, todos agregables sin reescribir
   nada:

   - **"padel" sin tilde.** El texto usa siempre *pádel* (correcto), pero muchísima gente busca
     escribiendo **padel**. El título ya lo tiene sin tilde; conviene que la descripción incluya
     las dos formas al menos una vez cada una.
   - **"smartwatch"**, como sinónimo de *reloj* / *Wear OS*. Es como busca quien todavía no sabe
     que su reloj corre Wear OS.
   - **Nombres de dispositivo**: *Galaxy Watch*, *Pixel Watch*. Son búsquedas frecuentes del tipo
     "app padel galaxy watch" y hoy no matchean.
   - **"contador de puntos" / "anotador"**. Hoy *contador* aparece solo en "Contador de golpes";
     como frase de búsqueda para el marcador no está.
5. **Capturas 6, 7 y 8**: confirmar si siguen siendo las del diseño viejo (ver sección 5). Las
   primeras cinco están bien; estas no se ven sin scrollear el carrusel.
6. **Badge "AI" en las capturas de teléfono.** Las cinco lo muestran, y son screenshots reales de
   la app con marco y titular. Verificar que la declaración de IA esté aplicada solo donde
   corresponde (las 4 imágenes de tablet, sección 5) y no a capturas propias.

### Lo que NO conviene hacer

**Pagar Google Ads App Campaigns.** No hay mínimo formal, pero el piso realista para que el
Smart Bidding tenga datos es **USD 20–50 por día** (600–1500 al mes). Para una app gratuita sin
monetización es tirar plata: se compran instalaciones que además no retienen, y la retención es
justo la señal que hoy manda en el ranking.

---

## Plan de releases

Replanificado el 03/08/2026: el rechazo del reloj adelantó su 1.1.0, así que la tanda del
teléfono quedó sola.

### Release 1 — Wear OS 1.1.0 · ✅ APROBADA (11/08/2026)

`wear 350110103`. Aprobada y publicándose al 100% en los 177 países.

Corrige el rechazo por WO-V1 (`Button`→`Chip`, tutorial centrado, textos acortados), verificado
en 192dp y 227dp con la fuente al máximo, y arrastra el `targetSdk 35` y el pausado de los
canales de prueba de las vueltas anteriores.

Cuando se apruebe: **desinstalar el APK de debug del reloj antes de bajarla de Play.** Está
firmado con la debug keystore y Play no puede actualizar sobre otra firma; el error que da no
explica la causa.

### Release 2 — `:mobile` 1.1.0 · ✅ PUBLICADA (12/08/2026)

`mobile 360110100`, mergeado a `main` (11/08/2026) y compilado:
**`release-artifacts/padel-mobile-v1.1.0-vc360110100.aab`**.

> El build `00` (`360110000`) se compiló y archivó el mismo día pero **nunca se subió**:
> antes de enviarlo entró el bloque de reglas del texto de compartir, así que se bumpeó el
> build a `01` y se borró el artefacto anterior. No hay dos AABs distintos con el mismo
> versionCode.

Es lo próximo. El Centro de políticas marca *"La aplicación debe estar orientada a Android 16
(nivel 36 de la API) o a una versión posterior"*, con fecha límite **31 de agosto**. Eso es el
artefacto de teléfono, y la aprobación del reloj **no lo resuelve**: hay que subir este AAB.
Pasada esa fecha no se pueden publicar actualizaciones del teléfono.

La dependencia que bloqueaba esta release **quedó resuelta**: el artefacto de Wear OS ya está
aprobado, así que el envío del teléfono no arrastra nada rechazado. El plan de contingencia de
desactivar el form factor de Wear OS alrededor del 20 de agosto ya no hace falta.

**Verificado sobre el AAB generado** (11/08/2026):

| Qué | Resultado |
|-----|-----------|
| `versionCode` / `versionName` | `360110000` / `1.1.0` |
| `targetSdk` del manifest (lo que evalúa Play) | **36** ✅ |
| Firma vs. el bundle publicado | **idéntica** — `5A:39:B2:…:5E:BB` ✅ |
| `android_wear_capabilities` en recursos | **presente** → arregla el cartel del companion ✅ |
| Suite completa (`:shared` + `:mobile` + `:wear`) | verde |
| Edge-to-edge en teléfono real | ✅ verificado por uso del build del branch |

SHA-256 del AAB: `4d08f141917548408cb41cc1533b44ee00c84182f6fd7af8497b7ff83410cd90`

**Enviado el 11/08/2026 10:57 (Envío 14).** Producción → teléfono (Teléfonos, Tablets, Chrome OS,
Android XR), 100%, 177 países. Verificado antes de enviar: el bundle listado era el `360110100`
con SDK objetivo 36; el track de Wear OS seguía **Activo** con el `350110103` publicado y no
apareció como reemplazado ni en "No incluido"; y "Cambios que aún no se han enviado a revisión"
mostraba **un solo ítem** (Producción `360110100`), sin arrastrar ficha, capturas ni precios.
Única advertencia: la de símbolos de depuración nativos, opcional.

Contenido extra respecto del plan original: **el texto de compartir ahora incluye las reglas
del partido** — duración, modo (Deuce / Punto de Oro / Star Point) y desempate configurado
(Tie-break a 7 / Súper tie-break a 10), más una línea aparte cuando el partido efectivamente
se definió en tie-break. Un partido de carga manual omite duración, modo y desempate, porque
esos campos quedan en el default del constructor y publicarlos sería inventar datos; sí
conserva formato y tie-break, que son reales (el primero lo elige el usuario, el segundo se
deriva del marcador).

Contenido, todo ya commiteado y con la suite en verde:

- `targetSdk 36` → cierra ese plazo
- migración de edge-to-edge → resuelve 2 de los 3 advisories de la consola (sección 1)
- las features del 31/7 que nunca se publicaron: carga manual, compartir, backup (sección 2)
- el cartel equivocado del companion, que se arregla solo al publicar mobile (sección 2)

Antes de subirla:

- ✅ **Probar el edge-to-edge en el teléfono** — era lo único que ningún test cubre (los
  screenshot tests de Paparazzi no renderizan la Activity, así que no ven las barras del
  sistema). Cubierto por uso real del build del branch en el teléfono.
- ✅ Generar el AAB y archivarlo en `release-artifacts/`
- Revisar el checklist de `publishing-guide.md` §9
- Se sube al track de **teléfono** (el selector por defecto), no al de Wear OS

### Release 3 — Wear OS: el partido duplicado y su duración · ✅ PUBLICADA (Envío 15)

`wear 350110203`, compilado y archivado en
**`release-artifacts/padel-wear-v1.1.0-vc350110203.aab`**
(SHA-256 `bb89f503faf809dcd2db8073907ac62598b93964276797dae72d9d002aa6f518`).
El versionName sigue en **1.1.0**: es compartido con `:mobile`, que está en revisión con ese
nombre, y subirlo por un bugfix del reloj arrastraría al teléfono sin motivo.

Arregla el bug de la sección 4, que tenía **dos síntomas del mismo origen**: la fila duplicada en
el historial y la duración disparatada (se vio una de 85h 30min). Las **cuatro** piezas del fix
están detalladas en esa sección.

**Enviado el 12/08/2026 17:49 (Envío 15).** Producción → Wear OS, 100%, 75 wearables. Verificado
antes de enviar: un solo ítem en "Cambios en revisión" (Producción (Wear OS) `350110203`), el
`350110103` correctamente en "No incluido", el único factor de forma **Wearable** y **0
dispositivos que dejan de ser compatibles**. Única advertencia: la de símbolos de depuración
nativos, opcional.

Fue posible enviarlo solo porque el Envío 14 (mobile `360110100`) ya estaba aprobado, así que el
reloj no comparte envío con nada que tenga fecha límite.

#### ✅ Verificado en emulador (12/08/2026)

No hizo falta emparejar un teléfono. **La cola de sincronización del reloj es un punto de
observación mejor que el historial del teléfono**: persiste cada `Match` serializado en JSON y,
sin nodo conectado, `isMobileReachable()` devuelve false y no se vacía. Se lee con
`adb shell run-as com.gonzalocamera.padelcounter cat files/datastore/wear_sync_queue.preferences_pb`
y muestra exactamente qué encoló el reloj — id, timestamps y golpes — en vez del resultado
indirecto.

Partido de prueba en `Wear_OS_Large_Round`: al mejor de 1 set, ganado 6-0.

| Escenario | Partidos en la cola | `finishedAt` |
|-----------|--------------------|--------------|
| Al terminar el partido | 1 — id `1786564407024-60` | `1786564721924` (5m 15s, real) |
| **Código nuevo**, 3 arranques en frío | **1** (sin cambios) | **sin cambios** |
| **Código viejo** (`cc38f16`), 1 arranque en frío | **2** — el segundo con id UUID `f460ac3f-…` | **`1786564902921`** → 8m 16s |
| Código nuevo otra vez, 2 arranques más | sigue en 2, **no crece** | sin cambios |

El contrafactual es lo que le da valor: instalando el build anterior **sobre el mismo estado**, un
único arranque en frío reprodujo el bug — `startedAt` idéntico, `finishedAt` 3 minutos más tarde
(justo el tiempo transcurrido hasta reabrir la app) y una fila nueva con id aleatorio. Es el
mecanismo del partido de 85h en miniatura, y confirma que la prueba **detecta** el defecto en vez
de solo pasar.

Truco necesario en el emulador: `adb shell svc power stayon true`. Sin eso la pantalla del reloj
se duerme, los taps se pierden y hasta abre los ajustes del sistema.

#### Lo que queda por verificar en hardware

Dos cosas que el emulador no cubre, ninguna bloqueante:

- **Los golpes.** `strokesPerSet` vino `null` porque el emulador no genera datos de acelerómetro.
  El mecanismo por el que se perdían exigía un **segundo** encolado, y eso es justo lo que quedó
  demostrado que ya no ocurre, así que está cubierto por implicación — pero conviene confirmarlo
  con el reloj real.
- **El lado del teléfono** (que no aparezca la fila duplicada). Es consecuencia de lo anterior más
  el `INSERT OR IGNORE`, que ya existía y no se tocó.

Antes de instalar en el reloj real, resetear el partido en curso (ver la nota de migración de la
sección 4).

### Release 4 — ficha nueva + `:mobile` 1.2.0 · ✅ PUBLICADA (Envíos 16 y 17)

Fue en dos envíos a propósito, no en uno: agrupar un cambio de ficha con un binario hace que la
revisión del texto retenga al AAB (regla anotada en `publishing-guide.md`).

- **Envío 16 (20/08/2026)** — solo ficha: nombre a *Simple Padel Score: Marcador*, descripción y
  capturas en es-419. Publicado.
- **Envío 17 (27/08/2026 14:05 → publicado 14:22)** — `mobile 360120000` (1.2.0), lanzamiento
  completo. Lleva la **invitación a calificar** basada en puntaje de uso (`ReviewPolicy` en
  `:shared`, disparada desde `NavGraph`) y el selector de involucramiento de la calculadora.

### Release 5 — Wear OS 1.2.0: fix del crash en Android 16 · ✅ AAB LISTO (02/09/2026)

`wear 350120003`, versionName 1.2.0 (se alinea con el mobile ya publicado). Compilado y
archivado en **`release-artifacts/padel-wear-v1.2.0-vc350120003.aab`**
(SHA-256 `a10688d02e0deb60389173057deb472edcef59ea08789a40630c38efce68aee4`).

Contenido: el fix del crash del foreground service (sección 4, verificado con contrafactual
en emulador API 36), la recalibración de sensibilidad (26/32/50 — el 32 validado en cancha
pasa a ser el default) con leyendas nuevas en el selector (verificadas a 192dp · fuente 1.30),
y el snapshot del reloj cuadrado como test.

Verificado sobre el AAB: versionCode `350120003`, versionName `1.2.0`, targetSdk `35`,
minSdk `30`, firma idéntica al bundle publicado (`5A:39:B2:…:5E:BB`). Suites de `:shared` y
`:wear` verdes.

**Conviene publicarla pronto**: es un crash real en producción (19 eventos, Galaxy Watch7 /
Android 16) y la base de Watch7+ con Wear OS 6 solo va a crecer. Al enviarla: track de
**Wear OS**, lanzamiento completo, y sin mezclar con cambios de ficha en el mismo envío
(checklist completo en `publishing-guide.md` §9).

### Release 6 — migración de toolchain

AGP / Kotlin 2.x / Compose BOM / Paparazzi 2.x, y recién ahí `compileSdk 36`. Va sola, sin
mezclar con cambios funcionales, porque lo que rompe es la UI y los screenshot tests al mismo
tiempo. **Plan detallado: [`toolchain-migration-plan.md`](toolchain-migration-plan.md).**

### Sin fecha

Los detalles menores de la sección 4 y las verificaciones de la consola de la sección 5,
cuando molesten.
