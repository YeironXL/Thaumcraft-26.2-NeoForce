# Compatibilidad con otros mods (capacidades NeoForge)

Todas las integraciones usan las capacidades estándar de NeoForge 26.2 (`net.neoforged.neoforge.capabilities.Capabilities`, API de transferencia `ResourceHandler`). Cualquier mod que hable estas capacidades (tuberías, Create, Mekanism, Farmer's Delight, tolvas…) funciona sin código específico. Registro: `art.arcane.thaumcraft.registry.ModCapabilities`.

## Objetos (`Capabilities.Item.BLOCK`)

| Bloque | Reglas por cara |
|---|---|
| Horno infernal | Solo entrada por ARRIBA y solo objetos fundibles; nada se extrae (sale por delante, como TC6). |
| Fundidor de esencia (alambique/smelter, 3 variantes) | Laterales: material; abajo: combustible / extraer el recipiente vacío; arriba: nada (cara del alambique). |
| Spa / baño | Según `WorldlyContainer` (sales de baño). |
| Mesa de deconstrucción | Según `WorldlyContainer` (sin cara → arriba). |
| Cofre hambriento | Inventario completo. |
| Pedestales (arcano/antiguo/eldritch) y pedestal de recarga | 1 objeto; el de recarga solo admite objetos recargables. |
| Espejo mágico | Insertar un objeto lo envía al espejo vinculado. |
| Taumatorio (catalizador) y pulverizador de pociones | Solo inserción (la automatización no saca el catalizador/la poción). |
| Prensa de gólems | Solo extracción del gólem terminado. |

Bloques de GUI pura (mesa arcana, mesa de investigación, manipulador focal) no exponen inventario, igual que TC6.

## Fluidos (`Capabilities.Fluid.BLOCK`)

| Bloque | Reglas |
|---|---|
| Urna siempre llena | Solo vaciado, cara de ARRIBA (o sin cara); tanque de 1000 mB de agua que se rellena con vis. No acepta llenado. |
| Crisol | Solo acepta agua (hasta 1000 mB), no se puede vaciar (TC6 `TileCrucible.fill`). Transaccional. |
| Spa / baño | Tanque completo (llenar y vaciar). |

## Energía

Generador de vis: `Capabilities.Energy.BLOCK` por su cara frontal.

## Sellos de gólem

Los sellos de contenedores (recoger→llenar, vaciar, proveer, abastecer, fluidos) usan exclusivamente `Capabilities.Item.BLOCK` / `Capabilities.Fluid.BLOCK` del bloque objetivo (`golems/InvHelper.java`, `SealFluid`), así que funcionan con cualquier máquina de otro mod que exponga la capacidad, no solo con `Container` vanilla.

## Esencia (API pública)

La esencia **no** es un fluido. Se expone como capacidad propia:

```java
BlockCapability<EssentiaPort, @Nullable Direction> ModCapabilities.ESSENTIA  // id "thaumcraft:essentia"
```

`art.arcane.thaumcraft.essentia.EssentiaPort`:

- `connects(Direction side)` — si la cara acepta tubos.
- `suction()` / `suction(side)` / `minimumSuction()` — succión; la esencia fluye hacia la mayor succión.
- `wanted()` — aspecto que pide (`""` = cualquiera).
- `aspect()` / `offered(requested, side)` — aspecto que ofrece.
- `extractOne(aspect, side)` — extrae 1 punto; devuelve si tuvo éxito.

Registrada para jarras, tubos, centrífuga, taumatorio (base y parte superior), pulverizador de pociones y prensa de gólems. Los tubos de esencia buscan primero un `BlockEntity` que implemente `EssentiaPort` y, si no, consultan la capacidad `thaumcraft:essentia` del vecino: un mod externo solo tiene que registrar esa capacidad en su bloque para conectarse a la red de tubos. Los ids de aspecto son los de TC6 (`ignis`, `aqua`, …).

## Pruebas

`CompatCapabilitiesVerification` (`compat_capabilities`) y `GolemancyVerification.capability` (`golem_capability`).
