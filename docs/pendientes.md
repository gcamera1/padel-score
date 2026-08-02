# Pendientes — Simple Padel Score

Estado al **1 de agosto de 2026**, después de enviar la app de Wear OS a revisión.

## Estado actual de la publicación

| Artefacto | versionCode | versionName | Estado |
|-----------|-------------|-------------|--------|
| mobile | 350100000 | 1.0.0 | Producción, 177 países (publicado 30/07/2026) |
| wear | 340100003 | 1.0.0 | **En revisión** — Envío 11, 01/08/2026 01:37 |

La release de reloj va a producción al 100% en 177 países. Con *Publicación gestionada
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
cumple igual. **Pendiente menor:** cuando Paparazzi 2.x sea estable, subir `compileSdk` a 36
y regrabar los snapshots de `:mobile`.

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

Los tres son de "calidad técnica" / "experiencia de usuario", ninguno bloqueante:

- `androidx.fragment` desactualizado (viene por transitividad, no se usa directo)
- Edge-to-edge: "es posible que la vista de extremo a extremo no funcione para todos los
  usuarios"
- Edge-to-edge: "tu aplicación usa APIs o parámetros obsoletos"

Los dos de edge-to-edge se resuelven juntos y hay un skill (`google-edge-to-edge`) que cubre
la migración. Conviene atacarlos en la misma tanda que el salto a `targetSdk 36`, porque el
36 endurece justamente ese comportamiento.

---

## 3. Detalles menores de código

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

## 4. Cosas a verificar en Play Console

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

## 5. Housekeeping local

- **Desinstalar del reloj el APK de prueba.** El instalado en el Galaxy Watch 6 está firmado
  con la **debug keystore** (bundletool la usó para poder instalarlo local). Play **no puede
  actualizar sobre una firma distinta**: hay que desinstalarlo antes de bajar la app de la
  tienda, o el error no va a ser claro.
- **`release-artifacts/padel-wear-v1.0.0-vc11.aab`** quedó de una tanda anterior, con el
  esquema de versionCode viejo. Se puede borrar.
- **El video del foreground service** tiene que seguir accesible en YouTube como **No
  listado** (no Privado) mientras dure la revisión: si el revisor no lo puede abrir, rechaza.

---

## Orden sugerido

1. Esperar la aprobación del reloj (nada que hacer).
2. Una sola tanda para `:mobile` + `:wear`: `targetSdk` 36 y 35, edge-to-edge, y de paso
   publicar las features del 31/7 y arreglar el cartel del companion. Cierra el plazo del
   31 de agosto y la deuda funcional de una vez.
3. Sacar de circulación el bundle vc5 del canal cerrado.
4. Los detalles del punto 3 cuando molesten.
