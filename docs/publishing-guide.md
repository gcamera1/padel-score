# Guía de Publicación — Simple Padel Score

Un único listing en Google Play (`com.gonzalocamera.padelcounter`) con **dos artefactos**:
el del teléfono y el del reloj. Cada form factor se publica en su propio track.

## Estado actual

| Artefacto | versionCode | versionName | targetSdk | Estado |
|-----------|-------------|-------------|-----------|--------|
| mobile | 350100000 | 1.0.0 | 35 | Producción, 177 países (desde 30/07/2026) |
| wear | 340100003 | 1.0.0 | 34 | Rechazada (Envío 11) |
| wear | 350110003 | 1.1.0 | 35 | Rechazada (Envío 12, 05/08/2026) — WO-V1 |
| wear | 350110103 | 1.1.0 | 35 | ✅ Publicada (11/08/2026, Envío 13) |
| mobile | 360110100 | 1.1.0 | **36** | ✅ Publicada (12/08/2026, Envío 14) |
| wear | 350110203 | 1.1.0 | 35 | ✅ Publicada (Envío 15) |
| mobile | **360120000** | **1.2.0** | 36 | **Borrador subido, sin enviar** (Envío 16) — invitación a calificar |

Cambios de ficha (nombre a "Simple Padel Score: Marcador", descripción y capturas, es-419):
**en revisión** desde el 20/08/2026, en un envío aparte del binario. El Envío 16 espera a que
esos se aprueben antes de mandarse.

El requisito de **12 testers / 14 días** ya fue cumplido con la app de teléfono. No se
repite al agregar el reloj: la habilitación de producción es a nivel de app, y como ambos
artefactos comparten `applicationId`, Play los trata como un solo listing con varios form
factors. Lo que sí falta para el reloj es el **opt-in al form factor Wear OS** y la
**revisión manual** de Google contra las
[Wear OS app quality guidelines](https://developer.android.com/docs/quality-guidelines/wear-app-quality).

## 1. Generar Keystore de Release

Si todavía no tenés un keystore:

```bash
keytool -genkey -v \
  -keystore padel-release.jks \
  -keyalg RSA -keysize 2048 \
  -validity 10000 \
  -alias padel-score \
  -storepass TU_STORE_PASSWORD \
  -keypass TU_KEY_PASSWORD
```

Guardá el archivo `.jks` en un lugar seguro fuera del repositorio.

**Ambos artefactos se firman con la misma key** — es requisito de Play para la relación
companion (WO-G7). Para verificarlo:

```bash
keytool -printcert -jarfile release-artifacts/padel-wear-v1.0.0-vc340100003.aab | grep SHA256
keytool -printcert -jarfile release-artifacts/padel-mobile-v1.0.0-vc350100000.aab | grep SHA256
# Los dos SHA256 deben ser idénticos.
```

## 2. Configurar Propiedades de Firma

En `~/.gradle/gradle.properties` (NO en el repo), agregá:

```properties
PADEL_STORE_FILE=/ruta/absoluta/a/padel-release.jks
PADEL_STORE_PASSWORD=tu_store_password
PADEL_KEY_ALIAS=padel-score
PADEL_KEY_PASSWORD=tu_key_password
```

Si estas propiedades no están presentes, el build genera AABs **sin firmar** y muestra un
warning.

## 3. Versionado

`versionName` es compartido; el `versionCode` es **por módulo** — Play exige un versionCode
distinto por form factor y recomienda que el del teléfono sea el más alto.

En `gradle.properties` de la raíz:

```properties
PADEL_MOBILE_VERSION_CODE=350100000
PADEL_WEAR_VERSION_CODE=340100003
PADEL_VERSION_NAME=1.0.0
```

Esquema de 9 dígitos: `[targetSdk 2][versión comercial 3][build 2][form factor 2]`

```
mobile: 35 · 010 · 00 · 00  ->  350100000
wear:   34 · 010 · 00 · 03  ->  340100003
```

**Reglas al versionar:**

- Cada AAB que subís a Play debe tener un versionCode **mayor** al de la última release
  publicada de **ese mismo track**. Subir el reloj no obliga a tocar el del teléfono.
- **Target API requerido por Play desde el 31 de agosto de 2026:** `:mobile` → 36
  (Android 16), `:wear` → 35 (Android 15). Wear OS queda exceptuado del 36, pero no del 35;
  la consola lo dice textual: *"La aplicación para Wear OS debe estar orientada a Android 15
  (nivel 35 de la API) o a una versión posterior"*. Ambos módulos ya cumplen.
  El umbral de API 34 que figura en la guía de calidad como WO-P1 es el que rigió desde
  agosto de **2025** — quedó viejo, la consola es la fuente autoritativa.
- **`:mobile` usa `compileSdk 35` con `targetSdk 36`** a propósito: Paparazzi 1.3.4 no
  soporta `compileSdk 36`. Ver el comentario en `mobile/build.gradle.kts`. Lo que Play
  evalúa es el targetSdk del manifest, así que el requisito se cumple igual.

## 4. Generar AABs Firmados

```bash
./gradlew :wear:bundleRelease       # -> wear/build/outputs/bundle/release/wear-release.aab
./gradlew :mobile:bundleRelease     # -> mobile/build/outputs/bundle/release/mobile-release.aab
```

Archivá el resultado en `release-artifacts/` con el nombre versionado (el directorio está
gitignorado):

```bash
cp wear/build/outputs/bundle/release/wear-release.aab \
   release-artifacts/padel-wear-v1.0.0-vc340100003.aab
```

Verificá el contenido antes de subir:

```bash
bundletool dump manifest --bundle wear/build/outputs/bundle/release/wear-release.aab
```

Chequeá que salga `versionCode`, `minSdkVersion="30"`, `targetSdkVersion="34"`,
`uses-feature android.hardware.type.watch` y `standalone = false`.

## 5. Capturas de la ficha (Wear OS)

```bash
./scripts/wear-store-screenshots.sh
```

Graba los snapshots de Paparazzi y los deja aplanados en
`release-artifacts/store-assets/wear/` (PNG 900x900, sin alfa).

**Por qué no se suben los PNG de Paparazzi directamente:** salen con las esquinas
transparentes por el recorte de pantalla redonda, y Play rechaza capturas con canal alfa
(WO-G5: 1:1, sin marcos de dispositivo, sin fondo transparente). El script las aplana sobre
negro, que además es el fondo que pide WO-V13.

## 6. Agregar el form factor Wear OS en Play Console

La relación companion sale automáticamente de tres cosas que ya están en el código:

- Ambos artefactos comparten `applicationId` y **la misma key de firma**
- El manifest de `:wear` declara `com.google.android.wearable.standalone = false`
- El manifest de `:wear` declara `<uses-feature android:name="android.hardware.type.watch" />`

> **No agregar `android:required="false"` a ese `uses-feature`, ni declararlo en el manifest
> de `:mobile`.** La [doc oficial lo prohíbe](https://developer.android.com/training/wearables/packaging):
> resulta en un único APK para teléfono y reloj, que no es una configuración soportada. El
> manifest del teléfono simplemente no menciona el feature.
>
> Tampoco agregar `<supports-screens>` al manifest del reloj: esos buckets legacy los usa
> Play para filtrar dispositivos y pueden excluir relojes de pantalla grande. El targeting a
> relojes lo hace `uses-feature android.hardware.type.watch`.

Pasos en la consola:

1. **Test and release → Advanced settings → pestaña Form factors → + Add form factor → Wear OS**
2. Subir las capturas de Wear OS a la ficha (las de `release-artifacts/store-assets/wear/`)
3. Subir el AAB del reloj a un **track de testing** (paso obligatorio del flujo; no hay
   espera de días, el requisito de testers ya está cumplido)
4. Volver a **Advanced settings** → **"Opt in to Wear OS and agree to the review policy"**
5. **Production** → selector de form factor arriba a la derecha → **"Wear OS only"** →
   **Create new release** → subir el AAB
6. Esperar la revisión de Google contra las Wear OS app quality guidelines

### Reenviar después de un rechazo

Al rechazar, Play **vacía el borrador de la versión** pero deja el cambio pendiente descrito en
el Resumen de publicación con el bundle viejo. Es confuso pero no es un problema: se abre el
borrador existente, se agrega el bundle nuevo, y el rechazado aparece solo en **"No incluido"**,
que es justo donde la remediación lo quiere. Conviene reconfirmarlo en la pantalla de revisión.

**Si al arrastrar el AAB dice "El código de versión N ya se ha usado":** no bumpees el
versionCode. Ese error casi siempre significa que **el bundle sí se ingirió** y el reintento
chocó con su propia copia. Usá **"Añadir de la biblioteca"** en la misma caja de subida y
agregalo desde ahí — es el mismo artefacto ya verificado, y evita rebuildear y volver a validar.
Pasó exactamente así en el Envío 13.

Ojo con un detalle de la biblioteca: **el bundle rechazado no figura** en ella, porque sigue
asociado a la versión de producción rechazada. Eso es normal, no significa que se haya perdido.

**Para auditar qué se envió de verdad** (útil si una subida se cortó y no quedó claro qué pasó):
Resumen de publicación → **Actividad de envíos**. Lista un renglón por envío con su ID, fecha,
canales y estado, y la flechita de la derecha abre el detalle con el `versionCode` incluido.
Subir un bundle y enviar a revisión son cosas **distintas**: se puede subir varias veces y enviar
una sola, así que las subidas fallidas no aparecen acá — y si no aparecen, no se enviaron.

### Wear OS y teléfono son releases separadas

Con Wear OS configurado como **tipo de versión** propio, cada form factor tiene su release de
Producción independiente. Al crear la del reloj:

- En **"Versión anterior → No incluido"** aparece solo el bundle de reloj anterior (el que se
  reemplaza) y ahí **tiene que quedarse**: no hay que apretarle "Incluir". Si además apareciera el
  bundle del **teléfono**, ese sí hay que incluirlo — dejarlo fuera lo despublica.
- En **"Cambios en tus dispositivos admitidos"** el único factor de forma debe ser **Wearable**, y
  la columna *"Dispositivos que ya no son compatibles"* debe dar **0**.
- En **Actividad de envíos** los envíos del reloj figuran como **"Producción (Wear OS)"** y los del
  teléfono como **"Producción"** a secas. Es la forma más rápida de auditar que un envío no se
  llevó puesto el otro form factor.

### No agrupar cambios de ficha con un binario

Play revisa **junto** todo lo que mandás en un mismo envío. Los cambios de ficha (nombre,
descripción, capturas) y una release de binario tienen riesgos y costos de arreglo muy
distintos, así que conviene mandarlos en envíos separados:

- El **nombre de la app** es el campo más sensible a política de todos (límite de 30 caracteres
  y prohibición de keyword stuffing). Si lo rebotan agrupado con un AAB, se frena el AAB también
  y no sabés cuál de los cambios lo causó.
- Un rechazo de ficha se arregla editando texto y reenviando, gratis. Un rechazo de binario
  puede costar rebuild y pelea con el "código de versión ya usado".

**Orden recomendado: primero la ficha, después el binario.** Además de lo anterior, la
invitación a calificar de la 1.2.0 manda tráfico a la ficha: si el binario sale primero, la
primera camada de usuarios que va a calificar aterriza en la vidriera vieja.

**Si lo que querés es que todo salga a la vez**, agrupar envíos no te lo da: con *Publicación
gestionada* desactivada cada cambio se publica solo en cuanto se aprueba. Para un go-live
coordinado hay que **prender Publicación gestionada** (Resumen de publicación, arriba a la
izquierda).

**El nombre del launcher no viene de la ficha.** Sale de `app_name` en
`mobile/src/main/res/values/strings.xml` y `wear/.../strings.xml`. Que el título de la tienda sea
más largo y descriptivo que la etiqueta del icono es lo normal y no hay que sincronizarlos.

### Si cambiás el bundle después de abrir el borrador

**Revisá el campo "Nombre de la versión".** Play lo autocompleta con el `versionCode` que había
en el borrador y **no lo actualiza** al reemplazar el bundle, así que puede quedar apuntando a un
artefacto que ya no existe. Pasó en el Envío 14: el nombre decía `360110000 (1.1.0)` con el
bundle `360110100` subido. No afecta la publicación —es una etiqueta interna— pero deja el
historial de versiones mintiendo sobre qué se envió, que es exactamente lo que uno va a consultar
dentro de seis meses.

## 7. Declaración de foreground service

El artefacto del reloj declara permisos que el del teléfono no tenía, por el contador de
golpes:

| Permiso | Para qué |
|---------|----------|
| `FOREGROUND_SERVICE` + `FOREGROUND_SERVICE_HEALTH` | `StrokeCounterService` (`foregroundServiceType="health"`) muestrea el acelerómetro durante el partido |
| `HIGH_SAMPLING_RATE_SENSORS` | frecuencia de muestreo necesaria para detectar picos de golpe |
| `POST_NOTIFICATIONS` | notificación del foreground service |

Play exige completar **App content → Foreground service permissions** con una justificación
y un **video demostrativo** del uso. Es la causa de rechazo más común en este escenario, así
que conviene tenerlo listo antes de mandar a revisión: un clip mostrando que el contador se
inicia al empezar un partido y se detiene al terminarlo.

## 8. Requisitos de calidad relevantes

Los que aplican a esta app, de la
[lista completa](https://developer.android.com/docs/quality-guidelines/wear-app-quality):

| ID | Requisito | Estado |
|----|-----------|--------|
| WO-P1 | targetSdk | ✅ `targetSdk = 35` — cumple el requisito que rige desde el 31/08/2026 |
| WO-P2 | No crashea al instalar/abrir | verificar en reloj real |
| WO-P5 | La app non-standalone conecta con el companion | ✅ `CompanionDetector` |
| WO-V3 | Swipe para cerrar funciona | verificar (el marcador usa swipe-left para ajustes) |
| WO-V1 | Respeta el tamaño de fuente del sistema sin cortar texto | ⚠️ **causó dos rechazos** — ver abajo |
| WO-V13 | Fondo negro | ✅ tema negro-oro |
| WO-V14 | Mínimo 12sp en texto esencial | ✅ |
| WO-V16 | Contenido dentro del display, círculo de 192dp mínimo | ✅ `ScreenMetrics` (`fw² + fh² ≤ 1.0`); los screenshot tests cubren 225dp y 198dp |
| WO-G5 | Capturas 1:1, sin marco, sin alfa | ✅ vía `scripts/wear-store-screenshots.sh` |
| WO-G7 | Mismo package y misma key que el companion | ✅ verificado con `keytool` |
| — | Soporte 64-bit (obligatorio 15/09/2026) | ✅ `arm64-v8a` presente |

### WO-V1 — texto con la fuente del sistema en grande

Es lo que rechazó la app dos veces. Reglas que salieron de ahí:

- **Nunca usar `Button`/`OutlinedButton` de Wear Material con una etiqueta de texto.** Son
  botones **circulares para iconos**: `size(52.dp)` fijo y sin padding interno, así que con la
  fuente en Largest el texto envuelve y pierde la primera y la última letra contra el borde de
  la píldora. Para botones de texto usar el helper `WideTextButton` (`Chip`/`OutlinedChip`).
- **Texto centrado, nunca alineado a la izquierda**, en pantallas de texto largo. En una pantalla
  redonda el ancho disponible a distancia `y` del centro es `√(R²−y²)`: si todas las líneas
  arrancan en el mismo `x`, las de arriba y abajo caen en la curva y se comen la primera letra.
- **Items cortos en `ScalingLazyColumn`.** La lista escala los items contra el borde curvo, pero
  solo si el item entra en la pantalla: uno más alto que el display se dibuja a escala 1.0 y sus
  líneas extremas quedan en las esquinas. Regla práctica: ≤3 líneas con la fuente en Largest.
- **Presupuesto vertical** de un reloj de 192dp con la fuente al máximo: ~122dp de contenido
  (192 menos el `contentPadding` de 40 arriba y 30 abajo). Un título de dos líneas más un mensaje
  de dos líneas ya no deja lugar para un botón.

El peor caso está **medido**, no estimado, en Ajustes → Accesibilidad → Tamaño del texto de
Wear OS (API 36): el slider de fuente tiene 7 pasos y termina en **1.24**; el de tamaño de
pantalla ya arranca en su máximo (320dpi = 192dp) y solo se puede bajar, lo que da *más* dp. Con
lo cual el peor caso alcanzable es **192dp · font_scale 1.24 · negrita**.

Verificación obligatoria antes de subir (los screenshot tests **no** alcanzan: las pantallas con
`ScalingLazyColumn` renderizan vacías en Paparazzi):

```bash
emulator -avd Wear_OS_Small_Round &            # 384x384 @ 320dpi = 192dp, el mínimo de WO-V16
adb shell settings put system font_scale 1.30  # por encima del tope de la UI, para tener margen
adb shell settings put secure font_weight_adjustment 300   # negrita
adb shell pm clear com.gonzalocamera.padelcounter
# recorrer TODAS las pantallas capturando con máscara circular, porque el framebuffer es
# cuadrado y el bisel recorta lo que queda fuera del círculo:
adb exec-out screencap -p > cap.png
# al terminar, restaurar:
adb shell settings put system font_scale 1.0
adb shell settings put secure font_weight_adjustment 0
```

## 9. Checklist Pre-Upload

- [ ] `applicationId` es `com.gonzalocamera.padelcounter` en ambos módulos
- [ ] `PADEL_WEAR_VERSION_CODE` ≠ `PADEL_MOBILE_VERSION_CODE`, y mayor al último publicado de su propio track
- [ ] `PADEL_VERSION_NAME` coincide en ambos módulos (el build lo verifica con `checkVersionConsistency`)
- [ ] El manifest de `:wear` tiene `standalone = false` y `uses-feature android.hardware.type.watch` **sin** `required="false"`
- [ ] El manifest de `:wear` **no** tiene `<supports-screens>`
- [ ] El manifest de `:mobile` **no** menciona `android.hardware.type.watch`
- [ ] Ambos AABs firmados con la misma key (`keytool -printcert -jarfile`, comparar SHA256)
- [ ] `./gradlew :shared:test :wear:test :mobile:test` pasa
- [ ] `./gradlew :wear:verifyPaparazziDebug` pasa (sin diffs visuales inesperados)
- [ ] Ningún `Button`/`OutlinedButton` de Wear con texto: `grep -n "OutlinedButton\|Button(" wear/src/main/**/MainActivity.kt` solo debe dar los dos botones de icono de `StrokeTestScreen`
- [ ] Recorrido completo en emulador de 192dp con `font_scale 1.30`, sin texto cortado (§8, WO-V1)
- [ ] Capturas de Wear OS generadas y sin alfa
- [ ] Declaración de foreground service completa en Play Console
