package art.arcane.thaumcraft.block;

import art.arcane.thaumcraft.events.ServerEvents;
import art.arcane.thaumcraft.fx.GenericParticleOptions;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.InsideBlockEffectApplier;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.AirBlock;
import net.minecraft.world.level.block.state.BlockState;

/** TC6 BlockEffect: invisible, air-like effect blocks (shock, sap, glimmer). Shock and sap vanish on a random tick. */
public final class EffectBlock extends AirBlock {
    public enum Kind { SHOCK, SAP, GLIMMER }
    private final Kind kind;
    public EffectBlock(Kind kind, Properties p) {
        super(p.air().noCollision().noLootTable().replaceable().randomTicks().explosionResistance(999.0f * 3 / 5).lightLevel(s -> kind == Kind.GLIMMER ? 15 : 7));
        this.kind = kind;
    }
    @Override public MapCodec<AirBlock> codec() { return simpleCodec(p -> new EffectBlock(kind, p)); }

    @Override
    protected void entityInside(BlockState state, Level world, BlockPos pos, Entity entity, InsideBlockEffectApplier applier, boolean precise) {
        if (!(world instanceof ServerLevel sl)) return;
        if (kind == Kind.SHOCK) {
            if (entity instanceof LivingEntity le) ServerEvents.addRunnableServer(sl, () -> {
                le.hurtServer(sl, sl.damageSources().magic(), 1.0f);
                le.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 20, 0, true, true));
            }, 0);
            if (sl.getRandom().nextInt(100) == 0) sl.removeBlock(pos, false);
        } else if (kind == Kind.SAP && entity instanceof LivingEntity le && !(entity instanceof art.arcane.thaumcraft.entity.EldritchMob) && !le.hasEffect(MobEffects.WITHER)) {
            ServerEvents.addRunnableServer(sl, () -> {
                le.addEffect(new MobEffectInstance(MobEffects.WITHER, 40, 0, true, true));
                le.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 40, 1, true, true));
                le.addEffect(new MobEffectInstance(MobEffects.HUNGER, 40, 1, true, true));
            }, 0);
        }
    }

    @Override
    protected void randomTick(BlockState state, ServerLevel world, BlockPos pos, RandomSource rand) {
        if (kind != Kind.GLIMMER) world.removeBlock(pos, false);
    }

    /** updateTick also served scheduled ticks (the warden's field frenzy schedules its sap to clear). */
    @Override
    protected void tick(BlockState state, ServerLevel world, BlockPos pos, RandomSource rand) {
        if (kind != Kind.GLIMMER) world.removeBlock(pos, false);
    }

    @Override
    public void animateTick(BlockState state, Level w, BlockPos pos, RandomSource r) {
        if (kind == Kind.GLIMMER) return;
        float h = r.nextFloat() * 0.33f;
        if (kind == Kind.SHOCK) spark(pos.getX() + r.nextFloat(), pos.getY() + 0.1515f + h / 2.0f, pos.getZ() + r.nextFloat(), 3.0f + h * 6.0f, 0.65f + r.nextFloat() * 0.1f, 1.0f, 1.0f, 0.8f, r);
        else spark(pos.getX() + r.nextFloat(), pos.getY() + 0.1515f + h / 2.0f, pos.getZ() + r.nextFloat(), 3.0f + h * 6.0f, 0.3f - r.nextFloat() * 0.1f, 0.0f, 0.5f + r.nextFloat() * 0.2f, 1.0f, r);
        if (r.nextInt(50) == 0) w.playLocalSound(pos.getX(), pos.getY(), pos.getZ(), art.arcane.thaumcraft.registry.ModSounds.get("jacobs"), SoundSource.AMBIENT, 0.25f, 1.0f + (r.nextFloat() - r.nextFloat()) * 0.2f, false);
    }

    /** FXDispatcher.spark. */
    private static void spark(double x, double y, double z, float size, float red, float g, float b, float a, RandomSource r) {
        art.arcane.thaumcraft.client.fx.FXDispatcher.generic(x, y, z, 0, 0, 0, GenericParticleOptions.builder().maxAge(5 + r.nextInt(5)).alpha(a).color(red, g, b)
                .grid(16).particles(8 + r.nextInt(3) * 16, 8, 1).scale(size).flipped(r.nextBoolean()).build(), 0);
    }
}
