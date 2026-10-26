# Nodos de aura (TC4) adaptados a TC6: fuentes y decisiones

TC6 (6.1.BETA26) no tiene nodos: usa aura por chunk (`AuraHandler`/`AuraChunk`, vis + flux). Solo quedó la textura
`textures/misc/auranodes.png` (la `nodes.png` de TC4: rejilla de 32x32 fotogramas de 64 px). Este documento separa lo
que viene de fuentes sobre TC4 de lo que se inventó para encajar en TC6.

No encontré el código descompilado de TC4 accesible desde esta sesión. La búsqueda de código en GitHub está
bloqueada y el repositorio `Azanor/thaumcraft-api` no está habilitado. Por eso las constantes marcadas como
**(memoria)** salen de mi recuerdo del código de TC4 y no las he verificado en línea.

## Fuentes consultadas

- FTB Forum, "What do the different node types do? [ThaumCraft 4]" (TC 4.0.3a/4.0.5b): https://forum.feed-the-beast.com/goto/post?id=495581
- FTB Forum, "thaumcraft node nbt data": https://forum.feed-the-beast.com/threads/thaumcraft-node-nbt-data.138771/
- FTB Forum, "Thaumcraft 4.2 node capturing strategies": https://forum.feed-the-beast.com/threads/thaumcraft-4-2-node-capturing-strategies.56147/latest
- FTB Forum, "Thaumcraft node manipulation - what?" (2016, 1.7.10): https://forum.feed-the-beast.com/threads/thaumcraft-node-manipulation-what.159705/
- FTB Forum, "Thaumcraft 4 Hungry Node": https://forum.feed-the-beast.com/goto/post?id=540647
- Thaumcraft 4 Wiki, "The Beginning": https://thaumcraft-4.fandom.com/wiki/The_Beginning
- Thaumcraft 4 Wiki, "Advanced Node Stabilizer": https://thaumcraft-4.fandom.com/wiki/Advanced_Node_Stabilizer

## Hechos con fuente (TC4)

| Hecho | Fuente |
|---|---|
| Tipos: normal, inestable, siniestro (dark), corrupto, hambriento, puro. NBT `nodetype` 0..5 en ese orden | foro 495581, foro nbt |
| Modificadores: brillante (regenera más rápido), pálido (más lento), menguante (no regenera) | foro 495581 |
| Siniestro: crea bioma Eerie y genera zombis furiosos; cada Eerie se centra en uno | foro 495581, wiki The Beginning |
| Corrupto: crea Tainted Lands alrededor | foro 495581 |
| Puro: purifica (taint → bosque mágico); casi siempre sale de árboles de plata, rara vez salvaje; un puro pálido no convierte | foro 495581 |
| Hambriento: absorbe bloques y entidades (también al jugador) y gana sus aspectos | foro 495581 |
| La obsidiana es indestructible para el nodo hambriento | foro 540647 |
| Inestable: cambia sus aspectos al azar (en versiones antiguas quizá soltaba flux, sin confirmar) | foro 495581 |
| Los aspectos drenados regeneran despacio, salvo si se drenan por completo | foro 159705 |
| Nodos a 4 o más bloques no se "acosan" (bullying); el mayor drena al menor y puede crecer en ese aspecto | foros 159705 y 56147 |
| Drenar nodos a 0 los destruye rápido | foro 56147 |
| Escanear con el taumómetro revela aspectos y tipo y da puntos de investigación (~1/10 de la fuerza) | wiki The Beginning |
| Los nodos se ven sin taumómetro; las gafas los muestran a través de paredes | wiki The Beginning |
| Meter un nodo en un tarro cuesta vis y puede dañarlo; un nodo brillante sigue brillante | foro 159705 |
| Estabilizador: bloquea el acoso y reduce la regeneración. El avanzado acosa sin ser acosado | foro 159705, wiki |
| Nodo energizado (transductor): solo primales, salida ~ √(aspecto mayor), ×1.2 brillante / ×0.8 pálido | foro 56147 |

## Constantes de memoria (TC4, sin verificar en línea)

- `Config.nodeRarity = 36` (un intento por chunk) y `specialNodeRarity = 18`; modificador en ~1 de cada 9.
- `TileNode` regenera +1 por aspecto cada 600 ticks (brillante 400, pálido 900, menguante nunca).
- Valores de aspecto al generarse: ~15..40; nodos de árbol de plata más pequeños.
- El hambriento atrae a ~15 bloques.

## Implementación en el port

Código: `aura/node/{NodeType,NodeModifier,AuraNodeGen}.java`, `block/AuraNodeBlock.java`,
`blockentity/AuraNodeBlockEntity.java`, `item/JarredNodeItem.java`, `client/render/AuraNodeRenderer.java`.

### Fiel a TC4 (con fuente o de memoria)

- Seis tipos y tres modificadores con el orden NBT de TC4 (`nodetype`, `nodemod`).
- Regeneración de 600/400/900/0 ticks. Si un aspecto se drena a 0, el máximo baja 1, y si el nodo se queda vacío desaparece.
- Acoso: el nodo mayor drena 1 punto cada 2 s de otro menor a menos de 4 bloques y crece en ese aspecto con una probabilidad de 1/5.
- Hambriento: atrae entidades en 15 bloques, traga ítems y gana sus aspectos, muerde a los seres vivos y devora un bloque al azar cada 5 s en un radio de 5. No toca obsidiana, bloques irrompibles ni bloques con block entity.
- Inestable: cambia un punto de aspecto al azar (~1 vez cada 20 s).
- Siniestro: genera zombis furiosos (`brainy_zombie` de TC6) en la oscuridad, como máximo 3 en 16 bloques.
- Corrupto: coloca `taint_fibre` cerca. Puro: limpia bloques de taint cerca, salvo si es pálido.
- Generación: 1/36 por chunk sobre la superficie; 1/18 de tipo especial; 1/9 con modificador; siniestros en Eerie; corruptos en Tainted Lands; más puros en el bosque mágico; un nodo puro pequeño en el tronco de cada árbol de plata generado en el mundo.
- Escaneo: el taumómetro da `!AuraNode`, los aspectos del nodo (conocimiento de observación) y una clave genérica por nodo, así que cada nodo solo puntúa una vez.
- Gafas: muestran los aspectos (`ContainerAspects`) y el nombre del tipo y el modificador (`IGogglesDisplayExtended`). Con gafas el nodo se dibuja a plena intensidad y sin prueba de profundidad, así que se ve a través de paredes. Sin gafas se dibuja tenue.
- Render: una capa por aspecto (tira 0, con el color del aspecto, tamaño según √cantidad y pulso propio) más un núcleo según el tipo. Brillante: más grande. Pálido: más tenue. Menguante: parpadea.

### Inventado o adaptado (TC6 no tiene varitas ni pools de vis por aspecto)

- **Integración con el aura de TC6:** cada segundo, si vis + flux del chunk está por debajo de `base del bioma + max_total_del_nodo / 2`, el nodo gasta 1 punto de un aspecto (nunca el último) y lo convierte en vis: 1 si es primal, 2 si es compuesto. Así el nodo sube el aura de su chunk y su regeneración limita el ritmo.
- **Flux por tipo:** el inestable añade 0,05 flux/s, el corrupto 0,1 flux/s y el puro retira 0,2 flux/s.
- **"Tapping" sin varita:** con el guantelete de lanzador (cualquier `CasterItem`), clic derecho sobre el nodo extrae 5 puntos del aspecto mayor y da 1 cristal de vis (`crystal_essence`) de ese aspecto.
- **Tarro de nodos:** en TC4 se hacía con una varita sobre una estructura de vidrio con losas de madera. Aquí es el objeto `node_jar`, que se fabrica en la mesa arcana con 3 losas de madera arriba y vidrio en U, 50 de vis y cristales de aer, ordo y terra. Usarlo sobre el nodo cuesta 20 de vis del chunk y tiene un 25 % de probabilidad de empeorar el modificador (brillante → normal → pálido → menguante). El resultado es `jarred_node`, con los datos del nodo en `custom_data`. Usarlo sobre cualquier cara lo libera allí.
- **Asignación de tiras de `auranodes.png` por tipo:** la saqué inspeccionando la textura (0 capas, 1 inestable, 2 siniestro, 3 corrupto, 4 hambriento con anillos, 5 puro, 6 normal, 7 chispa extra del inestable). No está verificada contra el `TileNodeRenderer` de TC4.
- **Investigación:** `!AuraNode` (hexágono de escaneo en BASICS), `AURANODES` (AUROMANCY, requiere escanear un nodo y OBSERVATION;AUROMANCY;1) y `NODEJAR` (THEORY;AUROMANCY;1 y un cristal de aer, desbloquea la receta). Textos en `en_us` y `es_es`.
- **Sin implementar:** estabilizadores, transductor y nodos energizados. TC6 no tiene red de centivis, así que no tienen consumidor.

## Pruebas (gametests)

`AuraNodesVerification` añade cuatro tests: `nodes_generation`, `nodes_aura`, `nodes_jar` y `nodes_hungry`. Pasan los 64 requeridos (60 previos + 4).

Escena de depuración: `-PdebugHold=first -PdebugNodes=1` coloca un nodo de cada tipo y los modificadores a medianoche.
