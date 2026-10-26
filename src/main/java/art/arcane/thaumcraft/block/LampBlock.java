package art.arcane.thaumcraft.block;

import art.arcane.thaumcraft.blockentity.ArcaneLampBlockEntity;
import art.arcane.thaumcraft.registry.ModBlockEntities;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
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

/** TC6 BlockLamp (arcane lamp): hangs from the block it was placed on, gives light 15 while enabled (unpowered). */
public final class LampBlock extends BaseEntityBlock {
    public static final MapCodec<LampBlock> CODEC = simpleCodec(LampBlock::new);
    public static final EnumProperty<Direction> FACING = BlockStateProperties.FACING;
    public static final BooleanProperty ENABLED = BlockStateProperties.ENABLED;
    private static final VoxelShape SHAPE = Block.box(4, 2, 4, 12, 14, 12);

    public LampBlock(Properties p) {
        super(p.strength(1.0f, 20.0f).sound(SoundType.METAL).noOcclusion().lightLevel(s -> s.getValue(ENABLED) ? 15 : 0));
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.DOWN).setValue(ENABLED, true));
    }
    @Override protected MapCodec<? extends BaseEntityBlock> codec() { return CODEC; }
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> b) { b.add(FACING, ENABLED); }
    @Override protected RenderShape getRenderShape(BlockState s) { return RenderShape.MODEL; }
    @Override protected VoxelShape getShape(BlockState s, BlockGetter l, BlockPos p, CollisionContext c) { return SHAPE; }
    @Override public BlockEntity newBlockEntity(BlockPos pos, BlockState state) { return new ArcaneLampBlockEntity(pos, state); }
    @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide() ? null : createTickerHelper(type, ModBlockEntities.LAMP_ARCANE.get(), ArcaneLampBlockEntity::tick);
    }
    @Override public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        return defaultBlockState().setValue(FACING, ctx.getClickedFace().getOpposite()).setValue(ENABLED, false);
    }
    @Override protected void onPlace(BlockState s, Level l, BlockPos pos, BlockState old, boolean moved) { updateState(l, pos, s); }
    @Override
    protected void neighborChanged(BlockState state, Level world, BlockPos pos, Block b, Orientation o, boolean moved) {
        if (world.isClientSide()) return;
        if (world.isEmptyBlock(pos.relative(state.getValue(FACING)))) { world.destroyBlock(pos, true); return; }
        if (world.getBlockEntity(pos) instanceof ArcaneLampBlockEntity te && state.getValue(ENABLED) && world.hasNeighborSignal(pos)) te.removeLights();
        updateState(world, pos, state);
    }
    private static void updateState(Level l, BlockPos pos, BlockState s) {
        if (l.isClientSide()) return;
        boolean flag = !l.hasNeighborSignal(pos);
        if (flag != s.getValue(ENABLED)) l.setBlock(pos, s.setValue(ENABLED, flag), 3);
    }
}
