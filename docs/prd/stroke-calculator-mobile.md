# PRD — Calculadora de Golpes (Mobile)

**Producto:** Padel Score — Wear OS + companion Android
**Feature:** Simulador / calculadora manual para estimar golpes sin reloj
**Plataforma:** Android (celular), API 26+, Material3 Compose
**Módulos afectados:** `:mobile`, `:shared`
**Estado:** Final — listo para spec
**Autor:** Gonzalo Cámera
**Fecha:** 2026-06-20

> **Origen:** sección 2 ("Simulador de Impactos / Calculadora Manual") del PRD original
> "Analítica de Impactos". La sección 1 de ese PRD ya está implementada (etapa 2,
> [`stroke-stats-mobile.md`](stroke-stats-mobile.md)). Este documento cubre la pieza restante.

---

## 1. Contexto

La app ya interpreta los golpes reales que manda el reloj (PGG + veredicto por categoría, `StrokeStats` en `:shared`). Pero en partidos donde el usuario **no usó el reloj**, no hay ningún dato. La calculadora llena ese hueco: permite **estimar** los golpes a mano a partir de tres variables simples, y devuelve el mismo veredicto que un partido real, reutilizando la lógica ya existente.

## 2. Problema

Sin reloj no hay número. El usuario que jugó "a ojo" no puede ubicar su partido en la escala de intensidad. Tampoco puede explorar escenarios hipotéticos ("¿y si hubiera sido un partido de puro desgaste?"). Falta una herramienta de estimación rápida y reactiva.

## 3. Objetivos

- Estimar **tus golpes** y tu **PGG** a partir de 3 inputs (games, intensidad, involucramiento) + categoría.
- Devolver el **veredicto** reutilizando `PadelCategory.verdict()` (la misma lógica que los partidos reales).
- **Reactividad total:** sin botón "calcular"; el resultado se actualiza al mover cualquier control.
- Lógica de estimación **pura y testeable** (`:shared`).
- Cero fricción: accesible desde Ajustes, sin tocar el historial.

## 4. Fuera de alcance (v1)

- **Guardar la simulación como registro** en el historial. La calculadora es 100% volátil.
- **Cambios en el reloj** o en el flujo de sync.
- **Persistir** los valores de los controles entre sesiones (arranca siempre en defaults).
- **Notificación push** de sync tardío (mencionada en el PRD original, fuera de alcance acá).
- **Gráfico de barras por set** (eso es de la sección 1 / datos reales, no de la calculadora).

## 5. Modelo de cálculo

Tres inputs del usuario + categoría → estimación. Fórmula:

```
puntos_totales = games × pts_por_game(intensidad)
golpes_totales = puntos_totales × golpes_por_punto(categoria)   // los 4 jugadores
tus_golpes     = round(golpes_totales × involucramiento)
tu_PGG         = tus_golpes / games
veredicto      = categoria.verdict(tu_PGG)
```

**Constantes:**
- `pts_por_game`: Baja **4.5** · Media **5.5** (default) · Alta **7.0**
- `involucramiento`: **15%** · **25%** (default) · **35%**
- `golpes_por_punto` **por categoría** (promedio entre los 4 jugadores): 7ma **4.3** · 6ta **6.25** · 5ta **7.5**
  (7ma y 6ta vienen del FDD validado; 5ta extrapolada sobre la misma escalera)

**Consecuencia intencional del modelo:** el **PGG no depende de los games** (se cancelan: `PGG = pts_por_game × GOLPES_POR_PUNTO × involucramiento`). Por lo tanto:
- El slider de **games** afecta **"tus golpes estimados"** (el total), pero **no** el veredicto.
- El **veredicto** lo definen **intensidad + involucramiento + categoría**.

Esto es correcto: el veredicto mide intensidad por game, no volumen total. Se documenta para que no se lea como bug.

**Ejemplo (defaults: 18 games, Media, 25%, 6ta):** puntos = 99 → golpes_totales = 99 × 6.25 = 619 → tus_golpes ≈ **155** → PGG ≈ **8.6** → ⚖️ **Equilibrado**. (Calza con un partido real de referencia: PGG ≈ 9.7.)

**Copy de feedback:** además del badge, la card muestra un texto descriptivo según el veredicto (estilo FDD), por veredicto (no por categoría, para cubrir también 5ta).

## 6. User Stories

- Como jugador, quiero estimar cuántos golpes pegué en un partido donde no llevé el reloj, moviendo unos pocos controles.
- Como jugador, quiero ver el veredicto (Heladera/Normal/Alto desgaste/Maratón) de mi estimación, igual que en un partido real.
- Como jugador, quiero simular escenarios hipotéticos cambiando intensidad e involucramiento y ver el resultado al instante.
- Como jugador, quiero elegir la categoría dentro de la calculadora para simular en cualquier nivel sin cambiar mi ajuste global.
- Como jugador, quiero que la calculadora no ensucie mi historial de partidos reales.

## 7. Requisitos funcionales

### RF1 — Acceso desde Ajustes
Un botón **"Calculadora de golpes"** en la pantalla de Ajustes que abre la pantalla de la calculadora.
- Criterios de aceptación:
  - [ ] El botón aparece en Ajustes.
  - [ ] Al tocarlo, navega a la pantalla de la calculadora.
  - [ ] Desde la calculadora se puede volver a Ajustes (back).

### RF2 — Controles de entrada (4 controles)
1. **Games totales** (slider): rango **12–36**, default **18**, paso entero.
2. **Intensidad de puntos** (selector segmentado): Baja / Media (default) / Alta.
3. **Tu involucramiento** (selector segmentado): 15% / 25% (default) / 35%.
4. **Categoría** (selector segmentado): 7ma / 6ta (default) / 5ta. Propio de la calculadora, **no** afecta el ajuste global.
- Criterios de aceptación:
  - [ ] Los 4 controles aparecen con sus defaults.
  - [ ] El slider de games muestra el valor actual.
  - [ ] La categoría elegida acá no cambia la de Ajustes.

### RF3 — Cálculo reactivo y salida
Al mover **cualquier** control, recalcular y mostrar al instante (sin botón):
- **Tus golpes estimados** (entero, protagonista).
- **PGG** estimado (1 decimal).
- **Badge de veredicto** (mismo `StrokeVerdictBadge` que el resto de la app).
- Criterios de aceptación:
  - [ ] El resultado se actualiza en tiempo real al mover cualquier control.
  - [ ] El veredicto usa `PadelCategory.verdict()` con la categoría seleccionada en la calculadora.
  - [ ] Cambiar solo los games cambia el total pero no el veredicto (consistente con el modelo).

### RF4 — Volatilidad
La calculadora no persiste nada: ni el historial, ni los valores de los controles.
- Criterios de aceptación:
  - [ ] Nada de lo simulado aparece en Historial ni en Estadísticas.
  - [ ] Al reabrir la calculadora, los controles vuelven a los defaults.

## 8. Requisitos no funcionales

- **Testabilidad:** la fórmula de estimación es pura (`:shared`), testeable sin emulador.
- **Consistencia:** reusa `PadelCategory`, `StrokeVerdict`, `StrokeVerdictBadge` y `verdict()` existentes — sin duplicar lógica de rangos.
- **Aislamiento:** no toca Room, DataStore ni el sync.

## 9. Decisiones tomadas (registro de la discusión)

- **Ubicación:** botón "Calculadora de golpes" en **Ajustes** (no tab nuevo, no dentro de Estadísticas).
- **Categoría:** **selector propio** en la calculadora, independiente del ajuste global.
- **Guardar:** **no** en v1 — 100% volátil.
- **Golpes por punto:** constante **por categoría** (FDD): 7ma 4.3 · 6ta 6.25 · 5ta 7.5 (extrapolada). Solo afecta la calculadora.
- **Umbrales de 7ma:** se **actualizan** a los del FDD (`<4.2 / 4.2–6.5 / 6.6–8.5 / >8.5`), reemplazando la extrapolación previa (`2.0/…/10.0`). Esto **también re-diagnostica los partidos reales en 7ma** (etapa 2), sin migración (el veredicto se deriva en lectura). 6ta y 5ta sin cambios.
- **Categorías:** se mantiene **5ta** (además de 7ma/6ta del FDD).
- **Copy:** texto largo de feedback **solo en la calculadora**, por veredicto. Los badges cortos del resto de la app no cambian (Heladera/Normal/Alto desgaste/Maratón).

## 10. Métricas de éxito

- La estimación con defaults cae en un veredicto coherente con partidos reales equivalentes.
- El usuario puede llegar a un resultado en < 10 s sin instrucciones.
