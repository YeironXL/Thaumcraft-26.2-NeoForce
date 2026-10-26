package art.arcane.thaumcraft.casters.foci;

import art.arcane.thaumcraft.casters.*;
import art.arcane.thaumcraft.fx.GenericParticleOptions;
import art.arcane.thaumcraft.registry.ModSounds;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;

public final class FocusEffectAir extends FocusEffect {
    @Override public String getResearch() { return "FOCUSELEMENTAL"; }
    @Override public String getKey() { return "thaumcraft.AIR"; }
    @Override public String getAspect() { return "aer"; }
    @Override public int getComplexity() { return getSettingValue("power") * 2; }
    @Override public float getDamageForDisplay(float finalPower) { return (1 + getSettingValue("power")) * finalPower; }

    @Override
    public boolean execute(HitResult target, Trajectory trajectory, float finalPower, int num) {
        Level world = getPackage().world;
        var hv = target.getLocation();
        FocusFx.impact(world, hv.x, hv.y, hv.z, getKey());
        world.playSound(null, hv.x, hv.y, hv.z, SoundEvents.ENDER_DRAGON_FLAP, SoundSource.PLAYERS, 0.5f, 0.66f);
        if (target instanceof EntityHitResult eh) {
            float damage = getDamageForDisplay(finalPower);
            Entity e = eh.getEntity();
            var src = world.damageSources().thrown(e, getPackage().getCaster());
            if (world instanceof ServerLevel sl) e.hurtServer(sl, src, damage);
            if (e instanceof LivingEntity le) {
                if (trajectory != null) le.knockback(damage * 0.25f, -trajectory.direction.x, -trajectory.direction.z, src, damage);
                else le.knockback(damage * 0.25f, -Mth.sin(e.getYRot() * 0.017453292f), Mth.cos(e.getYRot() * 0.017453292f), src, damage);
            }
            return true;
        }
        return false;
    }

    @Override public NodeSetting[] createSettings() {
        return new NodeSetting[] {new NodeSetting("power", "focus.common.power", new NodeSetting.NodeSettingIntRange(1, 5))};
    }

    @Override
    public void renderParticleFX(Level world, double x, double y, double z, double mx, double my, double mz) {
        var r = world.getRandom();
        float s = (float) (2.0 + r.nextGaussian() * 0.5);
        FociFx.gen(x, y, z, mx, my, mz, GenericParticleOptions.builder().gravity(-0.1f).maxAge(20 + r.nextInt(10)).alpha(0.5f, 0.0f).grid(32)
                .particles(337, 5, 1).slowDown(0.75).rotation(0, (float) r.nextGaussian() / 2.0f).scale(s, s * 2.0f), 0);
    }

    @Override
    public void onCast(Entity caster) {
        caster.level().playSound(null, caster.blockPosition().above(), ModSounds.get("wind"), SoundSource.PLAYERS, 0.125f, 2.0f);
    }
}
