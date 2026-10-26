package art.arcane.thaumcraft.block.world;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/** TC6 BlockCandle (tallow candle): light 14, needs a solid top face below, smoke + flame. */
public final class TcCandleBlock extends Block implements art.arcane.thaumcraft.crafting.infusion.IInfusionStabiliser {
    @Override public boolean canStabaliseInfusion(net.minecraft.world.level.Level world, net.minecraft.core.BlockPos pos) { return true; }
    private static final VoxelShape SHAPE = Block.box(6, 0, 6, 10, 8, 10);
    private final DyeColor dye;

    public TcCandleBlock(DyeColor dye, Properties p) {
        super(p.mapColor(dye).strength(0.1f).sound(net.minecraft.world.level.block.SoundType.WOOL).lightLevel(s -> 14).noCollision().noOcclusion());
        this.dye = dye;
    }

    public DyeColor dye() { return dye; }
    @Override protected MapCodec<? extends Block> codec() { return simpleCodec(p -> new TcCandleBlock(dye, p)); }
    @Override protected VoxelShape getShape(BlockState s, BlockGetter l, BlockPos p, CollisionContext c) { return SHAPE; }

    @Override
    protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        return level.getBlockState(pos.below()).isFaceSturdy(level, pos.below(), Direction.UP);
    }

    @Override
    protected BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess ticks, BlockPos pos, Direction dir, BlockPos npos, BlockState nstate, RandomSource r) {
        return dir == Direction.DOWN && !canSurvive(state, level, pos) ? Blocks.AIR.defaultBlockState() : super.updateShape(state, level, ticks, pos, dir, npos, nstate, r);
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource r) {
        double x = pos.getX() + 0.5, y = pos.getY() + 0.7, z = pos.getZ() + 0.5;
        level.addParticle(ParticleTypes.SMOKE, x, y, z, 0, 0, 0);
        level.addParticle(ParticleTypes.FLAME, x, y, z, 0, 0, 0);
    }
}
