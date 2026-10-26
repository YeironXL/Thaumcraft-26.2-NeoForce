package art.arcane.thaumcraft.worldgen;

import java.util.Random;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

/** Port of TC6 WorldGenCustomFlowers. */
public final class CustomFlowers {
    private CustomFlowers() {}
    public static boolean generate(LevelAccessor world, Random r, BlockPos pos, Block plant) {
        for (int i = 0; i < 18; ++i) {
            BlockPos bp = new BlockPos(pos.getX() + r.nextInt(8) - r.nextInt(8), pos.getY() + r.nextInt(4) - r.nextInt(4), pos.getZ() + r.nextInt(8) - r.nextInt(8));
            var below = world.getBlockState(bp.below());
            if (world.isEmptyBlock(bp) && (below.is(Blocks.GRASS_BLOCK) || below.is(Blocks.SAND))) world.setBlock(bp, plant.defaultBlockState(), 3);
        }
        return true;
    }
}
