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
import net.minecraft.world.phys.HitResult;

public final class FocusEffectBreak extends FocusEffect {
    @Override public String getResearch() { return "FOCUSBREAK"; }
    @Override public String getKey() { return "thaumcraft.BREAK"; }
    @Override public String getAspect() { return "perditio"; }
    @Override public int getComplexity() {
        return getSettingValue("power") * 3 + getSettingValue("silk") * 4 + (getSettingValue("fortune") == 0 ? 0 : (getSettingValue("fortune") + 1) * 3);
    }

    @Override
    public boolean execute(HitResult target, Trajectory trajectory, float finalPower, int num) {
        if (target instanceof BlockHitResult bh && bh.getType() == HitResult.Type.BLOCK) {
            Level world = getPackage().world;
            var bp = bh.getBlockPos();
            FocusFx.impact(world, bp.getX() + 0.5, bp.getY() + 0.5, bp.getZ() + 0.5, getKey());
            boolean silk = getSettingValue("silk") > 0;
            int fortune = getSettingValue("fortune");
            float strength = getSettingValue("power") * finalPower;
            float dur = (float) Math.sqrt(world.getBlockState(bp).getDestroySpeed(world, bp) * 100.0f);
            if (getPackage().getCaster() instanceof Player p && world instanceof ServerLevel sl)
                ServerEvents.addBreaker(sl, bp, world.getBlockState(bp), p, true, silk, fortune, strength, dur, dur, (int) (dur / strength / 3.0f * num),
                        0.25f + (silk ? 0.25f : 0.0f) + fortune * 0.1f, null);
            return true;
        }
        return true;
    }

    @Override public NodeSetting[] createSettings() {
        return new NodeSetting[] {new NodeSetting("power", "focus.break.power", new NodeSetting.NodeSettingIntRange(1, 5)),
                new NodeSetting("fortune", "focus.common.fortune", new NodeSetting.NodeSettingIntList(new int[] {0, 1, 2, 3, 4}, new String[] {"focus.common.no", "I", "II", "III", "IV"})),
                new NodeSetting("silk", "focus.common.silk", new NodeSetting.NodeSettingIntList(new int[] {0, 1}, new String[] {"focus.common.no", "focus.common.yes"}))};
    }

    @Override
    public void renderParticleFX(Level world, double x, double y, double z, double mx, double my, double mz) {
        var r = world.getRandom();
        FociFx.gen(x, y, z, mx, my, mz, GenericParticleOptions.builder().maxAge(6 + r.nextInt(6)).particles(704 + r.nextInt(4) * 3, 3, 1).slowDown(0.8)
                .scale((float) (1.7 + r.nextGaussian() * 0.3)), 0);
    }

    @Override
    public void onCast(Entity caster) {
        caster.level().playSound(null, caster.blockPosition().above(), SoundEvents.END_GATEWAY_SPAWN, SoundSource.PLAYERS, 0.1f, 2.0f + (float) (caster.level().getRandom().nextGaussian() * 0.05));
    }
}
