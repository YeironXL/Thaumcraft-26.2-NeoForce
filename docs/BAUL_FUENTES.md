# Baúl viajero (Travelling Trunk) — fuentes y decisiones

Thaumcraft 6 no tiene el Baúl viajero. Lo recreamos a partir de Thaumcraft 4 (y de Thaumcraft 2, donde apareció por primera vez). Este documento separa lo que dicen las fuentes de lo que se ha **inventado** o **adaptado** para el port.

## Fuentes consultadas

1. Thaumcraft 4 Wiki, página «Golems», revisión 6174: https://thaumcraft4.fandom.com/wiki/Golems?oldid=6174
2. Technic Pack Wiki, «Traveling Trunk» (Thaumcraft 2): https://technicpack.fandom.com/wiki/Traveling_Trunk
3. Assets de TC6 1.12.2 BETA26 (`/home/claude/w/TC6-ref`): `textures/models/trunk.png` y `textures/models/trunkangry.png`. Son restos de TC4 sin código que los use.
4. Páginas que no aportaron datos del baúl: https://thaumcraft-5.fandom.com/wiki/Golemancy?oldid=238 y https://thaumcraft-4.fandom.com/wiki/Thaumonomicon.

## Hechos con fuente

| Hecho | Fuente |
|---|---|
| «Un cofre que te sigue llevando su contenido» | [1] |
| Se teletransporta hasta el jugador como perros y gatos | [1], [2] |
| Lo sigue entre dimensiones | [1] (**no implementado**) |
| Recogerlo con la Campana del golemante normalmente suelta su contenido | [1] |
| Se cura al darle de comer, o poco a poco por sí solo | [1]; en TC2 come la comida que lleva dentro [2] |
| Admite **una** mejora elemental | [1] |
| Aire: se mueve más rápido | [1] |
| Tierra: más casillas | [1] |
| Fuego: ataca a los monstruos hostiles y los prende fuego | [1] |
| Agua: casi invulnerable; en SMP solo lo usa quien lo colocó | [1] |
| Entropía: absorbe los objetos sueltos cercanos si le cabe | [1] |
| Orden: se puede recoger como objeto con su contenido | [1] |
| La versión enfadada tiene otros ojos | [2]; textura `trunkangry.png` [3] |
| Hay que investigarlo antes de fabricarlo (en TC2, «Lost Knowledge») | [2] |
| Opción de quedarse quieto (en TC2, botón en su GUI) | [2] |
| Aspecto: cofre de madera con herrajes, ojos amarillos en el frontal y tapa con boca | [3] |

## Inventado o adaptado (marcado)

- **INVENTADO**: patas cortas en el modelo, y la tapa que mastica al andar. El modelo de TC4 era solo el cofre, que saltaba. Las patas reutilizan la madera de `trunk.png`.
- **INVENTADO**: 27 casillas de base y 54 con Tierra. La fuente solo dice «más casillas».
- **INVENTADO**: 50 de vida, velocidad 0,30 (0,42 con Aire), daño 4 con Fuego (más 4 s de fuego), regeneración de 1 PV cada 2,5 s (cada 1 s con Agua) y 90 % menos de daño con Agua.
- **ADAPTADO**: las mejoras se aplican con **cristales de vis** (Aer, Terra, Ignis, Aqua, Ordo, Perditio), porque TC6 no tiene objetos de mejora de gólem. Cada mejora gasta un cristal y el baúl no acepta una segunda.
- **ADAPTADO (petición del usuario)**: al recogerlo, el baúl **siempre** conserva su contenido en el objeto (componente `minecraft:container`). En TC4 eso solo pasaba con la mejora de Orden.
- **ADAPTADO**: por eso Orden cambia de efecto. Si el baúl muere, vuelve a ser un objeto con su contenido. Sin Orden, al morir suelta el contenido y un baúl vacío (que conserva su mejora).
- **ADAPTADO**: la protección del Agua (solo el dueño) se aplica a todos los baúles. Solo el dueño puede abrirlo o darle órdenes, y ningún jugador puede herirlo.
- **INVENTADO**: controles. Clic derecho abre la GUI. Clic derecho agachado alterna quedarse o seguir. Golpearlo agachado (siendo el dueño) o usar la Campana del golemante lo recoge. Clic derecho con comida lo cura.
- **INVENTADO**: la receta. Es de infusión: cofre en el centro, mente mecánica, 3 tablones de gran madera y 2 cueros, con Vacuos 25, Motus 25, Machina 25 y Victus 15, inestabilidad 2. No encontré la receta original de TC4.
- **INVENTADO**: la investigación `TRAVELTRUNK` en Golemancia, en la posición (2,3). Requiere MINDCLOCKWORK e INFUSION, teoría de Golemancia 1 y de Artificio 1, y un cofre. La clave `TRAVELTRUNK` es la que dio el usuario.
- **INVENTADO**: sonidos. Al abrir y cerrar suenan los del cofre vanilla. Golpes, muerte y pasos usan sonidos de madera. Al cambiar de modo suena `clack`, al mejorarlo `upgrade`, al recogerlo `zap` y al atacar `crabclaw`.
- **NO IMPLEMENTADO**: seguir al dueño entre dimensiones.
