# Guía de Publicación — Simple Padel Score

Un único listing en Google Play (`com.gonzalocamera.padelcounter`) con **dos artefactos**:
el del teléfono y el del reloj. Cada form factor se publica en su propio track.

## Estado actual

| Artefacto | versionCode | versionName | Estado |
|-----------|-------------|-------------|--------|
| mobile | 350100000 | 1.0.0 | **Producción**, 177 países |
| wear | 340100003 | 1.0.0 | Pendiente de subir |

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
- El reloj se queda en `targetSdk 34` a propósito: el requisito de API 36 de agosto 2026
  **exceptúa Wear OS**, que pide API 34+ (WO-P1).

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
| WO-P1 | targetSdk 34+ | ✅ `targetSdk = 34` |
| WO-P2 | No crashea al instalar/abrir | verificar en reloj real |
| WO-P5 | La app non-standalone conecta con el companion | ✅ `CompanionDetector` |
| WO-V3 | Swipe para cerrar funciona | verificar (el marcador usa swipe-left para ajustes) |
| WO-V13 | Fondo negro | ✅ tema negro-oro |
| WO-V14 | Mínimo 12sp en texto esencial | ✅ |
| WO-V16 | Contenido dentro del display, círculo de 192dp mínimo | ✅ `ScreenMetrics` (`fw² + fh² ≤ 1.0`); los screenshot tests cubren 225dp y 198dp |
| WO-G5 | Capturas 1:1, sin marco, sin alfa | ✅ vía `scripts/wear-store-screenshots.sh` |
| WO-G7 | Mismo package y misma key que el companion | ✅ verificado con `keytool` |
| — | Soporte 64-bit (obligatorio 15/09/2026) | ✅ `arm64-v8a` presente |

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
- [ ] Capturas de Wear OS generadas y sin alfa
- [ ] Declaración de foreground service completa en Play Console
