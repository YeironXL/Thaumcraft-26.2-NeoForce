package art.arcane.thaumcraft.block;

import art.arcane.thaumcraft.ModBlocks;
import art.arcane.thaumcraft.blockentity.PedestalBlockEntity;
import art.arcane.thaumcraft.crafting.infusion.IInfusionStabiliserExt;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/** TC6 BlockPedestal: holds one item for infusion; acts as an infusion stabiliser with a symmetry penalty for uneven loads. */
public final class PedestalBlock extends BaseEntityBlock implements IInfusionStabiliserExt {
    public static final MapCodec<PedestalBlock> CODEC = simpleCodec(PedestalBlock::new);
    private static final VoxelShape SHAPE = Block.box(0, 0, 0, 16, 16, 16);

    public PedestalBlock(Properties p) {
        super(p.strength(2.0f, 20.0f).sound(SoundType.STONE).noOcclusion());
        registerDefaultState(stateDefinition.any().setValue(InlayBlock.CHARGE, 0));
    }
    @Override protected void createBlockStateDefinition(net.minecraft.world.level.block.state.StateDefinition.Builder<Block, BlockState> b) { b.add(InlayBlock.CHARGE); }

    /** randomDisplayTick: red runes while charged by an inlay network. */
    @Override public void animateTick(BlockState s, Level world, BlockPos pos, net.minecraft.util.RandomSource rand) {
        if (s.getValue(InlayBlock.CHARGE) > 0)
            world.addParticle(net.minecraft.core.particles.ColorParticleOption.create(art.arcane.thaumcraft.registry.ModParticles.SCAN_RUNE.get(), net.minecraft.util.ARGB.colorFromFloat(1, 1.0f, 0.0f, 0.0f)),
                    pos.getX() + 0.5, pos.getY() - 0.375 + 0.5, pos.getZ() + 0.5, -10, 0.0, 0);
    }
    @Override protected void onPlace(BlockState s, Level world, BlockPos pos, BlockState old, boolean moved) {
        if (world.isClientSide() || old.is(this)) return;
        InlayBlock.updateSurroundingInlay(world, pos, s);
        for (var d : net.minecraft.core.Direction.Plane.HORIZONTAL) InlayBlock.notifyInlayNeighborsOfStateChange(world, pos.relative(d));
    }
    @Override protected void affectNeighborsAfterRemoval(BlockState s, net.minecraft.server.level.ServerLevel world, BlockPos pos, boolean moved) {
        super.affectNeighborsAfterRemoval(s, world, pos, moved);
        for (var d : net.minecraft.core.Direction.Plane.HORIZONTAL) world.updateNeighborsAt(pos.relative(d), this);
        for (var d : net.minecraft.core.Direction.Plane.HORIZONTAL) InlayBlock.notifyInlayNeighborsOfStateChange(world, pos.relative(d));
    }
    @Override protected void neighborChanged(BlockState s, Level world, BlockPos pos, Block b, net.minecraft.world.level.redstone.Orientation o, boolean moved) {
        if (!world.isClientSide()) InlayBlock.updateSurroundingInlay(world, pos, s);
    }
    @Override protected MapCodec<? extends BaseEntityBlock> codec() { return CODEC; }
    @Override protected RenderShape getRenderShape(BlockState s) { return RenderShape.MODEL; }
    @Override public BlockEntity newBlockEntity(BlockPos pos, BlockState state) { return new PedestalBlockEntity(pos, state); }
    @Override protected VoxelShape getShape(BlockState s, BlockGetter l, BlockPos p, CollisionContext c) { return SHAPE; }

    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level world, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        return interact(stack, world, pos, player);
    }

    static InteractionResult interact(ItemStack stack, Level world, BlockPos pos, Player player) {
        if (world.isClientSide()) return InteractionResult.SUCCESS;
        if (!(world.getBlockEntity(pos) instanceof PedestalBlockEntity ped)) return InteractionResult.TRY_WITH_EMPTY_HAND;
        if (ped.item().isEmpty() && !stack.isEmpty() && ped.canPlaceItem(0, stack)) {
            ItemStack i = stack.copyWithCount(1);
            ped.setItem(0, i);
            stack.shrink(1);
            world.playSound(null, pos, SoundEvents.ITEM_PICKUP, SoundSource.BLOCKS, 0.2f, ((world.getRandom().nextFloat() - world.getRandom().nextFloat()) * 0.7f + 1.0f) * 1.6f);
            return InteractionResult.SUCCESS;
        }
        return take(world, pos, player, ped) ? InteractionResult.SUCCESS : InteractionResult.TRY_WITH_EMPTY_HAND;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level world, BlockPos pos, Player player, BlockHitResult hit) {
        return interactEmpty(world, pos, player);
    }

    static InteractionResult interactEmpty(Level world, BlockPos pos, Player player) {
        if (world.isClientSide()) return InteractionResult.SUCCESS;
        if (world.getBlockEntity(pos) instanceof PedestalBlockEntity ped && take(world, pos, player, ped)) return InteractionResult.SUCCESS;
        return InteractionResult.PASS;
    }

    /** InventoryUtils.dropItemsAtEntity: the held item pops out at the player. */
    private static boolean take(Level world, BlockPos pos, Player player, PedestalBlockEntity ped) {
        if (ped.item().isEmpty()) return false;
        ItemStack out = ped.removeItemNoUpdate(0);
        ped.setItem(0, ItemStack.EMPTY);
        var e = new ItemEntity(world, player.getX(), player.getY() + player.getEyeHeight() / 2.0f, player.getZ(), out);
        world.addFreshEntity(e);
        world.playSound(null, pos, SoundEvents.ITEM_PICKUP, SoundSource.BLOCKS, 0.2f, ((world.getRandom().nextFloat() - world.getRandom().nextFloat()) * 0.7f + 1.0f) * 1.5f);
        return true;
    }

    @Override public boolean canStabaliseInfusion(Level world, BlockPos pos) { return true; }
    @Override public float getStabilizationAmount(Level world, BlockPos pos) { return world.getBlockState(pos).is(ModBlocks.PEDESTAL_ELDRITCH.get()) ? 0.1f : 0.0f; }
    @Override public boolean hasSymmetryPenalty(Level world, BlockPos p1, BlockPos p2) {
        return world.getBlockEntity(p1) instanceof PedestalBlockEntity a && world.getBlockEntity(p2) instanceof PedestalBlockEntity b && a.item().isEmpty() != b.item().isEmpty();
    }
    @Override public float getSymmetryPenalty(Level world, BlockPos pos) { return 0.1f; }
}
