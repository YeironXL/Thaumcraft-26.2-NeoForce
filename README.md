
# Thaumcraft Remaster 26.2-NeoForce

**Port no oficial de Thaumcraft 6.1.BETA26 (Minecraft 1.12.2) a Minecraft 26.2 / NeoForge.**

Thaumcraft Remaster busca reproducir Thaumcraft 6 lo más fielmente posible en la versión actual de Minecraft, y además recupera contenido de Thaumcraft 4 y 5 que no llegó a TC6.

> Thaumcraft es obra de **Azanor**. Este proyecto es un port hecho por fans: no es oficial y no está afiliado a su autor ni respaldado por él.

## Versión actual

| | |
|---|---|
| Versión | **Thaumcraft Remaster 1.0** |
| Minecraft | 26.2 |
| Cargador | NeoForge 26.2.0.88 o superior |
| Java | 25 (lo incluyen el launcher oficial y CurseForge) |

Descarga el jar desde **[Releases](../../releases)**.

## Requisitos y mods opcionales

- **Obligatorio:** NeoForge 26.2.0.88+
- **Recomendado:** [Curios](https://www.curseforge.com/minecraft/mc-mods/curios), para los amuletos, anillos y cinturones (baubles)
- **Opcional:** JEI (recetas del crisol, la mesa arcana, la infusión, etc.) y Jade (información de frascos, crisoles e infusión)
- **Compatible con:** Sodium e Iris, incluidos shaderpacks

## Qué incluye

**Todo Thaumcraft 6 BETA26**
- Investigación y Thaumonomicon, aspectos y escaneo con el taumómetro
- Aura (vis y flux), warp y sus efectos
- Alquimia (crisol, esencia, frascos, tubos), artificio y mesa arcana
- Infusión (ritual, estabilidad, encantamientos de infusión)
- Auromancia (lanzadores y focos), golemancia (gólems, sellos, torretas)
- Criaturas, campeones, jefes eldritch y bosque mágico

**Añadidos de Thaumcraft 4 y 5**
- Tierras Exteriores (Outer Lands): obeliscos, portal y laberinto con jefes
- Nodos de aura
- Arnés y cinturón taumostáticos
- Baúl viajero
- Vainas de maná y mesa de deconstrucción
- Sellos de pesca, líquidos y esencia
- Focos de protección y de caos primordial
- Altar de wisps y obelisco siniestro

**Otros**
- Aspectos para todos los objetos y criaturas de Minecraft 26.2
- Aspectos opcionales para metales comunes de otros mods, Create, Mekanism y Farmer's Delight

## Instalación rápida

1. Crea un perfil de Minecraft 26.2 con NeoForge (por ejemplo en CurseForge).
2. Copia `Thaumcraft Remaster 1.0.jar` en la carpeta `mods` del perfil. Opcionalmente, añade Curios, JEI y Jade.
3. Asegúrate de no tener otra versión del mod en la misma carpeta.
4. Crea un mundo nuevo. Para empezar: recoge un cristal del suelo, duerme, fabrica Salis Mundus y úsalo con **Shift + clic derecho** sobre una estantería.

Más detalles en la **[Wiki](../../wiki)**.

## Estado del proyecto

- 82 pruebas automáticas (gametests) y comprobaciones visuales en cliente.
- **Aún no probado a fondo** en partidas largas de supervivencia ni en multijugador. Haz copias de seguridad de tus mundos.

¿Encontraste un fallo? Abre un **[Issue](../../issues)** e incluye el archivo de `crash-reports/` o `logs/latest.log`.

## Compilar desde el código

Requiere JDK 25.

```bash
./gradlew build          # genera el jar en build/libs/
./gradlew runGameTestServer   # ejecuta las pruebas automáticas
```

## Créditos y licencia

- **Thaumcraft**, su diseño, texturas, modelos, sonidos y textos: **Azanor**. Todos los derechos reservados.
- **Port a NeoForge 26.2:** YeironXL.
- Los añadidos de TC4/TC5 se basan en las wikis de la comunidad (fuentes en `docs/*_FUENTES.md`).

Este repositorio no concede ninguna licencia sobre el contenido original de Thaumcraft. Si el titular de los derechos lo solicita, el proyecto atenderá su petición.
