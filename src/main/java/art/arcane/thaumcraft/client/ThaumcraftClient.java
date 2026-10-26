package art.arcane.thaumcraft.client;

import art.arcane.thaumcraft.Thaumcraft;
import art.arcane.thaumcraft.network.ResearchSnapshotPayload;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.network.event.RegisterClientPayloadHandlersEvent;

@Mod(value = Thaumcraft.MOD_ID, dist = Dist.CLIENT)
public final class ThaumcraftClient {
    public ThaumcraftClient(IEventBus bus) {
        DebugScene.register();
        AspectTooltipClient.register(bus);
        bus.addListener((net.neoforged.neoforge.client.event.RegisterFluidModelsEvent e) -> {
            // FluidDeath: animatedglow tinted 0xF0440099; FluidPure: water textures tinted 0x77FFCCAA.
            var glow = new net.minecraft.client.resources.model.sprite.Material(net.minecraft.resources.Identifier.fromNamespaceAndPath("thaumcraft", "block/animatedglow"));
            e.register(new net.minecraft.client.renderer.block.FluidModel.Unbaked(glow, glow, null, net.neoforged.neoforge.client.fluid.FluidTintSources.constant(0xF0440099)),
                    art.arcane.thaumcraft.registry.ModFluids.DEATH, art.arcane.thaumcraft.registry.ModFluids.DEATH_FLOWING);
            e.register(new net.minecraft.client.renderer.block.FluidModel.Unbaked(
                    new net.minecraft.client.resources.model.sprite.Material(net.minecraft.resources.Identifier.withDefaultNamespace("block/water_still")),
                    new net.minecraft.client.resources.model.sprite.Material(net.minecraft.resources.Identifier.withDefaultNamespace("block/water_flow")),
                    new net.minecraft.client.resources.model.sprite.Material(net.minecraft.resources.Identifier.withDefaultNamespace("block/water_overlay")),
                    net.neoforged.neoforge.client.fluid.FluidTintSources.constant(0x77FFCCAA)),
                    art.arcane.thaumcraft.registry.ModFluids.PURE, art.arcane.thaumcraft.registry.ModFluids.PURE_FLOWING);
        });
        bus.addListener(ThaumcraftClient::registerPayloads);
        bus.addListener((net.neoforged.neoforge.client.event.RegisterPictureInPictureRenderersEvent e) -> e.register(art.arcane.thaumcraft.client.book.BlueprintRenderState.class, art.arcane.thaumcraft.client.book.BlueprintRenderer::new));
        bus.addListener(art.arcane.thaumcraft.client.fx.TcParticleLayers::register);
        bus.addListener(art.arcane.thaumcraft.client.fx.beam.TcBeams::register);
        bus.addListener(art.arcane.thaumcraft.golems.client.GolemRenderer::registerPipelines);
        bus.addListener((net.neoforged.neoforge.client.event.RegisterRenderPipelinesEvent e) -> e.registerPipeline(art.arcane.thaumcraft.client.book.ResearchBrowserScreen.GUI_ADDITIVE));
        bus.addListener(art.arcane.thaumcraft.client.fx.beam.TcBeams::registerGroup);
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener((net.neoforged.neoforge.client.event.ClientTickEvent.Post event)->art.arcane.thaumcraft.client.fx.FXDispatcher.tick());
        bus.addListener((net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent event)->event.registerItem(new ThaumometerChannelPose(),art.arcane.thaumcraft.ModItems.THAUMOMETER.get()));
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener((net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent.LoggingOut event)->art.arcane.thaumcraft.scan.ScanRegistry.clearDerived());
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener(ThaumometerHud::render);
        BaubleClient.register();
        CultistArmorClient.register(bus);
        FortressArmorClient.register(bus);
        if (net.neoforged.fml.ModList.get().isLoaded("curios")) art.arcane.thaumcraft.client.CuriosRenderers.register(bus);
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener(net.neoforged.bus.api.EventPriority.HIGHEST, (net.neoforged.neoforge.client.event.RecipesReceivedEvent e) -> ClientRecipes.set(e.getRecipeMap()));
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener((net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent.LoggingOut e) -> ClientRecipes.set(net.minecraft.world.item.crafting.RecipeMap.EMPTY));
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener((net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent.LoggingOut event)->ThaumometerHud.clear());
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener((net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent.LoggingOut event)->ClientResearch.clear());
        bus.addListener(ThaumcraftClient::registerMenus);
        bus.addListener(ThaumcraftClient::registerEntities);
        CasterClient.register(bus);
        HarnessClient.register(bus);
        WarpClient.register();
        GogglesTextClient.register();
        WardClient.register();
        EntityAspectTagsClient.register();
        art.arcane.thaumcraft.golems.client.SealRenderClient.register();
        bus.addListener(art.arcane.thaumcraft.client.render.entity.TcModelLayers::register);
        bus.addListener(ThaumcraftClient::registerParticles);
        bus.addListener(ThaumcraftClient::registerBlockColors);
        bus.addListener((net.neoforged.neoforge.client.event.RegisterColorHandlersEvent.ItemTintSources e) -> e.register(net.minecraft.resources.Identifier.fromNamespaceAndPath("thaumcraft", "golem_material"), art.arcane.thaumcraft.golems.client.GolemMaterialTint.MAP_CODEC));
        art.arcane.thaumcraft.client.render.entity.TaintedLayer.register(bus);
    }
    private static void registerEntities(net.neoforged.neoforge.client.event.EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(art.arcane.thaumcraft.registry.ModEntities.ALUMENTUM.get(),net.minecraft.client.renderer.entity.ThrownItemRenderer::new);
        event.registerEntityRenderer(art.arcane.thaumcraft.registry.ModEntities.CAUSALITY_COLLAPSER.get(),net.minecraft.client.renderer.entity.ThrownItemRenderer::new);
        event.registerEntityRenderer(art.arcane.thaumcraft.registry.ModEntities.SPECIAL_ITEM.get(),net.minecraft.client.renderer.entity.ItemEntityRenderer::new);
        event.registerEntityRenderer(art.arcane.thaumcraft.registry.ModEntities.FOLLOWING_ITEM.get(),net.minecraft.client.renderer.entity.ItemEntityRenderer::new);
        event.registerEntityRenderer(art.arcane.thaumcraft.registry.ModEntities.FOCUS_PROJECTILE.get(),net.minecraft.client.renderer.entity.NoopRenderer::new);
        event.registerEntityRenderer(art.arcane.thaumcraft.registry.ModEntities.FOCUS_CLOUD.get(),net.minecraft.client.renderer.entity.NoopRenderer::new);
        event.registerEntityRenderer(art.arcane.thaumcraft.registry.ModEntities.FOCUS_MINE.get(),art.arcane.thaumcraft.client.render.entity.FocusMineRenderer::new);
        event.registerEntityRenderer(art.arcane.thaumcraft.registry.ModEntities.SPELL_BAT.get(),art.arcane.thaumcraft.client.render.entity.SpellBatRenderer::new);
        event.registerEntityRenderer(art.arcane.thaumcraft.registry.ModEntities.WISP.get(),art.arcane.thaumcraft.client.render.entity.WispRenderer::new);
        event.registerEntityRenderer(art.arcane.thaumcraft.registry.ModEntities.TAINT_SEED.get(),art.arcane.thaumcraft.client.render.entity.TaintSeedRenderer::new);
        event.registerEntityRenderer(art.arcane.thaumcraft.registry.ModEntities.TAINT_SEED_PRIME.get(),art.arcane.thaumcraft.client.render.entity.TaintSeedRenderer::new);
        event.registerEntityRenderer(art.arcane.thaumcraft.registry.ModEntities.FIRE_BAT.get(),art.arcane.thaumcraft.client.render.entity.FireBatRenderer::new);
        event.registerEntityRenderer(art.arcane.thaumcraft.registry.ModEntities.BOTTLE_TAINT.get(),net.minecraft.client.renderer.entity.ThrownItemRenderer::new);
        event.registerEntityRenderer(art.arcane.thaumcraft.registry.ModEntities.GRAPPLE.get(),art.arcane.thaumcraft.client.render.entity.GrappleRenderer::new);
        event.registerEntityRenderer(art.arcane.thaumcraft.registry.ModEntities.THAUMIC_SLIME.get(),art.arcane.thaumcraft.client.render.entity.ThaumicSlimeRenderer::new);
        event.registerEntityRenderer(art.arcane.thaumcraft.registry.ModEntities.FLUX_RIFT.get(),art.arcane.thaumcraft.client.render.entity.FluxRiftRenderer::new);
        event.registerEntityRenderer(art.arcane.thaumcraft.registry.ModEntities.MIND_SPIDER.get(),art.arcane.thaumcraft.client.render.entity.MindSpiderRenderer::new);
        event.registerEntityRenderer(art.arcane.thaumcraft.registry.ModEntities.ELDRITCH_GUARDIAN.get(),art.arcane.thaumcraft.client.render.entity.EldritchGuardianRenderer::new);
        event.registerEntityRenderer(art.arcane.thaumcraft.registry.ModEntities.CULTIST_KNIGHT.get(),art.arcane.thaumcraft.client.render.entity.CultistRenderer::new);
        event.registerEntityRenderer(art.arcane.thaumcraft.registry.ModEntities.CULTIST_CLERIC.get(),art.arcane.thaumcraft.client.render.entity.CultistRenderer::new);
        event.registerEntityRenderer(art.arcane.thaumcraft.registry.ModEntities.CULTIST_PORTAL_LESSER.get(),art.arcane.thaumcraft.client.render.entity.CultistPortalLesserRenderer::new);
        event.registerEntityRenderer(art.arcane.thaumcraft.registry.ModEntities.ELDRITCH_ORB.get(),art.arcane.thaumcraft.client.render.entity.EldritchOrbRenderer::new);
        event.registerEntityRenderer(art.arcane.thaumcraft.registry.ModEntities.GOLEM_ORB.get(),art.arcane.thaumcraft.client.render.entity.ElectricOrbRenderer::new);
        event.registerEntityRenderer(art.arcane.thaumcraft.registry.ModEntities.TRAVELLING_TRUNK.get(),art.arcane.thaumcraft.golems.client.TravellingTrunkRenderer::new);
        event.registerEntityRenderer(art.arcane.thaumcraft.registry.ModEntities.GOLEM.get(),art.arcane.thaumcraft.golems.client.GolemRenderer::new);
        event.registerEntityRenderer(art.arcane.thaumcraft.registry.ModEntities.TURRET_BASIC.get(),art.arcane.thaumcraft.client.render.entity.TurretRenderers.Basic::new);
        event.registerEntityRenderer(art.arcane.thaumcraft.registry.ModEntities.TURRET_ADVANCED.get(),art.arcane.thaumcraft.client.render.entity.TurretRenderers.Advanced::new);
        event.registerEntityRenderer(art.arcane.thaumcraft.registry.ModEntities.ARCANE_BORE.get(),art.arcane.thaumcraft.client.render.entity.TurretRenderers.Bore::new);
        event.registerEntityRenderer(art.arcane.thaumcraft.registry.ModEntities.ELDRITCH_AUTOCASTER.get(),art.arcane.thaumcraft.client.render.entity.AutocasterRenderer::new);
        event.registerEntityRenderer(art.arcane.thaumcraft.registry.ModEntities.GOLEM_DART.get(),art.arcane.thaumcraft.golems.client.GolemDartRenderer::new);
        event.registerBlockEntityRenderer(art.arcane.thaumcraft.registry.ModBlockEntities.GOLEM_BUILDER.get(),art.arcane.thaumcraft.golems.client.GolemBuilderRenderer::new);
        event.registerEntityRenderer(art.arcane.thaumcraft.registry.ModEntities.TAINT_CRAWLER.get(),art.arcane.thaumcraft.client.render.entity.TaintCrawlerRenderer::new);
        event.registerEntityRenderer(art.arcane.thaumcraft.registry.ModEntities.TAINTACLE.get(),art.arcane.thaumcraft.client.render.entity.TaintacleRenderer::big);
        event.registerEntityRenderer(art.arcane.thaumcraft.registry.ModEntities.TAINTACLE_SMALL.get(),art.arcane.thaumcraft.client.render.entity.TaintacleRenderer::small);
        event.registerEntityRenderer(art.arcane.thaumcraft.registry.ModEntities.TAINT_SWARM.get(),net.minecraft.client.renderer.entity.NoopRenderer::new);
        event.registerEntityRenderer(art.arcane.thaumcraft.registry.ModEntities.BRAINY_ZOMBIE.get(),art.arcane.thaumcraft.client.render.entity.BrainyZombieRenderer::new);
        event.registerEntityRenderer(art.arcane.thaumcraft.registry.ModEntities.GIANT_BRAINY_ZOMBIE.get(),art.arcane.thaumcraft.client.render.entity.BrainyZombieRenderer::new);
        event.registerEntityRenderer(art.arcane.thaumcraft.registry.ModEntities.INHABITED_ZOMBIE.get(),art.arcane.thaumcraft.client.render.entity.InhabitedZombieRenderer::new);
        event.registerEntityRenderer(art.arcane.thaumcraft.registry.ModEntities.ELDRITCH_CRAB.get(),art.arcane.thaumcraft.client.render.entity.EldritchCrabRenderer::new);
        event.registerEntityRenderer(art.arcane.thaumcraft.registry.ModEntities.PECH.get(),art.arcane.thaumcraft.client.render.entity.PechRenderer::new);
        event.registerEntityRenderer(art.arcane.thaumcraft.registry.ModEntities.ELDRITCH_WARDEN.get(),art.arcane.thaumcraft.client.render.entity.EldritchWardenRenderer::new);
        event.registerEntityRenderer(art.arcane.thaumcraft.registry.ModEntities.ELDRITCH_GOLEM.get(),art.arcane.thaumcraft.client.render.entity.EldritchGolemRenderer::new);
        event.registerEntityRenderer(art.arcane.thaumcraft.registry.ModEntities.CULTIST_LEADER.get(),art.arcane.thaumcraft.client.render.entity.CultistLeaderRenderer::new);
        event.registerEntityRenderer(art.arcane.thaumcraft.registry.ModEntities.CULTIST_PORTAL_GREATER.get(),art.arcane.thaumcraft.client.render.entity.CultistPortalGreaterRenderer::new);
        event.registerEntityRenderer(art.arcane.thaumcraft.registry.ModEntities.TAINTACLE_GIANT.get(),art.arcane.thaumcraft.client.render.entity.TaintacleRenderer::giant);
        event.registerBlockEntityRenderer(art.arcane.thaumcraft.registry.ModBlockEntities.CRUCIBLE.get(),art.arcane.thaumcraft.client.render.CrucibleRenderer::new);
        event.registerBlockEntityRenderer(art.arcane.thaumcraft.registry.ModBlockEntities.HOLE.get(),art.arcane.thaumcraft.client.render.HoleRenderer::new);
        event.registerBlockEntityRenderer(art.arcane.thaumcraft.registry.ModBlockEntities.AURA_NODE.get(),art.arcane.thaumcraft.client.render.AuraNodeRenderer::new);
        event.registerBlockEntityRenderer(art.arcane.thaumcraft.registry.ModBlockEntities.MIRROR.get(),art.arcane.thaumcraft.client.render.MirrorRenderer::new);
        event.registerBlockEntityRenderer(art.arcane.thaumcraft.registry.ModBlockEntities.MIRROR_ESSENTIA.get(),art.arcane.thaumcraft.client.render.MirrorRenderer::new);
        event.registerBlockEntityRenderer(art.arcane.thaumcraft.registry.ModBlockEntities.ESSENTIA_JAR.get(),art.arcane.thaumcraft.client.render.JarRenderer::new);
        event.registerBlockEntityRenderer(art.arcane.thaumcraft.registry.ModBlockEntities.BRAIN_JAR.get(),art.arcane.thaumcraft.client.render.BrainJarRenderer::new);
        event.registerBlockEntityRenderer(art.arcane.thaumcraft.registry.ModBlockEntities.BANNER.get(),art.arcane.thaumcraft.client.render.BannerRenderer::new);
        event.registerBlockEntityRenderer(art.arcane.thaumcraft.registry.ModBlockEntities.PEDESTAL.get(),art.arcane.thaumcraft.client.render.PedestalRenderer::new);
        event.registerBlockEntityRenderer(art.arcane.thaumcraft.registry.ModBlockEntities.THAUMATORIUM.get(),art.arcane.thaumcraft.client.render.ThaumatoriumRenderer::new);
        event.registerBlockEntityRenderer(art.arcane.thaumcraft.registry.ModBlockEntities.RECHARGE_PEDESTAL.get(),art.arcane.thaumcraft.client.render.PedestalRenderer::new);
        event.registerBlockEntityRenderer(art.arcane.thaumcraft.registry.ModBlockEntities.INFUSION_MATRIX.get(),art.arcane.thaumcraft.client.render.InfusionMatrixRenderer::new);
        event.registerBlockEntityRenderer(art.arcane.thaumcraft.registry.ModBlockEntities.DIOPTRA.get(),art.arcane.thaumcraft.client.render.DioptraRenderer::new);
        event.registerBlockEntityRenderer(art.arcane.thaumcraft.registry.ModBlockEntities.PATTERN_CRAFTER.get(),art.arcane.thaumcraft.client.render.PatternCrafterRenderer::new);
    }
    private static void registerParticles(net.neoforged.neoforge.client.event.RegisterParticleProvidersEvent event){
        event.registerSpriteSet(art.arcane.thaumcraft.registry.ModParticles.SCAN_RUNE.get(),ScanRuneParticle.Provider::new);
        event.registerSpriteSet(art.arcane.thaumcraft.registry.ModParticles.GENERIC.get(),art.arcane.thaumcraft.client.fx.GenericParticle.Provider::new);
        event.registerSpriteSet(art.arcane.thaumcraft.registry.ModParticles.FOCUS_FIRE.get(),FocusFireParticle.Provider::new);
        event.registerSpriteSet(art.arcane.thaumcraft.registry.ModParticles.CRUCIBLE_ASPECT_BUBBLE.get(),CrucibleAspectBubbleParticle.Provider::new);
        event.registerSpriteSet(art.arcane.thaumcraft.registry.ModParticles.CRUCIBLE_OVERFLOW.get(),CrucibleOverflowParticle.Provider::new);
        event.registerSpriteSet(art.arcane.thaumcraft.registry.ModParticles.CRUCIBLE_FROTH.get(),CrucibleFrothParticle.Provider::new);
        event.registerSpriteSet(art.arcane.thaumcraft.registry.ModParticles.FOCAL_CRAFT.get(),FocalCraftParticle.Provider::new);
    }
    private static void registerBlockColors(net.neoforged.neoforge.client.event.RegisterColorHandlersEvent.BlockTintSources event) {
        net.minecraft.client.color.block.BlockTintSource filterTint=new net.minecraft.client.color.block.BlockTintSource(){
            public int color(net.minecraft.world.level.block.state.BlockState state){return -1;}
            public int colorInWorld(net.minecraft.world.level.block.state.BlockState state,net.minecraft.client.renderer.block.BlockAndTintGetter world,net.minecraft.core.BlockPos pos){
                if(world.getBlockEntity(pos) instanceof art.arcane.thaumcraft.blockentity.EssentiaTubeBlockEntity tube)return 0xFF000000 | art.arcane.thaumcraft.aspect.AspectRegistry.color(tube.filter());
                return -1;
            }
        };
        event.register(java.util.List.of(filterTint,filterTint),art.arcane.thaumcraft.ModBlocks.TUBE_FILTER.get());
        // ColorHandler: inlay tinted by its charge, stabiliser by its tile energy (tint index 0).
        event.register(java.util.List.of(new net.minecraft.client.color.block.BlockTintSource(){
            public int color(net.minecraft.world.level.block.state.BlockState state){return art.arcane.thaumcraft.block.InlayBlock.colorMultiplier(state.getValue(art.arcane.thaumcraft.block.InlayBlock.CHARGE));}
        }),art.arcane.thaumcraft.ModBlocks.INLAY.get());
        event.register(java.util.List.of(new net.minecraft.client.color.block.BlockTintSource(){
            public int color(net.minecraft.world.level.block.state.BlockState state){return art.arcane.thaumcraft.block.StabilizerBlock.colorMultiplier(0);}
            public int colorInWorld(net.minecraft.world.level.block.state.BlockState state,net.minecraft.client.renderer.block.BlockAndTintGetter world,net.minecraft.core.BlockPos pos){
                return art.arcane.thaumcraft.block.StabilizerBlock.colorMultiplier(world.getBlockEntity(pos) instanceof art.arcane.thaumcraft.blockentity.StabilizerBlockEntity te?te.getEnergy():0);
            }
        }),art.arcane.thaumcraft.ModBlocks.STABILIZER.get());
        event.register(art.arcane.thaumcraft.aspect.AspectRegistry.all().keySet().stream().map(tag -> net.minecraft.client.color.block.BlockTintSources.constant(0xFF000000 | art.arcane.thaumcraft.aspect.AspectRegistry.color(tag))).toList(),art.arcane.thaumcraft.ModBlocks.JAR_NORMAL.get(),art.arcane.thaumcraft.ModBlocks.JAR_VOID.get());
        // TC4 BlockManaPod: pod body tinted by the tile's aspect colour (tint index 0).
        event.register(java.util.List.of(new net.minecraft.client.color.block.BlockTintSource(){
            public int color(net.minecraft.world.level.block.state.BlockState state){return -1;}
            public int colorInWorld(net.minecraft.world.level.block.state.BlockState state,net.minecraft.client.renderer.block.BlockAndTintGetter world,net.minecraft.core.BlockPos pos){
                return world.getBlockEntity(pos) instanceof art.arcane.thaumcraft.blockentity.ManaPodBlockEntity pod?pod.tint():-1;
            }
        }),art.arcane.thaumcraft.ModBlocks.MANA_POD.get());
        // TC6 ColorHandler: greatwood leaves use biome foliage, candles their dye map colour.
        event.register(java.util.List.of(net.minecraft.client.color.block.BlockTintSources.foliage()),art.arcane.thaumcraft.ModBlocks.LEAVES_GREATWOOD.get());
        event.register(java.util.List.of(net.minecraft.client.color.block.BlockTintSources.grassBlock()),art.arcane.thaumcraft.ModBlocks.GRASS_AMBIENT.get());
        for (var h : art.arcane.thaumcraft.ModBlocks.BLOCKS.getEntries()) {
            if (h.get() instanceof art.arcane.thaumcraft.block.world.TcCandleBlock c)
                event.register(java.util.List.of(net.minecraft.client.color.block.BlockTintSources.constant(0xFF000000 | c.dye().getMapColor().col)), c);
        }
    }
    private static void registerMenus(net.neoforged.neoforge.client.event.RegisterMenuScreensEvent event) {
        event.register(art.arcane.thaumcraft.registry.ModMenus.ARCANE_WORKBENCH.get(),ArcaneWorkbenchScreen::new);
        event.register(art.arcane.thaumcraft.registry.ModMenus.FOCUS_POUCH.get(),FocusPouchScreen::new);
        event.register(art.arcane.thaumcraft.registry.ModMenus.ESSENTIA_SMELTER.get(),EssentiaSmelterScreen::new);
        event.register(art.arcane.thaumcraft.registry.ModMenus.RESEARCH_TABLE.get(),ResearchTableScreen::new);
        event.register(art.arcane.thaumcraft.registry.ModMenus.FOCAL_MANIPULATOR.get(),FocalManipulatorScreen::new);
        event.register(art.arcane.thaumcraft.registry.ModMenus.SPA.get(),SpaScreen::new);
        event.register(art.arcane.thaumcraft.registry.ModMenus.DECONSTRUCTION_TABLE.get(),DeconstructionTableScreen::new);
        event.register(art.arcane.thaumcraft.registry.ModMenus.VOID_SIPHON.get(),VoidSiphonScreen::new);
        event.register(art.arcane.thaumcraft.registry.ModMenus.THAUMATORIUM.get(),ThaumatoriumScreen::new);
        event.register(art.arcane.thaumcraft.registry.ModMenus.POTION_SPRAYER.get(),PotionSprayerScreen::new);
        event.register(art.arcane.thaumcraft.registry.ModMenus.GOLEM_BUILDER.get(),art.arcane.thaumcraft.golems.client.GolemBuilderScreen::new);
        event.register(art.arcane.thaumcraft.registry.ModMenus.TURRET.get(),art.arcane.thaumcraft.golems.client.TurretScreen::new);
        event.register(art.arcane.thaumcraft.registry.ModMenus.LOGISTICS.get(),art.arcane.thaumcraft.golems.client.LogisticsScreen::new);
        event.register(art.arcane.thaumcraft.registry.ModMenus.SEAL.get(),art.arcane.thaumcraft.golems.client.SealScreen::new);
        event.register(art.arcane.thaumcraft.registry.ModMenus.PECH.get(),PechScreen::new);
        event.register(art.arcane.thaumcraft.registry.ModMenus.HAND_MIRROR.get(),HandMirrorScreen::new);
    }
    private static void registerPayloads(RegisterClientPayloadHandlersEvent event) {
        event.register(art.arcane.thaumcraft.network.GolemBuilderPayload.Stuff.TYPE,(payload,context)->context.enqueueWork(()->art.arcane.thaumcraft.golems.client.GolemBuilderScreen.receiveStuff(payload.has())));
        event.register(art.arcane.thaumcraft.network.SealSyncPayload.TYPE,(payload,context)->context.enqueueWork(()->{ if (Minecraft.getInstance().level != null) art.arcane.thaumcraft.golems.seals.SealHandler.receive(Minecraft.getInstance().level, payload); }));
        event.register(art.arcane.thaumcraft.network.FxPayload.TYPE,(payload,context)->context.enqueueWork(()->art.arcane.thaumcraft.client.fx.FXDispatcher.handle(payload)));
        event.register(art.arcane.thaumcraft.network.ResearchToastPayload.TYPE,(payload,context)->context.enqueueWork(()->{
            var entry=art.arcane.thaumcraft.research.ResearchCatalog.entries().get(payload.key());
            if(entry!=null)Minecraft.getInstance().gui.toastManager().addToast(new ResearchToast(payload.key(),net.minecraft.network.chat.Component.translatable(entry.name())));
        }));
        event.register(art.arcane.thaumcraft.network.ThaumometerAuraPayload.TYPE,(payload,context)->context.enqueueWork(()->ThaumometerHud.receive(payload)));
        event.register(art.arcane.thaumcraft.network.NotePayload.TYPE, art.arcane.thaumcraft.network.NotePayload::handle);
        event.register(art.arcane.thaumcraft.network.AspectSnapshotPayload.TYPE,(payload,context)->context.enqueueWork(()->art.arcane.thaumcraft.scan.ScanRegistry.installDerived(payload.values())));
        event.register(ResearchSnapshotPayload.TYPE, (payload, context) ->
                context.enqueueWork(() -> {
                    ClientResearch.receive(payload.data());
                    var minecraft = Minecraft.getInstance();
                    if (minecraft.gui.screen() instanceof ResearchSnapshotListener l) l.onSnapshot();
                    else if (com.google.gson.JsonParser.parseString(payload.data()).getAsJsonObject().get("open").getAsBoolean())
                        minecraft.gui.setScreen(new art.arcane.thaumcraft.client.book.ResearchBrowserScreen());
                }));
    }
}
