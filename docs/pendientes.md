# Pendientes — Simple Padel Score

Estado al **3 de agosto de 2026**.

## ⚠️ Google rechazó la v1.0.0 de Wear OS (3 ago 2026)

Tres violaciones de las Wear OS app quality guidelines. Las tres están **corregidas en el
branch `chore/target-sdk-bump`**, pero **falta verificarlas en hardware** antes de reenviar.

| Violación | Causa real | Estado |
|-----------|-----------|--------|
| **Tamaño de fuente de Wear** (WO-V1) | `WalkthroughScreen` era un `Column` sin scroll, sin padding lateral y con `\n` hardcodeados. Con la fuente del sistema en Largest (1.3) el contenido no entraba y se cortaba contra el borde curvo. | Corregido, sin verificar en reloj |
| **La funcionalidad no se comporta según lo descrito** | Misma causa que la anterior: "textos sin cortes cuando se selecciona un tamaño de fuente grande". | Corregido, sin verificar en reloj |
| **Falta la barra de desplazamiento** (WO-V8) | `MatchFinishedScreen` y `CompanionPromptScreen` tenían `positionIndicator = { }` vacío. | Corregido, sin verificar en reloj |

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

### El paso 0 del walkthrough: mejorado, con un límite geométrico

Con la fuente en Largest, los 48dp fijos de la pelota empujaban el "Tocá para continuar"
**fuera de la vista inicial**. La pelota pasa a escalar en sentido inverso a la fuente (34dp
desde `fontScale 1.1`, 24dp desde 1.25) y con eso el texto **ya aparece** en el Watch 6 real.

Queda al ras del borde: la última letra roza la curva. La causa es geométrica y no se arregla
con padding — se probó bajando el `top` del contentPadding y el spacing, sin ningún efecto. El
texto cae en la franja baja del círculo, donde a esa altura el ancho disponible es de ~98dp
(√(R²−y²) con R=101dp, y≈89dp) y "Tocá para continuar" a 13sp mide más que eso. Para que
entrara habría que acortar el texto o dejar que haga wrap en dos líneas, lo que lo empujaría
más abajo todavía.

Se deja así: el texto se lee, hay barra de desplazamiento, y un scroll mínimo lo muestra
completo. No es la infracción que marcó Play —que era texto recortado en un layout **sin**
posibilidad de scroll—, y es un caso de borde extremo: fuente máxima del sistema en el reloj
más chico.

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
| wear | 340100003 | 1.0.0 | 34 | **En revisión** — Envío 11, 01/08/2026 01:37 |
| mobile | 360110000 | 1.1.0 | 36 | Sin publicar — branch `chore/target-sdk-bump` |
| wear | 350110003 | 1.1.0 | 35 | Sin publicar — branch `chore/target-sdk-bump` |

La release de reloj 1.0.0 va a producción al 100% en 177 países. Con *Publicación gestionada
desactivada*, se publica automáticamente en cuanto Google la apruebe (plazo estimado: 7 días).

---

## 1. Fecha límite dura: 31 de agosto de 2026 — ✅ RESUELTO en código

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

### Limpiar el bundle vc5 del canal cerrado

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

## 4. Detalles menores de código

Ninguno bloquea nada. Ordenados por valor.

### `TimeText` sin `scrollAway` (UI de producción)

En las **5 pantallas con `ScalingLazyColumn`** de `:wear`, el título del contenido pasa por
debajo del reloj del sistema al scrollear. Se vio en la pantalla del companion prompt, pero
aplica a todas: `SettingsScreen` (L832), `TutorialScreen` (L1181), `NewMatchScreen` (L1243),
`MatchFinishedScreen` (L1371) y `CompanionPromptScreen` (L1458) en `MainActivity.kt`.

El arreglo canónico es `TimeText(modifier = Modifier.scrollAway(listState))` con
`import androidx.wear.compose.material.scrollAway`. Tres de las cinco pantallas ya tienen el
`listState` creado y pasado al `ScalingLazyColumn`; las otras dos hay que agregárselo.

### Config de test del Galaxy Watch

`CounterScreenshotTest.kt` usa `GALAXY_WATCH_40MM` con 792x792 @ XXXHIGH (198dp), que
corresponde al **Galaxy Watch 4/5 40mm**. El reloj real de prueba es un **Watch 6 40mm, que
es 432x432 (216dp)**. Conviene renombrar el config existente a `GALAXY_WATCH_4_40MM` (para que
el nombre no mienta) y, si se quiere cubrir el dispositivo propio, agregar uno de 864x864 @
XXXHIGH.

### Símbolos de depuración nativos (opcional)

Play advierte que el bundle del reloj no sube símbolos de depuración para su código nativo. Se
activa con `ndk.debugSymbolLevel = "SYMBOL_TABLE"` en el `buildTypes.release` del módulo. El
valor real es bajo: las dos `.so` del bundle (`libandroidx.graphics.path`,
`libdatastore_shared_counter`) son de dependencias de AndroidX, no código propio.

### Limitación conocida de Paparazzi

Las pantallas con `ScalingLazyColumn` renderizan **vacías** en Paparazzi: el componente
necesita una pasada de scroll que el render estático no hace. Por eso los screenshot tests
solo cubren `CounterScreen`. Forzarlo requeriría inyectar el `ScalingLazyListState` desde
afuera solo para el test. Ya está documentado en el header de `CounterScreenshotTest.kt`.

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
- **Descripción de la ficha.** Hoy describe solo la app de teléfono. WO-G2 pide listar las
  funciones principales; cuando el reloj esté aprobado conviene mencionarlo.
- **Canal de prueba interna de Wear OS.** Quedó sin testers asignados, así que no distribuye a
  nadie. Si se quiere usar más adelante, hay que crear la lista.

---

## 6. Housekeeping local

- **Desinstalar del reloj el APK de prueba.** El instalado en el Galaxy Watch 6 está firmado
  con la **debug keystore** (bundletool la usó para poder instalarlo local). Play **no puede
  actualizar sobre una firma distinta**: hay que desinstalarlo antes de bajar la app de la
  tienda, o el error no va a ser claro.
- **`release-artifacts/padel-wear-v1.0.0-vc11.aab`** quedó de una tanda anterior, con el
  esquema de versionCode viejo. Se puede borrar.
- **El video del foreground service** tiene que seguir accesible en YouTube como **No
  listado** (no Privado) mientras dure la revisión: si el revisor no lo puede abrir, rechaza.

---

## Plan de releases

Decidido el 02/08/2026. Tres publicaciones en secuencia, no todo junto:

### Release 1 — Wear OS 1.0.0 (en curso)

`wear 340100003`, ya enviada. **Nada que hacer**: esperar la aprobación de Google. Se publica
automáticamente al aprobarse.

### Release 2 — 1.1.0, ambos módulos (lista en el branch, sin subir)

Se sube **después** de que Google apruebe la Release 1, para no encimar dos revisiones del
mismo artefacto. Contenido:

- `targetSdk` 36 (mobile) y 35 (wear) → cierra el plazo del 31 de agosto (sección 1)
- migración de edge-to-edge → resuelve 2 de los 3 advisories de la consola (sección 1)
- las features del 31/7 que nunca se publicaron: carga manual, compartir, backup (sección 2)
- el cartel equivocado del companion, que se arregla solo al publicar mobile (sección 2)

Antes de subirla, hacer también:

- **Probar el edge-to-edge en el teléfono** — es lo único que ningún test cubre
- **Sacar de circulación el bundle vc5** del canal de prueba cerrada, o el aviso de API de
  Wear OS no se va (sección 1)
- Revisar el checklist de `publishing-guide.md` §9

### Release 3 — migración de toolchain

AGP / Kotlin 2.x / Compose BOM / Paparazzi 2.x, y recién ahí `compileSdk 36` en `:mobile`
(sección 3). Va sola, sin mezclar con cambios funcionales, porque lo que rompe es la UI y los
screenshot tests al mismo tiempo.

### Sin fecha

Los detalles menores de la sección 4 y las verificaciones de la consola de la sección 5,
cuando molesten.
