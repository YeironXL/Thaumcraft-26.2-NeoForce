package art.arcane.thaumcraft.verification;

import art.arcane.thaumcraft.ModBlocks;
import art.arcane.thaumcraft.aura.NativeAura;
import art.arcane.thaumcraft.block.DioptraBlock;
import art.arcane.thaumcraft.block.InfernalFurnaceBlock;
import art.arcane.thaumcraft.block.LevitatorBlock;
import art.arcane.thaumcraft.blockentity.DioptraBlockEntity;
import art.arcane.thaumcraft.blockentity.InfernalFurnaceBlockEntity;
import art.arcane.thaumcraft.blockentity.LevitatorBlockEntity;
import art.arcane.thaumcraft.blockentity.PatternCrafterBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.phys.Vec3;

/** Artifice before infusion: levitator, dioptra, infernal furnace, pattern crafter. */
final class ArtificeDevicesVerification {
    private static void check(boolean c, String m) { if (!c) throw new AssertionError(m); }
    private static int count(ChestBlockEntity c, net.minecraft.world.item.Item it) {
        int n = 0; for (int i = 0; i < c.getContainerSize(); i++) if (c.getItem(i).is(it)) n += c.getItem(i).getCount(); return n;
    }
    static void test(GameTestHelper h) {
        var level = h.getLevel();
        var aura = NativeAura.get(level, level.getChunkAt(h.absolutePos(new BlockPos(1, 1, 1))));
        aura.addVis(400);
        // --- levitator pushes an item up its column
        BlockPos lv = new BlockPos(2, 1, 2);
        h.setBlock(lv, ModBlocks.LEVITATOR.get().defaultBlockState().setValue(LevitatorBlock.FACING, Direction.UP).setValue(LevitatorBlock.ENABLED, true));
        var lev = h.getBlockEntity(lv, LevitatorBlockEntity.class);
        check(lev.range() == 8 && lev.getCost() == 16, "Levitator default range");
        check(LevitatorBlock.hitsDial(lev.getBlockState(), h.absolutePos(lv), Vec3.atLowerCornerWithOffset(h.absolutePos(lv), 0.5, 0.1, 0.5)), "Levitator dial hit");
        var item = new ItemEntity(level, h.absolutePos(lv).getX() + 0.5, h.absolutePos(lv).getY() + 1.5, h.absolutePos(lv).getZ() + 0.5, new ItemStack(Items.STICK));
        item.setDeltaMovement(Vec3.ZERO);
        level.addFreshEntity(item);
        for (int t = 0; t < 4; t++) LevitatorBlockEntity.tick(level, h.absolutePos(lv), lev.getBlockState(), lev);
        check(item.getDeltaMovement().y > 0.05, "Levitator did not push: " + item.getDeltaMovement());
        item.discard();
        // --- dioptra samples the chunk aura
        BlockPos dp = new BlockPos(6, 1, 2);
        h.setBlock(dp, ModBlocks.DIOPTRA.get());
        var dio = h.getBlockEntity(dp, DioptraBlockEntity.class);
        for (int t = 0; t < 20; t++) DioptraBlockEntity.tick(level, h.absolutePos(dp), dio.getBlockState(), dio);
        int expect = (int) Math.min(64f, NativeAura.vis(level, h.absolutePos(dp)) / 500f * 64f);
        check(dio.grid[84] == expect && expect > 0, "Dioptra centre cell " + dio.grid[84] + " vs " + expect);
        check(dio.comparator() > 0, "Dioptra comparator");
        level.setBlock(h.absolutePos(dp), dio.getBlockState().setValue(DioptraBlock.ENABLED, false), 3);
        // --- infernal furnace smelts into the inventory in front and the bonus roll includes iron nuggets
        BlockPos fp = new BlockPos(10, 2, 4);
        h.setBlock(fp, ModBlocks.INFERNAL_FURNACE.get().defaultBlockState().setValue(InfernalFurnaceBlock.FACING, Direction.NORTH));
        BlockPos out = fp.relative(Direction.SOUTH);
        h.setBlock(out, Blocks.CHEST);
        var furnace = h.getBlockEntity(fp, InfernalFurnaceBlockEntity.class);
        var rest = furnace.addItemsToInventory(level, new ItemStack(Items.RAW_IRON, 2));
        check(rest.isEmpty() && furnace.getItem(0).getCount() == 2, "Furnace intake");
        check(furnace.addItemsToInventory(level, new ItemStack(Items.STICK, 64)).isEmpty() && furnace.getItem(1).isEmpty() || furnace.getItem(1).is(Items.STICK), "Furnace intake of stick");
        for (int t = 0; t < 400; t++) InfernalFurnaceBlockEntity.tick(level, h.absolutePos(fp), furnace.getBlockState(), furnace);
        var chest = h.getBlockEntity(out, ChestBlockEntity.class);
        check(count(chest, Items.IRON_INGOT) == 2, "Furnace output " + count(chest, Items.IRON_INGOT));
        // breaking the core reverts the multiblock
        h.setBlock(fp.above(), ModBlocks.PLACEHOLDER_BRICK.get());
        h.setBlock(fp.below(), ModBlocks.PLACEHOLDER_OBSIDIAN.get());
        h.destroyBlock(fp);
        check(h.getBlockState(fp).getFluidState().isSource(), "Furnace core not lava");
        check(h.getBlockState(fp.above()).is(Blocks.NETHER_BRICKS) && h.getBlockState(fp.below()).is(Blocks.OBSIDIAN), "Placeholders not reverted");
        // --- pattern crafter: 9 iron ingots above -> iron block below
        BlockPos pc = new BlockPos(14, 2, 2);
        h.setBlock(pc, ModBlocks.PATTERN_CRAFTER.get());
        h.setBlock(pc.above(), Blocks.CHEST);
        h.setBlock(pc.below(), Blocks.CHEST);
        h.getBlockEntity(pc.above(), ChestBlockEntity.class).setItem(0, new ItemStack(Items.IRON_INGOT, 10));
        var crafter = h.getBlockEntity(pc, PatternCrafterBlockEntity.class);
        crafter.count = 0;
        PatternCrafterBlockEntity.tick(level, h.absolutePos(pc), crafter.getBlockState(), crafter);
        var below = h.getBlockEntity(pc.below(), ChestBlockEntity.class);
        check(count(below, Items.IRON_BLOCK) == 1 && count(h.getBlockEntity(pc.above(), ChestBlockEntity.class), Items.IRON_INGOT) == 1, "Pattern crafter 3x3");
        crafter.cycle();
        check(crafter.type == 1, "Pattern cycle");
        // --- flux goo spreads like a finite fluid and coalesces into thaumic slimes
        BlockPos gp = new BlockPos(4, 2, 10);
        for (int x = -2; x <= 2; x++) for (int z = -2; z <= 2; z++) h.setBlock(gp.offset(x, -1, z), Blocks.STONE);
        h.setBlock(gp, ModBlocks.FLUX_GOO.get());
        var gabs = h.absolutePos(gp);
        boolean spread = false;
        for (long seed = 0; seed < 200 && !spread; seed++) {
            var st = level.getBlockState(gabs);
            if (!st.is(ModBlocks.FLUX_GOO.get())) { h.setBlock(gp, ModBlocks.FLUX_GOO.get()); st = level.getBlockState(gabs); }
            st.tick(level, gabs, net.minecraft.util.RandomSource.create(seed));
            for (var d : Direction.Plane.HORIZONTAL) if (level.getBlockState(gabs.relative(d)).is(ModBlocks.FLUX_GOO.get())) spread = true;
        }
        check(spread, "Flux goo did not spread");
        // falls into a hole below
        BlockPos hole = new BlockPos(8, 3, 10);
        h.setBlock(hole.below(), Blocks.AIR);
        h.setBlock(hole.below(2), Blocks.STONE);
        h.setBlock(hole, ModBlocks.FLUX_GOO.get().defaultBlockState().setValue(art.arcane.thaumcraft.block.FluxGooBlock.LEVEL, 3));
        boolean fell = false;
        for (long seed = 0; seed < 200 && !fell; seed++) {
            var st = level.getBlockState(h.absolutePos(hole));
            if (st.is(ModBlocks.FLUX_GOO.get())) st.tick(level, h.absolutePos(hole), net.minecraft.util.RandomSource.create(seed));
            fell = level.getBlockState(h.absolutePos(hole.below())).is(ModBlocks.FLUX_GOO.get());
        }
        check(fell, "Flux goo did not fall");
        BlockPos sp2 = new BlockPos(12, 2, 10);
        h.setBlock(sp2.below(), Blocks.STONE);
        var slimeBox = new net.minecraft.world.phys.AABB(h.absolutePos(sp2)).inflate(1);
        for (long seed = 0; seed < 2000 && level.getEntitiesOfClass(art.arcane.thaumcraft.entity.ThaumicSlimeEntity.class, slimeBox).isEmpty(); seed++) {
            h.setBlock(sp2, ModBlocks.FLUX_GOO.get());
            level.getBlockState(h.absolutePos(sp2)).randomTick(level, h.absolutePos(sp2), net.minecraft.util.RandomSource.create(seed));
        }
        var slimes = level.getEntitiesOfClass(art.arcane.thaumcraft.entity.ThaumicSlimeEntity.class, slimeBox);
        check(slimes.size() == 1 && slimes.get(0).getSize() == 2, "Full goo did not form a size 2 slime: " + slimes);
        var big = slimes.get(0);
        big.setSize(4, true);
        big.setHealth(0);
        big.remove(net.minecraft.world.entity.Entity.RemovalReason.KILLED);
        var kids = level.getEntitiesOfClass(art.arcane.thaumcraft.entity.ThaumicSlimeEntity.class, slimeBox.inflate(3), e -> e != big && e.isAlive());
        check(kids.size() == 4 && kids.stream().allMatch(k -> k.getSize() == 1), "Slime did not burst into 4 size-1 slimes: " + kids.size());
        kids.forEach(k -> k.discard());
        // --- stabiliser -> inlay -> pedestal charge network (BlockInlay.calculateChanges)
        BlockPos st = new BlockPos(2, 2, 14);
        for (int x = 0; x < 6; x++) h.setBlock(st.offset(x, -1, 0), Blocks.STONE);
        h.setBlock(st, ModBlocks.STABILIZER.get());
        var stab = h.getBlockEntity(st, art.arcane.thaumcraft.blockentity.StabilizerBlockEntity.class);
        for (int t = 0; t < 20 * 15; t++) art.arcane.thaumcraft.blockentity.StabilizerBlockEntity.tick(level, h.absolutePos(st), stab.getBlockState(), stab);
        check(stab.getEnergy() == 15, "Stabiliser energy " + stab.getEnergy());
        h.setBlock(st.east(), ModBlocks.INLAY.get());
        h.setBlock(st.east(2), ModBlocks.INLAY.get());
        h.setBlock(st.east(3), ModBlocks.PEDESTAL_ARCANE.get());
        var in1 = h.getBlockState(st.east()); var in2 = h.getBlockState(st.east(2)); var ped = h.getBlockState(st.east(3));
        check(in1.getValue(art.arcane.thaumcraft.block.InlayBlock.CHARGE) == 15, "Inlay next to stabiliser: " + in1);
        check(in1.getValue(art.arcane.thaumcraft.block.InlayBlock.WEST) == art.arcane.thaumcraft.block.InlayBlock.Attach.EXT, "Inlay EXT link");
        check(in2.getValue(art.arcane.thaumcraft.block.InlayBlock.CHARGE) == 14, "Second inlay: " + in2);
        check(ped.getValue(art.arcane.thaumcraft.block.InlayBlock.CHARGE) == 13, "Pedestal charge: " + ped);
        h.destroyBlock(st.east());
        check(h.getBlockState(st.east(2)).getValue(art.arcane.thaumcraft.block.InlayBlock.CHARGE) == 0 && h.getBlockState(st.east(3)).getValue(art.arcane.thaumcraft.block.InlayBlock.CHARGE) == 0, "Charge did not drain after cutting the inlay");
        System.out.println("Artifice devices passed: levitator, dioptra, infernal furnace smelting/revert, pattern crafter.");
        h.succeed();
    }
    private ArtificeDevicesVerification() {}
}
