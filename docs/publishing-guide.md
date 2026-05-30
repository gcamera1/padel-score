# Guía de Publicación — Padel Score

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

## 2. Configurar Propiedades de Firma

En `~/.gradle/gradle.properties` (NO en el repo), agregá:

```properties
PADEL_STORE_FILE=/ruta/absoluta/a/padel-release.jks
PADEL_STORE_PASSWORD=tu_store_password
PADEL_KEY_ALIAS=padel-score
PADEL_KEY_PASSWORD=tu_key_password
```

Si estas propiedades no están presentes, el build generará AABs sin firmar y mostrará un warning.

## 3. Incrementar Versión

Antes de cada release, editá `gradle.properties` en la raíz del proyecto:

```properties
PADEL_VERSION_CODE=6        # Incrementar en 1 por cada release
PADEL_VERSION_NAME=1.1.0    # Seguir semver
```

Ambos módulos (`:mobile` y `:wear`) leen estos valores automáticamente. El build falla si difieren.

## 4. Generar AABs Firmados

```bash
# AAB del módulo mobile (teléfono)
./gradlew :mobile:bundleRelease

# AAB del módulo wear (reloj)
./gradlew :wear:bundleRelease
```

Los archivos resultantes están en:
- `mobile/build/outputs/bundle/release/mobile-release.aab`
- `wear/build/outputs/bundle/release/wear-release.aab`

Para generar ambos en un solo comando:

```bash
./gradlew :mobile:bundleRelease :wear:bundleRelease
```

## 5. Crear Listing en Google Play Console

### 5.1 Listing Único

1. Ir a [Google Play Console](https://play.google.com/console)
2. Crear una nueva app con el nombre **"Padel Score"**
3. El `applicationId` de ambos artefactos es `com.gonzalocamera.padelcounter`

### 5.2 Subir Artefacto Mobile (Teléfono)

1. Ir a **Release > Production** (o el track correspondiente)
2. Crear nueva release
3. Subir `mobile-release.aab`
4. Este artefacto se distribuye a dispositivos de teléfono

### 5.3 Subir Artefacto Wear OS

1. Ir a **Release > Wear OS**
2. Crear nueva release
3. Subir `wear-release.aab`
4. Este artefacto se distribuye exclusivamente a relojes Wear OS

### 5.4 Declarar Companion

La relación companion se establece automáticamente porque:
- Ambos artefactos comparten el mismo `applicationId` (`com.gonzalocamera.padelcounter`)
- El artefacto wear declara `com.google.android.wearable.standalone = false` en su AndroidManifest
- El artefacto mobile declara `<uses-feature android:name="android.hardware.type.watch" android:required="false" />`

Google Play Console reconoce esta relación al subir ambos artefactos al mismo listing.

## 6. Internal Testing

### Requisitos de la Política de Wear OS

Antes de promover a producción, la política de Google Play exige:
- **12 testers reales** como mínimo
- **14 días corridos** de testing activo en el track Internal Testing

### Procedimiento

1. Ir a **Testing > Internal testing**
2. Crear nueva release y subir ambos AABs (mobile y wear)
3. Crear una lista de testers con al menos 12 emails de cuentas Google
4. Enviar el link de opt-in a los testers
5. Los testers deben:
   - Aceptar la invitación desde el link
   - Instalar la app desde Google Play
   - Usarla al menos una vez
6. Esperar 14 días corridos desde que el primer tester se unió
7. Verificar en la consola que hay 12+ testers activos

### Ventaja de la App Companion

Con la app mobile como companion, los testers pueden participar del internal testing **instalando solo la app de teléfono**, sin necesidad de tener un reloj Wear OS. Esto simplifica enormemente alcanzar los 12 testers.

## 7. Promover a Producción

Una vez cumplidos los 14 días con 12+ testers:

1. Ir a **Release > Production**
2. Promover la release de internal testing a producción
3. Completar la ficha de la tienda (capturas, descripción, etc.)
4. Enviar para revisión

## 8. Checklist Pre-Upload

Antes de subir cada release, verificar:

- [ ] `applicationId` es `com.gonzalocamera.padelcounter` en ambos módulos
- [ ] `versionCode` es idéntico en `gradle.properties` (`PADEL_VERSION_CODE`)
- [ ] `versionName` es idéntico en `gradle.properties` (`PADEL_VERSION_NAME`)
- [ ] El manifest de `:wear` tiene `com.google.android.wearable.standalone = false`
- [ ] El manifest de `:mobile` tiene `<uses-feature android:name="android.hardware.type.watch" android:required="false" />`
- [ ] Ambos AABs están firmados con la misma key
- [ ] `./gradlew :shared:test` pasa todos los tests
- [ ] `./gradlew :mobile:compileDebugKotlin :wear:compileDebugKotlin` compila sin errores
- [ ] Se incrementó el `versionCode` respecto de la última release publicada
