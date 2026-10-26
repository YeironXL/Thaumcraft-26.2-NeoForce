package art.arcane.thaumcraft.worldgen;

import java.util.Random;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/** Port of TC6 WorldGenSilverwoodTrees. */
public final class SilverwoodTree extends TreeGen {
    private final int minTreeHeight, randomTreeHeight;
    private final boolean worldgen;

    public SilverwoodTree(boolean notify, int minTreeHeight, int randomTreeHeight) {
        super(notify);
        this.worldgen = !notify;
        this.minTreeHeight = minTreeHeight;
        this.randomTreeHeight = randomTreeHeight;
    }

    private static Block LOG() { return art.arcane.thaumcraft.ModBlocks.LOG_SILVERWOOD.get(); }
    private static Block LEAF() { return art.arcane.thaumcraft.ModBlocks.LEAVES_SILVERWOOD.get(); }

    private static boolean free(BlockState s) { return s.isAir() || isLeaves(s) || s.canBeReplaced(); }

    public boolean generate(LevelAccessor world, Random random, BlockPos pos) {
        int height = random.nextInt(randomTreeHeight) + minTreeHeight;
        boolean flag = true;
        int x = pos.getX(), y = pos.getY(), z = pos.getZ();
        int maxY = world.getMaxY() + 1;
        if (y < world.getMinY() + 1 || y + height + 1 > maxY) return false;
        for (int i1 = y; i1 <= y + 1 + height; ++i1) {
            int spread = 1;
            if (i1 == y) spread = 0;
            if (i1 >= y + 1 + height - 2) spread = 3;
            for (int j1 = x - spread; j1 <= x + spread && flag; ++j1)
                for (int k1 = z - spread; k1 <= z + spread && flag; ++k1) {
                    if (i1 < maxY) {
                        if (!free(world.getBlockState(new BlockPos(j1, i1, k1))) && i1 > y) flag = false;
                    } else flag = false;
                }
        }
        if (!flag) return false;
        if (!isSoil(world.getBlockState(new BlockPos(x, y - 1, z))) || y >= maxY - height - 1) return false;
        int start = y + height - 5;
        for (int end = y + height + 3 + random.nextInt(3), k2 = start; k2 <= end; ++k2) {
            int cty = Mth.clamp(k2, y + height - 3, y + height);
            for (int xx = x - 5; xx <= x + 5; ++xx)
                for (int zz = z - 5; zz <= z + 5; ++zz) {
                    double d3 = xx - x, d4 = k2 - cty, d5 = zz - z;
                    double dist = d3 * d3 + d4 * d4 + d5 * d5;
                    BlockPos lp = new BlockPos(xx, k2, zz);
                    BlockState s2 = world.getBlockState(lp);
                    if (dist < 10 + random.nextInt(8) && (s2.isAir() || isLeaves(s2))) place(world, lp, LEAF().defaultBlockState());
                }
        }
        int k2;
        for (k2 = 0; k2 < height; ++k2) {
            if (free(world.getBlockState(new BlockPos(x, y + k2, z)))) {
                place(world, new BlockPos(x, y + k2, z), log(LOG(), 1));
                place(world, new BlockPos(x - 1, y + k2, z), log(LOG(), 1));
                place(world, new BlockPos(x + 1, y + k2, z), log(LOG(), 1));
                place(world, new BlockPos(x, y + k2, z - 1), log(LOG(), 1));
                place(world, new BlockPos(x, y + k2, z + 1), log(LOG(), 1));
            }
        }
        place(world, new BlockPos(x, y + k2, z), log(LOG(), 1));
        place(world, new BlockPos(x - 1, y, z - 1), log(LOG(), 1));
        place(world, new BlockPos(x + 1, y, z + 1), log(LOG(), 1));
        place(world, new BlockPos(x - 1, y, z + 1), log(LOG(), 1));
        place(world, new BlockPos(x + 1, y, z - 1), log(LOG(), 1));
        if (random.nextInt(3) != 0) place(world, new BlockPos(x - 1, y + 1, z - 1), log(LOG(), 1));
        if (random.nextInt(3) != 0) place(world, new BlockPos(x + 1, y + 1, z + 1), log(LOG(), 1));
        if (random.nextInt(3) != 0) place(world, new BlockPos(x - 1, y + 1, z + 1), log(LOG(), 1));
        if (random.nextInt(3) != 0) place(world, new BlockPos(x + 1, y + 1, z - 1), log(LOG(), 1));
        place(world, new BlockPos(x - 2, y, z), log(LOG(), 0));
        place(world, new BlockPos(x + 2, y, z), log(LOG(), 0));
        place(world, new BlockPos(x, y, z - 2), log(LOG(), 2));
        place(world, new BlockPos(x, y, z + 2), log(LOG(), 2));
        place(world, new BlockPos(x - 2, y - 1, z), log(LOG(), 1));
        place(world, new BlockPos(x + 2, y - 1, z), log(LOG(), 1));
        place(world, new BlockPos(x, y - 1, z - 2), log(LOG(), 1));
        place(world, new BlockPos(x, y - 1, z + 2), log(LOG(), 1));
        int h4 = y + (height - 4), h5 = y + (height - 5);
        place(world, new BlockPos(x - 1, h4, z - 1), log(LOG(), 1));
        place(world, new BlockPos(x + 1, h4, z + 1), log(LOG(), 1));
        place(world, new BlockPos(x - 1, h4, z + 1), log(LOG(), 1));
        place(world, new BlockPos(x + 1, h4, z - 1), log(LOG(), 1));
        if (random.nextInt(3) == 0) place(world, new BlockPos(x - 1, h5, z - 1), log(LOG(), 1));
        if (random.nextInt(3) == 0) place(world, new BlockPos(x + 1, h5, z + 1), log(LOG(), 1));
        if (random.nextInt(3) == 0) place(world, new BlockPos(x - 1, h5, z + 1), log(LOG(), 1));
        if (random.nextInt(3) == 0) place(world, new BlockPos(x + 1, h5, z - 1), log(LOG(), 1));
        place(world, new BlockPos(x - 2, h4, z), log(LOG(), 0));
        place(world, new BlockPos(x + 2, h4, z), log(LOG(), 0));
        place(world, new BlockPos(x, h4, z - 2), log(LOG(), 2));
        place(world, new BlockPos(x, h4, z + 2), log(LOG(), 2));
        // TC4: naturally generated silverwood trees hold a (small) pure node in the trunk.
        if (worldgen) art.arcane.thaumcraft.aura.node.AuraNodeGen.place(world, new BlockPos(x, y + height / 2, z), art.arcane.thaumcraft.aura.node.NodeType.PURE, null, net.minecraft.util.RandomSource.create(random.nextLong()), true);
        if (worldgen) CustomFlowers.generate(world, random, new BlockPos(x, y, z), art.arcane.thaumcraft.ModBlocks.SHIMMERLEAF.get());
        return true;
    }

    private void place(LevelAccessor w, BlockPos p, BlockState s) {
        if (free(w.getBlockState(p))) set(w, p, s);
    }
}
