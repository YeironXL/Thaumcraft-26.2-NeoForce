package art.arcane.thaumcraft.block;

import art.arcane.thaumcraft.blockentity.EverfullUrnBlockEntity;
import art.arcane.thaumcraft.registry.ModBlockEntities;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;
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
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.neoforge.transfer.fluid.FluidUtil;

/** TC6 BlockWaterJug: buckets and fluid containers use its tank; a glass bottle takes 333 mB as a water bottle. */
public final class EverfullUrnBlock extends BaseEntityBlock {
    public static final MapCodec<EverfullUrnBlock> CODEC = simpleCodec(EverfullUrnBlock::new);
    private static final VoxelShape SHAPE = Block.box(3, 0, 3, 13, 16, 13);
    public EverfullUrnBlock(Properties p) { super(p.strength(2.0f, 20.0f).sound(SoundType.STONE).noOcclusion()); }
    @Override protected MapCodec<? extends BaseEntityBlock> codec() { return CODEC; }
    @Override protected RenderShape getRenderShape(BlockState s) { return RenderShape.MODEL; }
    @Override protected VoxelShape getShape(BlockState s, BlockGetter l, BlockPos p, CollisionContext c) { return SHAPE; }
    @Override public BlockEntity newBlockEntity(BlockPos pos, BlockState state) { return new EverfullUrnBlockEntity(pos, state); }
    @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return createTickerHelper(type, ModBlockEntities.EVERFULL_URN.get(), EverfullUrnBlockEntity::tick);
    }

    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level world, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (world.isClientSide()) return InteractionResult.SUCCESS;
        if (!(world.getBlockEntity(pos) instanceof EverfullUrnBlockEntity urn)) return InteractionResult.SUCCESS;
        if (FluidUtil.interactWithFluidHandler(player, hand, pos, urn.tank)) {
            world.playSound(null, pos, SoundEvents.BOTTLE_FILL, SoundSource.BLOCKS, 0.33f, 1.0f + (world.getRandom().nextFloat() - world.getRandom().nextFloat()) * 0.3f);
        } else if (stack.is(Items.GLASS_BOTTLE) && urn.amount() >= 333) {
            ItemStack water = PotionContents.createItemStack(Items.POTION, Potions.WATER);
            if (!player.getAbilities().instabuild) stack.shrink(1);
            if (stack.isEmpty()) player.setItemInHand(hand, water);
            else if (!player.getInventory().add(water)) player.drop(water, false);
            try (var tx = net.neoforged.neoforge.transfer.transaction.Transaction.openRoot()) {
                urn.tank.extract(0, net.neoforged.neoforge.transfer.fluid.FluidResource.of(net.minecraft.world.level.material.Fluids.WATER), 333, tx);
                tx.commit();
            }
            world.playSound(null, pos, SoundEvents.BOTTLE_FILL, SoundSource.BLOCKS, 0.33f, 1.0f + (world.getRandom().nextFloat() - world.getRandom().nextFloat()) * 0.3f);
        }
        return InteractionResult.SUCCESS;
    }

    @Override protected InteractionResult useWithoutItem(BlockState s, Level w, BlockPos p, Player pl, BlockHitResult h) { return InteractionResult.SUCCESS; }

    @Override
    public void animateTick(BlockState state, Level world, BlockPos pos, RandomSource rand) {
        if (world.getBlockEntity(pos) instanceof EverfullUrnBlockEntity urn && urn.amount() >= 1000)
            art.arcane.thaumcraft.client.fx.FXDispatcher.jarSplashFx(pos.getX() + 0.5, pos.getY() + 1, pos.getZ() + 0.5);
    }
}
