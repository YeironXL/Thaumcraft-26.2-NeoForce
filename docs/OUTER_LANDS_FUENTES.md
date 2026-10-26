# Tierras Exteriores (Outer Lands): fuentes y decisiones

Thaumcraft 6.1.BETA26 no trae las Tierras Exteriores. Solo quedan restos: el bioma `thaumcraft:eldritch`, `dimensionOuterId=-42` en la configuración, las texturas `eldritch_stone_*`, `eldritch_door`, `obelisk_*`, `eldritch_lock` y `misc/eldritch_portal`, y algunas claves de idioma (`tile.eldritch.altar/obelisk/cap/lock/door/teleporter/crabspawner`, `item.eldritch_eye`, `item.runed_tablet`). El código de TC6 también conserva algunos comportamientos ligados a la dimensión exterior. Esta rama recrea la dimensión a partir de la información pública de Thaumcraft 4 (1.7.10) y Thaumcraft 5.

No encontré el código descompilado de TC4 (`MazeHandler`, `GenCommon`, `WorldProviderOuter`, `TileEldritchLock`). Las búsquedas en GitHub no dieron resultados, así que la estructura interna del laberinto es una reconstrucción a partir de las descripciones de las wikis.

## Fuentes consultadas

1. Wiki de Thaumcraft 4 (fandom), «The Outer Lands»: https://thaumcraft-4.fandom.com/wiki/The_Outer_Lands
2. Wiki de Thaumcraft 5 (fandom), «The Outer Lands»: https://thaumcraft-5.fandom.com/wiki/The_Outer_Lands
3. Wiki de Thaumcraft 5 (fandom), «Opening the Eye»: https://thaumcraft-5.fandom.com/wiki/Opening_the_Eye?oldid=1121
4. FTB Wiki, «Runed Tablet (Thaumcraft 4)»: https://ftb.gamepedia.com/Runed_Tablet_(Thaumcraft_4)
5. Foro FTB, «Thaumcraft Opening the Eye question»: https://forum.feed-the-beast.com/goto/post?id=957412
6. Foro FTB, «Thaumcraft 4.2.3.5 Eldritch tab question»: https://forum.feed-the-beast.com/threads/thaumcraft-4-2-3-5-eldritch-tab-question.139263/post-1544046
7. Código fuente de TC6 BETA26 (`/home/claude/w/TC6-ref`): `PlayerEvents.handleMisc`, `EntityEldritchGuardian`, `ThaumcraftWorldGenerator`, `EntityEvents` y el archivo de idioma.

8. Wiki de Thaumcraft 4 (fandom), «The Outer Lands» (segunda consulta, octubre de 2026): sala de runas, aberturas costrosas, generadores de mind spiders con telarañas, parches de taint y sala de taintáculos.
9. Wiki de Thaumcraft 5 (fandom), «The Outer Lands» (segunda consulta): «pools of all types of liquids, from lava to Liquid Death» y zonas con taint.

10. Wiki de Thaumcraft 5 (fandom), «The Outer Lands» (tercera consulta): Ancient Autocasters «scattered throughout the halls», con un foco por autolanzador que pueden soltar al destruirse junto con lingotes de metal del vacío y otro botín.
11. Archivo de idioma de Thaumcraft 5 (GitHub Azanor/thaumcraft-5, `en_US.lang`): `entity.Thaumcraft.TurretEldritch.name=Ancient Autocaster`, `tile.stone.crust.name=Crusted Stone`, `tile.stone.crust_glow.name=Glowing Crusted Stone`.
12. Sugerencias de Thaumcraft en GitHub (Azanor/thaumcraft-suggestions, issue #2): el bloque «Ancient Doorway» se usa también como soporte de los autolanzadores antiguos.
13. MC百科 (mcmod.cn), «Ancient Autocaster Placer»: autolanzador hostil que aparece de forma natural en las Tierras Exteriores, con un foco ofensivo aleatorio.

(La página de Primordial Pearl de la wiki de GTNH dio error al descargarla y no se usó.)

## Hechos usados (con su fuente)

| Hecho | Fuente | Implementación |
|---|---|---|
| Dimensión final: una mazmorra enorme sin salida, con muros, monstruos y tesoro, que empieza como vacío | [1] | Dimensión de datapack `thaumcraft:outer_lands`: generador plano sin capas (vacío), bioma `thaumcraft:eldritch`, cielo `end`, sin luz de cielo y con niebla oscura |
| Se entra por un portal sobrenatural abierto con el ritual «Opening the Eye» en un obelisco sobrenatural | [1][3] | `EldritchAltarBlock` bajo el obelisco: con 4 ojos se ejecuta el ritual y se abre `eldritch_portal` |
| Hacen falta 4 ojos sobrenaturales que se colocan en el pedestal central del obelisco | [3][5] | El altar acepta hasta 4 ojos |
| Cada ojo aumenta la frecuencia de aparición de Guardianes, también con el portal ya abierto | [3] | Cada 200 ticks hay una probabilidad de `ojos/8` de que aparezca un Guardián (máximo 3 en 32 bloques) |
| Cada obelisco abre su propio segmento de las Tierras Exteriores | [1][2][3] | `MazeData` asigna una sección del laberinto por altar (`GlobalPos`), separadas 1024 bloques |
| El portal funciona en los dos sentidos | [2] | Hay un portal de salida en la sala de entrada que devuelve al jugador junto a su altar |
| Sala de llegada de 7×7×7 junto a un portal de salida con remate y obelisco de piedra antigua | [1][2] | Sala de entrada de 7×7×7 con el portal de salida y un obelisco «ancient» flotando encima |
| Las esquinas de esa sala son fosos llenos de «Portal to Nothing» | [1] | `eldritch_void` en las 4 esquinas, con 2 bloques de profundidad |
| Portal to Nothing: 8 corazones de daño que ignora la armadura, destruye objetos y es irrompible | [1] | `EldritchVoidBlock`: 16 de daño de tipo `fell_out_of_world` cada 10 ticks, elimina los ítems que caen y es irrompible |
| Hay lecho de roca alrededor para impedir escapar | [1] | Capas de lecho de roca por encima y por debajo del laberinto |
| Pasillos de 5 de alto y 5 de ancho, con esquinas | [1] | Pasillos de 5×5 que unen celdas de 16×16 |
| Salas de esquina de 7×7 en algunas intersecciones y callejones sin salida | [1] | Celdas `JUNCTION` de 7×7 |
| Muros y suelos de piedra antigua con piedra glífica incrustada | [1] | Los muros mezclan `stone_ancient`, `stone_ancient_glyphed` y las piedras sobrenaturales |
| Túneles normalmente sin iluminar; muchos callejones sin salida | [2] | Dimensión con `ambient_light` 0 y laberinto generado como árbol de expansión (DFS) |
| Salas de botín con urnas y cajas | [1][2] | Sala `LOOT` con 3 a 6 urnas o cajas (común, poco común o rara) |
| Sala de cangrejos: rara, con hasta 10 cangrejos sobrenaturales más urnas y cajas | [1] | Sala `CRAB` (15 % de los callejones sin salida) con 3 a 7 cangrejos persistentes y 2 contenedores |
| Sala de la tablilla: una sala grande con un pedestal que sostiene una tablilla rúnica | [1][4] | Sala `TABLET` de 11×11×7 con `pedestal_ancient` y la tablilla, y roca antigua como decoración |
| Las tablillas rúnicas siempre aparecen en la mazmorra; son la única llave y no se pueden fabricar | [1][4] | Una sala de tablilla por cada sala de jefe; la tablilla no tiene receta |
| Salas de jefe grandes, selladas por un portón antiguo («Ancient Doorway») | [1][2] | Sala `BOSS` de 13×13×9 tras un portón de 5×5 de `eldritch_door` |
| Se abre haciendo clic derecho en el bloque central («Ancient Locking Mechanism») con la tablilla; la puerta se abre despacio | [1][4] | `EldritchLockBlock` consume la tablilla y abre la puerta bloque a bloque (uno cada 3 ticks, de fuera hacia dentro) |
| La sala de jefe se aleatoriza al insertar la tablilla; si no se abre, está vacía | [1] | El jefe se elige al desbloquear y aparece cuando la puerta termina de abrirse |
| Cuatro jefes: Guardián Sobrenatural (Warden), Portal Carmesí Mayor (con el Pretor), Tentáculo Gigante y Constructo Sobrenatural | [1][2] | Se usan las entidades ya portadas: `eldritch_warden`, `cultist_portal_greater`, `taintacle_giant` y `eldritch_golem` |
| Sala del Warden: dos obeliscos decorativos; sala del Constructo: tres obeliscos de piedra antigua | [1] | Se colocan 2 o 3 obeliscos «ancient» al abrir la sala |
| Las salas de jefe contienen urnas y cajas | [2] | 4 contenedores en las esquinas, con más probabilidad de rareza alta |
| Los jefes sueltan una perla primordial | [1][2] | Ya lo hacen las entidades del port |
| Los obeliscos del overworld tienen una base de obsidiana que puede bajar varios bloques | [2] | `EldritchObeliskGen`: disco de obsidiana y baldosa sobrenatural con 4 bloques de profundidad |
| Los obeliscos hacen aparecer Caballeros y Clérigos carmesí | [6] | Al cargarse por primera vez, un altar natural genera 2 caballeros y 1 clérigo |
| El objeto del obelisco solo crea el pilar flotante; el portal se coloca dentro del obelisco | [1] | Obelisco flotante sobre el altar; el portal se abre en el hueco entre ambos |
| El ojo se fabrica por infusión: ojo de Ender + lingote de oro + semilla del vacío; Motus 16, Tenebrae 16, Vacuos 16 y Alienis 64; inestabilidad moderada | [3] | `recipe/eldritcheye.json` (infusión, inestabilidad 5) |
| Los ojos sobrenaturales son drops raros de los Guardianes | [3] | 1 de cada 20 Guardianes muertos por un jugador suelta un ojo |
| Investigación «Opening the Eye» (OCULUS) en la pestaña Eldritch | [3][5] | Entrada `OCULUS` en `research/eldritch.json` (warp 5) |
| Texto de la tablilla: «It seems to be part of something larger.» | [7] (lang de TC6) | Descripción emergente de `RunedTabletItem` |
| No se puede volar en las Tierras Exteriores | [7] `PlayerEvents.handleMisc` | `OuterLandsEvents`: se desactiva el vuelo en supervivencia con el mensaje `tc.break.fly` |
| Los Guardianes de la dimensión exterior reciben un escudo de media vida que se regenera 1 punto cada 25 ticks | [7] `EntityEldritchGuardian` | Añadido en `EldritchGuardianEntity` |
| Los monstruos aparecen sin importar el nivel de luz | [2] | `monster_spawn_light_level` 15 en el `dimension_type`; el bioma ya incluye Guardián y zombi habitado |
| Sala de runas: un único bloque de «piedra rúnica» (piedra antigua con runas grises); al acercarse aparecen glifos morados flotando | [8] | Sala `RUNE` de 9×9×5 con `eldritch_rune_stone` en el centro y partículas de glifos alrededor si hay un jugador a 6 bloques |
| A los 5 segundos la piedra ataca con arcos eléctricos parecidos a los de los wisps, que además dan warp | [8] | `EldritchRuneStoneBlockEntity`: tras 100 ticks lanza un arco morado (FX `zap`), 3 de daño mágico y 1 de warp temporal, y repite cada 40 ticks mientras sigas cerca |
| Aberturas costrosas («Crusted Opening», `tile.eldritch.crabspawner`): montículos grises con un agujero negro de donde salen cangrejos sobrenaturales | [8], lang de TC6 [7] | `eldritch_crab_spawner` en las paredes de las salas de cangrejos y de algunos pasillos; humo y partículas de portal |
| A veces hay un generador de mind spiders rodeado de telarañas | [8] | Sala `SPIDER`: generador vanilla de `mind_spider` y telarañas más densas cerca del centro |
| Parches de taint frecuentes en los pasillos; sala de taintáculos llena de taint y fibras | [8][9] | Sala `TAINT` (costra y tierra contaminadas, roca contaminada, fibras, goo de flujo y un taintáculo) y parches en el 12 % de los pasillos |
| Charcos de líquidos, desde lava hasta Liquid Death, en los pasillos | [9] | Charcos de 3×3 en el 8 % de los pasillos: lava 40 %, agua 30 %, `liquid_death` 30 % |
| La sala de taintáculos se llena de taint y va sustituyendo bloques poco a poco; la zona se vuelve bioma Tierras Contaminadas | [8] | Versión 3: la sala contaminada lleva una semilla de taint (la mecánica de propagación de TC6 del port: `TaintHelper.spreadFibres`) y su celda pasa al nuevo bioma `thaumcraft:tainted_lands` |
| Las salas de cangrejos son de piedra costrosa | [8][11] | Versión 3: las caras interiores de la sala de cangrejos pasan a `crusted_stone` |
| Piedra costrosa luminosa: piedra costrosa con una luz amarilla, más pequeña que un bloque entero; se suelta a sí misma | [8][11] | `crusted_stone_glow`: bloque de 12×12×12 píxeles con luz 13 que se suelta a sí mismo; aparece en el 15 % de pasillos y cruces y en cada sala de cangrejos |
| Cristales extraños: racimos de cristales blancos en techos y paredes; sueltan un fragmento equilibrado | [8] | `crystal_strange`: racimo blanco orientado (modelo OBJ de los cristales de TC6) con luz 5 y destellos; aparece en el 12 % de pasillos y cruces y en un tercio de las salas de botín |
| Autolanzadores antiguos repartidos por los pasillos, sobre un bloque «Ancient Doorway», con un foco ofensivo; al destruirse pueden soltar el foco y metal del vacío | [10][11][12][13] | Entidad `eldritch_autocaster`: inmóvil, sobre `eldritch_door`, ataca a los jugadores a la vista. Suelta `focus_1` (25 %) y 1-2 `ingot_void` (33 %) |

## Invenciones (sin datos de TC4 o adaptaciones a TC6)

- **Sala de Tentáculos Gigantes (versión 4; frecuencia y detalles, invención).** TC4 tenía salas contaminadas en las Tierras Exteriores con entre 1 y 5 Tentáculos Gigantes; no encontré datos de frecuencia ni de forma. En el port, el 40 % de los laberintos nuevos (flujo aleatorio propio, así que las salas de versiones 1-3 no se mueven) convierte un callejón sin salida de botín o vacío en una sala de 11×11 y 7 de alto con suelo y paredes contaminados, fibras, baba de flux, una semilla de taint, bioma Tierras Contaminadas y 1-5 `taintacle_giant` persistentes. El taint puede extenderse dentro de la sala igual que en las salas contaminadas. Modo de depuración: `-PdebugOuter=taintacles`.

- **Coste de vis del ritual.** TC4 pedía 500 de cada aspecto primal con una varita. TC6 no tiene varitas ni reservas primales, así que el ritual de este port se completa solo, 100 ticks después de colocar el cuarto ojo, sin coste de vis.
- **Forma exacta del obelisco y del altar.** El obelisco del overworld tiene 4 bloques (base, cuerpo, cuerpo, remate) y flota 2 bloques por encima del altar. El de salida tiene 3 bloques. Las texturas salen de `obelisk_side`, `obelisk_side_2`, `obelisk_cap` y `obelisk_cap_altar` de TC6, compuestas sobre fondo oscuro. TC4 las renderizaba sobre un campo estelar.
- **Tamaño del laberinto.** 9×9 celdas de 16×16 bloques por sección, suelo a Y=64, secciones separadas 1024 bloques en X y generadas de golpe la primera vez que alguien entra. TC4 generaba por chunks a partir de `MazeHandler`, pero se desconocen las dimensiones exactas.
- **Distribución de salas.** Los callejones sin salida más lejanos de la entrada se convierten en salas de jefe (2 si hay 6 o más callejones, si no 1). Los siguientes son salas de tablilla (una por jefe). Del resto, el 15 % son salas de cangrejos, el 55 % salas de botín y el resto salas de 7×7. La mitad de los cruces de 3 o más pasillos son salas de 7×7, y se añaden algunos bucles.
- **Tamaños de sala.** Botín y cangrejos de 9×9×5, tablilla de 11×11×7 y jefe de 13×13×9.
- **Mezcla de piedras.** 80 % piedra antigua, 8 % `eldritch_stone`, 6 % `eldritch_stone_carved`, 4 % piedra glífica y 2 % `eldritch_stone_runed`, que emite luz 6 y sustituye a la «Glowing Crusted Stone». No hay piedra costrosa ni cristales extraños; el suelo de la sala de cangrejos usa `eldritch_stone_carved`.
- **Contenido no portado.** No hay sala aparte con taintáculos gigantes (1-5 según [8]): el taintáculo gigante solo es uno de los jefes.
- **Versión 3 del laberinto.** Los contenidos nuevos (piedra costrosa, piedra luminosa, cristales, autolanzadores, semilla y bioma de la sala contaminada) solo aparecen en las secciones creadas a partir de ahora y salen de otra secuencia aleatoria. Las secciones de versión 1 y 2 conservan su plano y sus bloques.
- **Límites del taint (invención).** TC6 no tiene bioma de taint y TC4 no documenta límites. Dentro de un laberinto, el taint solo puede extenderse en las salas contaminadas de versión 3: su aire y la primera capa de suelo, paredes y techo. El lecho de roca, el resto de los muros y las demás celdas son inmunes, así que el laberinto no se deshace. El taint generado en los laberintos (versión 2 o superior) tampoco se marchita aunque no haya semilla cerca, porque TC4 no tenía semillas que lo mantuvieran.
- **Bioma Tierras Contaminadas (invención).** El nuevo `thaumcraft:tainted_lands` tiene cielo negro, niebla morada `#4a2058`, agua `#6b1d7a` y hierba y follaje `#6d4189`, y en él aparecen reptadores de taint y Guardianes. Los colores son aproximados a los de TC4. Solo se aplica a la celda de 16×16 de la sala, no se extiende.
- **Autolanzador antiguo (invención).** El port no tiene los focos de TC4/TC5, así que cada autolanzador elige uno de dos «focos»: un orbe sobrenatural (como el del Guardián) o una descarga en arco (como la de los wisps, 4,5 de daño). Lanza cada 30-50 ticks si ve al objetivo a menos de 20 bloques. Tiene 40 de vida y 6 de armadura, y no se mueve ni lo empujan. El modelo es la cabeza giratoria del taladro arcano con una textura morada y negra nueva. Frecuencias: 7 % de pasillos, 20 % de cruces y 25 % de salas de botín.
- **Drop de los cristales extraños (invención).** TC6 no tiene fragmento equilibrado, así que sueltan un cristal de cada aspecto primal.
- **Texturas nuevas.** `crusted_stone` y `crusted_stone_glow` (piedra antigua gris con costras y, en la luminosa, manchas amarillas) y `autocaster_eldritch`.
- **Versión del laberinto.** `MazeData.Maze` guarda ahora `version` (las partidas antiguas se leen como 1). Las secciones de versión 1 conservan su plano exacto; las nuevas (versión 2) reciben las salas y peligros nuevos, que salen de una secuencia aleatoria aparte para no mover jefes ni tablillas.
- **Frecuencias nuevas (inventadas).** Entre los callejones que eran de botín o de 7×7 se garantiza una sala de runas, una de arañas y una contaminada; el resto pasa a runas 6 %, arañas 8 % y taint 10 %. Los cruces de 3 o más pasillos que son salas de 7×7 pueden ser contaminados (10 %) o con telarañas (8 %). En pasillos: taint 12 %, charco 8 % y abertura costrosa 6 %. Cada sala de cangrejos lleva hasta 4 aberturas.
- **Mecánica de las aberturas (invención).** Con un jugador en 16 bloques, cada 15-30 s sale un cangrejo por una cara libre, con un máximo de 4 cangrejos alrededor. TC4 no documenta los tiempos. Es un bloque completo y no un montículo más pequeño; la textura (piedra antigua con anillo de costra gris y agujero negro) es nueva.
- **Piedra rúnica.** Daño (3), warp (1 temporal), alcance (6) y repetición (2 s) inventados. La textura es piedra antigua con las runas de `inlay_rune0` en gris, y los glifos usan la partícula vanilla `enchant` (se ven blanquecinos) con chispas `witch` moradas.
- **Charco de agua.** La wiki dice «todo tipo de líquidos»; el agua es el tercer líquido elegido.
- **Probabilidades.** Un obelisco cada 200 chunks en tierra firme por encima del nivel del mar; el ojo cae con 1/20; la frecuencia de Guardianes del altar es inventada.
- **Cerradura fuera de la dimensión.** En TC4 colgaba el mundo. Aquí la cerradura no hace nada y avisa con `tc.eldritch.lock.inert`.
- **Bloques nuevos.** `eldritch_stone`, `eldritch_stone_carved` y `eldritch_stone_runed` (una por cada textura sobrante `eldritch_stone_1/2/3`), `eldritch_obelisk_base`, `eldritch_obelisk_body`, `eldritch_capstone` (con la propiedad `ancient`), `eldritch_altar`, `eldritch_portal`, `eldritch_door`, `eldritch_lock` y `eldritch_void`. Se inventaron las texturas de `eldritch_lock` (puerta con un anillo y un glifo), `eldritch_void` (estrellas moradas), el ojo (ojo de Ender teñido de morado) y la tablilla (piedra antigua con glifo).
- **Investigación OCULUS.** En TC4 colgaba de las revelaciones del warp («Eldritch Revelation»). Aquí cuelga de BASEELDRITCH y PRIMALCRUSHER, con conocimiento OBSERVATION y THEORY de ELDRITCH nivel 2 y warp 5. Los textos son nuevos, inspirados en la descripción de la wiki y del Thaumonomicon de TC4 [4].
- **Llaves.** TC4 no tenía «llaves» aparte de la tablilla rúnica [1][4], así que no se crearon objetos de llave adicionales.
- **Aviso de ajustes experimentales.** Una dimensión de datapack marca el mundo como «experimental» en 26.2. Minecraft avisa una vez por mundo al cargarlo; es lo habitual en los mods con dimensiones propias.

## Cómo llegar en el juego

1. Busca un Obelisco sobrenatural: un disco de obsidiana con un altar oscuro y un pilar flotante, guardado por cultistas carmesí. También puedes colocar un `eldritch_altar` en creativo.
2. Consigue 4 Ojos sobrenaturales (drop raro de los Guardianes o infusión de la investigación «Opening the Eye»).
3. Haz clic derecho en el altar con los ojos. Al cuarto ojo empieza el ritual y, a los 5 segundos, se abre el portal entre el altar y el obelisco.
4. Atraviesa el portal y llegarás a la sala de entrada. El portal del centro te devuelve a tu altar.
5. Recorre el laberinto, coge una Tablilla rúnica del pedestal de una sala de tablilla y úsala en el mecanismo de cierre de un portón: la puerta se abre y aparece el jefe.

Propiedades de depuración (cliente): `-PdebugHold=first -PdebugOuter=altar|altar_open|maze|corridor|lock|boss|loot|tablet|rune|spider|taint|pool|opening|crust|crystal|glow|autocaster` (las vistas usan una sección de la versión actual).
