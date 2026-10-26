package art.arcane.thaumcraft.registry;

import art.arcane.thaumcraft.essentia.EssentiaPort;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.world.Container;
import net.minecraft.world.WorldlyContainer;
import net.neoforged.neoforge.capabilities.BlockCapability;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.transfer.DelegatingResourceHandler;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.item.VanillaContainerWrapper;
import net.neoforged.neoforge.transfer.item.WorldlyContainerWrapper;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;
import org.jspecify.annotations.Nullable;

/**
 * Standard NeoForge capabilities so pipes, hoppers, golems and other mods' machines can reach Thaumcraft blocks.
 * See docs/API_COMPAT.md.
 */
public final class ModCapabilities {
    /** Public essentia capability ("thaumcraft:essentia"): tubes connect to any block exposing it. */
    public static final BlockCapability<EssentiaPort, @Nullable Direction> ESSENTIA =
            BlockCapability.createSided(Identifier.fromNamespaceAndPath("thaumcraft", "essentia"), EssentiaPort.class);

    public static void register(RegisterCapabilitiesEvent event) {
        // ---- fluids ----
        event.registerBlockEntity(Capabilities.Fluid.BLOCK, ModBlockEntities.EVERFULL_URN.get(), (urn, side) -> side == null || side == Direction.UP ? urn.drainOnly : null);
        event.registerBlockEntity(Capabilities.Fluid.BLOCK, ModBlockEntities.SPA.get(), (spa, side) -> spa.tank);
        event.registerBlockEntity(Capabilities.Fluid.BLOCK, ModBlockEntities.CRUCIBLE.get(), (c, side) -> c.waterInput);

        // ---- items: sided machines (ISidedInventory in TC6) ----
        event.registerBlockEntity(Capabilities.Item.BLOCK, ModBlockEntities.INFERNAL_FURNACE.get(), (f, side) -> sided(f, side));
        event.registerBlockEntity(Capabilities.Item.BLOCK, ModBlockEntities.SPA.get(), (spa, side) -> sided(spa, side));
        event.registerBlockEntity(Capabilities.Item.BLOCK, ModBlockEntities.ESSENTIA_SMELTER.get(), (s, side) -> sided(s, side));
        event.registerBlockEntity(Capabilities.Item.BLOCK, ModBlockEntities.DECONSTRUCTION_TABLE.get(),
                (t, side) -> new WorldlyContainerWrapper(t, side == null ? Direction.UP : side));

        // ---- items: plain inventories ----
        event.registerBlockEntity(Capabilities.Item.BLOCK, ModBlockEntities.HUNGRY_CHEST.get(), (chest, side) -> VanillaContainerWrapper.of(chest));
        event.registerBlockEntity(Capabilities.Item.BLOCK, ModBlockEntities.PEDESTAL.get(), (p, side) -> VanillaContainerWrapper.of(p));
        event.registerBlockEntity(Capabilities.Item.BLOCK, ModBlockEntities.RECHARGE_PEDESTAL.get(), (p, side) -> VanillaContainerWrapper.of(p));
        event.registerBlockEntity(Capabilities.Item.BLOCK, ModBlockEntities.MIRROR.get(), (m, side) -> VanillaContainerWrapper.of(m));
        // catalyst / potion slots: automation may load them but not pull them out
        event.registerBlockEntity(Capabilities.Item.BLOCK, ModBlockEntities.THAUMATORIUM.get(), (t, side) -> insertOnly(t));
        event.registerBlockEntity(Capabilities.Item.BLOCK, ModBlockEntities.POTION_SPRAYER.get(), (p, side) -> insertOnly(p));
        // golem builder output: extraction only
        event.registerBlockEntity(Capabilities.Item.BLOCK, ModBlockEntities.GOLEM_BUILDER.get(), (g, side) -> extractOnly(g));

        // ---- energy ----
        event.registerBlockEntity(Capabilities.Energy.BLOCK, ModBlockEntities.VIS_GENERATOR.get(), (be, side) -> side == be.getBlockState().getValue(art.arcane.thaumcraft.block.VisGeneratorBlock.FACING) ? be.handler : null);

        // ---- essentia ----
        event.registerBlockEntity(ESSENTIA, ModBlockEntities.ESSENTIA_JAR.get(), ModCapabilities::port);
        event.registerBlockEntity(ESSENTIA, ModBlockEntities.ESSENTIA_TUBE.get(), ModCapabilities::port);
        event.registerBlockEntity(ESSENTIA, ModBlockEntities.ESSENTIA_CENTRIFUGE.get(), ModCapabilities::port);
        event.registerBlockEntity(ESSENTIA, ModBlockEntities.THAUMATORIUM.get(), ModCapabilities::port);
        event.registerBlockEntity(ESSENTIA, ModBlockEntities.THAUMATORIUM_TOP.get(), ModCapabilities::port);
        event.registerBlockEntity(ESSENTIA, ModBlockEntities.POTION_SPRAYER.get(), ModCapabilities::port);
        event.registerBlockEntity(ESSENTIA, ModBlockEntities.GOLEM_BUILDER.get(), ModCapabilities::port);
    }

    private static @Nullable EssentiaPort port(EssentiaPort p, @Nullable Direction side) { return side == null || p.connects(side) ? p : null; }

    private static ResourceHandler<ItemResource> sided(WorldlyContainer c, @Nullable Direction side) {
        return side == null ? VanillaContainerWrapper.of(c) : new WorldlyContainerWrapper(c, side);
    }

    public static ResourceHandler<ItemResource> insertOnly(Container c) {
        return new DelegatingResourceHandler<>(VanillaContainerWrapper.of(c)) {
            @Override public int extract(int index, ItemResource resource, int amount, TransactionContext tx) { return 0; }
            @Override public int extract(ItemResource resource, int amount, TransactionContext tx) { return 0; }
        };
    }

    public static ResourceHandler<ItemResource> extractOnly(Container c) {
        return new DelegatingResourceHandler<>(VanillaContainerWrapper.of(c)) {
            @Override public int insert(int index, ItemResource resource, int amount, TransactionContext tx) { return 0; }
            @Override public int insert(ItemResource resource, int amount, TransactionContext tx) { return 0; }
        };
    }

    private ModCapabilities() {}
}
