# Contenido extra de TC4/TC5 (rama `tc45more`): fuentes e invenciones

Tres bloques: charcos de las Tierras Exteriores, focos clásicos que faltaban y estructuras de mundo. No se ha
accedido a sitios bloqueados (la página de FTB *Thaumcraft 5* en breezewiki devolvió "Access Denied" y no se usó).

## Fuentes consultadas

1. Wiki TC4, *Wand Foci*: https://thaumcraft-4.fandom.com/wiki/Wand_Foci
2. Wiki TC4, *The Outer Lands*: https://thaumcraft-4.fandom.com/wiki/The_Outer_Lands
3. Wiki TC5, *The Outer Lands*: https://thaumcraft-5.fandom.com/wiki/The_Outer_Lands
4. FTB Wiki, *Barrow Hill*: https://feed-the-beast.fandom.com/wiki/Barrow_Hill
5. FTB Wiki, *Greatwood Tree*: https://feed-the-beast.fandom.com/wiki/Greatwood_Tree
6. Foro FTB, "Thaumcraft: How do you increase wand damage?": https://forum.feed-the-beast.com/goto/post?id=1281617
7. Código TC6 BETA26 (`TC6-ref`): `BlockFluidDeath`.

## 1. Charcos de las Tierras Exteriores (laberinto versión 5)

| Hecho | Fuente | Port |
|---|---|---|
| "Pools of all types of liquids, from lava to Liquid Death, can be found throughout the halls" | [3] | Charcos de lava, agua, Muerte líquida y baba de flujo en los pasillos |
| "Some areas contain Taint" / parches de taint en los pasillos | [2][3] | Ya existían (versión 2). El charco de **baba de flujo** con borde de costra de taint es una **invención** que lo enlaza con esas zonas |
| Muerte líquida: ralentiza y disuelve (daño 4..1 según profundidad, ignora armadura) | [7] | Ya portado tal cual en `LiquidDeathBlock`. No se cambia. Las wikis no describen nada distinto en TC4/TC5 |
| Forma, tamaño y profundidad de los charcos | ninguna | **Invención**: elipse irregular a lo largo del pasillo, de 5 a 9 bloques de largo y hasta 3 de ancho. Siempre deja un bloque de pasillo libre a cada lado. Tiene 2 de profundidad en el centro y 1 en los bordes |
| Bordes | ninguna | **Invención**: magma alrededor de la lava, piedra costrosa alrededor de la Muerte líquida y costra de taint alrededor de la baba |
| Proporciones | ninguna | **Invención**: lava 30 %, agua 25 %, Muerte líquida 25 %, baba 20 % |

Versionado: `MazeData.VERSION` pasa a 5 y solo los laberintos nuevos usan `MazePools`. La posición de cada charco
sigue saliendo del flujo aleatorio de los peligros de la versión 2, y se consumen las mismas tiradas, así que los
demás peligros no se mueven. La forma y el tipo salen de un flujo propio por celda. Los laberintos 1–4 conservan el
charco de 3×3 de siempre.

## 2. Focos clásicos

Comparado con los efectos del port (`casters/foci`): Fuego, Escarcha, Aire, Tierra, Flujo, Romper (=Excavación),
Grieta (=Agujero portátil), Intercambio (=Comercio equitativo), Maldición (=Maldición del Pech), Curación,
Protección y Caos primigenio, más el medio Murciélago de hechizo (=Nueve Infiernos / Hellbat). De la lista de [1]
solo faltaba **Descarga (Shock)**.

### Efecto de foco: Descarga (`thaumcraft.SHOCK`)

| Hecho | Fuente | Port |
|---|---|---|
| Ataque de rayo "target-seeking" con daño considerable | [1] | Daño de rayo `(3 + potencia) × poder`. Que "busque objetivo" se consigue con los medios de TC6 (Rayo, Proyectil con Buscador) |
| Ya no transforma mobs como un rayo de verdad | [1] | No se crea ninguna entidad de rayo. Solo se aplica daño `lightningBolt`, así que no hay creepers cargados ni cerdos convertidos (lo comprueba un gametest) |
| Coste 0,25 Aer por lanzamiento | [1] | El coste de vis de TC6 sale de la complejidad: `2 × potencia + 3 × cadena`. Aspecto `potentia` (**invención**: el medio Rayo de TC6 también usa potentia) |
| "Chain Lightning" | [6] (los jugadores la mencionan como mejora del foco de rayo; la wiki no da detalles) | Ajuste **cadena** de 0 a 3: salta a la criatura viva más cercana a menos de 6 bloques, con un 75 % del daño anterior, y nunca al lanzador. Alcance y factor son **invención** |
| "Ball Lightning", "Earth Shock" | [6] solo da el nombre de Ball Lightning | No implementados |

Investigación `FOCUSSHOCK` (Auromancia, [-1,3], padre `FOCUSELEMENTAL`): THEORY Auromancia 1, haber descubierto
`potentia` y entregar un **pararrayos**. Los requisitos son invención. El icono `textures/foci/shock.png` es un
glifo blanco de 32×32 al estilo TC6 (lo tiñe el color del elemento).

## 3. Estructuras de mundo

| Estructura | Estado | Fuente |
|---|---|---|
| Greatwood infestado (telarañas, generador de arañas bajo el tronco y cofre debajo) | **Ya existía** (`GreatwoodTree(spiders=true)`, 1 de cada 8 grandes robles). Coincide con [5] | [5] |
| Altar de fuegos fatuos y obelisco siniestro | Ya existían (rama `tc4c`) | ver FOCOS_ESTRUCTURAS_FUENTES.md |
| **Guarida túmulo (Barrow Den)** | **Nueva** (`worldgen/BarrowDenGen.java`) | [4] |
| Campamentos o portales carmesí en el mundo | Sin fuente. No se implementa | — |
| "Tótems" de TC5 | Sin fuente. No se implementa | — |

### Guarida túmulo

[4] atribuye la Barrow Hill a Thaumcraft 3. El informe previo (TC4_TC5_FALTANTES.md) la daba también como
estructura de TC5 según FTB, pero esa página no se pudo volver a abrir.

| Hecho ([4]) | Port |
|---|---|
| Colinas irregulares en praderas o biomas llanos (ejemplo: sabana) | Montículo de tierra y hierba de radio ~9 con borde irregular. Biomas: llanura, llanura de girasoles, pradera y sabanas. Solo se genera si el terreno es llano (±3 bloques) |
| Entrada de piedra en el lado oeste | Túnel de 3×3 de ladrillos de piedra hacia −X, con jambas de piedra antigua |
| Nivel superior en anillo y planta principal con un foso central | Repisa anular (radio 3,6–5,5), planta 3 bloques más abajo con escalera de mano, y foso de radio 2 con borde de piedra antigua con glifos |
| Dos generadores en las esquinas oeste: zombi y esqueleto | Igual |
| Tres cofres en el fondo del foso con oro, lingotes de taumio, herramientas de acero o taumio y libros encantados | 3 cofres con la tabla `thaumcraft:chests/barrow_den` (oro, hierro, taumio, espada y pico de taumio, libro encantado, huesos, manzana dorada). Los "Knowledge Fragments" y el "Oblivion Frame" son de TC3 y se omiten |
| Siempre tiene un nodo oscuro, a menudo muy grande y con mucha vis, que genera fuegos fatuos hostiles | Nodo `DARK` y `BRIGHT` con la vis base doblada. Los nodos oscuros del port ya generan fuegos fatuos furiosos |
| Tamaño exacto y rareza | **Invención**: 1 de cada 260 chunks válidos |

## Pruebas

Gametests nuevos: `foci_shock` (cadena de 3, respeta el ajuste, no transforma), `barrow_den` (nodo oscuro, 3 cofres
en el foso, generadores de zombi y esqueleto al oeste, entrada, techo y filtro de bioma) y `outer_pools`
(laberinto v5: charcos más grandes que 3×3, de 2 de profundidad y con los 4 tipos). Pasan los 85 tests.
