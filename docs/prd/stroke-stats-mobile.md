# PRD — Estadísticas de Golpes (Mobile)

**Producto:** Padel Score — Wear OS + companion Android
**Feature:** Procesamiento y visualización de los golpes recibidos del reloj (lado celular)
**Plataforma:** Android (celular), API 26+, Material3 Compose
**Módulos afectados:** `:mobile`, `:shared`
**Estado:** Final — listo para spec
**Autor:** Gonzalo Cámera
**Fecha:** 2026-06-20

> **Alcance de este PRD:** la **etapa 2** del contador de golpes — *qué hace el celular con el dato crudo que el reloj ya envía*. El reloj (etapa 1, ya en producción) manda `Match.strokesPerSet` (golpes del usuario por set). Este documento cubre persistir ese dato, derivar métricas e interpretarlas con un veredicto, y mostrarlo en Historial y Estadísticas.

---

## 1. Contexto

La etapa 1 (PRD [`stroke-counter-wear.md`](stroke-counter-wear.md)) dejó al reloj contando golpes por set y enviándolos al celular dentro del `Match` que ya se sincroniza vía Wearable `DataClient`. El `SyncBridgeListener` del celular **decodifica** el `Match` completo —`strokesPerSet` incluido— pero al persistir en Room ese campo **se descarta**: `MatchEntity` no tiene columna para él, así que `Match.toEntity()` lo pierde silenciosamente.

Resultado actual: el dato llega al celular y se tira. Este feature cierra ese hueco y construye encima la capa de interpretación que vive **solo en el celular** (el reloj nunca interpreta, solo cuenta).

## 2. Problema

El usuario tiene el número crudo de golpes pero ningún contexto para entenderlo. "Pegué 180 golpes" no dice nada por sí solo: ¿es mucho, poco, normal? La respuesta **depende de la categoría** en la que juega (el volumen de juego de 7ma no es el de 6ta ni el de 5ta). Hace falta convertir el número en un **veredicto interpretable** y calibrado por categoría.

## 3. Objetivos

- **Persistir** `strokesPerSet` en Room sin romper partidos viejos ni el flujo de sync actual.
- Derivar la métrica reina: **PGG (Promedio de Golpes por Game) = golpes totales ÷ games totales**, que normaliza por largo de partido.
- Traducir el PGG en un **veredicto** (Heladera / Normal / Alto desgaste / Maratón) **calibrado por categoría**, configurable por el usuario.
- Mostrar el veredicto **por set y del partido completo** en el detalle de Historial.
- Mostrar un agregado histórico en la pantalla de Estadísticas.
- Toda la lógica de cálculo es **pura y testeable** (`:shared`).

## 4. Fuera de alcance (v1)

- **Cambios en el reloj.** El reloj ya manda el dato; no se toca `:wear`.
- **Clasificar tipo de golpe** (drive, revés, volea, smash).
- **Gráficos de evolución temporal** del PGG (tendencia partido a partido). Posible v2.
- **Comparación contra otros jugadores / nube.** Todo es local.
- **Editar manualmente el conteo de golpes** de un partido.
- **Re-calibrar categorías con datos propios.** Los umbrales son fijos por categoría (configurables solo cambiando de categoría).

## 5. Base de calibración (data de referencia)

Veredicto manejado por **PGG individual**. Umbrales por categoría (ancladas en 6ta, extrapoladas 7ma y 5ta sobre la misma escalera):

| Categoría | 🧊 Heladera | ⚖️ Normal | 🔨 Alto desgaste | 🦸 Maratón |
|---|---|---|---|---|
| **7ma** | < 2.0 | 2.0 – 6.5 | 6.6 – 10.0 | > 10.0 |
| **6ta** (default) | < 6.5 | 6.5 – 11.0 | 11.1 – 14.5 | > 14.5 |
| **5ta** | < 11.0 | 11.0 – 15.5 | 15.6 – 19.0 | > 19.0 |

Validación externa: la franja de 6ta (≈6–6.5 golpes totales por punto sumando 4 jugadores) es coherente con estudios de pádel amateur/sub-élite (6–7 golpes/punto en jugadores jóvenes; 4–6 en recreativo), por debajo del profesional (8–11). Los números son de copy y fáciles de ajustar.

## 6. User Stories

- Como jugador, quiero que mis golpes se guarden en el historial del celular para no perder el dato que mandó el reloj.
- Como jugador, quiero elegir mi categoría en ajustes para que el veredicto esté calibrado a mi nivel.
- Como jugador, quiero ver en el detalle de cada partido cuántos golpes pegué por set y en total, con un diagnóstico de si toqué poco, normal, mucho o fue una maratón.
- Como jugador, quiero ver el diagnóstico **por set**, porque puedo haber tocado poco un set y muchísimo otro.
- Como jugador, quiero ver en Estadísticas mi volumen histórico de golpes y mi intensidad típica.
- Como jugador, quiero que los partidos sin dato de golpes (viejos, o jugados con el contador apagado) no rompan nada ni muestren métricas falsas.

## 7. Requisitos funcionales

### RF1 — Persistencia de `strokesPerSet` en Room
Guardar el conteo por set que llega del reloj.
- Nueva columna nullable en `MatchEntity` para `strokesPerSet` (serializada como JSON de `List<Int>`).
- Migración de Room que agrega la columna sin destruir datos.
- Mapear el campo en `Match.toEntity()` y `MatchEntity.toMatch()`.
- Criterios de aceptación:
  - [ ] Un `Match` con `strokesPerSet = [42,38]` recibido del reloj se guarda y se relee íntegro.
  - [ ] Un `Match` con `strokesPerSet = null` se guarda y relee como `null`.
  - [ ] Los partidos previos a la migración siguen leyéndose (columna `null`).
  - [ ] La migración no borra la base existente.

### RF2 — Cálculo de métricas de golpes (lógica pura, `:shared`)
Derivar las métricas a partir del `Match` y la categoría.
- **PGG** = golpes totales ÷ games totales del partido. Games por set = suma de games de ambos en `setsScore`.
- **Por set:** golpes, games y PGG de cada set.
- **Veredicto** (por set y total) = banda de PGG según la tabla de la categoría.
- Si `strokesPerSet == null`, no hay métricas (la función devuelve `null`).
- Criterios de aceptación:
  - [ ] PGG = total golpes / total games, con manejo de división por cero (games 0 → PGG 0).
  - [ ] El veredicto respeta los bordes de banda de la categoría (ej. en 6ta, PGG 6.5 → Normal, 11.0 → Normal, 11.1 → Alto desgaste).
  - [ ] El cálculo por set alinea `strokesPerSet[i]` con los games del set `i`.
  - [ ] Es función pura: mismos inputs → mismo output, sin dependencias Android.

### RF3 — Selección de categoría (Ajustes del celular)
Nuevo setting que define la calibración del veredicto.
- Opciones v1: **7ma / 6ta / 5ta**. Default **6ta**.
- Persiste en `UserPreferences` (DataStore del celular). Global, no por partido.
- Cambiar la categoría recalcula los veredictos mostrados (no se recalcula ni se persiste nada del partido; el veredicto se deriva en tiempo de lectura).
- Criterios de aceptación:
  - [ ] El selector aparece en Ajustes con las 3 categorías.
  - [ ] La selección persiste entre sesiones.
  - [ ] Cambiar la categoría cambia el veredicto mostrado en Historial y Estadísticas.

### RF4 — Sección "GOLPES" en el detalle del partido (Historial)
Nueva sección en `MatchDetailScreen`, **solo si el partido tiene `strokesPerSet`**.
- **Total del partido:** golpes totales (número protagonista).
- **Por set:** una fila por set con `golpes · games · PGG` + badge de veredicto del set.
- **Veredicto del partido:** PGG total + badge de veredicto del partido.
- Si el partido no tiene dato de golpes, la sección no se muestra (o muestra un texto tenue "Sin datos de golpes").
- Criterios de aceptación:
  - [ ] La sección aparece entre las existentes (POR SET / DETALLES) solo con dato disponible.
  - [ ] Muestra total, desglose por set con su veredicto, y veredicto del partido.
  - [ ] Un set con PGG bajo y otro alto muestran veredictos distintos (independientes del total).
  - [ ] Partidos sin dato no muestran métricas falsas.

### RF5 — Métricas de golpes en Estadísticas (agregado)
Nueva sección "GOLPES" en `StatsScreen`, agregando **solo partidos con `strokesPerSet`**.
- **Golpes acumulados:** suma histórica de todos los golpes registrados.
- **Intensidad típica (PGG promedio):** PGG agregado + badge de veredicto.
- **Partido más maratónico:** máximo de golpes en un solo partido.
- Empty-state claro si no hay ningún partido con dato.
- Criterios de aceptación:
  - [ ] Las 3 métricas se calculan solo sobre partidos con dato.
  - [ ] Con 0 partidos con dato, se muestra el empty-state y no números en 0 confusos.
  - [ ] El badge de intensidad usa la categoría seleccionada.

## 8. Requisitos no funcionales

- **Backward compatibility:** partidos previos y partidos sin contador (campo `null`) deben funcionar en todas las pantallas sin métricas falsas.
- **Migración segura:** `ADD COLUMN` nullable; cero pérdida de datos.
- **Aislamiento:** un partido sin golpes nunca debe romper Historial ni Estadísticas.
- **Testabilidad:** PGG, veredicto y agregados son lógica pura en `:shared`, testeable sin emulador.
- **Consistencia visual:** la sección GOLPES reusa los componentes y el lenguaje visual existentes (`SectionHeader`, tiles, badges).

## 9. Decisiones tomadas (registro de la discusión)

- **Veredicto manejado por PGG** (normaliza por largo de partido), no por total absoluto. El total se muestra como dato informativo.
- **Categoría = selector global en Ajustes** (no por partido, no hardcodeada). Default 6ta. v1 con 7ma/6ta/5ta.
- **Veredicto por set + del partido completo** en el detalle (un set puede diferir del overall).
- **Umbrales fijos por categoría**, derivados de la data de 6ta; ajustables en código.
- El **reloj no se toca**; ya manda el dato crudo. Toda la interpretación es del celular.
- El veredicto se **deriva en tiempo de lectura** desde `strokesPerSet` + categoría; no se persiste el veredicto (cambiar categoría lo recalcula).

## 10. Métricas de éxito

- 100% de los partidos recibidos del reloj con golpes quedan persistidos (no se pierde el dato).
- El veredicto coincide con la percepción del usuario en cancha (validación cualitativa).
- Cero crashes / métricas falsas en partidos sin dato.
