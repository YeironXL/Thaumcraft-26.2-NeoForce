package art.arcane.thaumcraft.casters.foci;

import art.arcane.thaumcraft.casters.*;
import art.arcane.thaumcraft.events.ServerEvents;
import art.arcane.thaumcraft.fx.GenericParticleOptions;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;

public final class FocusEffectEarth extends FocusEffect {
    @Override public String getResearch() { return "FOCUSELEMENTAL"; }
    @Override public String getKey() { return "thaumcraft.EARTH"; }
    @Override public String getAspect() { return "terra"; }
    @Override public int getComplexity() { return getSettingValue("power") * 3; }
    @Override public float getDamageForDisplay(float finalPower) { return 2 * getSettingValue("power") * finalPower; }

    @Override
    public boolean execute(HitResult target, Trajectory trajectory, float finalPower, int num) {
        Level world = getPackage().world;
        var hv = target.getLocation();
        FocusFx.impact(world, hv.x, hv.y, hv.z, getKey());
        if (target instanceof EntityHitResult eh) {
            if (world instanceof ServerLevel sl) eh.getEntity().hurtServer(sl, sl.damageSources().thrown(eh.getEntity(), getPackage().getCaster()), getDamageForDisplay(finalPower));
            return true;
        }
        if (target instanceof BlockHitResult bh && bh.getType() == HitResult.Type.BLOCK && world instanceof ServerLevel sl) {
            var pos = bh.getBlockPos();
            if (getPackage().getCaster() instanceof Player p && world.getBlockState(pos).getDestroySpeed(world, pos) <= getDamageForDisplay(finalPower) / 25.0f)
                ServerEvents.addBreaker(sl, pos, world.getBlockState(pos), p, false, false, 0, 1.0f, 0.0f, 1.0f, num, 0.1f, null);
        }
        return false;
    }

    @Override public NodeSetting[] createSettings() {
        return new NodeSetting[] {new NodeSetting("power", "focus.common.power", new NodeSetting.NodeSettingIntRange(1, 5))};
    }

    @Override
    public void renderParticleFX(Level world, double x, double y, double z, double mx, double my, double mz) {
        var r = world.getRandom();
        float s = (float) (1.0 + r.nextGaussian() * 0.2);
        FociFx.gen(x, y, z, mx, my, mz, GenericParticleOptions.builder().gravity(0.4f).layer(1).maxAge(20 + r.nextInt(10)).alpha(1.0f, 0.0f)
                .particles(75 + r.nextInt(4), 1, 1).slowDown(0.9).rotation(0, (float) r.nextGaussian()).scale(s, s / 2.0f), 0);
    }

    @Override
    public void onCast(Entity caster) {
        caster.level().playSound(null, caster.blockPosition().above(), SoundEvents.ENDER_DRAGON_SHOOT, SoundSource.PLAYERS, 0.25f, 1.0f + (float) (caster.level().getRandom().nextGaussian() * 0.05));
    }
}
