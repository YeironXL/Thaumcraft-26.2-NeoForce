package art.arcane.thaumcraft.block;

import art.arcane.thaumcraft.blockentity.EssentiaLampBlockEntity;
import art.arcane.thaumcraft.registry.ModBlockEntities;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.redstone.Orientation;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * TC6 BlockLamp with TileLampGrowth (herba) or TileLampFertility (desiderium): hangs from the block it was placed on,
 * draws its essentia through a tube on that side and lights up (15) while charged and unpowered.
 */
public final class EssentiaLampBlock extends BaseEntityBlock {
    public enum Kind { GROWTH, FERTILITY }
    public static final EnumProperty<Direction> FACING = BlockStateProperties.FACING;
    public static final BooleanProperty ENABLED = BlockStateProperties.ENABLED;
    private static final VoxelShape SHAPE = Block.box(4, 2, 4, 12, 14, 12);
    public final Kind kind;

    public EssentiaLampBlock(Properties p, Kind kind) {
        super(p.strength(1.0f, 20.0f).sound(SoundType.METAL).noOcclusion().lightLevel(s -> s.getValue(ENABLED) ? 15 : 0));
        this.kind = kind;
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.DOWN).setValue(ENABLED, false));
    }
    @Override protected MapCodec<? extends BaseEntityBlock> codec() { return simpleCodec(p -> new EssentiaLampBlock(p, kind)); }
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> b) { b.add(FACING, ENABLED); }
    @Override protected RenderShape getRenderShape(BlockState s) { return RenderShape.MODEL; }
    @Override protected VoxelShape getShape(BlockState s, BlockGetter l, BlockPos p, CollisionContext c) { return SHAPE; }
    @Override public BlockEntity newBlockEntity(BlockPos pos, BlockState state) { return new EssentiaLampBlockEntity(pos, state); }
    @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide() ? null : createTickerHelper(type, ModBlockEntities.LAMP_ESSENTIA.get(), EssentiaLampBlockEntity::tick);
    }
    @Override public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        return defaultBlockState().setValue(FACING, ctx.getClickedFace().getOpposite()).setValue(ENABLED, false);
    }
    @Override
    protected void neighborChanged(BlockState state, Level world, BlockPos pos, Block b, Orientation o, boolean moved) {
        if (world.isClientSide()) return;
        if (world.isEmptyBlock(pos.relative(state.getValue(FACING)))) { world.destroyBlock(pos, true); return; }
        // BlockLamp: an uncharged lamp ignores redstone; otherwise power switches it off (BlockTCDevice.updateState).
        if (!(world.getBlockEntity(pos) instanceof EssentiaLampBlockEntity te) || te.charges <= 0) return;
        boolean on = !world.hasNeighborSignal(pos);
        if (on != state.getValue(ENABLED)) world.setBlock(pos, state.setValue(ENABLED, on), 3);
    }
}
