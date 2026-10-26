package art.arcane.thaumcraft.block.world;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.InsideBlockEffectApplier;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.VegetationBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/** TC6 BlockPlantCinderpearl / BlockPlantShimmerleaf / BlockPlantVishroom. */
public final class TcPlantBlock extends VegetationBlock {
    public enum Kind { CINDERPEARL, SHIMMERLEAF, VISHROOM }
    private static final VoxelShape SHAPE = Block.box(3.2, 0, 3.2, 12.8, 12.8, 12.8); // BlockBush default AABB
    private final Kind kind;

    public TcPlantBlock(Kind kind, Properties p) { super(p); this.kind = kind; }

    @Override protected MapCodec<? extends VegetationBlock> codec() { return simpleCodec(p -> new TcPlantBlock(kind, p)); }
    @Override protected VoxelShape getShape(BlockState s, BlockGetter l, BlockPos p, CollisionContext c) { return SHAPE.move(s.getOffset(p)); }

    @Override
    protected boolean mayPlaceOn(BlockState state, BlockGetter level, BlockPos pos) {
        return switch (kind) {
            // canSustainBush: sand, dirt, (stained) hardened clay
            case CINDERPEARL -> state.is(BlockTags.SAND) || state.is(Blocks.DIRT) || state.is(BlockTags.TERRACOTTA);
            // grass, dirt
            case SHIMMERLEAF -> state.is(Blocks.GRASS_BLOCK) || state.is(Blocks.DIRT);
            // any full block
            case VISHROOM -> state.isCollisionShapeFullBlock(level, pos);
        };
    }

    @Override
    protected void entityInside(BlockState state, Level level, BlockPos pos, Entity entity, InsideBlockEffectApplier applier, boolean precise) {
        if (kind == Kind.VISHROOM && level instanceof ServerLevel && entity instanceof LivingEntity living && level.getRandom().nextInt(5) == 0) {
            living.addEffect(new MobEffectInstance(MobEffects.NAUSEA, 200, 0));
        }
    }

    @Override
    public void animateTick(BlockState state, Level world, BlockPos pos, RandomSource rand) {
        switch (kind) {
            case CINDERPEARL -> {
                if (rand.nextBoolean()) {
                    float xr = pos.getX() + 0.5f + (rand.nextFloat() - rand.nextFloat()) * 0.1f;
                    float yr = pos.getY() + 0.6f + (rand.nextFloat() - rand.nextFloat()) * 0.1f;
                    float zr = pos.getZ() + 0.5f + (rand.nextFloat() - rand.nextFloat()) * 0.1f;
                    world.addParticle(net.minecraft.core.particles.ParticleTypes.SMOKE, xr, yr, zr, 0, 0, 0);
                    world.addParticle(net.minecraft.core.particles.ParticleTypes.FLAME, xr, yr, zr, 0, 0, 0);
                }
            }
            case SHIMMERLEAF -> {
                if (rand.nextInt(3) == 0) {
                    float xr = (float) (pos.getX() + 0.5f + rand.nextGaussian() * 0.1);
                    float yr = (float) (pos.getY() + 0.4f + rand.nextGaussian() * 0.1);
                    float zr = (float) (pos.getZ() + 0.5f + rand.nextGaussian() * 0.1);
                    art.arcane.thaumcraft.client.fx.FXDispatcher.drawWispyMotes(xr, yr, zr, rand.nextGaussian() * 0.01, rand.nextGaussian() * 0.01, rand.nextGaussian() * 0.01,
                            10, 0.3f + rand.nextFloat() * 0.3f, 0.7f + rand.nextFloat() * 0.3f, 0.7f + rand.nextFloat() * 0.3f, 0.0f);
                }
            }
            case VISHROOM -> {
                if (rand.nextInt(3) == 0) {
                    float xr = pos.getX() + 0.5f + (rand.nextFloat() - rand.nextFloat()) * 0.4f;
                    float zr = pos.getZ() + 0.5f + (rand.nextFloat() - rand.nextFloat()) * 0.4f;
                    art.arcane.thaumcraft.client.fx.FXDispatcher.drawWispyMotes(xr, pos.getY() + 0.3f, zr, 0, 0, 0, 10, 0.5f, 0.3f, 0.8f, 0.001f);
                }
            }
        }
    }
}
