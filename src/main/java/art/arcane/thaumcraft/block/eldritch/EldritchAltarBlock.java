package art.arcane.thaumcraft.block.eldritch;

import art.arcane.thaumcraft.ModItems;
import art.arcane.thaumcraft.registry.ModBlockEntities;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/** TC4 BlockEldritch altar: the pedestal under an Eldritch Obelisk that takes four Eldritch Eyes (Opening the Eye). */
public final class EldritchAltarBlock extends BaseEntityBlock {
    public static final MapCodec<EldritchAltarBlock> CODEC = simpleCodec(EldritchAltarBlock::new);
    public EldritchAltarBlock(Properties p) { super(p); }
    @Override protected MapCodec<? extends BaseEntityBlock> codec() { return CODEC; }
    @Override protected RenderShape getRenderShape(BlockState s) { return RenderShape.MODEL; }
    @Override public BlockEntity newBlockEntity(BlockPos pos, BlockState s) { return new EldritchAltarBlockEntity(pos, s); }
    @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide() ? null : createTickerHelper(type, ModBlockEntities.ELDRITCH_ALTAR.get(), EldritchAltarBlockEntity::tick);
    }
    @Override protected InteractionResult useItemOn(ItemStack stack, BlockState s, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (!stack.is(ModItems.ELDRITCH_EYE.get())) return InteractionResult.TRY_WITH_EMPTY_HAND;
        if (level.isClientSide()) return InteractionResult.SUCCESS;
        if (!(level.getBlockEntity(pos) instanceof EldritchAltarBlockEntity be) || !be.insertEye()) return InteractionResult.FAIL;
        if (!player.getAbilities().instabuild) stack.shrink(1);
        level.playSound(null, pos, SoundEvents.END_PORTAL_FRAME_FILL, SoundSource.BLOCKS, 1.0f, 0.8f);
        if (!(player instanceof net.minecraft.server.level.ServerPlayer sp) || sp.connection != null) player.sendOverlayMessage(Component.translatable("tc.eldritch.altar.eyes", be.eyes()));
        return InteractionResult.SUCCESS;
    }
}
