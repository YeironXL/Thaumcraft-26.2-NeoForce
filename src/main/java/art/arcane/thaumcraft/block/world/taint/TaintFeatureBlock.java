package art.arcane.thaumcraft.block.world.taint;

import art.arcane.thaumcraft.ModBlocks;
import art.arcane.thaumcraft.aura.NativeAura;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.level.redstone.Orientation;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/** TC6 BlockTaintFeature: the glowing taint pustule grown on leaves beside tainted logs. */
public final class TaintFeatureBlock extends Block implements ITaintBlock {
    public static final EnumProperty<Direction> FACING = BlockStateProperties.FACING;
    private static final VoxelShape[] SHAPES = {
            Block.box(2, 10, 2, 14, 16, 14), Block.box(2, 0, 2, 14, 6, 14), Block.box(2, 2, 10, 14, 14, 16),
            Block.box(2, 2, 0, 14, 14, 6), Block.box(10, 2, 2, 16, 14, 14), Block.box(0, 2, 2, 6, 14, 14) };

    public TaintFeatureBlock(Properties p) {
        super(p.mapColor(MapColor.COLOR_PURPLE).strength(0.1f).sound(TaintFibreBlock.GORE).lightLevel(s -> 9).randomTicks().noOcclusion().pushReaction(PushReaction.DESTROY));
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.UP));
    }
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> b) { b.add(FACING); }
    @Override public BlockState getStateForPlacement(BlockPlaceContext ctx) { return defaultBlockState().setValue(FACING, ctx.getClickedFace()); }
    @Override protected VoxelShape getShape(BlockState s, BlockGetter l, BlockPos p, CollisionContext c) { return SHAPES[s.getValue(FACING).get3DDataValue()]; }

    @Override public void die(Level world, BlockPos pos, BlockState state) { world.setBlockAndUpdate(pos, ModBlocks.FLUX_GOO.get().defaultBlockState()); }

    @Override
    protected void randomTick(BlockState state, ServerLevel world, BlockPos pos, RandomSource random) {
        if (!(TaintHelper.isNearTaintSeed(world, pos) || art.arcane.thaumcraft.world.outer.MazeGenerator.permanentTaint(world, pos)) && random.nextInt(10) == 0) { die(world, pos, state); return; }
        TaintHelper.spreadFibres(world, pos);
        BlockState below = world.getBlockState(pos.below());
        if (below.is(ModBlocks.TAINT_LOG.get()) && below.getValue(RotatedPillarBlock.AXIS) == Direction.Axis.Y && world.getRandom().nextInt(100) == 0)
            world.setBlockAndUpdate(pos, ModBlocks.TAINT_GEYSER.get().defaultBlockState());
    }

    /** breakBlock: a third of the time a taint crawler bursts out, otherwise the aura is polluted. */
    @Override
    protected void affectNeighborsAfterRemoval(BlockState state, ServerLevel level, BlockPos pos, boolean moved) {
        if (level.getRandom().nextFloat() < 0.333f) {
            var e = art.arcane.thaumcraft.registry.ModEntities.TAINT_CRAWLER.get().create(level, net.minecraft.world.entity.EntitySpawnReason.TRIGGERED);
            if (e != null) {
                e.snapTo(pos.getX() + 0.5f, pos.getY() + 0.5f, pos.getZ() + 0.5f, level.getRandom().nextInt(360), 0.0f);
                level.addFreshEntity(e);
            }
        } else NativeAura.addFlux(level, pos, 1.0f);
        super.affectNeighborsAfterRemoval(state, level, pos, moved);
    }

    @Override
    protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block block, Orientation o, boolean moved) {
        Direction f = state.getValue(FACING);
        BlockPos support = pos.relative(f.getOpposite());
        if (!level.isClientSide() && !level.getBlockState(support).isFaceSturdy(level, support, f)) level.removeBlock(pos, false);
    }
}
