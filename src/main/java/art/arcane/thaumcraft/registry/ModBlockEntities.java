package art.arcane.thaumcraft.registry;

import art.arcane.thaumcraft.Thaumcraft;
import art.arcane.thaumcraft.blockentity.ThaumcraftMachineBlockEntity;
import art.arcane.thaumcraft.ModBlocks;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModBlockEntities {
    public static final DeferredRegister<BlockEntityType<?>> TYPES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, Thaumcraft.MOD_ID);

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<ThaumcraftMachineBlockEntity>> MACHINE =
            TYPES.register("thaumcraft_machine", () -> new BlockEntityType<>(
                    ThaumcraftMachineBlockEntity::new,
                    ModBlocks.INFUSION_MATRIX.get(),
                    ModBlocks.THAUMATORIUM.get()
            ));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<art.arcane.thaumcraft.blockentity.PedestalBlockEntity>> PEDESTAL =
            TYPES.register("pedestal", () -> new BlockEntityType<>(art.arcane.thaumcraft.blockentity.PedestalBlockEntity::new, ModBlocks.PEDESTAL_ARCANE.get(), ModBlocks.PEDESTAL_ANCIENT.get(), ModBlocks.PEDESTAL_ELDRITCH.get()));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<art.arcane.thaumcraft.blockentity.RechargePedestalBlockEntity>> RECHARGE_PEDESTAL =
            TYPES.register("recharge_pedestal", () -> new BlockEntityType<>(art.arcane.thaumcraft.blockentity.RechargePedestalBlockEntity::new, ModBlocks.RECHARGE_PEDESTAL.get()));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<art.arcane.thaumcraft.blockentity.VisGeneratorBlockEntity>> VIS_GENERATOR =
            TYPES.register("vis_generator", () -> new BlockEntityType<>(art.arcane.thaumcraft.blockentity.VisGeneratorBlockEntity::new, ModBlocks.VIS_GENERATOR.get()));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<art.arcane.thaumcraft.blockentity.ManaPodBlockEntity>> MANA_POD =
            TYPES.register("mana_pod", () -> new BlockEntityType<>(art.arcane.thaumcraft.blockentity.ManaPodBlockEntity::new, ModBlocks.MANA_POD.get()));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<art.arcane.thaumcraft.blockentity.DeconstructionTableBlockEntity>> DECONSTRUCTION_TABLE =
            TYPES.register("deconstruction_table", () -> new BlockEntityType<>(art.arcane.thaumcraft.blockentity.DeconstructionTableBlockEntity::new, ModBlocks.DECONSTRUCTION_TABLE.get()));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<art.arcane.thaumcraft.blockentity.SpaBlockEntity>> SPA =
            TYPES.register("spa", () -> new BlockEntityType<>(art.arcane.thaumcraft.blockentity.SpaBlockEntity::new, ModBlocks.SPA.get()));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<art.arcane.thaumcraft.blockentity.EverfullUrnBlockEntity>> EVERFULL_URN =
            TYPES.register("everfull_urn", () -> new BlockEntityType<>(art.arcane.thaumcraft.blockentity.EverfullUrnBlockEntity::new, ModBlocks.EVERFULL_URN.get()));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<art.arcane.thaumcraft.blockentity.PotionSprayerBlockEntity>> POTION_SPRAYER =
            TYPES.register("potion_sprayer", () -> new BlockEntityType<>(art.arcane.thaumcraft.blockentity.PotionSprayerBlockEntity::new, ModBlocks.POTION_SPRAYER.get()));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<art.arcane.thaumcraft.golems.builder.GolemBuilderBlockEntity>> GOLEM_BUILDER =
            TYPES.register("golem_builder", () -> new BlockEntityType<>(art.arcane.thaumcraft.golems.builder.GolemBuilderBlockEntity::new, ModBlocks.GOLEM_BUILDER.get()));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<art.arcane.thaumcraft.blockentity.ThaumatoriumBlockEntity>> THAUMATORIUM =
            TYPES.register("thaumatorium", () -> new BlockEntityType<>(art.arcane.thaumcraft.blockentity.ThaumatoriumBlockEntity::new, ModBlocks.THAUMATORIUM.get()));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<art.arcane.thaumcraft.blockentity.ThaumatoriumTopBlockEntity>> THAUMATORIUM_TOP =
            TYPES.register("thaumatorium_top", () -> new BlockEntityType<>(art.arcane.thaumcraft.blockentity.ThaumatoriumTopBlockEntity::new, ModBlocks.THAUMATORIUM_TOP.get()));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<art.arcane.thaumcraft.blockentity.CondenserBlockEntity>> CONDENSER =
            TYPES.register("condenser", () -> new BlockEntityType<>(art.arcane.thaumcraft.blockentity.CondenserBlockEntity::new, ModBlocks.CONDENSER.get()));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<art.arcane.thaumcraft.blockentity.EssentiaTransfuserBlockEntity>> ESSENTIA_TRANSFUSER =
            TYPES.register("essentia_transfuser", () -> new BlockEntityType<>(art.arcane.thaumcraft.blockentity.EssentiaTransfuserBlockEntity::new, ModBlocks.ESSENTIA_INPUT.get(), ModBlocks.ESSENTIA_OUTPUT.get()));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<art.arcane.thaumcraft.blockentity.TcBannerBlockEntity>> BANNER =
            TYPES.register("banner", () -> new BlockEntityType<>(art.arcane.thaumcraft.blockentity.TcBannerBlockEntity::new, ModBlocks.BANNER_BLACK.get(),ModBlocks.BANNER_BLUE.get(),ModBlocks.BANNER_BROWN.get(),ModBlocks.BANNER_CRIMSON_CULT.get(),ModBlocks.BANNER_CYAN.get(),ModBlocks.BANNER_GRAY.get(),ModBlocks.BANNER_GREEN.get(),ModBlocks.BANNER_LIGHTBLUE.get(),ModBlocks.BANNER_LIME.get(),ModBlocks.BANNER_MAGENTA.get(),ModBlocks.BANNER_ORANGE.get(),ModBlocks.BANNER_PINK.get(),ModBlocks.BANNER_PURPLE.get(),ModBlocks.BANNER_RED.get(),ModBlocks.BANNER_SILVER.get(),ModBlocks.BANNER_WHITE.get(),ModBlocks.BANNER_YELLOW.get()));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<art.arcane.thaumcraft.blockentity.BrainJarBlockEntity>> BRAIN_JAR =
            TYPES.register("brain_jar", () -> new BlockEntityType<>(art.arcane.thaumcraft.blockentity.BrainJarBlockEntity::new, ModBlocks.JAR_BRAIN.get()));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<art.arcane.thaumcraft.blockentity.BarrierStoneBlockEntity>> BARRIER_STONE =
            TYPES.register("barrier_stone", () -> new BlockEntityType<>(art.arcane.thaumcraft.blockentity.BarrierStoneBlockEntity::new, ModBlocks.PAVING_STONE_BARRIER.get()));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<art.arcane.thaumcraft.blockentity.VoidSiphonBlockEntity>> VOID_SIPHON =
            TYPES.register("void_siphon", () -> new BlockEntityType<>(art.arcane.thaumcraft.blockentity.VoidSiphonBlockEntity::new, ModBlocks.VOID_SIPHON.get()));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<art.arcane.thaumcraft.blockentity.EssentiaLampBlockEntity>> LAMP_ESSENTIA =
            TYPES.register("lamp_essentia", () -> new BlockEntityType<>(art.arcane.thaumcraft.blockentity.EssentiaLampBlockEntity::new, ModBlocks.LAMP_GROWTH.get(), ModBlocks.LAMP_FERTILITY.get()));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<art.arcane.thaumcraft.blockentity.ArcaneLampBlockEntity>> LAMP_ARCANE =
            TYPES.register("lamp_arcane", () -> new BlockEntityType<>(art.arcane.thaumcraft.blockentity.ArcaneLampBlockEntity::new, ModBlocks.LAMP_ARCANE.get()));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<art.arcane.thaumcraft.blockentity.StabilizerBlockEntity>> STABILIZER =
            TYPES.register("stabilizer", () -> new BlockEntityType<>(art.arcane.thaumcraft.blockentity.StabilizerBlockEntity::new, ModBlocks.STABILIZER.get()));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<art.arcane.thaumcraft.blockentity.PatternCrafterBlockEntity>> PATTERN_CRAFTER =
            TYPES.register("pattern_crafter", () -> new BlockEntityType<>(art.arcane.thaumcraft.blockentity.PatternCrafterBlockEntity::new, ModBlocks.PATTERN_CRAFTER.get()));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<art.arcane.thaumcraft.blockentity.InfernalFurnaceBlockEntity>> INFERNAL_FURNACE =
            TYPES.register("infernal_furnace", () -> new BlockEntityType<>(art.arcane.thaumcraft.blockentity.InfernalFurnaceBlockEntity::new, ModBlocks.INFERNAL_FURNACE.get()));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<art.arcane.thaumcraft.blockentity.DioptraBlockEntity>> DIOPTRA =
            TYPES.register("dioptra", () -> new BlockEntityType<>(art.arcane.thaumcraft.blockentity.DioptraBlockEntity::new, ModBlocks.DIOPTRA.get()));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<art.arcane.thaumcraft.blockentity.LevitatorBlockEntity>> LEVITATOR =
            TYPES.register("levitator", () -> new BlockEntityType<>(art.arcane.thaumcraft.blockentity.LevitatorBlockEntity::new, ModBlocks.LEVITATOR.get()));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<art.arcane.thaumcraft.blockentity.RedstoneRelayBlockEntity>> REDSTONE_RELAY =
            TYPES.register("redstone_relay", () -> new BlockEntityType<>(art.arcane.thaumcraft.blockentity.RedstoneRelayBlockEntity::new, ModBlocks.REDSTONE_RELAY.get()));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<art.arcane.thaumcraft.blockentity.MirrorBlockEntity>> MIRROR =
            TYPES.register("mirror", () -> new BlockEntityType<>(art.arcane.thaumcraft.blockentity.MirrorBlockEntity::new, ModBlocks.MIRROR.get()));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<art.arcane.thaumcraft.blockentity.EssentiaMirrorBlockEntity>> MIRROR_ESSENTIA =
            TYPES.register("mirror_essentia", () -> new BlockEntityType<>(art.arcane.thaumcraft.blockentity.EssentiaMirrorBlockEntity::new, ModBlocks.MIRROR_ESSENTIA.get()));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<art.arcane.thaumcraft.blockentity.ArcaneEarBlockEntity>> ARCANE_EAR =
            TYPES.register("arcane_ear", () -> new BlockEntityType<>(art.arcane.thaumcraft.blockentity.ArcaneEarBlockEntity::new, ModBlocks.ARCANE_EAR.get(), ModBlocks.ARCANE_EAR_TOGGLE.get()));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<art.arcane.thaumcraft.blockentity.InfusionMatrixBlockEntity>> INFUSION_MATRIX =
            TYPES.register("infusion_matrix", () -> new BlockEntityType<>(art.arcane.thaumcraft.blockentity.InfusionMatrixBlockEntity::new, ModBlocks.INFUSION_MATRIX.get()));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<art.arcane.thaumcraft.blockentity.AuraNodeBlockEntity>> AURA_NODE =
            TYPES.register("aura_node", () -> new BlockEntityType<>(art.arcane.thaumcraft.blockentity.AuraNodeBlockEntity::new, ModBlocks.AURA_NODE.get()));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<art.arcane.thaumcraft.blockentity.HoleBlockEntity>> HOLE =
            TYPES.register("hole", () -> new BlockEntityType<>(art.arcane.thaumcraft.blockentity.HoleBlockEntity::new, ModBlocks.HOLE.get()));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<art.arcane.thaumcraft.block.eldritch.EldritchAltarBlockEntity>> ELDRITCH_ALTAR =
            TYPES.register("eldritch_altar", () -> new BlockEntityType<>(art.arcane.thaumcraft.block.eldritch.EldritchAltarBlockEntity::new, ModBlocks.ELDRITCH_ALTAR.get()));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<art.arcane.thaumcraft.block.eldritch.EldritchPortalBlockEntity>> ELDRITCH_PORTAL =
            TYPES.register("eldritch_portal", () -> new BlockEntityType<>(art.arcane.thaumcraft.block.eldritch.EldritchPortalBlockEntity::new, ModBlocks.ELDRITCH_PORTAL.get()));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<art.arcane.thaumcraft.block.eldritch.EldritchLockBlockEntity>> ELDRITCH_LOCK =
            TYPES.register("eldritch_lock", () -> new BlockEntityType<>(art.arcane.thaumcraft.block.eldritch.EldritchLockBlockEntity::new, ModBlocks.ELDRITCH_LOCK.get()));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<art.arcane.thaumcraft.block.eldritch.EldritchCrabSpawnerBlockEntity>> ELDRITCH_CRAB_SPAWNER =
            TYPES.register("eldritch_crab_spawner", () -> new BlockEntityType<>(art.arcane.thaumcraft.block.eldritch.EldritchCrabSpawnerBlockEntity::new, ModBlocks.ELDRITCH_CRAB_SPAWNER.get()));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<art.arcane.thaumcraft.block.eldritch.EldritchRuneStoneBlockEntity>> ELDRITCH_RUNE_STONE =
            TYPES.register("eldritch_rune_stone", () -> new BlockEntityType<>(art.arcane.thaumcraft.block.eldritch.EldritchRuneStoneBlockEntity::new, ModBlocks.ELDRITCH_RUNE_STONE.get()));
    public static void register(IEventBus bus) {
        TYPES.register(bus);
    }
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<art.arcane.thaumcraft.blockentity.ArcaneWorkbenchBlockEntity>> ARCANE_WORKBENCH =
            TYPES.register("arcane_workbench", () -> new BlockEntityType<>(art.arcane.thaumcraft.blockentity.ArcaneWorkbenchBlockEntity::new,ModBlocks.ARCANE_WORKBENCH.get()));
    public static final DeferredHolder<BlockEntityType<?>,BlockEntityType<art.arcane.thaumcraft.blockentity.CrucibleBlockEntity>> CRUCIBLE =
            TYPES.register("crucible",() -> new BlockEntityType<>(art.arcane.thaumcraft.blockentity.CrucibleBlockEntity::new,ModBlocks.CRUCIBLE.get()));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<art.arcane.thaumcraft.blockentity.HungryChestBlockEntity>> HUNGRY_CHEST =
            TYPES.register("hungry_chest", () -> new BlockEntityType<>(
                    art.arcane.thaumcraft.blockentity.HungryChestBlockEntity::new, ModBlocks.HUNGRY_CHEST.get()));

    public static final DeferredHolder<BlockEntityType<?>,BlockEntityType<art.arcane.thaumcraft.blockentity.EssentiaJarBlockEntity>> ESSENTIA_JAR =
        TYPES.register("essentia_jar", () -> new BlockEntityType<>(art.arcane.thaumcraft.blockentity.EssentiaJarBlockEntity::new,ModBlocks.JAR_NORMAL.get(),ModBlocks.JAR_VOID.get(),ModBlocks.ALEMBIC.get()));
    public static final DeferredHolder<BlockEntityType<?>,BlockEntityType<art.arcane.thaumcraft.blockentity.EssentiaTubeBlockEntity>> ESSENTIA_TUBE =
        TYPES.register("essentia_tube",()->new BlockEntityType<>(art.arcane.thaumcraft.blockentity.EssentiaTubeBlockEntity::new,ModBlocks.TUBE.get(),ModBlocks.TUBE_VALVE.get(),ModBlocks.TUBE_FILTER.get(),ModBlocks.TUBE_RESTRICT.get(),ModBlocks.TUBE_ONEWAY.get(),ModBlocks.TUBE_BUFFER.get()));
    public static final DeferredHolder<BlockEntityType<?>,BlockEntityType<art.arcane.thaumcraft.blockentity.EssentiaSmelterBlockEntity>> ESSENTIA_SMELTER =
        TYPES.register("essentia_smelter",()->new BlockEntityType<>(art.arcane.thaumcraft.blockentity.EssentiaSmelterBlockEntity::new,ModBlocks.SMELTER_BASIC.get(),ModBlocks.SMELTER_THAUMIUM.get(),ModBlocks.SMELTER_VOID.get()));
    public static final DeferredHolder<BlockEntityType<?>,BlockEntityType<art.arcane.thaumcraft.blockentity.EssentiaCentrifugeBlockEntity>> ESSENTIA_CENTRIFUGE =
        TYPES.register("essentia_centrifuge",()->new BlockEntityType<>(art.arcane.thaumcraft.blockentity.EssentiaCentrifugeBlockEntity::new,ModBlocks.CENTRIFUGE.get()));
    public static final DeferredHolder<BlockEntityType<?>,BlockEntityType<art.arcane.thaumcraft.blockentity.ResearchTableBlockEntity>> RESEARCH_TABLE = TYPES.register("research_table",()->new BlockEntityType<>(art.arcane.thaumcraft.blockentity.ResearchTableBlockEntity::new,ModBlocks.RESEARCH_TABLE.get()));
    public static final DeferredHolder<BlockEntityType<?>,BlockEntityType<art.arcane.thaumcraft.blockentity.FocalManipulatorBlockEntity>> FOCAL_MANIPULATOR = TYPES.register("focal_manipulator",()->new BlockEntityType<>(art.arcane.thaumcraft.blockentity.FocalManipulatorBlockEntity::new,ModBlocks.WAND_WORKBENCH.get()));
    private ModBlockEntities() {}
}
