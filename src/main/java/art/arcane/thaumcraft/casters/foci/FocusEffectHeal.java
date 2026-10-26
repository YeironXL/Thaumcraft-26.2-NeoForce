package art.arcane.thaumcraft.casters.foci;

import art.arcane.thaumcraft.casters.*;
import art.arcane.thaumcraft.fx.GenericParticleOptions;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.EntityTypeTags;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;

public final class FocusEffectHeal extends FocusEffect {
    @Override public String getResearch() { return "FOCUSHEAL"; }
    @Override public String getKey() { return "thaumcraft.HEAL"; }
    @Override public String getAspect() { return "victus"; }
    @Override public int getComplexity() { return getSettingValue("power") * 4; }
    @Override public float getDamageForDisplay(float finalPower) { return -getSettingValue("power") * finalPower; }

    @Override
    public boolean execute(HitResult target, Trajectory trajectory, float finalPower, int num) {
        Level world = getPackage().world;
        var hv = target.getLocation();
        FocusFx.impact(world, hv.x, hv.y, hv.z, getKey());
        if (target instanceof EntityHitResult eh && eh.getEntity() instanceof LivingEntity le) {
            if (le.isInvertedHealAndHarm()) {
                if (world instanceof ServerLevel sl) le.hurtServer(sl, sl.damageSources().indirectMagic(getPackage().getCaster(), getPackage().getCaster()), getSettingValue("power") * finalPower * 1.5f);
            } else le.heal(getSettingValue("power") * finalPower);
        }
        return false;
    }

    @Override public NodeSetting[] createSettings() {
        return new NodeSetting[] {new NodeSetting("power", "focus.heal.power", new NodeSetting.NodeSettingIntRange(1, 5))};
    }

    @Override
    public void onCast(Entity caster) {
        caster.level().playSound(null, caster.blockPosition().above(), SoundEvents.CHORUS_FLOWER_GROW, SoundSource.PLAYERS, 2.0f, 2.0f + (float) (caster.level().getRandom().nextGaussian() * 0.1));
    }

    @Override
    public void renderParticleFX(Level world, double x, double y, double z, double vx, double vy, double vz) {
        var r = world.getRandom();
        FociFx.gen(x, y, z, vx + r.nextGaussian() * 0.01, vy + r.nextGaussian() * 0.01, vz + r.nextGaussian() * 0.01, GenericParticleOptions.builder()
                .maxAge((int) (10.0f + 10.0f * r.nextFloat())).color(1, 1, 1).alpha(0.0f, 0.7f, 0.7f, 0.0f).grid(64).particles(0, 1, 1)
                .scale(r.nextFloat() * 2.0f, r.nextFloat()).slowDown(0.8).gravity((float) (r.nextGaussian() * 0.1)).randomMovement(0.0125f, 0.0125f, 0.0125f)
                .rotation((float) r.nextGaussian()), r.nextInt(4));
    }
}
