package art.arcane.thaumcraft.verification;

import art.arcane.thaumcraft.ModBlocks;
import art.arcane.thaumcraft.block.InfernalFurnaceBlock;
import art.arcane.thaumcraft.blockentity.CrucibleBlockEntity;
import art.arcane.thaumcraft.blockentity.EverfullUrnBlockEntity;
import art.arcane.thaumcraft.blockentity.InfernalFurnaceBlockEntity;
import art.arcane.thaumcraft.blockentity.PedestalBlockEntity;
import art.arcane.thaumcraft.registry.ModCapabilities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HopperBlock;
import net.minecraft.world.level.block.entity.HopperBlockEntity;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.Transaction;

/** Interop through standard NeoForge capabilities (hoppers/pipes/other mods). */
public final class CompatCapabilitiesVerification {
    private static void check(boolean v, String m) { if (!v) throw new AssertionError(m); }

    public static void test(GameTestHelper h) {
        var world = h.getLevel();
        // --- infernal furnace: top-only insertion of smeltables, no extraction
        BlockPos fp = new BlockPos(2, 2, 2);
        h.setBlock(fp, ModBlocks.INFERNAL_FURNACE.get().defaultBlockState().setValue(InfernalFurnaceBlock.FACING, Direction.NORTH));
        var furnace = h.getBlockEntity(fp, InfernalFurnaceBlockEntity.class);
        var top = world.getCapability(Capabilities.Item.BLOCK, h.absolutePos(fp), Direction.UP);
        var side = world.getCapability(Capabilities.Item.BLOCK, h.absolutePos(fp), Direction.EAST);
        check(top != null && side != null && side.size() == 0, "Infernal furnace sided capability");
        try (var tx = Transaction.openRoot()) {
            check(top.insert(ItemResource.of(Items.STICK), 1, tx) == 0, "Infernal furnace accepted unsmeltable");
            check(top.insert(ItemResource.of(Items.RAW_IRON), 3, tx) == 3, "Infernal furnace rejected raw iron");
            check(top.extract(ItemResource.of(Items.RAW_IRON), 1, tx) == 0, "Infernal furnace allowed extraction");
            tx.commit();
        }
        int iron = 0; for (int i = 0; i < furnace.getContainerSize(); i++) if (furnace.getItem(i).is(Items.RAW_IRON)) iron += furnace.getItem(i).getCount();
        check(iron == 3, "Infernal furnace stored " + iron);

        // --- live vanilla hopper (pipe stand-in) into a pedestal through the capability
        BlockPos pp = new BlockPos(6, 1, 2);
        h.setBlock(pp, ModBlocks.PEDESTAL_ARCANE.get());
        h.setBlock(pp.above(), Blocks.HOPPER.defaultBlockState().setValue(HopperBlock.FACING, Direction.DOWN));
        h.getBlockEntity(pp.above(), HopperBlockEntity.class).setItem(0, new ItemStack(Items.DIAMOND, 2));

        // --- crucible: water via fluid capability, no drain
        BlockPos cp = new BlockPos(2, 1, 6);
        h.setBlock(cp, ModBlocks.CRUCIBLE.get());
        var crucible = h.getBlockEntity(cp, CrucibleBlockEntity.class);
        var cfl = world.getCapability(Capabilities.Fluid.BLOCK, h.absolutePos(cp), Direction.UP);
        check(cfl != null, "Crucible fluid capability missing");
        try (var tx = Transaction.openRoot()) {
            check(cfl.insert(FluidResource.of(Fluids.LAVA), 100, tx) == 0, "Crucible accepted lava");
            check(cfl.insert(FluidResource.of(Fluids.WATER), 600, tx) == 600, "Crucible rejected water");
        }
        check(crucible.mixture().water() == 0, "Aborted crucible fill kept water");
        try (var tx = Transaction.openRoot()) {
            check(cfl.insert(FluidResource.of(Fluids.WATER), 1500, tx) == 1000, "Crucible capacity");
            check(cfl.extract(FluidResource.of(Fluids.WATER), 100, tx) == 0, "Crucible drained");
            tx.commit();
        }
        check(crucible.mixture().water() == 1000, "Crucible water " + crucible.mixture().water());

        // --- everfull urn: water provider through the fluid capability (top), fill refused
        BlockPos up = new BlockPos(6, 1, 6);
        h.setBlock(up, ModBlocks.EVERFULL_URN.get());
        var urn = h.getBlockEntity(up, EverfullUrnBlockEntity.class);
        try (var tx = Transaction.openRoot()) { urn.tank.insert(0, FluidResource.of(Fluids.WATER), 1000, tx); tx.commit(); }
        var ufl = world.getCapability(Capabilities.Fluid.BLOCK, h.absolutePos(up), Direction.UP);
        check(ufl != null && world.getCapability(Capabilities.Fluid.BLOCK, h.absolutePos(up), Direction.NORTH) == null, "Urn sided fluid capability");
        try (var tx = Transaction.openRoot()) {
            check(ufl.insert(FluidResource.of(Fluids.WATER), 100, tx) == 0, "Urn accepted fill");
            check(ufl.extract(FluidResource.of(Fluids.WATER), 250, tx) == 250, "Urn did not provide water");
            tx.commit();
        }
        check(urn.amount() == 750, "Urn amount " + urn.amount());

        // --- golem builder output is extract-only, essentia capability on jars
        BlockPos gp = new BlockPos(10, 1, 2);
        h.setBlock(gp, ModBlocks.GOLEM_BUILDER.get());
        var gcap = world.getCapability(Capabilities.Item.BLOCK, h.absolutePos(gp), Direction.UP);
        check(gcap != null, "Golem builder item capability missing");
        try (var tx = Transaction.openRoot()) {
            check(gcap.insert(ItemResource.of(art.arcane.thaumcraft.ModItems.GOLEM_PLACER.get()), 1, tx) == 0, "Golem builder accepted insertion");
        }
        BlockPos jp = new BlockPos(10, 1, 6);
        h.setBlock(jp, ModBlocks.JAR_NORMAL.get());
        check(world.getCapability(ModCapabilities.ESSENTIA, h.absolutePos(jp), Direction.UP) != null, "Jar essentia capability missing");

        h.runAfterDelay(30, () -> {
            var ped = h.getBlockEntity(pp, PedestalBlockEntity.class);
            check(ped.getItem(0).is(Items.DIAMOND) && ped.getItem(0).getCount() == 1, "Hopper did not feed pedestal: " + ped.getItem(0));
            System.out.println("Compat capabilities passed: infernal furnace, pedestal hopper, crucible water, everfull urn, golem builder, essentia.");
            h.succeed();
        });
    }
}
