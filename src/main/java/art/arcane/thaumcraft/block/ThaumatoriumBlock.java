package art.arcane.thaumcraft.block;

import art.arcane.thaumcraft.ModBlocks;
import art.arcane.thaumcraft.blockentity.ThaumatoriumBlockEntity;
import art.arcane.thaumcraft.registry.ModBlockEntities;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
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
import net.minecraft.world.level.redstone.Orientation;
import net.minecraft.world.phys.BlockHitResult;

/**
 * TC6 BlockThaumatorium: the formed two-block machine above a crucible. Breaking either half (or losing the crucible
 * below) turns the rest back into alchemical construct blocks; it drops an alchemical construct.
 */
public final class ThaumatoriumBlock extends BaseEntityBlock {
    public static final EnumProperty<Direction> FACING = BlockStateProperties.HORIZONTAL_FACING;
    private final boolean top;
    public static final MapCodec<ThaumatoriumBlock> CODEC = simpleCodec(p -> new ThaumatoriumBlock(p, false));
    public ThaumatoriumBlock(Properties p, boolean top) {
        super(p.strength(2.0f, 20.0f).sound(SoundType.METAL).noOcclusion());
        this.top = top;
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
    }
    @Override protected MapCodec<? extends BaseEntityBlock> codec() { return CODEC; }
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> b) { b.add(FACING); }
    @Override protected RenderShape getRenderShape(BlockState s) { return top ? RenderShape.INVISIBLE : RenderShape.MODEL; }
    @Override public BlockEntity newBlockEntity(BlockPos pos, BlockState state) { return top ? new art.arcane.thaumcraft.blockentity.ThaumatoriumTopBlockEntity(pos, state) : new ThaumatoriumBlockEntity(pos, state); }
    @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return top ? null : createTickerHelper(type, ModBlockEntities.THAUMATORIUM.get(), ThaumatoriumBlockEntity::tick);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState s, Level world, BlockPos pos, Player player, BlockHitResult hit) {
        if (!world.isClientSide() && !player.isShiftKeyDown()) {
            BlockPos base = top ? pos.below() : pos;
            if (world.getBlockEntity(base) instanceof ThaumatoriumBlockEntity te) player.openMenu(te, buf -> buf.writeBlockPos(base));
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    protected void affectNeighborsAfterRemoval(BlockState state, ServerLevel level, BlockPos pos, boolean moved) {
        if (top && level.getBlockState(pos.below()).is(ModBlocks.THAUMATORIUM.get())) level.setBlockAndUpdate(pos.below(), ModBlocks.METAL_ALCHEMICAL.get().defaultBlockState());
        if (!top && level.getBlockState(pos.above()).is(ModBlocks.THAUMATORIUM_TOP.get())) level.setBlockAndUpdate(pos.above(), ModBlocks.METAL_ALCHEMICAL.get().defaultBlockState());
        super.affectNeighborsAfterRemoval(state, level, pos, moved);
    }

    @Override
    protected void neighborChanged(BlockState state, Level world, BlockPos pos, Block b, Orientation o, boolean moved) {
        if (!top && !world.isClientSide() && !world.getBlockState(pos.below()).is(ModBlocks.CRUCIBLE.get())) {
            world.setBlockAndUpdate(pos, ModBlocks.METAL_ALCHEMICAL.get().defaultBlockState());
            if (world.getBlockState(pos.above()).is(ModBlocks.THAUMATORIUM_TOP.get())) world.setBlockAndUpdate(pos.above(), ModBlocks.METAL_ALCHEMICAL.get().defaultBlockState());
        }
    }

    @Override protected boolean hasAnalogOutputSignal(BlockState s) { return !top; }
    @Override protected int getAnalogOutputSignal(BlockState s, Level world, BlockPos pos, Direction d) {
        return world.getBlockEntity(pos) instanceof ThaumatoriumBlockEntity te ? AbstractContainerMenu.getRedstoneSignalFromContainer(te) : 0;
    }
}
