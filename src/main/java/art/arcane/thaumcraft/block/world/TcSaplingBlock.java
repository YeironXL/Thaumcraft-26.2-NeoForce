package art.arcane.thaumcraft.block.world;

import art.arcane.thaumcraft.ModBlocks;
import art.arcane.thaumcraft.worldgen.GreatwoodTree;
import art.arcane.thaumcraft.worldgen.SilverwoodTree;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SaplingBlock;
import net.minecraft.world.level.block.grower.TreeGrower;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/** TC6 BlockSaplingTC: greatwood needs a 2x2 of saplings; silverwood grows alone. */
public final class TcSaplingBlock extends SaplingBlock {
    private static final VoxelShape SHAPE = Block.box(1.6, 0, 1.6, 14.4, 12.8, 14.4);
    private final boolean greatwood;

    public TcSaplingBlock(boolean greatwood, Properties p) {
        super(new TreeGrower("thaumcraft_" + (greatwood ? "greatwood" : "silverwood"), Optional.empty(), Optional.empty(), Optional.empty()), p);
        this.greatwood = greatwood;
    }

    @Override protected VoxelShape getShape(BlockState s, BlockGetter l, BlockPos p, CollisionContext c) { return SHAPE; }

    @Override
    public void advanceTree(ServerLevel level, BlockPos pos, BlockState state, RandomSource rand) {
        if (state.getValue(STAGE) == 0) level.setBlock(pos, state.cycle(STAGE), 4);
        else generateTree(level, pos, state, rand);
    }

    private void generateTree(ServerLevel level, BlockPos pos, BlockState state, RandomSource rand) {
        int i = 0, j = 0;
        boolean flag = false;
        if (greatwood) {
            outer:
            for (i = 0; i >= -1; --i) for (j = 0; j >= -1; --j)
                if (twoByTwo(level, pos, i, j)) { flag = true; break outer; }
            if (!flag) return;
        }
        BlockState air = Blocks.AIR.defaultBlockState();
        if (flag) {
            level.setBlock(pos.offset(i, 0, j), air, 4); level.setBlock(pos.offset(i + 1, 0, j), air, 4);
            level.setBlock(pos.offset(i, 0, j + 1), air, 4); level.setBlock(pos.offset(i + 1, 0, j + 1), air, 4);
        } else level.setBlock(pos, air, 4);
        java.util.Random jr = new java.util.Random(rand.nextLong());
        boolean ok = greatwood ? new GreatwoodTree(true, false).generate(level, jr, pos.offset(i, 0, j))
                : new SilverwoodTree(true, 7, 4).generate(level, jr, pos.offset(i, 0, j));
        if (!ok) {
            if (flag) {
                level.setBlock(pos.offset(i, 0, j), state, 4); level.setBlock(pos.offset(i + 1, 0, j), state, 4);
                level.setBlock(pos.offset(i, 0, j + 1), state, 4); level.setBlock(pos.offset(i + 1, 0, j + 1), state, 4);
            } else level.setBlock(pos.offset(i, 0, j), state, 4);
        }
    }

    private boolean twoByTwo(Level l, BlockPos p, int a, int b) {
        return l.getBlockState(p.offset(a, 0, b)).is(this) && l.getBlockState(p.offset(a + 1, 0, b)).is(this)
                && l.getBlockState(p.offset(a, 0, b + 1)).is(this) && l.getBlockState(p.offset(a + 1, 0, b + 1)).is(this);
    }

    @Override public boolean isValidBonemealTarget(LevelReader level, BlockPos pos, BlockState state) { return true; }
    @Override public boolean isBonemealSuccess(Level level, RandomSource random, BlockPos pos, BlockState state) { return level.getRandom().nextFloat() < 0.25f; }
}
