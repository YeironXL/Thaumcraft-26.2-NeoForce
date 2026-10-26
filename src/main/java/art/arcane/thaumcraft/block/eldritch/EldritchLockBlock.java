package art.arcane.thaumcraft.block.eldritch;

import art.arcane.thaumcraft.ModItems;
import art.arcane.thaumcraft.registry.ModBlockEntities;
import art.arcane.thaumcraft.world.outer.OuterLands;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;

/** TC4 BlockEldritchLock ("Ancient Locking Mechanism"): insert a Runed Tablet and the Ancient Doorway slowly opens. */
public final class EldritchLockBlock extends BaseEntityBlock {
    public static final MapCodec<EldritchLockBlock> CODEC = simpleCodec(EldritchLockBlock::new);
    public static final EnumProperty<Direction> FACING = HorizontalDirectionalBlock.FACING;
    public EldritchLockBlock(Properties p) { super(p); registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH)); }
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> b) { b.add(FACING); }
    @Override public BlockState getStateForPlacement(BlockPlaceContext c) { return defaultBlockState().setValue(FACING, c.getHorizontalDirection().getOpposite()); }
    @Override protected MapCodec<? extends BaseEntityBlock> codec() { return CODEC; }
    @Override protected RenderShape getRenderShape(BlockState s) { return RenderShape.MODEL; }
    @Override public BlockEntity newBlockEntity(BlockPos pos, BlockState s) { return new EldritchLockBlockEntity(pos, s); }
    @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide() ? null : createTickerHelper(type, ModBlockEntities.ELDRITCH_LOCK.get(), EldritchLockBlockEntity::tick);
    }
    @Override protected InteractionResult useItemOn(ItemStack stack, BlockState s, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (!stack.is(ModItems.RUNED_TABLET.get())) return InteractionResult.TRY_WITH_EMPTY_HAND;
        if (level.isClientSide()) return InteractionResult.SUCCESS;
        if (!(level.getBlockEntity(pos) instanceof EldritchLockBlockEntity be) || be.opening()) return InteractionResult.FAIL;
        if (!OuterLands.is(level)) { if (!(player instanceof net.minecraft.server.level.ServerPlayer sp) || sp.connection != null) player.sendOverlayMessage(Component.translatable("tc.eldritch.lock.inert")); return InteractionResult.FAIL; }
        if (!player.getAbilities().instabuild) stack.shrink(1);
        be.unlock();
        level.playSound(null, pos, SoundEvents.END_PORTAL_FRAME_FILL, SoundSource.BLOCKS, 1.0f, 0.6f);
        return InteractionResult.SUCCESS;
    }
}
