# Inventario de referencia y pendientes de Thaumcraft 6

Base de comportamiento: Thaumcraft 1.12.2-6.1.BETA26. La rama 1.8.9 sirve como referencia histórica; sus mecánicas no se mezclan automáticamente con TC6.

Fuentes verificadas:
- [Publicación de Azanor](https://www.curseforge.com/minecraft/mc-mods/thaumcraft/files/2629023).
- [Reconstrucción comunitaria de código](https://github.com/TheDarkTower314/Thaumcraft-6-Source-Code), commit `954022bb777b7546281fb36df8522f0ba6b43f81`.

Se inventariaron **902 archivos Java** de la referencia. `AUDITORIA_TC6.json` conserva las rutas y hashes para revisar cada sistema. Esto es un inventario, no una afirmación de haber leído y portado cada clase.

El port contiene actualmente **99 recetas nativas**: {'thaumcraft:shapeless_arcane': 7, 'thaumcraft:arcane': 16, 'thaumcraft:crucible': 44, 'minecraft:crafting_shaped': 15, 'minecraft:crafting_shapeless': 16, 'thaumcraft:magic_dust': 1}. Los costes y requisitos de las recetas añadidas se contrastaron con `ConfigRecipes.java`; no todas las máquinas u objetos producidos tienen aún su comportamiento.

| Sistema | Estado actual | Trabajo restante |
| --- | --- | --- |
| Aspectos y cristales | 37 aspectos; variantes persistentes; cristales primales en cuevas; gotas por tamaño | Distribución exacta de biomas y caras múltiples de la geometría original |
| Inicio | Cristales, diario, Salis Mundus, Thaumonomicon con entrega inicial única solicitada, etapas y mesa arcana | Prueba manual del recorrido completo y requisitos antiguos aún sin migrar |
| Investigación | Catálogo, etapas, observaciones, descubrimientos, costes por aspecto y mesa con quince cartas y ayudas de biblioteca/crisol/manipulador | Cartas/ayudas específicas restantes, escaneo del cielo y todos los disparadores especiales |
| Aura | Vis/flux por chunk, gasto y regeneración lunar | Difusión entre chunks, generación y simulación completa de contaminación y grietas |
| Alquimia | Crisol con agua/calor/aspectos; Nitor; Alumentum combustible y explosivo; metales y cristales | Todas las recetas, etiquetas derivadas, fluidos y transporte por tuberías/tolvas |
| Esencia | Destilación con combustible, interfaz e inventario automático por caras, alambiques de 128, tubos normales y cinco variantes con succión, filtros, redstone, direcciones y buffer, tarros de 250 con líquido visible y vacío automático; frascos de 10; centrífuga con separación, redstone y persistencia | Aspectos derivados completos, auxiliares, etiquetas, usos restantes de fuelles y automatización avanzada |
| Auromancia | Recetas iniciales | Guantelete funcional, componentes de focos, manipulador, hechizos y efectos |
| Infusión | Registros/modelos conservados | Altar, pedestales, estabilidad, consumo de esencia, fallos y encantamientos |
| Golemancia | Registros/recetas de referencia inventariados | Constructor, materiales, rasgos, IA, sellos y tareas |
| Dispositivos | Hungry Chest y radio del cargador de mesa | Horno infernal funcional, espejos, lámparas, baterías, generadores y demás dispositivos |
| Herramientas/armaduras | Objetos y recursos conservados; algunas recetas | Estadísticas, durabilidad, equipamiento, habilidades y encantamientos propios |
| Entidades | Datos de escaneo para entidades vanilla; proyectil Alumentum funcional | Las demás entidades propias del mod, modelos, animaciones, IA y aparición |
| Mundo | Cristales primales en chunks nuevos | Árboles, plantas, minerales, estructuras, biomas, corrupción y contenido eldritch |
| Visual/sonido | Menús iniciales adaptados; Nitor visible; sonidos registrados | Renderizadores y partículas originales restantes, animaciones y revisión de modelos |

El control general de bugs queda después de la implementación. Las pruebas de compilación, datos y transacciones que acompañan cada cambio son validación de desarrollo.

## Grupos de código original

| Ruta | Archivos Java |
| --- | ---: |
| `com/sasmaster/glelwjgl/java` | 6 |
| `thaumcraft/Registrar.java` | 1 |
| `thaumcraft/Thaumcraft.java` | 1 |
| `thaumcraft/api/OreDictionaryEntries.java` | 1 |
| `thaumcraft/api/ThaumcraftApi.java` | 1 |
| `thaumcraft/api/ThaumcraftApiHelper.java` | 1 |
| `thaumcraft/api/ThaumcraftInvHelper.java` | 1 |
| `thaumcraft/api/ThaumcraftMaterials.java` | 1 |
| `thaumcraft/api/aspects/Aspect.java` | 1 |
| `thaumcraft/api/aspects/AspectEventProxy.java` | 1 |
| `thaumcraft/api/aspects/AspectHelper.java` | 1 |
| `thaumcraft/api/aspects/AspectList.java` | 1 |
| `thaumcraft/api/aspects/AspectRegistryEvent.java` | 1 |
| `thaumcraft/api/aspects/AspectSourceHelper.java` | 1 |
| `thaumcraft/api/aspects/IAspectContainer.java` | 1 |
| `thaumcraft/api/aspects/IAspectSource.java` | 1 |
| `thaumcraft/api/aspects/IEssentiaContainerItem.java` | 1 |
| `thaumcraft/api/aspects/IEssentiaTransport.java` | 1 |
| `thaumcraft/api/aura/AuraHelper.java` | 1 |
| `thaumcraft/api/blocks/BlocksTC.java` | 1 |
| `thaumcraft/api/blocks/ILabelable.java` | 1 |
| `thaumcraft/api/capabilities/IPlayerKnowledge.java` | 1 |
| `thaumcraft/api/capabilities/IPlayerWarp.java` | 1 |
| `thaumcraft/api/capabilities/ThaumcraftCapabilities.java` | 1 |
| `thaumcraft/api/casters/CasterTriggerRegistry.java` | 1 |
| `thaumcraft/api/casters/FocusEffect.java` | 1 |
| `thaumcraft/api/casters/FocusEngine.java` | 1 |
| `thaumcraft/api/casters/FocusMedium.java` | 1 |
| `thaumcraft/api/casters/FocusMediumRoot.java` | 1 |
| `thaumcraft/api/casters/FocusMod.java` | 1 |
| `thaumcraft/api/casters/FocusModSplit.java` | 1 |
| `thaumcraft/api/casters/FocusNode.java` | 1 |
| `thaumcraft/api/casters/FocusPackage.java` | 1 |
| `thaumcraft/api/casters/ICaster.java` | 1 |
| `thaumcraft/api/casters/ICasterTriggerManager.java` | 1 |
| `thaumcraft/api/casters/IFocusBlockPicker.java` | 1 |
| `thaumcraft/api/casters/IFocusElement.java` | 1 |
| `thaumcraft/api/casters/IInteractWithCaster.java` | 1 |
| `thaumcraft/api/casters/NodeSetting.java` | 1 |
| `thaumcraft/api/casters/Trajectory.java` | 1 |
| `thaumcraft/api/crafting/ContainerDummy.java` | 1 |
| `thaumcraft/api/crafting/CrucibleRecipe.java` | 1 |
| `thaumcraft/api/crafting/IArcaneRecipe.java` | 1 |
| `thaumcraft/api/crafting/IArcaneWorkbench.java` | 1 |
| `thaumcraft/api/crafting/IDustTrigger.java` | 1 |
| `thaumcraft/api/crafting/IInfusionStabiliser.java` | 1 |
| `thaumcraft/api/crafting/IInfusionStabiliserExt.java` | 1 |
| `thaumcraft/api/crafting/IStabilizable.java` | 1 |
| `thaumcraft/api/crafting/IThaumcraftRecipe.java` | 1 |
| `thaumcraft/api/crafting/InfusionRecipe.java` | 1 |
| `thaumcraft/api/crafting/IngredientNBTTC.java` | 1 |
| `thaumcraft/api/crafting/Part.java` | 1 |
| `thaumcraft/api/crafting/RecipeMisc.java` | 1 |
| `thaumcraft/api/crafting/ShapedArcaneRecipe.java` | 1 |
| `thaumcraft/api/crafting/ShapelessArcaneRecipe.java` | 1 |
| `thaumcraft/api/damagesource/DamageSourceIndirectThaumcraftEntity.java` | 1 |
| `thaumcraft/api/damagesource/DamageSourceThaumcraft.java` | 1 |
| `thaumcraft/api/entities/IEldritchMob.java` | 1 |
| `thaumcraft/api/entities/ITaintedMob.java` | 1 |
| `thaumcraft/api/golems/EnumGolemTrait.java` | 1 |
| `thaumcraft/api/golems/GolemHelper.java` | 1 |
| `thaumcraft/api/golems/IGolemAPI.java` | 1 |
| `thaumcraft/api/golems/IGolemProperties.java` | 1 |
| `thaumcraft/api/golems/ISealDisplayer.java` | 1 |
| `thaumcraft/api/golems/ProvisionRequest.java` | 1 |
| `thaumcraft/api/golems/parts` | 7 |
| `thaumcraft/api/golems/seals` | 7 |
| `thaumcraft/api/golems/tasks` | 1 |
| `thaumcraft/api/internal/CommonInternals.java` | 1 |
| `thaumcraft/api/internal/DummyInternalMethodHandler.java` | 1 |
| `thaumcraft/api/internal/IInternalMethodHandler.java` | 1 |
| `thaumcraft/api/internal/WeightedRandomLoot.java` | 1 |
| `thaumcraft/api/internal/WorldCoordinates.java` | 1 |
| `thaumcraft/api/items/IArchitect.java` | 1 |
| `thaumcraft/api/items/IGoggles.java` | 1 |
| `thaumcraft/api/items/IGogglesDisplayExtended.java` | 1 |
| `thaumcraft/api/items/IRechargable.java` | 1 |
| `thaumcraft/api/items/IRevealer.java` | 1 |
| `thaumcraft/api/items/IScribeTools.java` | 1 |
| `thaumcraft/api/items/IVisDiscountGear.java` | 1 |
| `thaumcraft/api/items/IWarpingGear.java` | 1 |
| `thaumcraft/api/items/ItemGenericEssentiaContainer.java` | 1 |
| `thaumcraft/api/items/ItemsTC.java` | 1 |
| `thaumcraft/api/items/RechargeHelper.java` | 1 |
| `thaumcraft/api/potions/PotionFluxTaint.java` | 1 |
| `thaumcraft/api/potions/PotionVisExhaust.java` | 1 |
| `thaumcraft/api/research/IScanThing.java` | 1 |
| `thaumcraft/api/research/ResearchAddendum.java` | 1 |
| `thaumcraft/api/research/ResearchCategories.java` | 1 |
| `thaumcraft/api/research/ResearchCategory.java` | 1 |
| `thaumcraft/api/research/ResearchEntry.java` | 1 |
| `thaumcraft/api/research/ResearchEvent.java` | 1 |
| `thaumcraft/api/research/ResearchStage.java` | 1 |
| `thaumcraft/api/research/ScanAspect.java` | 1 |
| `thaumcraft/api/research/ScanBlock.java` | 1 |
| `thaumcraft/api/research/ScanBlockState.java` | 1 |
| `thaumcraft/api/research/ScanEntity.java` | 1 |
| `thaumcraft/api/research/ScanItem.java` | 1 |
| `thaumcraft/api/research/ScanMaterial.java` | 1 |
| `thaumcraft/api/research/ScanOreDictionary.java` | 1 |
| `thaumcraft/api/research/ScanningManager.java` | 1 |
| `thaumcraft/api/research/theorycraft` | 14 |
| `thaumcraft/client/ColorHandler.java` | 1 |
| `thaumcraft/client/fx/FXDispatcher.java` | 1 |
| `thaumcraft/client/fx/ParticleEngine.java` | 1 |
| `thaumcraft/client/fx/beams` | 4 |
| `thaumcraft/client/fx/other` | 6 |
| `thaumcraft/client/fx/particles` | 19 |
| `thaumcraft/client/gui/GuiArcaneBore.java` | 1 |
| `thaumcraft/client/gui/GuiArcaneWorkbench.java` | 1 |
| `thaumcraft/client/gui/GuiFocalManipulator.java` | 1 |
| `thaumcraft/client/gui/GuiFocusPouch.java` | 1 |
| `thaumcraft/client/gui/GuiGolemBuilder.java` | 1 |
| `thaumcraft/client/gui/GuiHandMirror.java` | 1 |
| `thaumcraft/client/gui/GuiLogistics.java` | 1 |
| `thaumcraft/client/gui/GuiPech.java` | 1 |
| `thaumcraft/client/gui/GuiPotionSprayer.java` | 1 |
| `thaumcraft/client/gui/GuiResearchBrowser.java` | 1 |
| `thaumcraft/client/gui/GuiResearchPage.java` | 1 |
| `thaumcraft/client/gui/GuiResearchTable.java` | 1 |
| `thaumcraft/client/gui/GuiSmelter.java` | 1 |
| `thaumcraft/client/gui/GuiSpa.java` | 1 |
| `thaumcraft/client/gui/GuiThaumatorium.java` | 1 |
| `thaumcraft/client/gui/GuiTurretAdvanced.java` | 1 |
| `thaumcraft/client/gui/GuiTurretBasic.java` | 1 |
| `thaumcraft/client/gui/GuiVoidSiphon.java` | 1 |
| `thaumcraft/client/gui/ResearchToast.java` | 1 |
| `thaumcraft/client/gui/plugins` | 7 |
| `thaumcraft/client/lib/CustomRenderItem.java` | 1 |
| `thaumcraft/client/lib/RenderCubes.java` | 1 |
| `thaumcraft/client/lib/TexturedQuadTC.java` | 1 |
| `thaumcraft/client/lib/UtilsFX.java` | 1 |
| `thaumcraft/client/lib/ender` | 2 |
| `thaumcraft/client/lib/events` | 4 |
| `thaumcraft/client/lib/obj` | 11 |
| `thaumcraft/client/renderers/block` | 1 |
| `thaumcraft/client/renderers/entity` | 36 |
| `thaumcraft/client/renderers/models` | 28 |
| `thaumcraft/client/renderers/tile` | 22 |
| `thaumcraft/codechicken/lib/colour` | 3 |
| `thaumcraft/codechicken/lib/lighting` | 4 |
| `thaumcraft/codechicken/lib/math` | 1 |
| `thaumcraft/codechicken/lib/raytracer` | 3 |
| `thaumcraft/codechicken/lib/render` | 11 |
| `thaumcraft/codechicken/lib/util` | 1 |
| `thaumcraft/codechicken/lib/vec` | 19 |
| `thaumcraft/common/blocks/BlockTC.java` | 1 |
| `thaumcraft/common/blocks/BlockTCDevice.java` | 1 |
| `thaumcraft/common/blocks/BlockTCTile.java` | 1 |
| `thaumcraft/common/blocks/IBlockEnabled.java` | 1 |
| `thaumcraft/common/blocks/IBlockFacing.java` | 1 |
| `thaumcraft/common/blocks/IBlockFacingHorizontal.java` | 1 |
| `thaumcraft/common/blocks/basic` | 13 |
| `thaumcraft/common/blocks/crafting` | 10 |
| `thaumcraft/common/blocks/devices` | 23 |
| `thaumcraft/common/blocks/essentia` | 10 |
| `thaumcraft/common/blocks/misc` | 8 |
| `thaumcraft/common/blocks/world` | 18 |
| `thaumcraft/common/config/ConfigAspects.java` | 1 |
| `thaumcraft/common/config/ConfigBlocks.java` | 1 |
| `thaumcraft/common/config/ConfigEntities.java` | 1 |
| `thaumcraft/common/config/ConfigItems.java` | 1 |
| `thaumcraft/common/config/ConfigRecipes.java` | 1 |
| `thaumcraft/common/config/ConfigResearch.java` | 1 |
| `thaumcraft/common/config/ModConfig.java` | 1 |
| `thaumcraft/common/container/ContainerArcaneBore.java` | 1 |
| `thaumcraft/common/container/ContainerArcaneWorkbench.java` | 1 |
| `thaumcraft/common/container/ContainerFocalManipulator.java` | 1 |
| `thaumcraft/common/container/ContainerFocusPouch.java` | 1 |
| `thaumcraft/common/container/ContainerGolemBuilder.java` | 1 |
| `thaumcraft/common/container/ContainerHandMirror.java` | 1 |
| `thaumcraft/common/container/ContainerLogistics.java` | 1 |
| `thaumcraft/common/container/ContainerPech.java` | 1 |
| `thaumcraft/common/container/ContainerPotionSprayer.java` | 1 |
| `thaumcraft/common/container/ContainerResearchTable.java` | 1 |
| `thaumcraft/common/container/ContainerSmelter.java` | 1 |
| `thaumcraft/common/container/ContainerSpa.java` | 1 |
| `thaumcraft/common/container/ContainerThaumatorium.java` | 1 |
| `thaumcraft/common/container/ContainerTurretAdvanced.java` | 1 |
| `thaumcraft/common/container/ContainerTurretBasic.java` | 1 |
| `thaumcraft/common/container/ContainerVoidSiphon.java` | 1 |
| `thaumcraft/common/container/InventoryArcaneWorkbench.java` | 1 |
| `thaumcraft/common/container/InventoryFake.java` | 1 |
| `thaumcraft/common/container/InventoryFocusPouch.java` | 1 |
| `thaumcraft/common/container/InventoryHandMirror.java` | 1 |
| `thaumcraft/common/container/InventoryLogistics.java` | 1 |
| `thaumcraft/common/container/InventoryPech.java` | 1 |
| `thaumcraft/common/container/slot` | 14 |
| `thaumcraft/common/entities/EntityFallingTaint.java` | 1 |
| `thaumcraft/common/entities/EntityFluxRift.java` | 1 |
| `thaumcraft/common/entities/EntityFollowingItem.java` | 1 |
| `thaumcraft/common/entities/EntitySpecialItem.java` | 1 |
| `thaumcraft/common/entities/ai` | 7 |
| `thaumcraft/common/entities/construct` | 5 |
| `thaumcraft/common/entities/monster` | 44 |
| `thaumcraft/common/entities/projectile` | 12 |
| `thaumcraft/common/golems/EntityThaumcraftGolem.java` | 1 |
| `thaumcraft/common/golems/GolemInteractionHelper.java` | 1 |
| `thaumcraft/common/golems/GolemProperties.java` | 1 |
| `thaumcraft/common/golems/ItemGolemBell.java` | 1 |
| `thaumcraft/common/golems/ItemGolemPlacer.java` | 1 |
| `thaumcraft/common/golems/ai` | 15 |
| `thaumcraft/common/golems/client` | 14 |
| `thaumcraft/common/golems/parts` | 3 |
| `thaumcraft/common/golems/seals` | 20 |
| `thaumcraft/common/golems/tasks` | 1 |
| `thaumcraft/common/items/IThaumcraftItems.java` | 1 |
| `thaumcraft/common/items/ItemTCBase.java` | 1 |
| `thaumcraft/common/items/ItemTCEssentiaContainer.java` | 1 |
| `thaumcraft/common/items/armor` | 12 |
| `thaumcraft/common/items/baubles` | 7 |
| `thaumcraft/common/items/casters` | 24 |
| `thaumcraft/common/items/consumables` | 10 |
| `thaumcraft/common/items/curios` | 7 |
| `thaumcraft/common/items/misc` | 2 |
| `thaumcraft/common/items/resources` | 2 |
| `thaumcraft/common/items/tools` | 23 |
| `thaumcraft/common/lib/BehaviorDispenseAlumetum.java` | 1 |
| `thaumcraft/common/lib/CommandThaumcraft.java` | 1 |
| `thaumcraft/common/lib/CreativeTabThaumcraft.java` | 1 |
| `thaumcraft/common/lib/InternalMethodHandler.java` | 1 |
| `thaumcraft/common/lib/RefGui.java` | 1 |
| `thaumcraft/common/lib/SoundsTC.java` | 1 |
| `thaumcraft/common/lib/capabilities` | 2 |
| `thaumcraft/common/lib/crafting` | 13 |
| `thaumcraft/common/lib/enchantment` | 1 |
| `thaumcraft/common/lib/events` | 10 |
| `thaumcraft/common/lib/network` | 43 |
| `thaumcraft/common/lib/potions` | 7 |
| `thaumcraft/common/lib/research` | 43 |
| `thaumcraft/common/lib/utils` | 9 |
| `thaumcraft/common/tiles/TileThaumcraft.java` | 1 |
| `thaumcraft/common/tiles/TileThaumcraftInventory.java` | 1 |
| `thaumcraft/common/tiles/crafting` | 13 |
| `thaumcraft/common/tiles/devices` | 20 |
| `thaumcraft/common/tiles/essentia` | 14 |
| `thaumcraft/common/tiles/misc` | 5 |
| `thaumcraft/common/world/ThaumcraftWorldGenerator.java` | 1 |
| `thaumcraft/common/world/aura` | 4 |
| `thaumcraft/common/world/biomes` | 4 |
| `thaumcraft/common/world/objects` | 5 |
| `thaumcraft/proxies/ClientProxy.java` | 1 |
| `thaumcraft/proxies/CommonProxy.java` | 1 |
| `thaumcraft/proxies/IProxy.java` | 1 |
| `thaumcraft/proxies/ProxyBlock.java` | 1 |
| `thaumcraft/proxies/ProxyEntities.java` | 1 |
| `thaumcraft/proxies/ProxyGUI.java` | 1 |
| `thaumcraft/proxies/ProxyTESR.java` | 1 |
| `thaumcraft/proxies/ServerProxy.java` | 1 |
| `vazkii/botania/api/item` | 1 |

## Avance fix34

Mesa de investigación nativa, recetas de mesa/tintero, recarga, conversión, nueve cartas básicas y ayuda de biblioteca. Papel, tinta, inspiración, persistencia y cobro único de teorías. Pantalla en español revisada visualmente. El alcance por etapas de `ETAPAS_EARLY_GAME.md` sigue pendiente de completarse; no se certifica aún el inicio integral ni el primer foco.

## Avance fix35

Seis cartas específicas de alquimia/auromancia, tres ayudas iniciales seleccionables, costes por aspecto, niveles y riesgo de distorsión. Recetas de materiales y foco vacío, descubrimiento de fuego y fabricación del manipulador: etapas reales de BASEAUROMANCY probadas. La configuración y el lanzamiento del foco todavía no están portados; no se certifica el early game integral.


## Avance fix36

Manipulador con inventario, menú y pantalla para configurar Toque/Fuego, fabricación con costes originales, persistencia y ranura bloqueada. Pruebas de servidor y revisión de interfaz realizadas. El lanzamiento del guantelete, otros grafos y recorrido integral temprano siguen pendientes; no se certifica el early game.


## Avance fix37

Guantelete nativo con montaje/intercambio/retirada del foco completo y lanzamiento Toque/Fuego. Pruebas de obstáculos, alcance, coste fraccionario, falta de aura y enfriamiento entre manos superadas. Diez pruebas de servidor pasan. Descuentos de equipo/agotamiento y motor general de nodos siguen pendientes. Próximo paso: aspectos derivados de recetas y recorrido integral temprano. No se cierra todavía el early game.


## Avance fix38

23 registros base y mapa activo de 14 derivados iniciales con sincronización, adiciones/copias y limpieza. Destilación real de mesa arcana probada: 78 ticks, salida de Auram y conservación de 39 aspectos incluyendo flux. Diez pruebas de servidor pasan. Pendientes cobertura ampliada, conexión real con recarga y recorrido integral temprano. No se cierra early game.


## Recorrido continuo: introducción a la auromancia

`verifyInitialAuromancy` reutiliza el jugador del inicio y la alquimia, sin asignar investigaciones o etapas directamente. Publica PlayerTickEvent.Post en profundidad y altura, verifica que un descubrimiento solo no permite avanzar, fabrica el resonador con 50 vis y Aer/Aqua, y el guantelete con 100 vis y los seis cristales primales. El guantelete consume el resonador recién fabricado y el taumómetro realmente producido al inicio; todos los ingredientes y cristales quedan consumidos. Completa UNLOCKAUROMANCY y abre BASEAUROMANCY. Diez pruebas requeridas pasan (`auromancy-route-server.log`).

El aviso de descubrimiento se envía únicamente si existe conexión; los desbloqueos siguen ejecutándose para jugadores mock. Los materiales, aura y desplazamientos son fixtures. Continúan pendientes con este mismo jugador la mesa de investigación/teorías, fabricación y configuración del primer foco, montaje y lanzamiento; esos sistemas tienen pruebas independientes, pero eso no demuestra todavía el recorrido continuo completo. Tampoco se ha validado obtención natural de recursos ni una sesión de cliente conectado. Última entrega empaquetada: fix38.


## Fix39: recorrido continuo temprano verificado en servidor

El mismo jugador mock supervivencia completa inicio, alquimia, introducción a auromancia, primer foco y una sesión de investigación sin asignar capítulos ni etapas a mano. La prueba recoge el objeto físico producido por el crisol, fabrica un segundo resonador para el manipulador, comprueba la puerta de descubrimiento de fuego y completa BASEAUROMANCY por fabricación real. Configura Touch/Fire mínimo desde el menú: dos niveles, Aversio/Ignis, 43 vis en tres segundos. Monta ese foco en el guantelete fabricado antes y usa el ítem contra un husk: daño4, ignición y 0,8 vis. Después fabrica herramientas de escritura, forma mesa de investigación y termina una sesión con costes de papel/tinta, recursos especializados y cobro único de teorías. Diez pruebas pasan (`continuous-theory-server.log`).

Limitaciones: agua, materiales, aspectos del foco, cristales compuestos, experiencia y aura se suministran; escaneos invocan el motor de recompensas y se simulan desplazamientos/ticks. La piedra/losas/pedestal y herramientas se ensamblan por recetas cargadas, sin simular extracción de materiales ni interfaz de fabricación vanilla; la colocación del manipulador y mesa usa fixtures. El jugador no tiene conexión real. No demuestra obtención natural de todos los recursos, sincronización de una sesión conectada ni partida manual. La investigación se comprueba después del foco; no representa un orden obligatorio de progresión. No se certifica early game completo ni paridad total; pendientes auditoría del suministro natural de componentes, conexión y recorrido manual, y ampliación de cartas/focos/aspectos.


## Fix40: suministro de Aversio para el primer foco

Se incorpora la adición compleja original de Aversio5 de las flechas (ConfigAspects.java de TC6). Su receta normal aporta Terra1, Instrumentum1, Aer1 y Volatus1 tras escalado por cuatro salidas; Herba del palo no alcanza el redondeo mínimo. El mapa activo pasa de 14 a 15 derivados. No se inventan aspectos de armas ni se amplía automáticamente toda la lista.

La prueba del mismo jugador abre BASEALCHEMY por ResearchProgression, ensambla flechas con pedernal/palo/pluma y nueve pepitas de cuarzo por receta cargada, disuelve una flecha en un crisol caliente, cataliza una pepita y recoge el cristal físico de Aversio. Se cobran dos Aversio y 50 mB y quedan tres Aversio. Ese cristal entra en su inventario y se consume después al preparar su primer foco; se elimina la fixture de Aversio para esa preparación. Siguen suministrados Ignis, materias primas, agua/calor, aura y XP, así como los aspectos de fabricación del foco vacío. No es aún prueba de abastecimiento natural completo ni sesión conectada.

Validación fix40: las diez pruebas requeridas pasan (`server-verification-fix40.log`), incluido el uso del cristal producido en el foco continuo; compilación de producción correcta (`build-verification-fix40.log`).


## Comprobación posterior a fix40: materias del foco vacío

La prueba continua deja de inyectar directamente Vitreus20/Praecantatio10/Auram5. Con el mismo jugador, convierte otra mesa de fabricación mediante Salis Mundus, destruye la mesa arcana y recoge su drop real por loot table. Disuelve ese objeto (incluidos sus aspectos derivados), un Salis adicional y cuatro vidrios mediante CrucibleBlockEntity.processOne. Verifica las cantidades requeridas, usa cristal Ordo como catalizador y comprueba 50 mB de coste (900 mB después del Nitor) y 39 aspectos residuales. El mismo foco físico continúa por configuración, montaje y lanzamiento.

Diez pruebas pasan (`focus-materials-server.log`). El primer intento detectó una fixture que sobrescribía el guantelete seleccionado al equipar Salis; se conserva el objeto en inventario antes de convertir. No fue un fallo de fabricación del mod. Siguen suministrados materias primas, agua/aura/experiencia y cristales primales; no se certifica obtención natural, partida manual o conexión real. No hay cambio de producción posterior a fix40; la ampliación del banco de pruebas/documentación queda en el proyecto de trabajo para la siguiente entrega.


## Fix41: prioridad del crisol y cristal Ignis producido

El crisol selecciona ahora la receta compatible, conocida y pagable con mayor suma de aspectos; en empate conserva la primera. Es la regla de ThaumcraftCraftingManager.findMatchingCrucibleRecipe en TC6. Antes tomaba la primera compatible sin comparar el coste. La suma usa long para evitar desbordamiento. Se verifican prioridad, empate y exclusión por elegibilidad, junto al recorrido continuo real.

El jugador produce ahora también Ignis: disuelve un carbón (Ignis10/Potentia10), usa las nueve pepitas de cuarzo producidas por receta, recoge nueve cristales incluyendo Ignis y conserva dos aspectos residuales. Se consumen 450 mB de agua. Se retira la fixture de Ignis de la preparación del foco; ambos cristales requeridos proceden ahora de objetos físicos producidos por crisol. Los aspectos del foco vacío proceden de la mesa arcana convertida/recuperada, Salis y vidrio, según la comprobación posterior a fix40.

Se reubican fixtures de crisol/mesa dentro del área inicial tras un ensayo donde no se recuperó el cristal esperado; no se atribuye ese fallo a una causa de producción no demostrada. Diez pruebas requeridas pasan (`fire-supply-server.log`). Siguen suministrados recursos base, cristales primales para catalizador/construcción, agua, aura y XP; ticks/desplazamientos/escaneos son simulados. No se cierra early game ni se certifica partida manual/conexión real/paridad completa. Última función corregida: prioridad de recetas del crisol. Próximas etapas: abastecimiento base y cobertura de aspectos; ampliación de alquimia/artificio hacia infusión sin convertir registros vacíos en contenido terminado.


## Fix42: receta nativa del taumio e inicio de metalurgia

Se confirma la receta existente `thaumiumingot.json`: crisol, METALLURGY@2, lingote de hierro, Praecantatio5/Terra5, un lingote de taumio. Coincide con ConfigRecipes de TC6. La búsqueda inicial omitió el archivo; comparar el ZIP fix41 confirma que la receta ya existía con esos mismos datos. El cambio de producción del JSON se limita a normalizar espacios/saltos; la novedad funcional de esta entrega es la prueba continua de metalurgia. No se dan por completas herramientas, armaduras, propiedades especiales ni infusión por registrar este material.

La prueba continua inicia METALLURGY sin conceder etapas; disuelve Salis y piedra, verifica que intentar hierro antes de etapa2 no produce taumio ni paga agua (se disuelve como material), disuelve pedernal y fabrica latón, cobra 16 observaciones Alchemy y avanza a etapa2. Fabrica taumio con los aspectos ya presentes, cobra exactamente cinco Magic/cinco Earth y 50 mB y comprueba un lingote físico. Diez pruebas pasan (`metallurgy-server.log`). La teoría requerida para completar el capítulo aún no se incorpora a este paso, ni se certifica abastecimiento natural/partida conectada. Los recursos base, calor/agua/aura y XP permanecen suministrados; early game completo sigue pendiente de comprobación manual y auditoría del suministro natural.


## Fix43: fabricación del horno de esencia de taumio

Se añade la receta ausente `smelter_thaumium.json`, contrastada con ConfigRecipes.EssentiaSmelterThaumium: ESSENTIASMELTERTHAUMIUM, 250 vis, Ignis2, patrón BFB/IGI/III, dos placas de latón, cinco de taumio, horno básico y construcción alquímica. Un horno de taumio por operación. El bloque/procesamiento ya existía; esta entrega habilita su fabricación, no completa accesorios ni toda la etapa.

Una fixture independiente verifica bloqueo sin investigación, fallo sin consumir materiales con 249 vis, éxito con 250, consumo de todos los ingredientes/cristales y craft history. Esta prueba concede el capítulo completado a un jugador separado para aislar la receta: no demuestra aún la cadena continua de obtención de teoría/tubos y desbloqueo del capítulo. Diez pruebas pasan (`smelter-upgrade-server.log`). Se mantienen los límites del recorrido temprano y de las entregas anteriores.


## Fix44: fuelles conectados al horno de esencia

EssentiaSmelterBlockEntity cuenta hasta tres fuelles horizontales orientados hacia el horno, excluye la cara frontal y omite los vecinos alimentados por redstone, como la comprobación usada por depósitos nativos. Aplica la fórmula original de tiempo: entero de aspectos×2×(1−0,125×fuelles), mínimo1 para entrada válida. La interfaz y el procesamiento usan el mismo cálculo. La comprobación se actualiza desde los vecinos actuales, sin cachear una orientación obsoleta. No implementa todavía animación de fuelles ni SmelterAux.

Prueba viva con carbón: duración 40/35/30/25 según cero/uno/dos/tres fuelles, frontal excluido, mala orientación y redstone no cuentan. El carbón permanece hasta tick24 y se procesa en tick25 con tres; conserva los 20 aspectos entre depósito y flux. Diez pruebas pasan (`smelter-bellows-server.log`). El recorrido temprano conserva sus límites de materiales suministrados y jugador mock; no se certifica early game completo ni la etapa completa de alquimia.


## Fix45: accesorios auxiliares del horno de esencia

SmelterAux deja de ser un bloque genérico: tiene orientación horizontal y colocación solo contra caras laterales/posterior de un horno, rechazando frontal y vertical. El horno conserva su salida principal y añade hasta tres columnas de alambiques encima de accesorios orientados hacia él. Cada intervalo original de salida intenta transferir un aspecto por columna sin duplicarlo; no requiere combustible ni inventario separado del accesorio. Se añade la receta original de ImprovedSmelting: 100 vis, Aer1/Terra1, madera de greatwood, filtro, placas latón/hierro, fuelle y construcción alquímica. Estados del modelo rotados por orientación.

Diez pruebas pasan (`smelter-aux-full-server.log`): columnas principal/tres auxiliares, frontal excluido, conservación exacta y receta con puerta de investigación y consumo de costes. La prueba de receta usa un jugador independiente con investigación concedida para aislarla: no certifica la cadena natural de obtención de materiales/capítulo. Una fixture necesitaba ejecutar el tick en el intervalo real de salida; ajusta/restaura tiempo de mundo dentro de la misma llamada. La búsqueda de drop de mesa arcana se delimita al bloque destruido en lugar de incluir bloques vecinos. Pendientes animación/modelo y colisiones fieles del accesorio, pruebas de colocación manual y recorrido conectado. Early game sigue sin certificado manual.


## Fix46: fuentes de Machina y entrada continua a artificio

Palanca y pistón se añaden al mapa de aspectos derivados (17 entradas): adición original Machina5 a palanca y Machina10/Motus10 a pistón. La base de receta mantiene la fórmula75% y redondeo TC6; palanca queda Terra3/Machina5 (el Herba del palo se redondea a cero); pistón Herba6/Terra15/Perditio3/Metallum11/Potentia7 más las adiciones. No se añaden todavía todas las máquinas/recetas.

Para etiquetas modernas, el adaptador prefiere un ingrediente con aspectos explícitos o tablas de las seis maderas históricas; si no lo encuentra conserva el primer candidato. Es una adaptación de compatibilidad: TC6 empleaba el primer candidato histórico, mientras las etiquetas actuales incluyen materiales nuevos no cubiertos. No evalúa exhaustivamente todos los candidatos ni certifica aspectos de contenido moderno.

El mismo jugador del recorrido inicia UNLOCKARTIFICE tras metalurgia2, comprueba bloqueo sin descubrimiento Machine, ensambla una palanca por receta cargada, invoca ScanProgression con sus aspectos reales y completa el capítulo pagando16 observaciones Artifice. No concede manualmente ese capítulo. Sigue pendiente raycast/conexión real/obtención natural de todos los recursos. Diez pruebas requeridas pasan (`artifice-entry-server.log`). Los intentos previos ajustaron el esperado de Herba del palo conforme al redondeo original; no alteraron la fórmula para hacer pasar la prueba.


## Fix47: gafas equipables y descuento de lanzamiento

Goggles deja de ser un objeto genérico: equipamiento de cabeza nativo con armadura1, dureza1, durabilidad350, encantabilidad25 y reparación con lingote de latón, conforme ItemGoggles/ARMORMAT_SPECIAL. Usa un asset moderno de equipo con la textura heredada copiada sin alterar a la ruta humanoid. Receta original UNLOCKARTIFICE: 50 vis, sin cristales, cuero4/latón2/taumómetros2.

Nueva interfaz VisDiscountGear para equipo: el lanzamiento Toque/Fuego suma descuentos de las cuatro ranuras de armadura y aplica el límite mínimo original del10% de consumo. Por ahora el único implementado es Goggles5%; no incluye todavía baubles/otros equipos ni VisExhaust, descuento de banco arcano, HUD de aura/popup de aspectos o soporte de gafas en cascos. No considerar gafas totalmente completas.

La prueba del mismo jugador que abre artificio fabrica las gafas con recursos suministrados, verifica costes/consumo, ranura HEAD, durabilidad y reparación latón; una prueba separada verifica que llevar gafas en inventario no descuenta y equiparlas reduce el lanzamiento real mínimo de0,8 a0,76 vis. Diez pruebas pasan (`goggles-full-server.log`). Textura vestida aún requiere comprobación visual en jugador conectado. Se mantienen límites de obtención natural y sesión manual del recorrido temprano.


## Fix48: carta Trastear de artificio

Se añade CardTinker con sus18 opciones originales de objeto, una inspiración, requisito de posesión que no consume el objeto y recompensa Artificio desde2×entero(sqrt(sumaAspectos)) hasta ese valor+10. La opción se guarda y transmite; la puntuación se vuelve a calcular en activación como el original. Aparece en el mazo normal tras UNLOCKARTIFICE completado. No añade todavía ayudas de yunque/mesa encantamiento ni las otras cartas de artificio. El cálculo depende de la cobertura parcial de aspectos del port: opciones aún sin aspectos reciben rango0–10 según la regla de entrada desconocida; eso no certifica todas sus puntuaciones originales.

Se mantiene compatibilidad de cartas guardadas: kind acepta0..15 y second0..17; las cartas anteriores conservan datos/efectos. En menú se transmite el índice de objeto en los datos reservados para el requisito. Tooltip y nombre localizados en español/inglés. Diez pruebas pasan (`tinker-full-server.log`): requisito sin cobro al faltar, objeto conservado, coste1, score14..24 para pistón en50 semillas, persistencia, activación única y puerta de categoría en200 semillas. Las15 cartas anteriores y los recorridos pasan.


Fix49: cinco interfaces adoptan recursos originales TC6 y paleta de pergamino/tinta. Referencias de texturas de guanteletes reparadas para el atlas moderno. Árbol original del libro, modelos restantes y animaciones siguen pendientes de fidelidad visual.


Fix53 — revisión artística: atlas originales de mesa arcana y horno con disposición nativa TC6; madera y metal en las otras estaciones. Auditoría completa por recurso disponible y colores de aspectos. La igualdad de archivos no demuestra uso ni paridad de renderizado; árbol, criaturas, animaciones y efectos siguen pendientes.


Referencia visual aportada: playlist de Danymaddox, video KgBnmFzYOFk. Escenas inspeccionadas y diferencias frente a fix53 en REFERENCIA_VISUAL_VIDEO_TC6.md. Prioridad: mapa de nodos, lectura a doble página y cartas verticales. El video inicial se presenta como 1.10.2; conservar BETA26 como referencia funcional y contrastar revisiones posteriores.


## Presentación fix54

Mapa del Thaumonomicon y lectura en doble página incorporados, con coordenadas y recursos originales. El avance continúa validado por el servidor. Esto no cierra una etapa funcional ni acredita paridad completa; siguen pendientes cartas verticales y páginas ilustradas.


## Presentación fix55

Opciones de teoría convertidas en cartas verticales con papel original, categoría y coste de inspiración. Totales compactos por icono con detalle al pasar el cursor. Se conserva la validación del servidor y el inventario. Diseño adaptado al tamaño de interfaz actual; animación de reparto y paridad completa pendientes.


## Efecto visual fix56

El primer foco de fuego sustituye la partícula vanilla por los diez fotogramas originales de TC6 (640–649 de la rejilla 64×64), duración de diez ticks, alfa 0,7, gravedad −0,2 y frenado 0,75. Recursos binarios conservados. Registro y geometría nativa verificados; mezcla aditiva original, vista integrada en mundo y efectos del manipulador pendientes. Sin cambios en costes, daño o desbloqueos.


## Efecto visual fix57

Espuma del crisol: sprites originales 64–66, duración de 4–6 ticks, tono (0,5; 0,5; 0,7), gravedad y tamaño de crucibleFroth. Solo emite con agua y calor >150, sobre la superficie del modelo de agua actual. Frecuencia adaptada a una emisión cada cinco ticks; azar visual separado del azar de recetas/derrames. Burbujeo por aspecto, rebose, mezcla aditiva original y revisión dentro del mundo siguen pendientes.


## Navegación fix58

Selector directo de categorías con iconos y marcos del navegador TC6. Solo Fundamentos y categorías cuyo UNLOCK esté completo aparecen, como en la referencia; rueda y clic del nombre también recorren únicamente esas categorías. El tooltip muestra investigación completada por el jugador, no porcentaje de implementación del port. Disposición horizontal adaptada; marcadores de novedades y disposición lateral exacta pendientes.


### Fix59 — vista de iconos sin mundo

El mapa extrae modelos nativos de objetos sin exigir un mundo abierto; la verificación prepara sus componentes de prueba. No altera la progresión ni acredita paridad de modelos en mano.

Verificación inicial: preparación nativa de veinte ticks incorporada tras reproducir búsquedas intermitentes sin objetos; cuatro ejecuciones completas posteriores aprobadas. Causa exacta pendiente de confirmación (INCIDENCIA_ENTIDADES_EARLY_GAME.md).


### Fix60 — conexiones originales del mapa

Conexiones con tramos, esquinas curvas y flechas del atlas original, incluida orientación REVERSE; progreso del servidor sin cambios. Enlaces entre hermanos y animaciones pendientes. Tres fotogramas de la guía de infusión documentados; esa etapa no se considera completa.


### Fix61 — relaciones secundarias del mapa

Enlaces siblings sin flecha, sin duplicar relaciones de requisito; tonos originales y coloreado según la etapa indicada por @. Instantánea gráfica ficticia de alquimia para revisar conexiones. No cambia las reglas de progreso ni completa la etapa de infusión.


### Fix62 — lectura y límites de página

Párrafos BR y saltos PAGE originales conservados; páginas vacías explícitas mantienen alineación. Descriptores IMG retirados de la prosa; ilustraciones y ornamentos LINE/DIV completos pendientes. No cambia progresión.


### Fix63 — ilustraciones del libro

Ilustraciones IMG, recortes UV originales, escala proporcional y ornamentos LINE/DIV incorporados a la paginación. Captura de la segunda etapa de investigación teórica; recetas ilustradas e interacción todavía pendientes. Sin cambios de progreso.


### Referencia visual de golemancia

Tres fotogramas revisados (09:17, 23:12, 37:08): constructor de piedra y metal, lacre rojo de sellos y filtro Blacklist. Constructor, pantallas de sellos, sincronización, IA y tareas continúan pendientes. El visor externo de recetas se excluye de la referencia artística del mod. Detalle en REFERENCIA_VISUAL_VIDEO_TC6.md.


### Fix64 — destellos del manipulador

Destellos centrales originales durante trabajo activo, fotogramas 448–456 y caducidad breve; densidad adaptada. Costes/progreso sin cambios. Haces, cristales orbitando, mezcla aditiva exacta y vista completa en mundo pendientes.


### Referencia de betas 20–24

Tres fotogramas de la revisión 1.12.2 documentados; dispositivos dorados/morados descritos sin atribuir identidad funcional no confirmada. Catálogo visual actualizado con las escenas realmente inspeccionadas de infusión y golemancia. Sin cambios de implementación ni marcas verdes nuevas.

## Fix65: carta Programación de Golemancia

Carta Programación de TC6: concede exactamente 25 puntos de Golemancia y cuesta 1 inspiración. Al elegirla consume 1 papel adicional y 2 usos de tinta en total: el uso propio de la carta y el uso común de activación, conforme CardScripting y ContainerResearchTable originales.

Disponible tras completar UNLOCKGOLEMANCY. Guarda la carta elegida con la sesión, mantiene propietario y revisión contra clics repetidos. Si falta papel o quedan menos de dos usos de tinta, rechaza sin modificar recursos, inspiración ni puntos. Texto español e inglés con costes explícitos, conservando la presentación de cartas y la fuente serif aprobada.

Comprobación del servidor: costes reales de la mesa, recursos insuficientes, guardado, recompensa, repetición y desbloqueo. El catálogo pasa a 17 de las 35 cartas inventariadas; no equivale a completar Golemancia ni el early game. Las cartas restantes, recetas ilustradas completas, haces del manipulador, gólems e infusión integrada siguen pendientes.

## Fix66: carta Curiosidad y ocho variantes

Carta Curiosidad de TC6, según CardCurio: consume una unidad del curio elegido y 1 inspiración. Otorga 5 puntos de Fundamentos y 5 en una categoría aleatoria. Las variantes Básica, Arcana, Preservada, Antigua, Sobrenatural, Conocimiento y Retorcida conceden además 25–35 puntos en Fundamentos, Auromancia, Alquimia, Golemancia, Sobrenatural, Infusión y Artificio, respectivamente. Ritos concede 15–20 de Sobrenatural y 10–15 de Auromancia. Dos sorteos independientes pueden conceder cero, uno o dos robos adicionales.

Se selecciona entre las pilas de curios presentes en los 36 espacios principales del inventario, igual que el original; la cantidad de una pila no aumenta su peso. El port representa las variantes como registros de objetos separados. Se guarda la variante al extraer la carta y se consume únicamente al activarla. Si se retira el objeto del inventario, se rechaza sin puntos ni coste de inspiración. Una activación no puede repetirse.

Se conservan cartas verticales, fuente serif y reglas del servidor. La descripción española e inglesa especifica consumo y recompensas. Esta incorporación usa los objetos ya registrados, pero no certifica su obtención natural, botín original ni otros usos pendientes de curios.

El catálogo incorpora 18 de las 35 cartas inventariadas. No certifica Golemancia, Infusión ni early game completos.

Validación fix66: diez pruebas del servidor y cliente nativo aprobados; carta Curiosidad inspeccionada en captura. Son siete variantes originales y el registro básico adicional del port, que usa la rama por defecto de CardCurio.

## Fix67: carta Revelación de Sobrenatural

Carta Revelación de TC6, contrastada con CardRevelation y la conversión de TileResearchTable: cuesta 1 inspiración, concede 30 puntos de Sobrenatural y otros 5–10 en una categoría aleatoria. Añade 5 de distorsión temporal y 1 normal; no añade distorsión permanente. Aumenta en uno penaltyStart, lo que permite que una categoría adicional de la teoría final evite la reducción de dos tercios.

La carta aparece después de completar UNLOCKELDRITCH y respeta categorías rechazadas. Los puntos aleatorios se sortean al activar, como en TC6, mientras que la elección y el umbral de conversión se guardan con la sesión. La revisión y el vaciado de elecciones impiden aplicarla dos veces.

Los valores de distorsión saturan en el máximo entero para no desbordarse. La interfaz en español e inglés describe el coste, la distorsión y el efecto sobre la conversión. Se conservan la tipografía serif y las cartas verticales.

Esta carta usa los contadores de distorsión existentes; no implementa los eventos originales de distorsión ni certifica la etapa Sobrenatural. El catálogo pasa a 19 de las 35 cartas inventariadas. Siguen pendientes early game completo, infusión integrada, gólems y los efectos visuales indicados en la auditoría.

Validación fix67: producción, diez pruebas del servidor y cliente nativo aprobados. Carta Revelación inspeccionada en captura, con icono de Sobrenatural y fuente serif.

## Fix68: cartas Verdad y Comprensión

Cartas Verdad y Comprensión de TC6, contrastadas con CardTruth y CardRealization. Ambas cuestan 1 inspiración y requieren completar UNLOCKELDRITCH para aparecer en el mazo normal; respetan las categorías rechazadas.

Verdad concede 10–25 puntos de Sobrenatural, 1 robo adicional y 3 de distorsión temporal. Comprensión concede 15 puntos de Sobrenatural y dos premios independientes de 5–10 en categorías aleatorias, que pueden coincidir entre sí o con Sobrenatural. Añade 5 de distorsión temporal y tiene un 50 % de probabilidad de añadir 1 de distorsión normal.

Ninguna cambia la distorsión permanente ni el umbral de conversión de la teoría. Las recompensas se sortean al activar, como en el original. La carta elegida, puntos, inspiración y robos adicionales permanecen al guardar la sesión; la revisión y el vaciado de elecciones impiden repetir la activación. La distorsión satura en el máximo entero para evitar desbordamientos.

Se conservan las cartas verticales, icono original de Sobrenatural y fuente serif aprobada. Textos español e inglés describen efectos y riesgos concretos.

El catálogo pasa a 21 de las 35 cartas inventariadas. Las ayudas y cartas restantes, los eventos originales de distorsión, la infusión integrada y los gólems siguen pendientes. Este cambio no certifica etapa Sobrenatural ni early game completos.

Validación fix68: producción, diez pruebas del servidor y cliente nativo aprobados. Captura final inspeccionada; títulos ajustados sin cortar Comprensión a mitad de palabra.

## Revisión visual posterior a fix68: beta 25 y crisol

Fotogramas 03:10, 07:56 y 12:42 inspeccionados y documentados en REFERENCIA_VISUAL_VIDEO_TC6.md. Diferencia confirmada por código: tinte de agua de bioma frente al tinte por concentración de aspectos del original. Pendiente sincronizar concentración y aplicar color original; sin cambio funcional ni nueva versión del JAR.

## Fix69: agua del crisol según concentración

El agua del crisol vuelve a usar los multiplicadores RGB de TileCrucibleRenderer.renderFluid de TC6 BETA26. La concentración depende de la suma de aspectos: recolor=n/500; para n>0 se transforma a 0.5+recolor/2 y se limita a 1. RGB: 1-recolor/3, 1-recolor, 1-recolor/2. Se conserva water_still animado y los cuatro niveles de agua existentes; se elimina la dependencia del color del bioma.

El servidor transmite el contenido del crisol al cargar el bloque y al cambiar agua o aspectos. También notifica las pérdidas por rebose. El cliente actualiza la malla después de recibir el contenido para evitar mantener un tinte anterior cuando el nivel de agua no cambia. Los cambios exclusivos de calor no envían una actualización visual cada tick.

No cambia recetas, pagos, cantidades, disolución, calor ni flux. Se verifican datos de actualización antes y después de fabricar Nitor y la paleta original de 0, 1, 250 y 500 aspectos; saturación y límites de los contadores incluidos. La captura de paleta representa los multiplicadores RGB, no una certificación de la apariencia completa del agua sobre el mundo.

Referencia visual: beta 25, fotograma 12:42, junto al contraste de código BETA26 documentado en REFERENCIA_VISUAL_VIDEO_TC6.md. Solo se inspeccionaron los fotogramas allí enumerados; no se afirma una revisión completa del video.

Vista integrada en mundo, mezcla/transparencia exacta del renderer original, burbujas coloreadas por aspecto y todos los efectos pendientes de la auditoría siguen sin certificarse. El contenido funcional conserva las 21 de 35 cartas implementadas de fix68; no certifica early game completo.

Validación fix69: producción, diez pruebas del servidor y cliente nativo aprobados. Captura de paleta 0/1/250/500 aspectos inspeccionada; datos de actualización antes/después de Nitor verificados.

## Fix70: burbujas del crisol por aspecto

Burbujas del crisol coloreadas según un aspecto presente en su mezcla. Contraste con TileCrucible.drawEffects y FXDispatcher.crucibleBubble de TC6 BETA26: probabilidad de 1/6 por tick de cliente, selección uniforme entre tipos de aspecto presentes, posición sobre el agua y dentro de la cuadrícula original de 32 unidades.

Proveedor nativo con vida de 15–24 ticks, tamaño base 0.3–0.6, gravedad -0.001, fricción 0.98 y movimiento gaussiano de 0.002 añadido cada tick. Usa el atlas original: fotograma 64 durante la burbuja y secuencia final 66, 66, 65, conforme la indexación inversa de finalFrames en FXGeneric.

El color se conserva como opción ARGB con codecs JSON y de red. Se emite en el cliente sobre el contenido sincronizado de fix69. No añade paquetes por burbuja ni consume aleatoriedad del servidor. Requiere agua y al menos un aspecto; no exige calor de ebullición, igual que el original. La altura se adapta a los cuatro niveles visibles de agua ya existentes.

No modifica recetas, concentración, disolución, costes, calor ni flux. Se conserva la espuma gris de ebullición como efecto separado. La capa translúcida moderna y brillo máximo son adaptaciones; iluminación y mezcla exactas del original no están certificadas.

Vista integrada en mundo, rebose por los laterales, efectos de disolución/cristalización y el resto indicado en la auditoría siguen pendientes. No se considera el sistema completo de efectos terminado ni se certifica early game completo.

Validación fix70: producción, diez pruebas del servidor y cliente nativo aprobados. Los 37 colores pasaron los codecs y el proveedor; captura de seis aspectos primarios inspeccionada.


# Estado del port — fix71

Rebose morado del crisol según TileCrucible.drawEffects y FXDispatcher.crucibleFrothDown de TC6 BETA26. Se activa con agua, calor superior a 150 y más de 500 aspectos; emite ocho partículas por tick de cliente desde los cuatro bordes. Conserva las burbujas por aspecto como efecto independiente.

Proveedor con vida de 12–23 ticks, color RGB 0.25/0/0.75, alfa 0.8, gravedad 0.05, fricción 0.98 y movimiento gaussiano de 0.001 por eje. Atlas original: fotograma 73 seguido de 66 y 65. La colisión se habilita cuando existe nivel de cliente; la vista de comprobación sin mundo la desactiva.

No altera recetas, consumo, calor ni flux. La capa translúcida moderna y brillo máximo son adaptaciones. Colisión real, iluminación, mezcla original y composición completa en mundo siguen pendientes de comprobación.

Validación: compilación de producción aprobada y diez pruebas requeridas del servidor aprobadas. Sincronización comprobada con 501 aspectos calientes, 500 tras el derrame original, calor 150 y crisol vacío. Cliente nativo: proveedor, geometría, duración y caducidad aprobados. Captura de los tres fotogramas inspeccionada. No certifica paridad visual ni early game completo.


# Estado del port — fix72

Corregida la sincronización del umbral de ebullición del crisol. El servidor notifica al cliente cuando pasa de calor 150 a 151 o de 151 a 150, aunque no cambien agua ni aspectos. Evita que los efectos condicionados por calor, incluido el rebose de fix71, conserven un estado de calor anterior.

Los ticks de calor que no cruzan el umbral continúan guardando el estado sin enviar actualizaciones visuales cada tick. Recetas, velocidad de calentamiento, consumo y derrames conservan sus reglas actuales.

Validación: compilación de producción aprobada; diez pruebas requeridas del servidor aprobadas. Prueba con ticks reales de la entidad y cambio de fuente de calor: 151→150→151, datos del paquete conservados y 30 aspectos intactos. No se instrumentó la recepción de paquetes en un cliente conectado. Presentación sin cambios respecto a la captura inspeccionada en fix71; no se generaron nuevas capturas ni se repitió la prueba de pantallas en esta revisión.

Pendientes: composición de efectos y colisiones en mundo, mezcla e iluminación exactas, y demás tareas de la auditoría. No certifica paridad ni early game completo.


## Preparación de páginas de recetas — 4 de octubre de 2026

Auditoría reproducible tools/audit_book_recipes.py: 253 referencias de etapas/addenda, 203 identificadores únicos, 20 coincidencias exactas con datapack, 8 candidatas por minúsculas y 175 sin resolver. Los grupos, planos, recetas ilustrativas y aliases requieren revisión individual. Véase AUDITORIA_RECETAS_LIBRO_TC6.md/json. No son 175 recetas necesariamente ausentes ni se certifican las 28 coincidencias como equivalentes. Próximo paso: páginas iniciales y aliases comprobados, respetando desbloqueos. Sin nueva entrega binaria; fix72 sigue siendo la última compilada.


# Estado del port — fix73

Primeras páginas ilustradas de fabricación arcana en el Thaumonomicon. Botón Recetas dentro de una investigación accesible con una receta arcana de cuadrícula disponible en su etapa actual. Muestra ingredientes, resultado, seis tipos de cristal cuando corresponda y coste vis; navegación entre recetas y regreso al libro. Fuente serif aprobada conservada.

processResources copia las recetas del datapack a recursos del cliente: la página utiliza exactamente el JSON enviado en la compilación, sin catálogo duplicado mantenido manualmente. Resolución por identificador exacto; no convierte automáticamente los aliases del original. Referencias ausentes, recetas ilustrativas, grupos y tipos todavía no compatibles no producen páginas ficticias.

Atlas gui_researchbook_overlay.png original: cuadrícula UV112,15 y marco de resultado UV20,3, escala2, disposición de ingredientes/resultados/cristales/coste según GuiResearchPage.drawArcaneCraftingPage. Contenedor y controles adaptados a la pantalla actual. Solo ingredientes simples de las recetas migradas actuales; rotación de ingredientes, grupos, addenda desbloqueadas, fabricación normal, crisol, infusión y planos pendientes.

Compilación de producción y diez pruebas requeridas del servidor aprobadas. Prueba de cliente verifica etapa actual, omisión de referencias ausentes, costes del taumómetro de20 vis/seis cristales y regreso/reapertura del libro. Cliente final aprobado y captura inspeccionada tras ajustar la altura al tamaño de interfaz de427x240; título, coste y controles visibles. No certifica igualdad de todo el libro, servidor remoto con datapack modificado ni paridad funcional. La página es de consulta, no fabrica ni altera investigación.


# Estado del port — fix74

Añadidas páginas de fabricación normal con cuadrícula al Thaumonomicon, junto a las arcanas de fix73. Usa la cuadrícula propia del atlas original gui_researchbook_overlay.png (UV60,15;51x52, escala2), resultado encima y cantidades nativas. Las páginas normales no dibujan costes de vis ni cristales.

Resolución exacta contra el JSON de recetas enviado en la compilación. Ingredientes simples comprobados contra el registro: referencias ausentes, tags todavía sin resolver o patrones fuera de3x3 se omiten completos. Evita presentar casillas vacías como una receta terminada. Acceso continúa limitado a la etapa actual de una investigación accesible y conserva el regreso al libro.

No implementa todavía ingredientes rotatorios por tag, fabricación sin forma, aliases, grupos, addenda, crisol, infusión ni planos. No certifica toda la lectura de recetas ni equivalencia de los datapacks de servidores externos.

Compilación de producción y diez pruebas requeridas del servidor aprobadas. Cliente nativo aprobado: placa de hierro con patrón BBB y resultado de3 unidades, omisión de referencias ausentes/tags sin resolver, regresión de página arcana y navegación. Captura final inspeccionada: título, cuadrícula, cantidad del resultado y controles visibles.


# Estado del port — fix75

Añadidas páginas de alquimia en crisol al Thaumonomicon para referencias exactas migradas con catalizador simple y aspectos conocidos. Muestra catalizador, resultado, iconos/cantidades de aspectos y tooltips; conserva navegación de recetas y regreso al libro. La consulta no fabrica ni altera el progreso del servidor.

Diseño de GuiResearchPage.drawCruciblePage: atlas original gui_researchbook_overlay.png con segmentos UV0,3/56x17, UV0,20/56x48 y flecha UV100,84/11x13, escalados por2. Posiciones del catalizador, resultado y aspectos derivadas del original; contenedor y fuente serif adaptados a la interfaz actual. Aspectos ordenados por identificador latino y distribuidos en filas de3. Se aceptan hasta9 tipos de aspectos para no desbordar esta página.

Fuente de datos: JSON exacto del datapack incluido en la compilación, copiado a recursos del cliente en processResources. Catalizadores no resolubles, grupos, aliases y recetas no compatibles quedan fuera. Infusión, planos, ingredientes por tag, grupos y addenda siguen pendientes. No certifica todo el libro ni datapacks modificados de servidores externos.

Compilación y diez pruebas requeridas del servidor aprobadas. Cliente final aprobado: Nitor con polvo de piedra luminosa,10 Ignis/10 Lux/10 Potentia, omisión de grupos sin resolver, regresión de recetas normal/arcana y navegación. Captura final inspeccionada, incluidos tintes originales de los aspectos. La vista sin mundo incorpora un fixture del catalizador; sus componentes se cargan normalmente al entrar en un mundo.


# Estado del port — fix76

Conectado el nombre legado thaumcraft:BrickArcane a la receta migrada thaumcraft:brickarcane dentro del libro. Comparado con ConfigRecipes líneas452–453: patrón KK/KK, cuatro piedras arcanas y resultado de cuatro ladrillos arcanos. Está disponible entre las recetas de FIRSTSTEPS etapa3, sujeto a la accesibilidad de esa entrada.

Alias explícito único; no convierte todos los nombres a minúsculas. StoneArcane, grupos inkwell, ingredientes por tag y demás referencias pendientes conservan su estado de revisión. No modifica recetas ni reglas del servidor.

Auditoría de enlaces actualizada con el alias contrastado. El empaquetado evita copiar el reporte de una prueba de servidor anterior cuando no hay ejecución de servidor en esta revisión.

Compilación de producción aprobada. Cliente aprobado: alias explícito, acceso a recetas de FIRSTSTEPS etapa3, patrón/cantidad conservados y referencias no verificadas omitidas. Captura final inspeccionada con cuatro piedras y cuatro ladrillos visibles; regresión de recetas arcana/normal/crisol aprobada. No se repitieron los diez tests de servidor de fix75 porque el cambio afecta exclusivamente a la consulta de recetas del cliente y la documentación; esos tests no se presentan como ejecutados en fix76.


# Estado del port — fix77

Ingredientes por etiquetas del registro del cliente para recetas de cuadrícula del Thaumonomicon. Rotación cada segundo con desplazamiento por ranura, como InventoryUtils.cycleItemStack del original. Usa las etiquetas recibidas del servidor, sin inventar variantes ni mantener un catálogo alternativo. Etiquetas vacías o sin cargar omiten la página hasta que sus ingredientes puedan resolverse.

Producción compilada. Prueba de cliente aprobada: recarga real de etiquetas del registro con fixture roble/abeto, ciclo y vuelta cada segundo, etiqueta inexistente rechazada y receta tablewood resuelta. Captura final inspeccionada. Regresiones de lectura y recetas previas aprobadas. No se repitieron pruebas de servidor: el cambio es exclusivamente de consulta del cliente.

Cerrado el cambio que estaba en curso al recibir hola. Automatización pausada. El nuevo pedido de interfaces1:1 requiere una revisión posterior; fix77 todavía conserva las composiciones adaptadas previas.


## Fix78 — composición original del libro y recetas

Restaurados proporciones del libro, ornamentos, flechas del atlas, papel cuadrado de recetas y fuente pixelada original. Producción y cliente aprobados; capturas inspeccionadas. Pendientes detallados en docs/INTERFACES_1_A_1_TC6.md; no se certifica paridad global. Automatización permanece pausada tras la señal hola.


## Fix79 — distribución del mapa y Taumómetro

Mapa a pantalla completa con categorías laterales, marco en teselas y botón de búsqueda con atlas correcto gui_research_browser.png. Recuperación del modelo OBJ del Taumómetro, alcance de entidades9, sonido privado y lectura del aura del servidor cada20 ticks con indicador original. Compilación y diez tests del servidor aprobados. No certificar paridad total: pendientes de efectos/objetivos especiales y comparación visual completa en docs/AUDITORIA_TAUMOMETRO_TC6.md.


## Fix80 — runas de escaneo

Animación FXBlockRunes portadas al uso local del Taumómetro: diez runas, dieciséis glifos, planos fijos, color/desvanecimiento/gravedad originales. Prueba del cliente aprobada: proveedor, geometría, vida45/90 y expiración. No se modifican recompensas del servidor; últimos diez tests de servidor en fix79. Resaltado pasivo, aspecto sobre objetivo y escaneos especiales siguen pendientes.


## Fix81 — investigación y manipulador originales

Restaurados fondos/ranuras de investigación255x255 y manipulador231x231 con inventario lateral. Controles de atlas nativo: crear/completar/descartar, flechas de ajustes, confirmar, ayudas por iconos y mazo clicable que usa tercera carta automáticamente con bonus. Conservados índices/transacciones. Diez tests del servidor aprobados. Editor completo y animaciones de cartas/requisitos siguen pendientes; no certificar1:1 global.

## Fix89 — materiales iniciales

Derivación activa de23 objetos: añadidos libro, botella, cubo, cuenco, cofre y horno. Diez tests del servidor aprobados con fórmulas y cantidades reales. Sin cierre prematuro de early game; abastecimiento natural y recorrido conectado pendientes.

## Fix90 — cristales en paredes y techo

Generación del feature revisada para las seis orientaciones, con prueba real de colocación y loot; diez tests aprobados. La distribución natural completa y el recorrido conectado siguen pendientes.

## Fix91 — investigación celeste

Obtención de notas celestiales conectada con reglas originales y trece variantes tipadas/modelos. Diez tests de servidor y regresión de cliente aprobados. Pendiente integración con CardCelestial; no marcar el sistema íntegro ni early game completo.

## Fix92 — carta Celestial

Notas del cielo usadas por la carta Celestial con variantes persistentes, consumo atómico y efectos originales. Lista actualizada; diez pruebas de servidor aprobadas. No declarar early game terminado.
