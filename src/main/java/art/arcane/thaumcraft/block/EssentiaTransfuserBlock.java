package art.arcane.thaumcraft.block;

import art.arcane.thaumcraft.blockentity.EssentiaTransfuserBlockEntity;
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
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * TC6 BlockEssentiaTransport: the filling (input) and emptying (output) essentia transfusers. Each one is a half block
 * on the face it was placed against, with its tube connection on that face, linking tubes and the warded jars around
 * it without tubes (EssentiaHandler, range 16).
 */
public final class EssentiaTransfuserBlock extends BaseEntityBlock {
    public static final EnumProperty<Direction> FACING = BlockStateProperties.FACING;
    public final boolean input;

    public EssentiaTransfuserBlock(Properties p, boolean input) {
        super(p.strength(1.0F, 10.0F).sound(SoundType.METAL).noOcclusion());
        this.input = input;
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.UP));
    }
    @Override protected MapCodec<? extends BaseEntityBlock> codec() { return simpleCodec(p -> new EssentiaTransfuserBlock(p, input)); }
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> b) { b.add(FACING); }
    @Override protected RenderShape getRenderShape(BlockState s) { return RenderShape.MODEL; }
    @Override public BlockState getStateForPlacement(BlockPlaceContext ctx) { return defaultBlockState().setValue(FACING, ctx.getClickedFace()); }
    @Override protected VoxelShape getShape(BlockState s, BlockGetter l, BlockPos p, CollisionContext c) {
        return switch (s.getValue(FACING)) {
            case DOWN -> box(4, 8, 4, 12, 16, 12);
            case UP -> box(4, 0, 4, 12, 8, 12);
            case NORTH -> box(4, 4, 8, 12, 12, 16);
            case SOUTH -> box(4, 4, 0, 12, 12, 8);
            case WEST -> box(8, 4, 4, 16, 12, 12);
            case EAST -> box(0, 4, 4, 8, 12, 12);
        };
    }
    @Override public BlockEntity newBlockEntity(BlockPos pos, BlockState state) { return new EssentiaTransfuserBlockEntity(pos, state); }
    @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide() ? null : createTickerHelper(type, ModBlockEntities.ESSENTIA_TRANSFUSER.get(), EssentiaTransfuserBlockEntity::tick);
    }
}
