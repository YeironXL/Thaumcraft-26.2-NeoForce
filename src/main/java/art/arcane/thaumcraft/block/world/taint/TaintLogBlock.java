package art.arcane.thaumcraft.block.world.taint;

import art.arcane.thaumcraft.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.MapColor;

/** TC6 BlockTaintLog: a tainted log that turns to flux goo once no taint seed is near, spreading fibres otherwise. */
public final class TaintLogBlock extends RotatedPillarBlock implements ITaintBlock {
    public TaintLogBlock(Properties p) { super(p.mapColor(MapColor.COLOR_PURPLE).strength(3.0f, 100.0f).sound(TaintFibreBlock.GORE).randomTicks()); }

    @Override public void die(Level world, BlockPos pos, BlockState state) { world.setBlockAndUpdate(pos, ModBlocks.FLUX_GOO.get().defaultBlockState()); }

    @Override
    protected void randomTick(BlockState state, ServerLevel world, BlockPos pos, RandomSource random) {
        if (!(TaintHelper.isNearTaintSeed(world, pos) || art.arcane.thaumcraft.world.outer.MazeGenerator.permanentTaint(world, pos))) die(world, pos, state);
        else TaintHelper.spreadFibres(world, pos);
    }

    @Override public int getFlammability(BlockState s, BlockGetter l, BlockPos p, Direction d) { return 4; }
    @Override public int getFireSpreadSpeed(BlockState s, BlockGetter l, BlockPos p, Direction d) { return 4; }
}
