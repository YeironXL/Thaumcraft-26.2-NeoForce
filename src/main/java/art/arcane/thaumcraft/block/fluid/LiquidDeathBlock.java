package art.arcane.thaumcraft.block.fluid;

import art.arcane.thaumcraft.registry.ModFluids;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.InsideBlockEffectApplier;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;

/** TC6 BlockFluidDeath: slows and dissolves living things (damage 4..1 by depth, bypasses armour). */
public final class LiquidDeathBlock extends LiquidBlock {
    public static final ResourceKey<DamageType> DISSOLVE = ResourceKey.create(Registries.DAMAGE_TYPE, Identifier.fromNamespaceAndPath("thaumcraft", "dissolve"));
    private static final int QUANTA = 4;

    public LiquidDeathBlock(Properties p) {
        super(ModFluids.DEATH.get(), p.mapColor(MapColor.COLOR_PURPLE).replaceable().noCollision().strength(100.0f).pushReaction(PushReaction.DESTROY).noLootTable().liquid());
    }
    /** BlockFluidClassic metadata 0..quanta-1 from the vanilla 0..7 level. */
    static int meta(BlockState s, int quanta) { int lvl = s.getValue(LEVEL); return lvl >= 8 ? 0 : lvl * quanta / 8; }
    static float quantaPercentage(BlockState s, int quanta) { return (quanta - meta(s, quanta)) / (float) quanta; }

    @Override
    protected void entityInside(BlockState state, Level level, BlockPos pos, Entity entity, InsideBlockEffectApplier fx, boolean precise) {
        float q = quantaPercentage(state, QUANTA);
        var m = entity.getDeltaMovement();
        entity.setDeltaMovement(m.x * (1.0f - q / 2.0f), m.y, m.z * (1.0f - q / 2.0f));
        if (level instanceof ServerLevel sl && entity instanceof LivingEntity) {
            entity.hurtServer(sl, sl.damageSources().source(DISSOLVE), (float) (QUANTA - meta(state, QUANTA) + 1));
        }
    }

    @Override
    public void animateTick(BlockState state, Level world, BlockPos pos, RandomSource rand) {
        if (rand.nextInt(20) == 0) {
            // FXSlimyBubble in purple.
            int meta = meta(state, QUANTA);
            int col = ((int) ((0.3f - rand.nextFloat() * 0.1f) * 255) << 16) | (int) ((0.4f + rand.nextFloat() * 0.1f) * 255);
            world.addParticle(new DustParticleOptions(col, 0.8f), pos.getX() + rand.nextFloat(), pos.getY() + 0.1f + 0.225f * (QUANTA - meta), pos.getZ() + rand.nextFloat(), 0, 0.02, 0);
        }
        if (rand.nextInt(50) == 0)
            world.playLocalSound(pos.getX() + rand.nextFloat(), pos.getY() + 1.0, pos.getZ() + rand.nextFloat(), SoundEvents.LAVA_POP, SoundSource.BLOCKS, 0.1f + rand.nextFloat() * 0.1f, 0.9f + rand.nextFloat() * 0.15f, false);
    }
}
