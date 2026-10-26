package art.arcane.thaumcraft.casters.foci;

import art.arcane.thaumcraft.casters.*;
import art.arcane.thaumcraft.fx.GenericParticleOptions;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.shapes.CollisionContext;

public final class FocusEffectFrost extends FocusEffect {
    @Override public String getResearch() { return "FOCUSELEMENTAL"; }
    @Override public String getKey() { return "thaumcraft.FROST"; }
    @Override public String getAspect() { return "gelum"; }
    @Override public int getComplexity() { return getSettingValue("duration") + getSettingValue("power") * 2; }
    @Override public float getDamageForDisplay(float finalPower) { return (3 + getSettingValue("power")) * finalPower; }

    @Override
    public boolean execute(HitResult target, Trajectory trajectory, float finalPower, int num) {
        Level world = getPackage().world;
        var hv = target.getLocation();
        FocusFx.impact(world, hv.x, hv.y, hv.z, getKey());
        if (target instanceof EntityHitResult eh) {
            float damage = getDamageForDisplay(finalPower);
            int duration = 20 * getSettingValue("duration");
            int potency = (int) (1.0f + getSettingValue("power") * finalPower / 3.0f);
            if (world instanceof ServerLevel sl) eh.getEntity().hurtServer(sl, sl.damageSources().thrown(eh.getEntity(), getPackage().getCaster()), damage);
            if (eh.getEntity() instanceof LivingEntity le) le.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, duration, potency));
        } else if (target instanceof BlockHitResult bh && bh.getType() == HitResult.Type.BLOCK) {
            float f = Math.min(16.0f, 2 * getSettingValue("power") * finalPower);
            BlockPos c = bh.getBlockPos();
            for (BlockPos p : BlockPos.betweenClosed(c.offset((int) -f, (int) -f, (int) -f), c.offset((int) f, (int) f, (int) f))) {
                if (p.distToCenterSqr(hv.x, hv.y, hv.z) > f * f) continue;
                var st = world.getBlockState(p);
                if (st.getFluidState().is(Fluids.WATER) && st.getFluidState().isSource() && st.is(Blocks.WATER)
                        && Blocks.FROSTED_ICE.defaultBlockState().canSurvive(world, p) && world.isUnobstructed(Blocks.FROSTED_ICE.defaultBlockState(), p, CollisionContext.empty())) {
                    world.setBlockAndUpdate(p, Blocks.FROSTED_ICE.defaultBlockState());
                    world.scheduleTick(p.immutable(), Blocks.FROSTED_ICE, Mth.nextInt(world.getRandom(), 60, 120));
                }
            }
        }
        return false;
    }

    @Override public NodeSetting[] createSettings() {
        return new NodeSetting[] {new NodeSetting("power", "focus.common.power", new NodeSetting.NodeSettingIntRange(1, 5)),
                new NodeSetting("duration", "focus.common.duration", new NodeSetting.NodeSettingIntRange(2, 10))};
    }

    @Override
    public void renderParticleFX(Level world, double x, double y, double z, double mx, double my, double mz) {
        var r = world.getRandom();
        FociFx.gen(x, y, z, mx, my, mz, GenericParticleOptions.builder().maxAge(40 + r.nextInt(40)).alpha(1.0f, 0.0f).particles(8, 1, 1).gravity(0.033f).slowDown(0.8)
                .randomMovement(0.0025f, 1.0E-4f, 0.0025f).scale((float) (0.7 + r.nextGaussian() * 0.3)).rotation(r.nextFloat() * 3.0f, (float) r.nextGaussian() / 4.0f), 0);
    }

    @Override
    public void onCast(Entity caster) {
        caster.level().playSound(null, caster.blockPosition().above(), SoundEvents.ZOMBIE_VILLAGER_CURE, SoundSource.PLAYERS, 0.2f, 1.0f + (float) (caster.level().getRandom().nextGaussian() * 0.05));
    }
}
