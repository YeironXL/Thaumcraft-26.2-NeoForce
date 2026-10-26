# Focos TC4 y estructuras TC5 recreados en el port TC6

Contenido añadido en la rama `tc4c`: dos efectos de foco recuperados de Thaumcraft 4 (Protección y Caos
primigenio) y dos estructuras de mundo de Thaumcraft 5 (Altar de fuegos fatuos y Obelisco siniestro).
TC6 6.1.BETA26 no tiene nada de esto, así que todo es una **recreación**. Este documento separa lo que dicen
las fuentes de lo que se ha inventado para encajarlo en TC6.

## Fuentes consultadas

- Wiki de Thaumcraft 4, página *Wand Foci*: https://thaumcraft-4.fandom.com/wiki/Wand_Foci
  - **Warding Focus**: hace que los bloques sólidos y opacos sean irrompibles e indestructibles para todos salvo
    el jugador que los protegió. Coste de vis por bloque: 0,1 Aqua, 0,25 Ordo y 0,25 Terra. La investigación se
    desbloquea escaneando cristal protegido u otros bloques protegidos. La receta usa una **estrella del Nether**.
  - **Primal Focus**: dispara un proyectil brillante de trayectoria errática que explota al impactar y daña a las
    criaturas y su entorno. Rara vez crea **taint fibroso** o **nodos pequeños**. Coste aleatorio de 0,5 a 2,0 de
    cada aspecto por lanzamiento. Investigación: escanear un amuleto primigenio y, desde 4.2, la epifanía
    eldritch (warp).
- Wiki de FTB, *Thaumcraft 5* (espejo breezewiki): https://breezewiki.discard.no/ftb/wiki/Thaumcraft_5, y el
  archivo *Sinister Aura Node*: https://ftb.fandom.com/wiki/File:Sinister_Aura_Node.png. Mencionan las
  estructuras de TC5 con nodo siniestro, pero no dan planos exactos.
- Recuerdos de la comunidad (hilos de FTB, p. ej. "Wisps out of nowhere"): los fuegos fatuos aparecen alrededor
  de santuarios con nodos siniestros.
- Código del propio port: `aura/node/*` (nodos estilo TC4, `NodeType.DARK` = siniestro), `WispEntity`,
  `EldritchObeliskGen` (patrón de generación) y docs/NODOS_FUENTES.md.

No se ha accedido a sitios bloqueados. No existe código fuente de TC4/TC5 en el repositorio de referencia
(`TC6-ref` es solo TC6), así que dimensiones y probabilidades son invenciones razonables.

## 1. Efecto de foco: Protección (`thaumcraft.WARD`)

Clase `casters/foci/FocusEffectWard.java`, lógica en `casters/ward/WardManager.java` y datos en
`casters/ward/WardData.java`.

| Aspecto | TC4 (fuente) | Port |
|---|---|---|
| Forma | Foco propio (ItemFocusWarding) | **Efecto de foco TC6** combinable en el Manipulador focal con cualquier medio (toque, proyectil, rayo, mina...) |
| Datos | El bloque se sustituía por `BlockWarded` + tile entity con el dueño | El bloque **se queda tal cual**; la protección vive en un *attachment* por chunk (`thaumcraft:wards`, posición empaquetada → UUID), guardado en disco y sincronizado al cliente |
| Protección | Irrompible e inalterable para los demás | Se cancela romper (`BreakBlockEvent`), la velocidad de minado baja a 0, se cancela el clic derecho de otros (puertas, cofres, herramientas que modifican el bloque), las explosiones lo sacan de la lista afectada, los pistones no se activan si empujarían o destruirían un bloque protegido, y los mobs no pueden destruirlo (`LivingDestroyBlockEvent`) |
| Quitar | — | **Invención**: el dueño lanza Protección otra vez sobre el bloque para quitarla; si el dueño rompe el bloque también se borra |
| Visual | Superposición tenue visible con gafas | Runas azuladas pulsantes en las caras expuestas, solo con gafas de revelación (`client/WardClient.java`, textura `textures/misc/ward_overlay.png`) |
| Coste | 0,1 Aqua + 0,25 Ordo + 0,25 Terra por bloque | Complejidad fija 4 (sin ajustes), aspecto `praemunio` para el cristal del Manipulador |
| Receta | Estrella del Nether | En TC6 los componentes de foco no se fabrican, se desbloquean por investigación. La estrella del Nether pasa a ser **objeto que hay que entregar** en la etapa 1 de `FOCUSWARD` |

Investigación `FOCUSWARD` (Auromancia, [2,8], padre `FOCUSBREAK`): THEORY Auromancia 2, descubrir `praemunio`
y entregar una estrella del Nether.

## 2. Efecto de foco: Caos primigenio (`thaumcraft.PRIMAL`)

**Decisión**: es un **efecto de foco** y no un objeto aparte. En TC6 todos los focos son paquetes construidos en
el Manipulador. Un objeto especial rompería ese sistema y no aprovecharía los medios. La "trayectoria errática"
de TC4 se obtiene combinándolo con el medio Proyectil y el modificador Dispersión.

Clase `casters/foci/FocusEffectPrimal.java`. Cada impacto:
- hace daño mágico `(2 + potencia) × poder` en un radio de 2,5 bloques y elige **un primigenio al azar**:
  - aer: empuje;
  - terra: fatiga minera;
  - ignis: fuego y una pequeña explosión que prende fuego (sin romper bloques);
  - aqua: lentitud;
  - ordo: debilidad;
  - perditio: daño extra y una pequeña explosión.
- tiene 1 entre 25 de poner **taint fibroso** sobre suelo sólido (invención: TC4 solo dice "rara vez");
- tiene 1 entre 100 de condensar un **nodo de aura pequeño e inestable** (`NodeType.UNSTABLE`, `small`);
- complejidad `6 + 3 × potencia` (ajuste de potencia 1–5) y aspecto `praecantatio`.

El coste aleatorio por aspecto de TC4 no se replica: el coste de vis de TC6 sale de la complejidad.

Investigación `FOCUSPRIMAL` (Auromancia, [-3,8], padres `FOCUSFLUX` y `FOCUSCURSE`): THEORY Auromancia 2 +
OBSERVATION Eldritch 1 y entregar una mota de perla primordial, en lugar del amuleto primigenio de TC4. Completarla da
**2 de warp**, como guiño a la epifanía eldritch de 4.2.

## 3. Estructuras TC5: Altar de fuegos fatuos y Obelisco siniestro

Clase `worldgen/SinisterShrinesGen.java`, llamada desde `ThaumcraftWorldFeature` una vez por chunk (no en
mundos planos ni en el Nether), solo sobre suelo sólido y seco por encima del nivel del mar.

- **Altar de fuegos fatuos** (biomas: `thaumcraft:magical_forest`, `thaumcraft:eerie`, `#c:is_spooky`,
  bosque oscuro). Probabilidad 1/140 por chunk válido (1/70 en eerie). Tiene:
  - un disco de piedra antigua de radio 3 con adoquín musgoso;
  - cuatro pilares rotos con piedra antigua con glifos y velas negras;
  - un **generador de fuegos fatuos** (`thaumcraft:wisp`) en el centro con piedra con glifos encima;
  - un **nodo siniestro** (`NodeType.DARK`) tres bloques por encima.
- **Obelisco siniestro** (biomas: eerie, `#c:is_spooky`, pantano, bosque oscuro). Probabilidad 1/110 (1/55 en
  eerie). Tiene:
  - un pedestal 5×5 de obsidiana sin esquinas, con el centro de obsidiana llorosa;
  - una aguja de obsidiana de 5–7 bloques con vetas de obsidiana llorosa y contrafuertes;
  - un **nodo siniestro** flotando dos bloques sobre la punta.

Los planos, materiales y probabilidades son **invención**: las fuentes solo confirman que existían santuarios
con nodo siniestro y fuegos fatuos. Los nodos se tiran con `AuraNodeGen.place`, así que sus aspectos dependen
del bioma, como cualquier nodo natural.

Investigación `SINISTERSHRINES` (Auromancia, [-5,-5], padre `AURANODES`): describe ambas estructuras.

## Texturas e idiomas

- `textures/foci/ward.png` (escudo con cerradura) y `textures/foci/primal.png` (estrella de seis puntas) son
  iconos blancos de 32×32 con alfa, al estilo de los iconos de foco de TC6, tintados por el color registrado.
  Protección usa 0x4040FF y Caos primigenio 0xFFFFFF.
- `textures/misc/ward_overlay.png`: marco y runas blancas con alfa, tintado azul y en modo aditivo.
- Textos en `en_us.json` y `es_es.json`: nombres/descripciones de los efectos y las tres investigaciones.

## Gametests

Archivo `FociStructuresVerification` con tres pruebas:
- `foci_ward`: el dueño queda guardado; otro jugador no puede romper ni quitar la protección; una explosión y un
  pistón no afectan al bloque; el dueño lo desprotege lanzando de nuevo y puede romperlo.
- `foci_primal`: los seis primigenios dañan y aplican su efecto; con taint y nodo forzados aparece taint fibroso y
  un nodo inestable.
- `shrines_structures`: el altar tiene un generador de fuegos fatuos y un nodo siniestro, y el obelisco tiene base
  de obsidiana y un nodo siniestro.
