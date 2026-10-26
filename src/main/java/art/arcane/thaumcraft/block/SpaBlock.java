package art.arcane.thaumcraft.block;

import art.arcane.thaumcraft.blockentity.SpaBlockEntity;
import art.arcane.thaumcraft.registry.ModBlockEntities;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.neoforged.neoforge.transfer.fluid.FluidUtil;

/** TC6 BlockSpa: a fluid container in hand fills the tank, otherwise the GUI opens. */
public final class SpaBlock extends BaseEntityBlock {
    public static final MapCodec<SpaBlock> CODEC = simpleCodec(SpaBlock::new);
    public SpaBlock(Properties p) { super(p.strength(2.0f, 20.0f).sound(SoundType.STONE)); }
    @Override protected MapCodec<? extends BaseEntityBlock> codec() { return CODEC; }
    @Override protected RenderShape getRenderShape(BlockState s) { return RenderShape.MODEL; }
    @Override public BlockEntity newBlockEntity(BlockPos pos, BlockState state) { return new SpaBlockEntity(pos, state); }
    @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide() ? null : createTickerHelper(type, ModBlockEntities.SPA.get(), SpaBlockEntity::tick);
    }

    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level world, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (player.isShiftKeyDown() || FluidUtil.getFirstStackContained(stack).isEmpty()) return InteractionResult.TRY_WITH_EMPTY_HAND;
        if (world.isClientSide()) return InteractionResult.SUCCESS;
        if (world.getBlockEntity(pos) instanceof SpaBlockEntity spa) {
            var fs = FluidUtil.getFirstStackContained(stack);
            var cur = spa.fluid();
            if (cur.getAmount() < 5000 && (cur.isEmpty() || net.neoforged.neoforge.fluids.FluidStack.isSameFluidSameComponents(cur, fs))
                    && FluidUtil.interactWithFluidHandler(player, hand, pos, spa.tank)) {
                world.playSound(null, pos, SoundEvents.BOTTLE_FILL, SoundSource.BLOCKS, 0.33f, 1.0f + (world.getRandom().nextFloat() - world.getRandom().nextFloat()) * 0.3f);
            }
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level world, BlockPos pos, Player player, BlockHitResult hit) {
        if (!world.isClientSide() && !player.isShiftKeyDown() && world.getBlockEntity(pos) instanceof SpaBlockEntity spa) player.openMenu(spa, buf -> buf.writeBlockPos(pos));
        return InteractionResult.SUCCESS;
    }
}
