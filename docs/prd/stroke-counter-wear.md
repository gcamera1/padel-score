# PRD — Contador de Golpes (Wear)

**Producto:** Padel Score — Wear OS + companion Android
**Feature:** Detección y conteo de golpes a la pelota durante el partido (lado reloj)
**Plataforma:** Wear OS (Galaxy Watch 6/7), API 30-34
**Módulos afectados:** `:wear`, `:shared`
**Estado:** Final — listo para spec
**Autor:** Gonzalo Cámera
**Fecha:** 2026-06-19

> **Alcance de este PRD:** solo el reloj — *qué cuenta, cómo lo cuenta y cómo lo manda al celular*.
> El procesamiento de esos datos en el celular (badges, métricas derivadas, gráficos) es una **etapa posterior** y queda fuera de este documento. El reloj envía el dato **crudo**; toda la interpretación vive en el celular.

---

## 1. Contexto

La app ya tiene el ciclo completo de partido en el reloj: conteo de puntos, finalización y envío de estadísticas al celular companion vía Wearable `DataClient`, donde se persisten en Room. La serialización del partido pasa por `MatchCodec` en `:shared`.

Este feature **agrega una capa de telemetría sobre la arquitectura existente**: detectar cada golpe usando el acelerómetro del reloj, contarlos agrupados por set durante el partido, y mandar ese conteo al celular dentro del payload de estadísticas que ya se sincroniza. No reemplaza nada del flujo actual; lo extiende.

## 2. Problema

Hoy la app sabe *cómo terminó* cada punto, pero no *cuánto trabajo físico costó*. Dos partidos con el mismo resultado pueden tener una carga de juego muy distinta. No hay forma de medir volumen de juego ni intensidad.

## 3. Objetivos

- Detectar golpes on-device durante el partido con **precisión razonable** (objetivo: error <20%). No se busca exactitud perfecta.
- Enviar el conteo **agrupado por set** dentro del payload existente, sin romper el flujo actual ni los partidos viejos.
- Dar al usuario una herramienta para **autocalibrar** la sensibilidad sin depender de calibración del desarrollador.
- Mantener un impacto de batería **aceptable** en un partido típico (1,5-3 hs).

## 4. Fuera de alcance (v1)

- **Procesamiento en el celular** (badges, métricas derivadas, gráficos, comparación con histórico). Etapa 2.
- **Clasificar tipo de golpe** (drive, revés, volea, smash). Requiere etiquetado + modelo.
- **Detección con ML.** v1 es heurística de picos.
- **Distinguir golpes propios de gestos no-golpe con perfección.** Se asume que el umbral + debounce + la posición del reloj filtran el grueso del ruido; afinar es iterativo.
- **Conteo en vivo durante el partido en el reloj.** En partido se cuenta en background; el número en vivo solo existe en el test mode.
- **Soporte fuera de Galaxy Watch 6/7 / Wear OS.**

## 5. Premisa de uso

El reloj se lleva en la **muñeca de la mano que sostiene la paleta** (caso del autor: zurdo, reloj en la izquierda). Esto maximiza la señal del swing en el acelerómetro y es **condición para que la detección funcione bien**. Se comunica al usuario en el walkthrough inicial del reloj.

## 6. User Stories

- Como jugador, quiero que el reloj cuente automáticamente mis golpes durante el partido, sin hacer nada manual mientras juego.
- Como jugador, quiero poder encender o apagar el contador desde ajustes, para decidir si quiero capturar ese dato (y ahorrar batería si no).
- Como jugador, quiero un modo de prueba donde vea el contador en vivo mientras pego, para verificar y ajustar la sensibilidad a mi estilo de juego.
- Como jugador, quiero ajustar la sensibilidad (alto/medio/bajo) si veo que cuenta de más o de menos.
- Como jugador, quiero que si la detección falla o el sensor no está disponible, el partido se registre igual sin el dato de golpes, sin perder el resto de las estadísticas.

## 7. Requisitos funcionales

### RF1 — Captura de sensores en background (Foreground Service)
Muestrear el acelerómetro durante el partido mediante un **foreground service**, independiente del estado de la pantalla (encendida o apagada).
- La captura arranca al iniciar el partido y se detiene al finalizarlo (atada al ciclo de vida existente).
- En partido se usa **sensor batching** (entrega en lotes) para minimizar wakeups de CPU; no se necesita el dato en vivo.
- El service muestra una notificación mínima mientras corre (requisito de Android).
- Criterios de aceptación:
  - [ ] Al iniciar partido con el feature ON, el service se inicia y registra el sensor.
  - [ ] El conteo continúa con la pantalla apagada.
  - [ ] Al finalizar partido, el service se detiene y desregistra el sensor (no queda corriendo).
  - [ ] Si el feature está OFF, el service no se inicia y no se muestrea nada.
  - [ ] Si el sensor no está disponible, el partido funciona igual y el conteo se reporta `null`.

### RF2 — Detección de golpes (heurística de picos)
Detectar golpes como picos de magnitud de aceleración sobre un umbral, con debounce.
- Magnitud = √(x² + y² + z²) del acelerómetro.
- Pico sobre umbral configurable + ventana de debounce (~350 ms) para no contar dos veces el mismo swing.
- La lógica de detección es **pura y testeable** (vive en `:shared`).
- Criterios de aceptación:
  - [ ] Un swing claro se cuenta como exactamente 1 golpe.
  - [ ] Dos picos dentro de la ventana de debounce cuentan como 1.
  - [ ] El umbral aplicado depende de la sensibilidad configurada (RF5.3).

### RF3 — Agrupación por set y snapshot por game
El conteo se acumula **por set**, derivando el set en curso de `PadelState.setsHistory.size`.
- Cada golpe detectado incrementa el casillero del set actual.
- Al **terminar cada game** se persiste un snapshot del acumulado en DataStore (robustez ante muerte/reinicio del service).
- Criterios de aceptación:
  - [ ] El resultado es una lista de enteros, un valor por set (ej. `[42, 38, 51]`).
  - [ ] Tras cerrar un game, el acumulado queda persistido.
  - [ ] Si el service se reinicia, retoma desde el último snapshot (pérdida máxima: golpes del game en curso).

### RF4 — Envío al celular
Extender el payload de estadísticas con el conteo por set, reutilizando el canal actual.
- Nuevo campo en `Match` (`:shared`): `strokesPerSet: List<Int>? = null`.
- Reutiliza `WearSyncQueue` + `DataClient` + `MatchCodec` sin cambios estructurales.
- Criterios de aceptación:
  - [ ] Al finalizar partido, el `Match` enviado incluye `strokesPerSet`.
  - [ ] El celular recibe y persiste el valor sin romper el registro existente.
  - [ ] Partidos viejos sin el campo siguen decodificando bien (backward-compatible).
  - [ ] Si el feature estaba OFF o el sensor no estaba disponible, `strokesPerSet` va `null`.

### RF5 — Ajustes del reloj (3 opciones nuevas)
1. **Contador de golpes** — toggle ON/OFF. Default **ON**. Global (opción A: persiste, no se pregunta por partido).
2. **Probar contador** — entra a la pantalla de test (RF6).
3. **Sensibilidad del sensor** — selector Alto / Medio / Bajo. Default **Medio**.
   - Semántica: Alto = más sensible = cuenta más (umbral bajo). Bajo = menos sensible = cuenta menos (umbral alto).
- Criterios de aceptación:
  - [ ] Las 3 opciones aparecen en ajustes del reloj.
  - [ ] El toggle persiste y controla si se captura o no.
  - [ ] El nivel de sensibilidad seleccionado afecta tanto al partido como al test mode.

### RF6 — Pantalla "Probar contador" (test mode)
Pantalla para verificar y calibrar la detección en vivo.
- Fondo: layout de cancha de pádel. Contador numérico grande y centrado.
- Cuenta **en tiempo real** (sin batching) usando la sensibilidad actual.
- Dos botones circulares inline abajo: **↻** reinicia el contador a 0 · **✕** vuelve a ajustes.
- Es efímero: no afecta el conteo de ningún partido ni se persiste.
- Para cambiar la sensibilidad, el usuario vuelve a ajustes, la cambia y vuelve a probar.
- Criterios de aceptación:
  - [ ] El contador se incrementa en vivo al detectar golpes.
  - [ ] ↻ resetea a 0. ✕ vuelve a ajustes.
  - [ ] No modifica `strokesPerSet` de partidos ni ningún dato persistido.

### RF7 — Walkthrough del reloj
Agregar al walkthrough inicial del reloj un aviso: **"El contador de golpes solo funciona si llevás el reloj en la muñeca de la paleta."**
- Criterio de aceptación:
  - [ ] El mensaje aparece en el onboarding del reloj.

## 8. Requisitos no funcionales

- **Batería:** el muestreo no debe degradar de forma notoria la duración de un partido típico. Mitigación: foreground service con sensor batching; captura solo con feature ON.
- **Backward compatibility:** los partidos previos (sin `strokesPerSet`) deben seguir leyéndose. Garantizado por campo nullable + `Json { ignoreUnknownKeys = true }`.
- **Aislamiento:** un fallo en la captura de golpes nunca debe afectar el conteo de puntos ni el envío del resto de stats.
- **Testabilidad:** la lógica de detección es pura (`:shared`), testeable con magnitudes simuladas sin emulador.

## 9. Riesgos y mitigaciones

| Riesgo | Mitigación |
|---|---|
| Golpes suaves (globos, dejadas) generan picos chicos y se pierden | Sensibilidad ajustable por el usuario; test mode para calibrar |
| Ruido de gestos no-golpe (festejos, agarrar pelota) cuenta de más | Umbral + debounce; sensibilidad Baja; reloj en muñeca de paleta |
| Service matado por el sistema en partido largo | Snapshot por game en DataStore; foreground service de alta prioridad |
| Consumo de batería | Sensor batching; captura opt-in |

## 10. Métricas de éxito

- Precisión de conteo: comparar contra conteo manual (tandas controladas en test mode). Objetivo: error <20%.
- Tasa de partidos con dato capturado correctamente (no queda `null` por bug): objetivo >95%.
- Impacto de batería percibido como aceptable a lo largo de varios partidos.

## 11. Decisiones tomadas (registro de la discusión)

- El reloj manda **crudo por set** (`strokesPerSet`); el celular deriva total y promedio por game. El reloj no interpreta.
- Mecanismo único: **foreground service** independiente de la pantalla (no se ata a "pantalla siempre encendida"). Batching en partido, realtime en test.
- Sensor: **SensorManager** (no Health Services — este último no expone accel crudo a alta frecuencia).
- Calibración por el **usuario** vía test mode + sensibilidad (alto/medio/bajo), no calibración del desarrollador. Default Medio con umbral tentativo.
- Robustez: **snapshot por game** a DataStore.
- Switch global persistente (opción A), default ON.
