package art.arcane.thaumcraft.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/** TC6 BlockSmelterVent: mounts on a side (not the front) of an essentia smelter and vents part of its flux. */
public final class SmelterVentBlock extends Block {
    public static final EnumProperty<Direction> FACING = BlockStateProperties.HORIZONTAL_FACING;
    public SmelterVentBlock(Properties p) {
        super(p.strength(1.0f, 10.0f).sound(SoundType.METAL).noOcclusion());
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
    }
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> b) { b.add(FACING); }
    @Override protected VoxelShape getShape(BlockState s, BlockGetter l, BlockPos p, CollisionContext c) {
        return switch (s.getValue(FACING)) {
            case SOUTH -> Block.box(2, 2, 8, 14, 14, 16);
            case WEST -> Block.box(0, 2, 2, 8, 14, 14);
            case EAST -> Block.box(8, 2, 2, 16, 14, 14);
            default -> Block.box(2, 2, 0, 14, 14, 8);
        };
    }
    private static boolean smelterAt(LevelReader l, BlockPos p, Direction side) {
        BlockState s = l.getBlockState(p.relative(side.getOpposite()));
        return s.getBlock() instanceof EssentiaSmelterBlock && s.getValue(EssentiaSmelterBlock.FACING) != side;
    }
    @Override public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        Direction side = ctx.getClickedFace();
        if (!side.getAxis().isHorizontal() || !smelterAt(ctx.getLevel(), ctx.getClickedPos(), side)) return null;
        return defaultBlockState().setValue(FACING, side.getOpposite());
    }
    @Override protected boolean canSurvive(BlockState s, LevelReader l, BlockPos p) { return smelterAt(l, p, s.getValue(FACING).getOpposite()); }
    @Override protected BlockState updateShape(BlockState s, LevelReader l, ScheduledTickAccess t, BlockPos p, Direction d, BlockPos np, BlockState ns, RandomSource r) {
        return canSurvive(s, l, p) ? s : Blocks.AIR.defaultBlockState();
    }
}
