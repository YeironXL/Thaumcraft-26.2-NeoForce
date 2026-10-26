package art.arcane.thaumcraft.registry;

import art.arcane.thaumcraft.Thaumcraft;
import art.arcane.thaumcraft.aspect.AspectRegistry;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.DeferredHolder;

public final class ModComponents {
    private static final DeferredRegister<DataComponentType<?>> TYPES = DeferredRegister.create(Registries.DATA_COMPONENT_TYPE, Thaumcraft.MOD_ID);
    /** ItemGolemPlacer NBT "props": the packed GolemProperties. */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Long>> GOLEM_PROPS = TYPES.register("golem_props", () -> DataComponentType.<Long>builder()
            .persistent(Codec.LONG).networkSynchronized(ByteBufCodecs.VAR_LONG).build());
    /** ItemGolemPlacer NBT "xp": rank experience of a smart golem. */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> GOLEM_XP = TYPES.register("golem_xp", () -> DataComponentType.<Integer>builder()
            .persistent(Codec.INT).networkSynchronized(ByteBufCodecs.VAR_INT).build());
    /** TileJarBrain xp, kept on the dropped jar. */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> BRAIN_XP = TYPES.register("brain_xp", () -> DataComponentType.<Integer>builder()
            .persistent(Codec.INT).networkSynchronized(ByteBufCodecs.VAR_INT).build());
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> CELESTIAL_NOTE = TYPES.register("celestial_note", () -> DataComponentType.<Integer>builder()
            .persistent(Codec.intRange(0,12)).networkSynchronized(ByteBufCodecs.fromCodec(Codec.intRange(0,12))).build());
    /** RechargeHelper "tc.charge": stored vis charge of an IRechargable item. */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> CHARGE = TYPES.register("charge", () -> DataComponentType.<Integer>builder()
            .persistent(Codec.INT).networkSynchronized(ByteBufCodecs.VAR_INT).build());
    /** TC6 item variant: ItemAmuletVis metadata (0 found, 1 crafted) and ItemVerdantCharm NBT byte "type" (0 normal, 1 life, 2 sustain). Item models select on it. */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> VARIANT = TYPES.register("variant", () -> DataComponentType.<Integer>builder()
            .persistent(Codec.intRange(0, 255)).networkSynchronized(ByteBufCodecs.VAR_INT).build());
    /** BlockMirrorItem / ItemHandMirror NBT linkX/linkY/linkZ/linkDim: the mirror this item is linked to (metadata 1 = linked). */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<net.minecraft.core.GlobalPos>> MIRROR_LINK = TYPES.register("mirror_link", () -> DataComponentType.<net.minecraft.core.GlobalPos>builder()
            .persistent(net.minecraft.core.GlobalPos.CODEC).networkSynchronized(net.minecraft.core.GlobalPos.STREAM_CODEC).build());
    /** ItemGrappleGun "loaded". */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<net.minecraft.util.Unit>> GRAPPLE_LOADED = TYPES.register("grapple_loaded", () -> DataComponentType.<net.minecraft.util.Unit>builder()
            .persistent(net.minecraft.util.Unit.CODEC).networkSynchronized(net.minecraft.network.codec.StreamCodec.unit(net.minecraft.util.Unit.INSTANCE)).build());
    /** TC6 jar "AspectFilter" (label) kept on the dropped jar item. */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<String>> JAR_FILTER = TYPES.register("jar_filter", () -> DataComponentType.<String>builder()
            .persistent(Codec.STRING).networkSynchronized(ByteBufCodecs.STRING_UTF8).build());
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<String>> ASPECT = TYPES.register("aspect", () -> DataComponentType.<String>builder()
            .persistent(Codec.STRING.validate(value -> AspectRegistry.get(value) == null ? DataResult.error(() -> "Unknown aspect " + value) : DataResult.success(value)))
            .networkSynchronized(ByteBufCodecs.STRING_UTF8).build());
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<art.arcane.thaumcraft.essentia.EssentiaContents>> ESSENTIA = TYPES.register("essentia", () -> DataComponentType.<art.arcane.thaumcraft.essentia.EssentiaContents>builder()
            .persistent(art.arcane.thaumcraft.essentia.EssentiaContents.CODEC)
            .networkSynchronized(ByteBufCodecs.fromCodec(art.arcane.thaumcraft.essentia.EssentiaContents.CODEC)).build());
    /** ItemFocus "package" NBT: the serialized FocusPackage. */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<net.minecraft.nbt.CompoundTag>> FOCUS_PACKAGE = TYPES.register("focus_package", () -> DataComponentType.<net.minecraft.nbt.CompoundTag>builder()
            .persistent(net.minecraft.nbt.CompoundTag.CODEC).networkSynchronized(ByteBufCodecs.COMPOUND_TAG).build());
    /** ItemCaster "focus": the mounted focus stack. */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<net.minecraft.world.item.component.ItemContainerContents>> CASTER_FOCUS = TYPES.register("caster_focus", () -> DataComponentType.<net.minecraft.world.item.component.ItemContainerContents>builder()
            .persistent(net.minecraft.world.item.component.ItemContainerContents.CODEC).networkSynchronized(net.minecraft.world.item.component.ItemContainerContents.STREAM_CODEC).build());
    /** ItemCaster "picked": block chosen for exchange/plan foci. */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<net.minecraft.world.item.component.ItemContainerContents>> PICKED_BLOCK = TYPES.register("picked_block", () -> DataComponentType.<net.minecraft.world.item.component.ItemContainerContents>builder()
            .persistent(net.minecraft.world.item.component.ItemContainerContents.CODEC).networkSynchronized(net.minecraft.world.item.component.ItemContainerContents.STREAM_CODEC).build());
    /** ItemFocusPouch inventory (18 slots). */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<net.minecraft.world.item.component.ItemContainerContents>> POUCH = TYPES.register("pouch", () -> DataComponentType.<net.minecraft.world.item.component.ItemContainerContents>builder()
            .persistent(net.minecraft.world.item.component.ItemContainerContents.CODEC)
            .networkSynchronized(net.minecraft.world.item.component.ItemContainerContents.STREAM_CODEC).build());
    /** ItemCaster architect area settings (areax/areay/areaz/aread). */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<java.util.List<Integer>>> CASTER_AREA = TYPES.register("caster_area", () -> DataComponentType.<java.util.List<Integer>>builder()
            .persistent(Codec.INT.listOf()).networkSynchronized(ByteBufCodecs.fromCodec(Codec.INT.listOf())).build());

    /** TC6 "infench" NBT list (EnumInfusionEnchantment id + level), in the order they were added. */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<java.util.List<art.arcane.thaumcraft.infusion.InfusionEnchantment.Entry>>> INFUSION_ENCHANTMENTS = TYPES.register("infusion_enchantments", () -> DataComponentType.<java.util.List<art.arcane.thaumcraft.infusion.InfusionEnchantment.Entry>>builder()
            .persistent(art.arcane.thaumcraft.infusion.InfusionEnchantment.Entry.CODEC.listOf()).networkSynchronized(ByteBufCodecs.fromCodec(art.arcane.thaumcraft.infusion.InfusionEnchantment.Entry.CODEC.listOf())).build());
    /** TC6 "TC.RUNIC" byte: runic shielding charge added by InfusionRunicAugmentRecipe. */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> RUNIC_CHARGE = TYPES.register("runic_charge", () -> DataComponentType.<Integer>builder()
            .persistent(Codec.intRange(0, 127)).networkSynchronized(ByteBufCodecs.VAR_INT).build());

    public static void register(IEventBus bus) { TYPES.register(bus); }
    private ModComponents() {}
}
