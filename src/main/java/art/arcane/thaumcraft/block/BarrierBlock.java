package art.arcane.thaumcraft.block;

import art.arcane.thaumcraft.ModBlocks;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.EntityCollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * TC6 BlockBarrier ("Warding Aura"): invisible and replaceable, solid only to non-player living creatures without a
 * player passenger, and only while the paving stone below (one or two blocks down) is unpowered. Disappears when it
 * is no longer standing on a barrier stone or another barrier.
 */
public final class BarrierBlock extends Block {
    public static final MapCodec<BarrierBlock> CODEC = simpleCodec(BarrierBlock::new);
    public BarrierBlock(Properties p) { super(p.noOcclusion().noLootTable().replaceable().pushReaction(PushReaction.DESTROY).isViewBlocking((s, l, pos) -> false).isSuffocating((s, l, pos) -> false)); }
    @Override protected MapCodec<? extends Block> codec() { return CODEC; }
    @Override protected RenderShape getRenderShape(BlockState s) { return RenderShape.INVISIBLE; }
    @Override protected VoxelShape getShape(BlockState s, BlockGetter l, BlockPos p, CollisionContext c) { return Shapes.empty(); }
    @Override protected VoxelShape getCollisionShape(BlockState s, BlockGetter world, BlockPos pos, CollisionContext ctx) {
        if (ctx instanceof EntityCollisionContext ec && ec.getEntity() instanceof LivingEntity e && !(e instanceof Player) && e.getPassengersAndSelf().noneMatch(x -> x instanceof Player)) {
            int a = world.getBlockState(pos.below()).is(ModBlocks.PAVING_STONE_BARRIER.get()) ? 1 : 2;
            if (!(world instanceof net.minecraft.world.level.Level lv && lv.hasNeighborSignal(pos.below(a)))) return Shapes.block();
        }
        return Shapes.empty();
    }
    @Override protected BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess ticks, BlockPos pos, Direction dir, BlockPos np, BlockState ns, RandomSource r) {
        BlockState below = level.getBlockState(pos.below());
        if (!below.is(ModBlocks.PAVING_STONE_BARRIER.get()) && !below.is(this)) return net.minecraft.world.level.block.Blocks.AIR.defaultBlockState();
        return state;
    }
    @Override protected ItemStack getCloneItemStack(LevelReader level, BlockPos pos, BlockState state, boolean includeData) { return ItemStack.EMPTY; }
}
