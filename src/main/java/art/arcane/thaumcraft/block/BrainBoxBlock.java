package art.arcane.thaumcraft.block;

import art.arcane.thaumcraft.ModBlocks;
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

/** TC6 BlockBrainBox (mnemonic matrix): must face a thaumatorium; each one adds two recipe slots. */
public final class BrainBoxBlock extends Block {
    public static final EnumProperty<Direction> FACING = BlockStateProperties.FACING;
    private static final VoxelShape SHAPE = Block.box(3, 3, 3, 13, 13, 13);
    public BrainBoxBlock(Properties p) {
        super(p.strength(1.0f, 10.0f).sound(SoundType.METAL).noOcclusion());
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.UP));
    }
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> b) { b.add(FACING); }
    @Override protected VoxelShape getShape(BlockState s, BlockGetter l, BlockPos p, CollisionContext c) { return SHAPE; }
    private static boolean isThaumatorium(BlockState s) { return s.is(ModBlocks.THAUMATORIUM.get()) || s.is(ModBlocks.THAUMATORIUM_TOP.get()); }
    @Override public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        Direction side = ctx.getClickedFace();
        BlockState target = ctx.getLevel().getBlockState(ctx.getClickedPos().relative(side.getOpposite()));
        if (!isThaumatorium(target) || target.getValue(ThaumatoriumBlock.FACING) == side) return null;
        return defaultBlockState().setValue(FACING, side.getOpposite());
    }
    @Override protected boolean canSurvive(BlockState s, LevelReader l, BlockPos p) { return isThaumatorium(l.getBlockState(p.relative(s.getValue(FACING)))); }
    @Override protected BlockState updateShape(BlockState s, LevelReader l, ScheduledTickAccess t, BlockPos p, Direction d, BlockPos np, BlockState ns, RandomSource r) {
        return canSurvive(s, l, p) ? s : Blocks.AIR.defaultBlockState();
    }
}
