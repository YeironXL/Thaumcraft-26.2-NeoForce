# Vainas de maná y Mesa de deconstrucción (recreaciones de TC4)

Ninguna de las dos existe en Thaumcraft 6.1.BETA26 (no hay código de referencia en TC6-ref). Se recrean a partir de lo que se recuerda/documenta de Thaumcraft 4 y se adaptan al sistema de conocimiento de TC6.

## Fuentes consultadas

- Hilo FTB "Thaumcraft 4 - Mana Bean Farm": https://forum.feed-the-beast.com/threads/thaumcraft-4-mana-bean-farm.39167/latest
  - Confirma: las judías crecen en el exterior de los troncos y (versión posterior) **solo en la cara inferior de los troncos**; las judías llevan nombre de aspecto (Aqua, Instrumentum...); unas son más frecuentes que otras; se cosechaban con gólems.
- FTB wiki Thaumcraft 4 (revisión): https://ftb.gamepedia.com/Thaumcraft_4?diff=42038 (listado de contenido de TC4, incluye Mana Pod y Deconstruction Table).
- Wiki TC4, Artifice: https://thaumcraft-4.fandom.com/wiki/Artifice (solo nombra la Deconstruction Table como máquina mágica; no da detalles).
- `/home/claude/outputs/TC4_TC5_FALTANTES.md` (análisis previo: Mana Pod → Recrear; Deconstruction Table → Adaptar dando OBSERVATION).
- No se pudo acceder a páginas específicas "Mana Pod (Thaumcraft 4)"/"Mana Bean" con detalle; no se han saltado bloqueos.

## Vainas de maná (`thaumcraft:mana_pod`) y Judías de maná (`thaumcraft:mana_bean`)

| Comportamiento | Origen |
|---|---|
| Cuelga de la cara inferior de troncos | Fuente (hilo FTB) |
| Troncos válidos: gran árbol / árbol de plata, o cualquier tronco en bioma `c:is_magical` | Recuerdo TC4 / encargo |
| Aspecto según el bioma (`BiomeHandler.randomTag`, el `getRandomBiomeTag` de TC4) con primal aleatorio de reserva | Recuerdo TC4 (no verificado) |
| 8 fases (AGE 0-7), crecimiento por random tick con probabilidad 1/4 | Fases: recuerdo TC4; probabilidad: **invención** |
| Cruce: al madurar, 1/3 de probabilidad de adoptar el aspecto compuesto con una vaina vecina horizontal de otro aspecto | Recuerdo difuso de TC4, **parámetros inventados** |
| Madura: suelta 1-3 judías del aspecto; inmadura: nada | **Invención** (cantidad) |
| Judía: comida siempre comestible (1 hambre, 0.3 sat), 60% de probabilidad de un efecto de poción aleatorio no instantáneo 5-15 s | Comestible: recuerdo TC4; efectos: **invención** |
| Judía en crisol = 1 punto de su aspecto (igual que el cristal de vis) | Encargo/TC4 |
| Plantar: clic derecho en la cara inferior de un tronco válido | Recuerdo TC4 |
| Worldgen: 24 intentos por chunk en el Bosque mágico de colocar vainas (fase 0-5) bajo troncos | **Invención** (frecuencia) |
| Modelos/texturas: vaina morada con tallo, 3 tamaños; judía en gris teñida por el color del aspecto (como el cristal de vis) | **Invención** estilo TC6 |

Investigación `MANAPODS` (Alquimia, [-4,0], padre HEDGEALCHEMY): fase 1 pide 1 OBSERVATION de Alquimia y una judía.

## Mesa de deconstrucción (`thaumcraft:deconstruction_table`)

TC4: descomponía objetos en puntos de aspectos primales para investigar. TC6 ya no usa puntos por primal, así que se adapta:

- Un hueco; acepta cualquier objeto con aspectos (`ObjectAspects`).
- Cada `100` ticks destruye 1 unidad y da al **dueño** (último jugador que abrió la mesa, que debe estar conectado) conocimiento `OBSERVATION` en cada categoría según `ObservationFormula.calculate` (la misma fórmula ponderada que el escaneo de TC6). Si ninguna categoría puntúa, 1 en BASICS. Ej.: lingote de hierro → ARTIFICE +4.
- A diferencia del escaneo, es repetible (coste: el objeto). Duración y repetibilidad: **invención/adaptación**.
- GUI con barra de progreso (textura derivada del panel de piedra de `gui_spa`), tooltip con el %.
- Receta arcana (invención inspirada en TC4 "mesa + pico"): pico de hierro sobre mesa de madera, 20 vis + 1 cristal perditio.
- Investigación `DECONSTRUCTION` (Artificio, [-2,1], padre BASEARTIFICE): fase 1 pide 1 OBSERVATION de Artificio y un pico de hierro.

## Pruebas

`ManaDeconstructionVerification`: `mana_pod_growth`, `mana_bean_crucible`, `deconstruction_table` (72/72 gametests pasan).

Escena de depuración: `-PdebugHold=first -PdebugMana=pods|gui`.
