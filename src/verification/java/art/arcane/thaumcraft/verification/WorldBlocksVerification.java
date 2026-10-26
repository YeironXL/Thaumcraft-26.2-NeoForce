package art.arcane.thaumcraft.verification;

import art.arcane.thaumcraft.ModBlocks;
import art.arcane.thaumcraft.ModItems;
import art.arcane.thaumcraft.block.world.TcSaplingBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SaplingBlock;

/** TC6 basic/world blocks: properties, drops, sapling growth, thaumium gear and aura generation. */
final class WorldBlocksVerification {
    private static void check(boolean c, String m) { if (!c) throw new AssertionError(m); }
    static void test(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        // Thaumium tools: diamond tier, 500 uses.
        var pick = new ItemStack(ModItems.THAUMIUM_PICK.get());
        check(pick.getMaxDamage() == 500, "Thaumium pick durability " + pick.getMaxDamage());
        check(pick.isCorrectToolForDrops(Blocks.OBSIDIAN.defaultBlockState()), "Thaumium pick cannot mine obsidian");
        check(new ItemStack(ModItems.THAUMIUM_CHEST.get()).getMaxDamage() == 25 * 16, "Thaumium chest durability");
        // Hardness from ConfigBlocks.
        check(ModBlocks.STONE_ARCANE.get().defaultBlockState().getDestroySpeed(level, BlockPos.ZERO) == 2.0f, "Arcane stone hardness");
        check(ModBlocks.ORE_CINNABAR.get().defaultBlockState().getDestroySpeed(level, BlockPos.ZERO) == 2.0f, "Cinnabar hardness");
        check(ModBlocks.LOG_SILVERWOOD.get().defaultBlockState().getLightEmission() == 5, "Silverwood log light");
        // Drops.
        var p = new BlockPos(1, 1, 1);
        h.setBlock(p, ModBlocks.ORE_AMBER.get());
        var drops = Block.getDrops(level.getBlockState(h.absolutePos(p)), level, h.absolutePos(p), null, null, new ItemStack(Items.IRON_PICKAXE));
        check(drops.size() == 1 && (drops.get(0).is(ModItems.AMBER.get()) && drops.get(0).getCount() <= 2 || drops.get(0).is(ModItems.CURIO_PRESERVED.get())), "Amber ore drops " + drops);
        h.setBlock(p, ModBlocks.ORE_QUARTZ.get());
        drops = Block.getDrops(level.getBlockState(h.absolutePos(p)), level, h.absolutePos(p), null, null, new ItemStack(Items.IRON_PICKAXE));
        check(drops.size() == 1 && drops.get(0).is(Items.QUARTZ), "Quartz ore drops " + drops);
        h.setBlock(p, ModBlocks.STONE_ARCANE.get());
        drops = Block.getDrops(level.getBlockState(h.absolutePos(p)), level, h.absolutePos(p), null, null, new ItemStack(Items.IRON_PICKAXE));
        check(drops.size() == 1 && drops.get(0).is(ModItems.BLOCK_STONE_ARCANE.get()), "Arcane stone drops " + drops);
        // Candle needs a solid face below.
        h.setBlock(new BlockPos(3, 1, 1), Blocks.STONE);
        check(ModBlocks.CANDLE_RED.get().defaultBlockState().canSurvive(level, h.absolutePos(new BlockPos(3, 2, 1))), "Candle cannot stand on stone");
        check(!ModBlocks.CANDLE_RED.get().defaultBlockState().canSurvive(level, h.absolutePos(new BlockPos(3, 4, 1))), "Candle floats");
        // Silverwood sapling grows a tree on its own.
        var soil = new BlockPos(10, 1, 10);
        h.setBlock(soil, Blocks.DIRT);
        var sp = soil.above();
        h.setBlock(sp, ModBlocks.SAPLING_SILVERWOOD.get().defaultBlockState().setValue(SaplingBlock.STAGE, 1));
        ((TcSaplingBlock) ModBlocks.SAPLING_SILVERWOOD.get()).advanceTree(level, h.absolutePos(sp), level.getBlockState(h.absolutePos(sp)), level.getRandom());
        if (!level.getBlockState(h.absolutePos(sp)).is(ModBlocks.LOG_SILVERWOOD.get())) {
            var a = h.absolutePos(sp); StringBuilder sb = new StringBuilder();
            for (int y = 0; y <= 12; y++) for (int x = -3; x <= 3; x++) for (int z = -3; z <= 3; z++) { var st = level.getBlockState(a.offset(x, y, z)); if (!st.isAir()) sb.append(a.offset(x,y,z).subtract(a)).append(st.getBlock()).append(' '); }
            throw new AssertionError("Silverwood did not grow at " + a + " below=" + level.getBlockState(a.below()) + " obstacles: " + sb);
        }
        // A single greatwood sapling never grows (needs 2x2).
        var gs = new BlockPos(2, 2, 15);
        h.setBlock(gs.below(), Blocks.DIRT);
        h.setBlock(gs, ModBlocks.SAPLING_GREATWOOD.get().defaultBlockState().setValue(SaplingBlock.STAGE, 1));
        ((TcSaplingBlock) ModBlocks.SAPLING_GREATWOOD.get()).advanceTree(level, h.absolutePos(gs), level.getBlockState(h.absolutePos(gs)), level.getRandom());
        check(level.getBlockState(h.absolutePos(gs)).is(ModBlocks.SAPLING_GREATWOOD.get()), "Lone greatwood sapling grew");
        // Every loaded chunk has an aura base in [0,500].
        int base = art.arcane.thaumcraft.aura.NativeAura.base(level, h.absolutePos(p));
        check(base > 0 && base <= 500, "Aura base " + base);
        System.out.println("World blocks passed: thaumium gear, hardness, ore/stone drops, candle support, sapling growth, aura base " + base + ".");
        h.succeed();
    }
}
