# Thaumcraft Remaster 

🌐 **English** · [Español](README.es.md)

**Unofficial port of Thaumcraft 6.1.BETA26 (Minecraft 1.12.2) to Minecraft 26.2 / NeoForge.**

Thaumcraft Remaster aims to reproduce Thaumcraft 6 as faithfully as possible on the current version of Minecraft, and also brings back Thaumcraft 4 and 5 content that never made it into TC6.

> Thaumcraft was created by **Azanor**. This is a fan-made port: it is unofficial and not affiliated with or endorsed by its author.

## Current version

| | |
|---|---|
| Version | **Thaumcraft Remaster 1.5** |
| Minecraft | 26.2 |
| Loader | NeoForge 26.2.0.88 or newer |
| Java | 25 (included with the official launcher and CurseForge) |

Download the jar from **[Releases](../../releases)**.

## Requirements and optional mods

- **Required:** NeoForge 26.2.0.88+
- **Recommended:** [Curios](https://www.curseforge.com/minecraft/mc-mods/curios), for amulets, rings and belts (baubles)
- **Optional:** JEI (crucible, Arcane Workbench, infusion recipes, etc.) and Jade (info for jars, crucibles and infusion)
- **Compatible with:** Sodium and Iris. Tested with Complementary (Reimagined and Unbound), BSL, Sildur's Vibrant, MakeUp Ultra Fast, Solas, Photon and Bliss

## What's included

**All of Thaumcraft 6 BETA26**
- Research and the Thaumonomicon, aspects and scanning with the Thaumometer
- Aura (vis and flux), warp and its effects
- Alchemy (crucible, essentia, jars, tubes), artifice and the Arcane Workbench
- Infusion (ritual, stability, infusion enchantments)
- Auromancy (gauntlets and foci), golemancy (golems, seals, turrets)
- Creatures, champions, eldritch bosses and the Magical Forest

**Thaumcraft 4 and 5 additions**
- The Outer Lands: obelisks, portal and a maze with bosses
- Aura nodes
- Thaumostatic Harness and Girdle
- Travelling Trunk
- Mana Pods and the Deconstruction Table
- Fishing, fluid and essentia seals
- Ward, Primal Chaos and Shock foci
- Wisp altar, Sinister obelisk and Barrow Den

**Remaster improvements**
- Tools and armour rebalanced against netherite, keeping each material's character (thaumium the most enchantable, void gear repairs itself)
- Thaumium, void metal and brass as armour trim materials at the smithing table
- A tree of 17 advancements that follows progression
- Reworked Barrow Den, Wisp Altar, Sinister Obelisk and Eldritch Obelisk that sit on the real terrain, with modern blocks and archaeology
- Ingots, nuggets and tools redrawn in the current style
- Essentia tubes optimised for large bases

**Other**
- Aspects for every Minecraft 26.2 item and creature
- Optional aspects for common metals from other mods, Create, Mekanism and Farmer's Delight
- Common `c:` tags for ingots, nuggets and metal blocks (Create and other mods recognise them)
- Standard NeoForge capabilities and a public essentia API for other mods

## Quick install

1. Create a Minecraft 26.2 profile with NeoForge (for example in CurseForge).
2. Copy `Thaumcraft Remaster 1.5.jar` into the profile's `mods` folder. Optionally add Curios, JEI and Jade.
3. Make sure there is no other version of the mod in the same folder.
4. Create a new world. To start: pick up a crystal from the ground, sleep, craft Salis Mundus and use it with **Shift + right click** on a bookshelf.

More details in the **[Wiki](../../wiki)** (English and Spanish).

## Project status

- 102 automated tests (gametests), visual checks in the client, exploration of freshly generated worlds and a performance test.
- **Not yet thoroughly tested** in long survival playthroughs or multiplayer. Back up your worlds.

Found a bug? Open an **[Issue](../../issues)** and include the file from `crash-reports/` or `logs/latest.log`.

## Building from source

Requires JDK 25.

```bash
./gradlew build          # builds the jar in build/libs/
./gradlew runGameTestServer   # runs the automated tests
```

## Credits and license

- **Thaumcraft**, its design, textures, models, sounds and texts: **Azanor**. All rights reserved.
- **NeoForge 26.2 port:** YeironXL.
- The TC4/TC5 additions are based on the community wikis (sources in `docs/*_FUENTES.md`).

This repository grants no license over the original Thaumcraft content. If the rights holder requests it, the project will comply.
