# Aspectos para otros mods y etiquetas comunes

Datos opcionales en `src/main/resources/data/thaumcraft/aspects/compat/`, aplicados por
`ObjectAspects.Engine.applyCompat()` justo después de `objects.json` y antes de derivar por recetas.

## Formato

```json
{"requires": ["create"], "entries": [ {"op": "set", "targets": ["create:raw_zinc", "#c:..."], "ops": [["add","metallum",7]]} ]}
```

- `entries` usa exactamente el mismo formato que `objects.json` (`set`/`complex`, `base`, `#tag`).
- `requires` (opcional, a nivel de archivo o de entrada): lista de mod ids; si alguno no está cargado
  (`ModList.isLoaded`) se omite. Una sola línea DEBUG lista los archivos omitidos.
- Ids desconocidos, ids mal formados, etiquetas vacías y aspectos inexistentes se ignoran en silencio.
- Las entradas explícitas por id de `objects.json` (las de TC6) tienen prioridad sobre las de compat
  si se repiten más tarde en `objects.json`; compat sólo añade lo que TC6 no conocía.
- Archivos: `common.json`, `create.json`, `mekanism.json`, `farmersdelight.json` (lista fija en `COMPAT`).

## Criterio

Sólo lo que no se puede derivar de recetas de crafteo/arcanas/crisol: menas, materiales en bruto,
lingotes/polvos/placas producidos por máquinas (prensa, triturador, infusor), cultivos, drops de mobs,
bloques de generación de mundo y comidas de olla/tabla de cortar. Bloques de almacenamiento, pepitas y
cualquier cosa crafteable se derivan solas.

Magnitudes TC6: lingote de metal = metallum 10 + aspecto secundario 5 (como cobre/estaño/plata/plomo de TC6);
mena = lingote + terra 5; polvo = lingote + perditio 1; material en bruto = metallum 7, secundario 3, terra 2
(como raw_copper); placa = lingote + fabrico 1. Secundarios: níquel/iridio ordo, osmio/titanio praemunio,
zinc/constantán permutatio, aluminio aer, platino/electrum desiderium, cobalto motus, latón instrumentum,
invar ordo, obsidiana refinada praemunio, piedra luminosa refinada lux. Gemas nuevas (rubí/zafiro/peridoto)
siguen las de TC6; fluorita vitreus 5 ordo 5.

## common.json (cualquier mod que use etiquetas c:)

`c:ingots|dusts|ores|raw_materials/{nickel,osmium,zinc,aluminum,platinum,iridium,cobalt,titanium}`,
`c:ingots|dusts/{brass,electrum,invar,constantan,refined_obsidian,refined_glowstone}`,
`c:raw_materials/{tin,lead,silver,uranium}`, `c:dusts/{uranium,steel,salt,sulfur,coal,diamond,obsidian,quartz,emerald,lapis,netherite}`,
`c:ores/{ruby,sapphire,peridot,fluorite}`, `c:gems/fluorite`, `c:plates/*` (hierro, oro, cobre, latón, estaño, plomo, acero, aluminio, zinc),
`c:crops/{rice,tomato,onion,cabbage,corn,lettuce,barley}`, `c:seeds/{rice,tomato,cabbage}`.
(Se quitó `c:dusts/refined_glowstone`: Mekanism no tiene ese polvo; el lingote se cubre con `c:ingots/refined_glowstone`.)
(Estaño, plomo, plata, uranio, bronce, acero, rubí, zafiro y peridoto ya venían de TC6 en `objects.json`.)

## Disponibilidad en 26.2 (consultado 2026-10-19 en Modrinth)

Ninguno de los tres tiene versión oficial para 26.x: Create 6.0.10, Mekanism 10.7.19 y Farmer's Delight 1.3.4
son para 1.21.1. Los ids usados son los de esas versiones 1.21.1. Existe un port no oficial
"Farmer's Delight 26 Neo Ver" (BuaichiZY) para 26.1.2/26.2 que conserva el namespace `farmersdelight`.
Al salir versiones oficiales conviene revisar los ids.

- **Create** (`create.json`): zinc (mena, bruto, lingote), latón, aleación de andesita, cuarzo rosa,
  harinas, masa, pepita de experiencia, láminas, minerales triturados, piedras de generación
  (limestone, scoria, scorchia, asurine, crimsite, ochrum, veridium), tubo de electrones, mecanismo de precisión, comidas.
- **Mekanism** (`mekanism.json`): osmio, fluorita, sal, serrín, biocombustible, sustrato, HDPE, aleaciones,
  circuito básico, materiales enriquecidos, yellow cake, pellets, obsidiana/piedra luminosa refinada, acero, bronce.
- **Farmer's Delight** (`farmersdelight.json`): cultivos y semillas, plantas silvestres, paja, corteza, jamón,
  cortes de carne/pescado, masas, tierra rica, compost, colonias de champiñones, bebidas y salsa.

## Verificación contra los jars reales (2026-10-19)

Comprobado contra `create-1.21.1-6.0.10.jar`, `Mekanism-1.21.1-10.7.19.85.jar` y
`FarmersDelight-1.21.1-1.3.4.jar` (NeoForge, Modrinth), leyendo `assets/<ns>/models/item`, `items/`,
`blockstates/`, claves `item.<ns>.*`/`block.<ns>.*` de `en_us.json` y `data/c/tags/item/`.

- Los 111 ids escritos de memoria existen tal cual (Create 43, Mekanism 30, Farmer's Delight 38); no hizo falta corregir ni quitar ninguno.
- Etiquetas `c:`: coinciden con las que usan los mods (`c:ingots/zinc`, `c:raw_materials/{zinc,osmium,tin,lead,uranium}`,
  `c:ores/{zinc,osmium,tin,lead,uranium,fluorite}`, `c:crops/{rice,tomato,onion,cabbage}`, `c:dusts/*` de Mekanism, `c:plates/{iron,gold,copper,brass}`...).
  Las de metales que no aportan estos mods (níquel, aluminio, platino, etc.) se mantienen como convención para otros mods.
- Añadidos (sin receta en el mod): Mekanism `tin_ore`, `lead_ore`, `uranium_ore` (+ deepslate), `raw_osmium`, `raw_tin`,
  `raw_lead`, `raw_uranium`, `dirty_netherite_scrap`; Create `crushed_raw_quicksilver`, `experience_block`;
  Farmer's Delight `rotten_tomato`, `sandy_shrub`.
- Ninguno publica versión 26.x en Modrinth. El port no oficial de Farmer's Delight 26 (BuaichiZY) no aparece en
  Modrinth y la API de CurseForge exige clave, así que sus ids no se pudieron comprobar.

## Pruebas

`compat_aspects` (gametest): un datapack de verificación mete `minecraft:flower_banner_pattern` en `c:ingots/tin`
y `minecraft:creeper_banner_pattern` en `c:raw_materials/zinc` y comprueba sus aspectos; `compat/verification.json`
(sólo en verificación) contiene ids desconocidos, una etiqueta vacía, un aspecto inexistente y entradas con
`requires` de un mod ausente, y se comprueba que no se aplican ni rompen la carga.
