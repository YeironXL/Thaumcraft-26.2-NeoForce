package art.arcane.thaumcraft.worldgen;

import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/** Shared helpers equivalent to 1.12 WorldGenAbstractTree. */
abstract class TreeGen {
    protected final boolean notify;
    TreeGen(boolean notify) { this.notify = notify; }

    /** WorldGenAbstractTree.isReplaceable / canGrowInto. */
    static boolean isReplaceable(LevelAccessor w, BlockPos p) {
        BlockState s = w.getBlockState(p);
        return s.isAir() || s.is(BlockTags.LEAVES) || s.is(Blocks.GRASS_BLOCK) || s.is(BlockTags.DIRT) || s.is(BlockTags.LOGS)
                || s.is(net.minecraft.tags.BlockItemTags.SAPLINGS.block()) || s.is(Blocks.VINE);
    }

    static boolean isLeaves(BlockState s) { return s.is(BlockTags.LEAVES); }

    /** Forge canSustainPlant(..., Blocks.SAPLING): grass/dirt family. */
    static boolean isSoil(BlockState s) { return s.is(BlockTags.SUPPORTS_VEGETATION) || s.is(BlockTags.DIRT); }

    protected void set(LevelAccessor w, BlockPos p, BlockState s) { w.setBlock(p, s, notify ? 3 : 2); }

    static BlockState log(net.minecraft.world.level.block.Block b, int meta) {
        var axis = switch (meta) { case 0 -> net.minecraft.core.Direction.Axis.X; case 2 -> net.minecraft.core.Direction.Axis.Z; default -> net.minecraft.core.Direction.Axis.Y; };
        return b.defaultBlockState().setValue(net.minecraft.world.level.block.RotatedPillarBlock.AXIS, axis);
    }

    static int floor(double d) { return net.minecraft.util.Mth.floor(d); }
}
