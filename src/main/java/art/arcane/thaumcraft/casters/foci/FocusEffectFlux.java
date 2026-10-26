package art.arcane.thaumcraft.casters.foci;

import art.arcane.thaumcraft.casters.*;
import art.arcane.thaumcraft.fx.GenericParticleOptions;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;

public final class FocusEffectFlux extends FocusEffect {
    @Override public String getResearch() { return "FOCUSFLUX"; }
    @Override public String getKey() { return "thaumcraft.FLUX"; }
    @Override public String getAspect() { return "vitium"; }
    @Override public int getComplexity() { return getSettingValue("power") * 3; }
    @Override public float getDamageForDisplay(float finalPower) { return (3 + getSettingValue("power")) * finalPower; }

    @Override
    public boolean execute(HitResult target, Trajectory trajectory, float finalPower, int num) {
        Level world = getPackage().world;
        var hv = target.getLocation();
        FocusFx.impact(world, hv.x, hv.y, hv.z, getKey());
        if (target instanceof EntityHitResult eh && world instanceof ServerLevel sl)
            eh.getEntity().hurtServer(sl, sl.damageSources().indirectMagic(eh.getEntity(), getPackage().getCaster()), getDamageForDisplay(finalPower));
        return false;
    }

    @Override public NodeSetting[] createSettings() {
        return new NodeSetting[] {new NodeSetting("power", "focus.common.power", new NodeSetting.NodeSettingIntRange(1, 5))};
    }

    @Override
    public void onCast(Entity caster) {
        caster.level().playSound(null, caster.blockPosition().above(), SoundEvents.CHORUS_FLOWER_GROW, SoundSource.PLAYERS, 2.0f, 2.0f + (float) (caster.level().getRandom().nextGaussian() * 0.1));
    }

    @Override
    public void renderParticleFX(Level world, double x, double y, double z, double vx, double vy, double vz) {
        var r = world.getRandom();
        FociFx.gen(x, y, z, vx + r.nextGaussian() * 0.01, vy + r.nextGaussian() * 0.01, vz + r.nextGaussian() * 0.01, GenericParticleOptions.builder()
                .maxAge((int) (15.0f + 10.0f * r.nextFloat())).color(0.25f + r.nextFloat() * 0.25f, 0.0f, 0.25f + r.nextFloat() * 0.25f).alpha(0.0f, 1.0f, 1.0f, 0.0f)
                .grid(64).particles(128, 14, 1).scale(2.0f + r.nextFloat(), 0.25f + r.nextFloat() * 0.25f).loop(true).slowDown(0.9)
                .gravity((float) (r.nextGaussian() * 0.1)).randomMovement(0.0125f, 0.0125f, 0.0125f).rotation((float) r.nextGaussian()), r.nextInt(4));
    }
}
