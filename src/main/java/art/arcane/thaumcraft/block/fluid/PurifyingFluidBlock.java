package art.arcane.thaumcraft.block.fluid;

import art.arcane.thaumcraft.progression.PlayerProgressManager;
import art.arcane.thaumcraft.registry.ModEffects;
import art.arcane.thaumcraft.registry.ModFluids;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.InsideBlockEffectApplier;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;

/** TC6 BlockFluidPure: a source block grants Warp Ward (shorter with more permanent warp) and is used up. */
public final class PurifyingFluidBlock extends LiquidBlock {
    private static final int QUANTA = 8;
    public PurifyingFluidBlock(Properties p) {
        super(ModFluids.PURE.get(), p.mapColor(MapColor.WOOL).replaceable().noCollision().strength(100.0f).pushReaction(PushReaction.DESTROY).noLootTable().liquid().lightLevel(s -> 5));
    }

    @Override
    protected void entityInside(BlockState state, Level level, BlockPos pos, Entity entity, InsideBlockEffectApplier fx, boolean precise) {
        float q = LiquidDeathBlock.quantaPercentage(state, QUANTA);
        var m = entity.getDeltaMovement();
        entity.setDeltaMovement(m.x * (1.0f - q / 2.0f), m.y, m.z * (1.0f - q / 2.0f));
        if (!level.isClientSide() && state.getFluidState().isSource() && entity instanceof ServerPlayer p && !p.hasEffect(ModEffects.WARP_WARD)) {
            p.addEffect(new MobEffectInstance(ModEffects.WARP_WARD, wardDuration(PlayerProgressManager.getWarp(p, PlayerProgressManager.WarpType.PERMANENT)), 0, true, true));
            level.removeBlock(pos, false);
        }
    }

    /** Warp ward length: 200000 / sqrt(permanent warp), capped at 32000 ticks. */
    public static int wardDuration(int permanentWarp) {
        int div = 1;
        if (permanentWarp > 0) div = Math.max(1, (int) Math.sqrt(permanentWarp));
        return Math.min(32000, 200000 / div);
    }

    @Override
    public void animateTick(BlockState state, Level world, BlockPos pos, RandomSource rand) {
        int meta = LiquidDeathBlock.meta(state, QUANTA);
        if (rand.nextInt(10) == 0)
            world.addParticle(ParticleTypes.WHITE_ASH, pos.getX() + rand.nextFloat(), pos.getY() + 0.125f * (8 - meta), pos.getZ() + rand.nextFloat(), 0, 0.01, 0);
        if (rand.nextInt(50) == 0)
            world.playLocalSound(pos.getX() + rand.nextFloat(), pos.getY() + 0.5, pos.getZ() + rand.nextFloat(), SoundEvents.LAVA_POP, SoundSource.BLOCKS, 0.1f + rand.nextFloat() * 0.1f, 0.9f + rand.nextFloat() * 0.15f, false);
    }
}
