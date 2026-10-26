package art.arcane.thaumcraft.block;

import art.arcane.thaumcraft.ModBlocks;
import art.arcane.thaumcraft.blockentity.BarrierStoneBlockEntity;
import art.arcane.thaumcraft.registry.ModBlockEntities;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

/**
 * TC6 BlockPavingStone. Travel: walking on it gives Speed II and Jump Boost for 2 seconds. Barrier: a TileBarrierStone
 * that keeps a warding barrier in the two blocks above (stopping non-player creatures) unless it gets redstone power.
 */
public class PavingStoneBlock extends Block implements EntityBlock {
    private static final VoxelShape SHAPE = Block.box(0, 0, 0, 16, 15, 16);
    private final boolean barrier;

    public PavingStoneBlock(Properties p, boolean barrier) {
        super(p.strength(2.5F).sound(SoundType.STONE).noOcclusion());
        this.barrier = barrier;
    }
    @Override protected MapCodec<? extends Block> codec() { return simpleCodec(p -> new PavingStoneBlock(p, barrier)); }
    @Override protected VoxelShape getShape(BlockState s, BlockGetter l, BlockPos p, CollisionContext c) { return SHAPE; }

    @Override public void stepOn(Level level, BlockPos pos, BlockState state, Entity e) {
        if (!level.isClientSide() && !barrier && e instanceof LivingEntity le) {
            le.addEffect(new MobEffectInstance(MobEffects.SPEED, 40, 1, false, false));
            le.addEffect(new MobEffectInstance(MobEffects.JUMP_BOOST, 40, 0, false, false));
        }
        super.stepOn(level, pos, state, e);
    }

    @Override public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) { return barrier ? new BarrierStoneBlockEntity(pos, state) : null; }
    @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return !barrier || level.isClientSide() || type != ModBlockEntities.BARRIER_STONE.get() ? null : (l, p, s, te) -> BarrierStoneBlockEntity.tick(l, p, s, (BarrierStoneBlockEntity) te);
    }

    private static void rune(Level world, BlockPos pos, double y, float r, float g, float b) {
        var rnd = world.getRandom();
        world.addParticle(new DustParticleOptions(((int) (r * 255) << 16) | ((int) (g * 255) << 8) | (int) (b * 255), 0.8f),
                pos.getX() + 0.2 + rnd.nextDouble() * 0.6, y, pos.getZ() + 0.2 + rnd.nextDouble() * 0.6, 0, -0.02, 0);
    }

    /** randomDisplayTick: blue runes when powered, red when the barrier stands, faint ones when a creature is near. */
    @Override public void animateTick(BlockState state, Level world, BlockPos pos, RandomSource random) {
        if (!barrier) return;
        if (world.hasNeighborSignal(pos)) {
            for (int a = 0; a < 4; ++a) rune(world, pos, pos.getY() + 0.7, 0.2f + random.nextFloat() * 0.4f, random.nextFloat() * 0.3f, 0.8f + random.nextFloat() * 0.2f);
        } else if (world.getBlockState(pos.above()).is(ModBlocks.BARRIER.get()) || world.getBlockState(pos.above(2)).is(ModBlocks.BARRIER.get())) {
            for (int a = 0; a < 6; ++a) rune(world, pos, pos.getY() + 0.7, 0.9f + random.nextFloat() * 0.1f, random.nextFloat() * 0.3f, random.nextFloat() * 0.3f);
        } else {
            for (Entity entity : world.getEntities(null, new AABB(pos).inflate(1.0))) {
                if (entity instanceof LivingEntity && !(entity instanceof Player)) {
                    rune(world, pos, pos.getY() + 0.6 + random.nextFloat() * Math.max(0.8f, entity.getEyeHeight()), 0.6f + random.nextFloat() * 0.4f, 0.0f, 0.3f + random.nextFloat() * 0.7f);
                    break;
                }
            }
        }
    }
}
