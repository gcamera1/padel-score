# Proyecto: Padel Score (Wear OS)

## Stack
- Kotlin, Jetpack Compose for Wear OS
- DataStore para persistencia
- Arquitectura simple: presentation (UI), logic (PadelLogic), repository (PadelRepository)

## Reglas de código
- Preferir funciones puras en lógica (input -> output), sin side effects.
- UI: evitar padding extra en 40mm; priorizar layout para pantallas pequeñas.
- No introducir librerías nuevas sin justificar.
- Mantener nombres en español para textos UI, código en inglés ok.

## UX
- Tap: suma punto; double tap: resta punto.
- Feedback háptico en tap y double tap.
- Ajustes: solo "Pantalla siempre encendida" y "Color de cancha".
- Nuevo partido: (TB7 vs SUPER10) + Punto de oro.

## Output esperado
- Cuando sugieras cambios, indicá: archivo(s) y diff/fragmentos exactos.
- Evitar reescribir archivos enteros salvo que se pida explícito.