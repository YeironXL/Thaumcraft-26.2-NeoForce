package art.arcane.thaumcraft.verification;

import art.arcane.thaumcraft.ModBlocks;
import art.arcane.thaumcraft.ModItems;
import art.arcane.thaumcraft.aura.NativeAura;
import art.arcane.thaumcraft.blockentity.ArcaneWorkbenchBlockEntity;
import art.arcane.thaumcraft.blockentity.CrucibleBlockEntity;
import art.arcane.thaumcraft.item.CrystalEssenceItem;
import art.arcane.thaumcraft.menu.ArcaneWorkbenchMenu;
import art.arcane.thaumcraft.progression.PlayerProgressManager;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.registries.DeferredRegister;
import java.util.function.Consumer;

/** Server world tests, compiled only for verification and excluded from the release. */
@Mod("thaumcraft")
public final class StarterTransactionsVerification {
    public StarterTransactionsVerification(IEventBus bus) {
        var functions=DeferredRegister.<Consumer<GameTestHelper>>create(Registries.TEST_FUNCTION,"thaumcraft");
        functions.register("alumentum",()->AlumentumVerification::test);
        functions.register("object_aspects",()->ObjectAspectsVerification::test);
        functions.register("compat_aspects",()->CompatAspectsVerification::test);
        functions.register("scanning",()->ScanVerification::test);
        functions.register("entity_aspects",()->EntityAspectsVerification::test);
        functions.register("world_blocks",()->WorldBlocksVerification::test);
        functions.register("focus_engine",()->FocusEngineVerification::test);
        functions.register("infusion_altar",()->InfusionAltarVerification::test);
        functions.register("infusion_craft",()->InfusionCraftVerification::craft);
        functions.register("infusion_instability",()->InfusionCraftVerification::instability);
        functions.register("infusion_enchantment",()->InfusionCraftVerification::enchantment);
        functions.register("infusion_collector_follow",()->InfusionCraftVerification::collectorFollow);
        functions.register("trunk_follow",()->TrunkVerification::follow);
        functions.register("mana_pod_growth",()->ManaDeconstructionVerification::podGrowth);
        functions.register("tc6_loot",()->Tc6GapsVerification::loot);
        functions.register("tc6_vis_battery",()->Tc6GapsVerification::visBattery);
        functions.register("tc6_void_siphon",()->Tc6GapsVerification::voidSiphon);
        functions.register("tc6_paving",()->Tc6GapsVerification::paving);
        functions.register("tc6_brain_jar",()->Tc6GapsVerification::brainJar);
        functions.register("tc6_transfuser",()->Tc6GapsVerification::transfuser);
        functions.register("tc6_lamps",()->Tc6GapsVerification::lamps);
        functions.register("tc6_condenser",()->Tc6GapsVerification::condenser);
        functions.register("tc6_banner",()->Tc6GapsVerification::banner);
        functions.register("mana_bean_crucible",()->ManaDeconstructionVerification::beanCrucible);
        functions.register("deconstruction_table",()->ManaDeconstructionVerification::deconstruct);
        functions.register("trunk_stay",()->TrunkVerification::stay);
        functions.register("trunk_pickup",()->TrunkVerification::pickup);
        functions.register("trunk_upgrade",()->TrunkVerification::upgrade);
        functions.register("trunk_dimension",()->TrunkVerification::dimension);
        functions.register("deconstruction_hopper",()->ManaDeconstructionVerification::deconstructHopper);
        functions.register("elemental_axe",()->ElementalGearVerification::axe);
        functions.register("elemental_shovel",()->ElementalGearVerification::shovel);
        functions.register("fortress_armor",()->ElementalGearVerification::fortress);
        functions.register("infusion_gear",()->ElementalGearVerification::infusionGear);
        functions.register("auromancy_devices",()->AuromancyDevicesVerification::test);
        functions.register("alchemy_devices",()->AlchemyDevicesVerification::test);
        functions.register("artifice_devices",()->ArtificeDevicesVerification::test);
        functions.register("smelter_automation",()->SmelterAutomationVerification::test);
        functions.register("compat_capabilities",()->CompatCapabilitiesVerification::test); functions.register("golem_capability",()->GolemancyVerification::capability);
        functions.register("smelter_menu",()->SmelterMenuVerification::test);
        functions.register("early_focus",()->EarlyFocusVerification::test);
        functions.register("research_specialised",()->SpecialisedTheoryVerification::test);
        functions.register("research_table",()->ResearchTableVerification::test);
        functions.register("research_costs",()->ResearchCostsVerification::test);
        functions.register("starter_transactions",() -> StarterTransactionsVerification::test); functions.register("tube_variants",()->TubeVariantsVerification::test); functions.register("centrifuge",()->CentrifugeVerification::test); functions.register("flux_rift",()->FluxRiftVerification::test); functions.register("fire_bat",()->FireBatVerification::test); functions.register("nodes_generation",()->AuraNodesVerification::generation); functions.register("nodes_aura",()->AuraNodesVerification::aura); functions.register("nodes_jar",()->AuraNodesVerification::jar); functions.register("nodes_hungry",()->AuraNodesVerification::hungry);functions.register("goggles",()->GogglesVerification::test); functions.register("hover_harness",()->HoverHarnessVerification::test); functions.register("hover_girdle",()->HoverGirdleVerification::test); functions.register("biome_spawns",()->BiomeSpawnsVerification::test); functions.register("wisp_taint",()->WispTaintVerification::test); functions.register("warp",()->WarpVerification::test); functions.register("baubles",()->BaublesVerification::test); functions.register("cultist_ritual",()->CultistRitualVerification::test); functions.register("golem_press",()->GolemancyVerification::press); functions.register("golem_pickup",()->GolemancyVerification::pickup); functions.register("golem_guard",()->GolemancyVerification::guard); functions.register("golem_turret",()->GolemancyVerification::turret); functions.register("golem_fishing",()->GolemancyVerification::fishing); functions.register("golem_fluid",()->GolemancyVerification::fluid); functions.register("golem_essentia",()->GolemancyVerification::essentia); functions.register("golem_bell",()->GolemancyVerification::bell);functions.register("mobs_mirror",()->MobsVerification::mirror); functions.register("mobs_champion",()->MobsVerification::champion); functions.register("mobs_taint",()->MobsVerification::taint); functions.register("mobs_pech",()->MobsVerification::pech); functions.register("boss_warden",()->BossesVerification::warden); functions.register("boss_golem",()->BossesVerification::golem); functions.register("boss_crimson_portal",()->BossesVerification::crimsonPortal); functions.register("boss_taintacle_giant",()->BossesVerification::taintacleGiant); functions.register("outer_altar",()->OuterLandsVerification::altarPortal); functions.register("outer_enter",()->OuterLandsVerification::enter); functions.register("outer_maze",()->OuterLandsVerification::maze); functions.register("outer_lock",()->OuterLandsVerification::lock); functions.register("outer_rooms",()->OuterLandsVerification::rooms); functions.register("outer_crab_spawner",()->OuterLandsVerification::crabSpawner); functions.register("outer_taint",()->OuterLandsVerification::taint); functions.register("outer_taint_spread",()->OuterLandsVerification::taintSpread); functions.register("outer_autocaster",()->OuterLandsVerification::autocaster); functions.register("outer_crystals",()->OuterLandsVerification::crystals); functions.register("outer_unloaded_mobs",()->OuterLandsVerification::unloadedMobs); functions.register("outer_giant_taintacles",()->OuterLandsVerification::giantTaintacles); functions.register("foci_ward",()->FociStructuresVerification::ward); functions.register("foci_primal",()->FociStructuresVerification::primal); functions.register("shrines_structures",()->FociStructuresVerification::structures); functions.register("foci_shock",()->FociStructuresVerification::shock); functions.register("barrow_den",()->FociStructuresVerification::barrow); functions.register("outer_pools",()->OuterLandsVerification::pools); functions.register(bus);
    }
    private static void require(boolean condition,String message) { if (!condition) throw new AssertionError(message); }
    private static void login(ServerPlayer player) {
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.post(new net.neoforged.neoforge.event.entity.player.PlayerEvent.PlayerLoggedInEvent(player));
    }
    private static void verifyStartingBook(GameTestHelper helper) {
        // TC6 gives no book on login: crystal -> sleep -> dream journal -> Salis Mundus on a bookshelf.
        var player=(ServerPlayer)helper.makeMockServerPlayer(GameType.SURVIVAL);
        login(player);
        require(player.getInventory().countItem(ModItems.THAUMONOMICON.get())==0,"Login must not grant the Thaumonomicon");
        require(!PlayerProgressManager.has(player,"!gotthaumonomicon") && !PlayerProgressManager.has(player,"!starter_thaumonomicon"),"Login marked the Thaumonomicon as obtained");
        login(player);
        require(player.getInventory().countItem(ModItems.THAUMONOMICON.get())==0,"Relog granted the Thaumonomicon");
        System.out.println("Starting Thaumonomicon passed: no login gift, as in TC6.");
    }
    private static void verifyActivityMilestones(GameTestHelper helper) {
        // TC6 ConfigResearch.checkPeriodicStuff: strict ">" thresholds, evaluated every 200 ticks.
        var player=(ServerPlayer)helper.makeMockServerPlayer(GameType.SURVIVAL);var stats=player.getStats();
        Object[][] marks={{"m_walker",net.minecraft.stats.Stats.WALK_ONE_CM,160000},{"m_runner",net.minecraft.stats.Stats.SPRINT_ONE_CM,80000},{"m_jumper",net.minecraft.stats.Stats.JUMP,500},{"m_swimmer",net.minecraft.stats.Stats.SWIM_ONE_CM,8000}};
        for(var mark:marks)stats.setValue(player,net.minecraft.stats.Stats.CUSTOM.get((net.minecraft.resources.Identifier)mark[1]),(Integer)mark[2]);
        player.tickCount=200;net.neoforged.neoforge.common.NeoForge.EVENT_BUS.post(new net.neoforged.neoforge.event.tick.PlayerTickEvent.Post(player));
        for(var mark:marks)require(!PlayerProgressManager.has(player,(String)mark[0]),"Milestone granted at exact threshold: "+mark[0]);
        for(var mark:marks)stats.setValue(player,net.minecraft.stats.Stats.CUSTOM.get((net.minecraft.resources.Identifier)mark[1]),(Integer)mark[2]+1);
        player.tickCount=199;net.neoforged.neoforge.common.NeoForge.EVENT_BUS.post(new net.neoforged.neoforge.event.tick.PlayerTickEvent.Post(player));
        require(!PlayerProgressManager.has(player,"m_walker"),"Milestones checked outside the 200-tick period");
        player.tickCount=400;net.neoforged.neoforge.common.NeoForge.EVENT_BUS.post(new net.neoforged.neoforge.event.tick.PlayerTickEvent.Post(player));
        for(var mark:marks)require(PlayerProgressManager.has(player,(String)mark[0]),"Milestone not granted above threshold: "+mark[0]);
        System.out.println("Activity milestones passed: walker/runner/jumper/swimmer strict thresholds every 200 ticks.");
    }
    private static void test(GameTestHelper helper) {
        verifyStartingBook(helper);
        verifyActivityMilestones(helper);
        var world=helper.getLevel();
        var fuelPos=new BlockPos(4,1,4);helper.setBlock(fuelPos,net.minecraft.world.level.block.Blocks.FURNACE);
        var fuelFurnace=helper.getBlockEntity(fuelPos,net.minecraft.world.level.block.entity.FurnaceBlockEntity.class);
        var alumentum=new ItemStack(ModItems.ALUMENTUM.get());
        require(alumentum.getBurnTime(net.minecraft.world.item.crafting.RecipeType.SMELTING,world.fuelValues())==4800,"Alumentum original fuel duration changed");
        fuelFurnace.setItem(0,new ItemStack(Items.IRON_ORE));fuelFurnace.setItem(1,alumentum);
        var relative=new BlockPos(1,1,1); helper.setBlock(relative,ModBlocks.ARCANE_WORKBENCH.get());
        var pos=helper.absolutePos(relative);
        var bench=helper.getBlockEntity(relative,ArcaneWorkbenchBlockEntity.class);
        var player=(ServerPlayer)helper.makeMockServerPlayer(GameType.SURVIVAL);
        player.setPos(pos.getX()+0.5,pos.getY()+1,pos.getZ()+0.5);
        var aura=NativeAura.get(world,world.getChunkAt(pos)); aura.spend(aura.vis()); aura.addVis(100);
        for (int index : new int[]{1,3,5,7}) bench.setItem(index,new ItemStack(Items.GOLD_INGOT,2));
        bench.setItem(4,new ItemStack(Items.GLASS_PANE,2));
        float initial=aura.vis();
        require(bench.preview(player).isEmpty() && bench.takeOutput(player,1).isEmpty() && aura.vis()==initial,"Research gate charged or produced an output");
        var research=PlayerProgressManager.get(player); research.unlock("FIRSTSTEPS"); research.setStage("FIRSTSTEPS",2);
        require(bench.preview(player).isEmpty() && bench.getItem(1).getCount()==2,"Missing crystals consumed ingredients");
        for (int i=0;i<6;i++) bench.setItem(9+i,CrystalEssenceItem.of(ArcaneWorkbenchBlockEntity.PRIMALS.get(i),2));
        aura.spend(aura.vis()); aura.addVis(19);
        require(bench.takeOutput(player,1).isEmpty() && aura.vis()==19 && bench.getItem(9).getCount()==2,"Insufficient vis partially charged a recipe");
        aura.addVis(21);
        require(bench.takeOutput(player,1).is(ModItems.THAUMOMETER.get()) && aura.vis()==20 && bench.getItem(9).getCount()==1
                && bench.getItem(1).getCount()==1 && research.crafted("thaumcraft:thaumometer"),"Arcane craft has incorrect costs/history");
        var menu=new ArcaneWorkbenchMenu(7,player.getInventory(),bench);
        menu.clicked(0,0,ContainerInput.SWAP,player);
        require(player.getInventory().getItem(0).is(ModItems.THAUMOMETER.get()) && aura.vis()==0 && bench.getItem(9).isEmpty(),"Hotbar swap bypassed payment");
        require(bench.takeOutput(player,1).isEmpty(),"Repeated output retrieval duplicated an item");
        bench.clearContent(); bench.setItem(0,new ItemStack(Items.OAK_PLANKS)); bench.setItem(3,new ItemStack(Items.OAK_PLANKS));
        require(bench.takeOutput(player,1).is(Items.STICK) && bench.getItem(15).getCount()==3,"Partial crafting result was lost or charged twice");
        require(bench.takeOutput(player,2).getCount()==2 && bench.getItem(15).getCount()==1 && aura.vis()==0,"Stored output cost changed on second extraction");
        helper.setBlock(new BlockPos(3,1,1),ModBlocks.CRUCIBLE.get());
        var crucible=helper.getBlockEntity(new BlockPos(3,1,1),CrucibleBlockEntity.class);
        crucible.mixture().fill(); for (int i=0;i<151;i++) crucible.mixture().heatTick(true);
        crucible.mixture().dissolve(java.util.Map.of("ignis",10,"potentia",10,"lux",10));
        var overflowPot=new CrucibleBlockEntity(crucible.getBlockPos(),crucible.getBlockState());overflowPot.setLevel(world);
        overflowPot.mixture().fill();for(int n=0;n<151;n++)overflowPot.mixture().heatTick(true);overflowPot.mixture().dissolve(java.util.Map.of("ignis",501));
        var overflowMirror=new CrucibleBlockEntity(crucible.getBlockPos(),crucible.getBlockState());
        overflowMirror.loadWithComponents(net.minecraft.world.level.storage.TagValueInput.create(net.minecraft.util.ProblemReporter.DISCARDING,world.registryAccess(),overflowPot.getUpdatePacket().getTag()));
        require(overflowMirror.mixture().overflowing(),"Synchronized hot crucible with501 aspects did not overflow");
        require(overflowPot.mixture().tickSpill(0)==.25F&&overflowPot.mixture().total()==500,"Overflow visual change altered original spilling mechanics");
        overflowMirror.loadWithComponents(net.minecraft.world.level.storage.TagValueInput.create(net.minecraft.util.ProblemReporter.DISCARDING,world.registryAccess(),overflowPot.getUpdatePacket().getTag()));
        require(!overflowMirror.mixture().overflowing(),"Overflow persisted after synchronized aspects reached500");
        overflowPot.mixture().dissolve(java.util.Map.of("ignis",1));overflowPot.mixture().heatTick(false);
        overflowMirror.loadWithComponents(net.minecraft.world.level.storage.TagValueInput.create(net.minecraft.util.ProblemReporter.DISCARDING,world.registryAccess(),overflowPot.getUpdatePacket().getTag()));
        require(!overflowMirror.mixture().overflowing()&&overflowMirror.mixture().heat()==150,"Cold synchronized crucible emitted overflow");
        overflowPot.mixture().empty();overflowMirror.loadWithComponents(net.minecraft.world.level.storage.TagValueInput.create(net.minecraft.util.ProblemReporter.DISCARDING,world.registryAccess(),overflowPot.getUpdatePacket().getTag()));require(!overflowMirror.mixture().overflowing(),"Empty synchronized crucible emitted overflow");
        System.out.println("Crucible overflow synchronization passed:501 hot,500 after original spill,150 heat and empty boundaries.");
        crucible.changed();
        var mirror=new CrucibleBlockEntity(crucible.getBlockPos(),crucible.getBlockState());
        var update=crucible.getUpdatePacket();require(update!=null&&update.getPos().equals(crucible.getBlockPos()),"Crucible visual packet missing");
        mirror.loadWithComponents(net.minecraft.world.level.storage.TagValueInput.create(net.minecraft.util.ProblemReporter.DISCARDING,world.registryAccess(),update.getTag()));
        require(mirror.mixture().aspects().equals(crucible.mixture().aspects())&&mirror.mixture().water()==1000&&mirror.mixture().heat()==151,"Crucible update lost aspect concentration, water or heat");
        var belowPos=crucible.getBlockPos().below();var previousHeatSource=world.getBlockState(belowPos);
        world.setBlock(belowPos,net.minecraft.world.level.block.Blocks.STONE.defaultBlockState(),3);
        CrucibleBlockEntity.tick(world,crucible.getBlockPos(),crucible.getBlockState(),crucible);
        mirror.loadWithComponents(net.minecraft.world.level.storage.TagValueInput.create(net.minecraft.util.ProblemReporter.DISCARDING,world.registryAccess(),crucible.getUpdatePacket().getTag()));
        require(!mirror.mixture().boiling()&&mirror.mixture().heat()==150&&mirror.mixture().total()==30,"Cooling boundary lost client visual state or contents");
        world.setBlock(belowPos,net.minecraft.world.level.block.Blocks.MAGMA_BLOCK.defaultBlockState(),3);
        CrucibleBlockEntity.tick(world,crucible.getBlockPos(),crucible.getBlockState(),crucible);
        mirror.loadWithComponents(net.minecraft.world.level.storage.TagValueInput.create(net.minecraft.util.ProblemReporter.DISCARDING,world.registryAccess(),crucible.getUpdatePacket().getTag()));
        require(mirror.mixture().boiling()&&mirror.mixture().heat()==151&&mirror.mixture().total()==30,"Heating boundary lost client visual state or contents");
        world.setBlock(belowPos,previousHeatSource,3);
        System.out.println("Live crucible heating/cooling boundaries passed:151 to150 to151, synchronized state and conserved contents.");
        research.unlock("UNLOCKALCHEMY"); research.setStage("UNLOCKALCHEMY",3);
        require(crucible.processOne(new ItemStack(Items.GLOWSTONE_DUST),player) && crucible.mixture().water()==950
                && crucible.mixture().total()==0 && research.crafted("thaumcraft:nitor_yellow"),"Nitor alchemy cost/research/craft record failed");
        mirror.loadWithComponents(net.minecraft.world.level.storage.TagValueInput.create(net.minecraft.util.ProblemReporter.DISCARDING,world.registryAccess(),crucible.getUpdatePacket().getTag()));
        require(mirror.mixture().total()==0&&mirror.mixture().water()==950,"Crucible consumption update retained stale aspect concentration");
        research.setStage("FIRSTSTEPS",4);
        require(art.arcane.thaumcraft.research.ResearchProgression.advance(research,"UNLOCKALCHEMY",3,items -> items.isEmpty())
                && art.arcane.thaumcraft.research.ResearchRules.complete(research,"UNLOCKALCHEMY"),"Nitor metadata prevented completing the original alchemy introduction");
        var nitor=ModBlocks.NITOR_YELLOW.get().defaultBlockState();
        require(nitor.getLightEmission()==15 && nitor.getCollisionShape(world,pos).isEmpty(),"Nitor failed to provide non-solid light");
        var crystal=ModBlocks.CRYSTAL_IGNIS.get().defaultBlockState().setValue(art.arcane.thaumcraft.block.CrystalClusterBlock.SIZE,3);
        var drops=net.minecraft.world.level.block.Block.getDrops(crystal,world,pos,null);
        require(drops.size()==1 && drops.getFirst().getCount()==4 && CrystalEssenceItem.aspect(drops.getFirst()).equals("ignis"),"Crystal drop lost its aspect or growth amount");
        var generatedOrigin=helper.absolutePos(new BlockPos(3,5,3));
        var ignis=(art.arcane.thaumcraft.block.CrystalClusterBlock)ModBlocks.CRYSTAL_IGNIS.get();
        for(var face:net.minecraft.core.Direction.values()){
            world.setBlockAndUpdate(generatedOrigin,net.minecraft.world.level.block.Blocks.AIR.defaultBlockState());
            for(var neighbor:net.minecraft.core.Direction.values())world.setBlockAndUpdate(generatedOrigin.relative(neighbor),net.minecraft.world.level.block.Blocks.AIR.defaultBlockState());
            world.setBlockAndUpdate(generatedOrigin.relative(face.getOpposite()),net.minecraft.world.level.block.Blocks.STONE.defaultBlockState());
            require(art.arcane.thaumcraft.block.CrystalClusterBlock.touchingRock(world,generatedOrigin),"Crystal does not see rock on face "+face);
            world.setBlockAndUpdate(generatedOrigin,ignis.attached(world,generatedOrigin,ignis.defaultBlockState().setValue(art.arcane.thaumcraft.block.CrystalClusterBlock.SIZE,2)));
            var generated=world.getBlockState(generatedOrigin);
            require(generated.getBlock() instanceof art.arcane.thaumcraft.block.CrystalClusterBlock&&generated.getValue(art.arcane.thaumcraft.block.CrystalClusterBlock.FACING)==face,"Crystal generated with wrong attachment "+face);
            var mined=net.minecraft.world.level.block.Block.getDrops(generated,world,generatedOrigin,null);
            require(mined.size()==1&&!CrystalEssenceItem.aspect(mined.getFirst()).isEmpty()&&mined.getFirst().getCount()==generated.getValue(art.arcane.thaumcraft.block.CrystalClusterBlock.SIZE)+1,"Generated crystal lost aspect/size on harvest");
        }
        world.setBlockAndUpdate(generatedOrigin,net.minecraft.world.level.block.Blocks.AIR.defaultBlockState());
        for(var neighbor:net.minecraft.core.Direction.values())world.setBlockAndUpdate(generatedOrigin.relative(neighbor),net.minecraft.world.level.block.Blocks.AIR.defaultBlockState());
        System.out.println("Crystal attachment passed: six rock faces and aspect/quantity drops.");
        var astronomer=(ServerPlayer)helper.makeMockServerPlayer(GameType.SURVIVAL);
        astronomer.setPos(helper.absolutePos(new BlockPos(3,5,3)).getX(),world.getMaxY()-10,helper.absolutePos(new BlockPos(3,5,3)).getZ());
        astronomer.getInventory().setItem(1,new ItemStack(ModItems.SCRIBING_TOOLS.get()));
        astronomer.getInventory().setItem(2,new ItemStack(Items.PAPER,2));
        require(!art.arcane.thaumcraft.scan.CelestialScanning.scan(astronomer),"Celestial notes bypass research");
        var astronomy=PlayerProgressManager.get(astronomer);astronomy.unlock("CELESTIALSCANNING");astronomy.setStage("CELESTIALSCANNING",2);
        int observed=-1;
        outer:for(int yaw:new int[]{-90,90})for(int pitch=0;pitch>=-90;pitch--){
            int target=art.arcane.thaumcraft.scan.CelestialScanning.target(world.getOverworldClockTime(),yaw,pitch,net.minecraft.core.Direction.NORTH);
            if(target==0||target>=5){astronomer.setYRot(yaw);astronomer.setXRot(pitch);observed=target;break outer;}
        }
        require(observed>=0,"Cannot find original sun/moon targeting angle");
        require(art.arcane.thaumcraft.scan.CelestialScanning.scan(astronomer),"Sky scan failed in open overworld");
        require(astronomer.getInventory().getItem(2).getCount()==1,"Sky scan did not consume exactly one paper");
        int notes=0;for(int slot=0;slot<astronomer.getInventory().getContainerSize();slot++){
            var note=astronomer.getInventory().getItem(slot);if(note.is(ModItems.CELESTIAL_NOTES.get())){notes+=note.getCount();require(note.get(art.arcane.thaumcraft.registry.ModComponents.CELESTIAL_NOTE.get())==observed,"Celestial note lost observed variant");}
        }
        require(notes==1,"Sky scan did not produce one physical note");
        require(art.arcane.thaumcraft.scan.CelestialScanning.scan(astronomer)&&astronomer.getInventory().getItem(2).getCount()==1,"Daily sky limit consumed extra paper");
        require(astronomer.getInventory().getItem(1).getDamageValue()==0,"Sky scan incorrectly damaged original scribing tool");
        require(art.arcane.thaumcraft.scan.CelestialScanning.target(18000,0,-30,net.minecraft.core.Direction.WEST)==3,"Night stars orientation wrong");
        require(art.arcane.thaumcraft.scan.CelestialScanning.target(6000,0,30,net.minecraft.core.Direction.NORTH)==-1,"Downward sky scan allowed");
        System.out.println("Celestial scan passed: research gate, actual sky targeting, paper cost, physical typed note, daily duplicate limit and stars direction.");
        var benchDrops=net.minecraft.world.level.block.Block.getDrops(bench.getBlockState(),world,pos,bench);
        require(benchDrops.size()==1 && benchDrops.getFirst().is(ModItems.BLOCK_ARCANE_WORKBENCH.get()),"Workbench block had no native loot table");
        require(!crucible.processOne(new ItemStack(Items.STONE),null) || crucible.mixture().total()==5,"Unknown owner granted researched alchemy");
        System.out.println("Native starter world checks passed: datapack recipes/worldgen loaded, research gates, costs, hotbar swap, partial output, Nitor alchemy.");
        var jarRelative=new BlockPos(1,1,3);
        helper.setBlock(jarRelative,ModBlocks.JAR_NORMAL.get());
        var jar=helper.getBlockEntity(jarRelative,art.arcane.thaumcraft.blockentity.EssentiaJarBlockEntity.class);
        var jarPos=helper.absolutePos(jarRelative);
        var hit=new net.minecraft.world.phys.BlockHitResult(net.minecraft.world.phys.Vec3.atCenterOf(jarPos),net.minecraft.core.Direction.UP,jarPos,false);
        player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,art.arcane.thaumcraft.item.EssentiaPhialItem.filled("ignis"));
        jar.getBlockState().useItemOn(player.getMainHandItem(),world,player,net.minecraft.world.InteractionHand.MAIN_HAND,hit);
        require(jar.contents().amount()==10 && jar.contents().aspect().equals("ignis") && player.getMainHandItem().is(ModItems.PHIAL_EMPTY.get()),"Filled phial transfer failed");
        jar.getBlockState().useItemOn(player.getMainHandItem(),world,player,net.minecraft.world.InteractionHand.MAIN_HAND,hit);
        require(jar.contents().amount()==0 && player.getMainHandItem().get(art.arcane.thaumcraft.registry.ModComponents.ASPECT.get()).equals("ignis"),"Empty phial extraction failed");
        require(jar.insert("aer",250) && !jar.insert("ignis",1) && !jar.insert("aer",1),"Jar capacity/mixing protection failed");
        var jarDrops=net.minecraft.world.level.block.Block.getDrops(jar.getBlockState(),world,jarPos,jar);
        require(jarDrops.size()==1 && jarDrops.getFirst().get(art.arcane.thaumcraft.registry.ModComponents.ESSENTIA.get()).equals(jar.contents()),"Breaking jar lost essentia");
        var jarRestored=new art.arcane.thaumcraft.blockentity.EssentiaJarBlockEntity(jarPos,jar.getBlockState());
        jarRestored.applyComponentsFromItemStack(jarDrops.getFirst());
        require(jarRestored.contents().equals(jar.contents()),"Placing carried jar lost essentia");
        var savedJar=jar.saveWithoutMetadata(world.registryAccess());
        jarRestored.loadWithComponents(net.minecraft.world.level.storage.TagValueInput.create(net.minecraft.util.ProblemReporter.DISCARDING,world.registryAccess(),savedJar));
        require(jarRestored.contents().equals(jar.contents()),"Jar world reload lost essentia");
        System.out.println("Native essentia jar checks passed: manual transfers, capacity, aspect identity, carried contents.");
        var smelterRel=new BlockPos(0,1,3);
        helper.setBlock(smelterRel,ModBlocks.SMELTER_BASIC.get());
        helper.setBlock(new BlockPos(0,2,3),ModBlocks.ALEMBIC.get());
        helper.setBlock(new BlockPos(1,2,3),ModBlocks.TUBE.get());
        helper.setBlock(new BlockPos(2,2,3),ModBlocks.TUBE.get());
        helper.setBlock(new BlockPos(2,1,3),ModBlocks.JAR_NORMAL.get());
        var smelter=helper.getBlockEntity(smelterRel,art.arcane.thaumcraft.blockentity.EssentiaSmelterBlockEntity.class);
        var destination=helper.getBlockEntity(new BlockPos(2,1,3),art.arcane.thaumcraft.blockentity.EssentiaJarBlockEntity.class);
        var smelterPos=helper.absolutePos(smelterRel);
        crucible.empty(); // Isolate distillation flux from the preceding crucible test.
        var outputAura=NativeAura.get(world,world.getChunkAt(smelterPos));
        float fluxBefore=outputAura.flux();
        var redstone=new ItemStack(Items.REDSTONE,2);
        require(smelter.insert(redstone,false) && smelter.insert(redstone,false) && redstone.isEmpty(),"Smelter input failed");
        player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,new ItemStack(Items.COAL));
        var fuelHit=new net.minecraft.world.phys.BlockHitResult(net.minecraft.world.phys.Vec3.atCenterOf(smelterPos),net.minecraft.core.Direction.NORTH,smelterPos,false);
        player.setShiftKeyDown(true);
        smelter.getBlockState().useItemOn(player.getMainHandItem(),world,player,net.minecraft.world.InteractionHand.MAIN_HAND,fuelHit);
        player.setShiftKeyDown(false);
        require(smelter.fuel().is(Items.COAL) && player.getMainHandItem().isEmpty(),"Smelter side fuel interaction failed");
        helper.runAfterDelay(500,()->{
            require(fuelFurnace.getItem(2).is(Items.IRON_INGOT) && fuelFurnace.getItem(2).getCount()==1 && fuelFurnace.getItem(1).isEmpty(),"Alumentum failed in a real vanilla furnace");
            System.out.println("Alumentum fuel passed: 4800 ticks and iron smelting in a vanilla furnace.");
            require(smelter.input().isEmpty() && smelter.total()==0,"Smelter did not process and export input");
            require(destination.contents().aspect().equals("potentia") && destination.contents().amount()>0,"No essentia reached the jar through pipes");
            var alembic=helper.getBlockEntity(new BlockPos(0,2,3),art.arcane.thaumcraft.blockentity.EssentiaJarBlockEntity.class);
            var firstTube=helper.getBlockEntity(new BlockPos(1,2,3),art.arcane.thaumcraft.blockentity.EssentiaTubeBlockEntity.class);
            var secondTube=helper.getBlockEntity(new BlockPos(2,2,3),art.arcane.thaumcraft.blockentity.EssentiaTubeBlockEntity.class);
            System.out.println("ESSENTIA DIAGNOSTIC jar="+destination.contents()+" alembic="+alembic.contents()+" tubes="+firstTube.aspect()+"/"+secondTube.aspect()+" flux="+(outputAura.flux()-fluxBefore));
            require(destination.contents().amount()+outputAura.flux()-fluxBefore==20,"Distillation/transport lost or duplicated essence");
            require(destination.getBlockState().getValue(art.arcane.thaumcraft.block.EssentiaJarBlock.FILL)>0,"Filled jar model state did not update");
            var restoredSmelter=new art.arcane.thaumcraft.blockentity.EssentiaSmelterBlockEntity(smelterPos,smelter.getBlockState());
            restoredSmelter.loadWithComponents(net.minecraft.world.level.storage.TagValueInput.create(net.minecraft.util.ProblemReporter.DISCARDING,world.registryAccess(),smelter.saveWithoutMetadata(world.registryAccess())));
            require(restoredSmelter.burn()==smelter.burn(),"Smelter fuel timer did not persist");
            System.out.println("End-to-end essentia checks passed: fuel -> distillation -> alembic -> two tubes -> jar, conservation including flux, visible fill, persistence.");
            require(jar.contents().amount()==250 && jar.contents().aspect().equals("aer"),"Pipe extracted from a jar below minimum suction");
            helper.setBlock(new BlockPos(3,2,3),ModBlocks.ALEMBIC.get());
            helper.setBlock(new BlockPos(4,2,3),ModBlocks.TUBE.get());
            helper.setBlock(new BlockPos(4,1,3),ModBlocks.JAR_VOID.get());
            var voidJar=helper.getBlockEntity(new BlockPos(4,1,3),art.arcane.thaumcraft.blockentity.EssentiaJarBlockEntity.class);
            var overflowSource=helper.getBlockEntity(new BlockPos(3,2,3),art.arcane.thaumcraft.blockentity.EssentiaJarBlockEntity.class);
            require(voidJar.insert("ignis",250) && overflowSource.insert("ignis",5),"Void jar test setup failed");
            helper.runAfterDelay(100,()->{
                require(voidJar.contents().amount()==250 && overflowSource.contents().amount()==0,"Full void jar did not absorb excess essentia");
                System.out.println("Void overflow and full-jar suction protection passed.");
                helper.succeed();
            });
        });
    }
}


