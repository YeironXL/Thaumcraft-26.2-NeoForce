package art.arcane.thaumcraft.block;

import art.arcane.thaumcraft.blockentity.CrucibleBlockEntity;
import art.arcane.thaumcraft.registry.ModBlockEntities;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUtils;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.*;

public final class CrucibleBlock extends BaseEntityBlock {
    private static final MapCodec<CrucibleBlock> CODEC=simpleCodec(CrucibleBlock::new);
    public static final IntegerProperty WATER=IntegerProperty.create("water",0,4);
    private static final VoxelShape SHAPE=Shapes.or(Block.box(0,0,0,16,5,16),Block.box(0,5,0,2,16,16),
            Block.box(14,5,0,16,16,16),Block.box(2,5,0,14,16,2),Block.box(2,5,14,14,16,16));
    public CrucibleBlock(Properties properties) { super(properties.strength(2).sound(SoundType.METAL).noOcclusion()); registerDefaultState(defaultBlockState().setValue(WATER,0)); }
    @Override protected MapCodec<CrucibleBlock> codec() { return CODEC; }
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> builder) { builder.add(WATER); }
    @Override protected VoxelShape getShape(BlockState state,BlockGetter level,BlockPos pos,CollisionContext context) { return SHAPE; }
    @Override public BlockEntity newBlockEntity(BlockPos pos,BlockState state) { return new CrucibleBlockEntity(pos,state); }
    @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level,BlockState state,BlockEntityType<T> type) {
        return createTickerHelper(type,ModBlockEntities.CRUCIBLE.get(),CrucibleBlockEntity::tick);
    }
    private int delay;
    /** BlockCrucible.onEntityCollidedWithBlock. */
    @Override protected void entityInside(BlockState state, Level level, BlockPos pos, net.minecraft.world.entity.Entity entity, net.minecraft.world.entity.InsideBlockEffectApplier effects, boolean precise) {
        if (!level.isClientSide() && level.getBlockEntity(pos) instanceof CrucibleBlockEntity tile) {
            if (entity instanceof net.minecraft.world.entity.item.ItemEntity item && !(entity instanceof art.arcane.thaumcraft.entity.SpecialItemEntity) && tile.mixture().boiling()) {
                tile.attemptSmelt(item);
            } else if (++delay >= 10) {
                delay = 0;
                if (entity instanceof net.minecraft.world.entity.LivingEntity && tile.mixture().boiling()) {
                    entity.hurt(level.damageSources().inFire(), 1.0f);
                    level.playSound(null, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, net.minecraft.sounds.SoundEvents.LAVA_EXTINGUISH, net.minecraft.sounds.SoundSource.BLOCKS, 0.4f, 2.0f + level.getRandom().nextFloat() * 0.4f);
                }
            }
        }
    }
    @Override public void animateTick(BlockState state, Level level, BlockPos pos, net.minecraft.util.RandomSource r) {
        if (r.nextInt(10) == 0 && level.getBlockEntity(pos) instanceof CrucibleBlockEntity tile && tile.mixture().boiling())
            level.playLocalSound(pos.getX(), pos.getY(), pos.getZ(), net.minecraft.sounds.SoundEvents.LAVA_POP, net.minecraft.sounds.SoundSource.BLOCKS, 0.1f + r.nextFloat() * 0.1f, 1.2f + r.nextFloat() * 0.2f, false);
    }
    @Override protected InteractionResult useItemOn(ItemStack stack,BlockState state,Level level,BlockPos pos,Player player,InteractionHand hand,BlockHitResult hit) {
        if (!player.mayUseItemAt(pos,hit.getDirection(),stack)) return InteractionResult.PASS;
        if (level.isClientSide()) return InteractionResult.SUCCESS;
        if (!(player instanceof ServerPlayer actor) || !(level.getBlockEntity(pos) instanceof CrucibleBlockEntity crucible)) return InteractionResult.PASS;
        if (stack.is(Items.WATER_BUCKET)) {
            if (crucible.mixture().water() < 1000 && crucible.mixture().fill()) {
                player.setItemInHand(hand,ItemUtils.createFilledResult(stack,player,new ItemStack(Items.BUCKET)));
                crucible.changed();
                level.playSound(null, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, net.minecraft.sounds.SoundEvents.BOTTLE_FILL, net.minecraft.sounds.SoundSource.BLOCKS, 0.33f, 1.0f + (level.getRandom().nextFloat() - level.getRandom().nextFloat()) * 0.3f);
                return InteractionResult.SUCCESS;
            }
        }
        // Right-clicking the top with an item dissolves or crafts one of it (not with a caster, not sneaking).
        if (!player.isShiftKeyDown() && !(stack.getItem() instanceof art.arcane.thaumcraft.item.CasterItem) && hit.getDirection() == Direction.UP
                && crucible.mixture().boiling() && crucible.attemptSmelt(stack.copyWithCount(1),actor) == 0) {
            stack.shrink(1); return InteractionResult.SUCCESS;
        }
        return InteractionResult.TRY_WITH_EMPTY_HAND;
    }
    @Override protected InteractionResult useWithoutItem(BlockState state,Level level,BlockPos pos,Player player,BlockHitResult hit) {
        if (!player.isShiftKeyDown() || !player.getMainHandItem().isEmpty()) return InteractionResult.PASS;
        if (!level.isClientSide() && level.getBlockEntity(pos) instanceof CrucibleBlockEntity crucible) crucible.empty();
        return InteractionResult.SUCCESS;
    }
    @Override protected boolean hasAnalogOutputSignal(BlockState state) { return true; }
    @Override protected int getAnalogOutputSignal(BlockState state,Level level,BlockPos pos,Direction direction) {
        if (!(level.getBlockEntity(pos) instanceof CrucibleBlockEntity crucible)) return 0;
        long total=crucible.mixture().total(); return Math.min(15,net.minecraft.util.Mth.floor(total/500.0f*14.0f)+(total>0?1:0));
    }
}
