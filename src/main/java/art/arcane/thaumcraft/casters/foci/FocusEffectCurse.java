package art.arcane.thaumcraft.casters.foci;

import art.arcane.thaumcraft.ModBlocks;
import art.arcane.thaumcraft.casters.*;
import art.arcane.thaumcraft.fx.Fx;
import art.arcane.thaumcraft.fx.GenericParticleOptions;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;

public final class FocusEffectCurse extends FocusEffect {
    @Override public String getResearch() { return "FOCUSCURSE"; }
    @Override public String getKey() { return "thaumcraft.CURSE"; }
    @Override public String getAspect() { return "mortuus"; }
    @Override public int getComplexity() { return getSettingValue("duration") + getSettingValue("power") * 3; }
    @Override public float getDamageForDisplay(float finalPower) { return (1.0f + getSettingValue("power")) * finalPower; }

    @Override
    public boolean execute(HitResult target, Trajectory trajectory, float finalPower, int num) {
        Level world = getPackage().world;
        var hv = target.getLocation();
        if (world instanceof ServerLevel sl) Fx.bamf(sl, hv.x, hv.y, hv.z, 6946821, true, true, null);
        if (target instanceof EntityHitResult eh) {
            float damage = getDamageForDisplay(finalPower);
            int duration = 20 * getSettingValue("duration");
            int eff = Math.max(0, (int) (getSettingValue("power") * finalPower / 2.0f));
            if (world instanceof ServerLevel sl) eh.getEntity().hurtServer(sl, sl.damageSources().indirectMagic(eh.getEntity(), getPackage().getCaster()), damage);
            if (eh.getEntity() instanceof LivingEntity le) {
                var r = world.getRandom();
                le.addEffect(new MobEffectInstance(MobEffects.POISON, duration, eff));
                float c = 0.85f;
                if (r.nextFloat() < c) { le.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, duration, eff)); c -= 0.15f; }
                if (r.nextFloat() < c) { le.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, duration, eff)); c -= 0.15f; }
                if (r.nextFloat() < c) { le.addEffect(new MobEffectInstance(MobEffects.MINING_FATIGUE, duration * 2, eff)); c -= 0.15f; }
                if (r.nextFloat() < c) { le.addEffect(new MobEffectInstance(MobEffects.HUNGER, duration * 3, eff)); c -= 0.15f; }
                if (r.nextFloat() < c) le.addEffect(new MobEffectInstance(MobEffects.UNLUCK, duration * 3, eff));
            }
        } else if (target instanceof BlockHitResult bh && bh.getType() == HitResult.Type.BLOCK) {
            float f = (float) Math.min(8.0, 1.5 * getSettingValue("power") * finalPower);
            BlockPos c = bh.getBlockPos();
            for (BlockPos p : BlockPos.betweenClosed(BlockPos.containing(c.getX() - f, c.getY() - f, c.getZ() - f), BlockPos.containing(c.getX() + f, c.getY() + f, c.getZ() + f))) {
                if (p.distToCenterSqr(hv.x, hv.y, hv.z) <= f * f && world.isEmptyBlock(p.above()) && world.getBlockState(p).isCollisionShapeFullBlock(world, p))
                    world.setBlockAndUpdate(p.above(), ModBlocks.EFFECT_SAP.get().defaultBlockState());
            }
        }
        return false;
    }

    @Override public NodeSetting[] createSettings() {
        return new NodeSetting[] {new NodeSetting("power", "focus.common.power", new NodeSetting.NodeSettingIntRange(1, 5)),
                new NodeSetting("duration", "focus.common.duration", new NodeSetting.NodeSettingIntRange(1, 10))};
    }

    @Override
    public void renderParticleFX(Level world, double x, double y, double z, double mx, double my, double mz) {
        var r = world.getRandom();
        FociFx.gen(x, y, z, mx, my, mz, GenericParticleOptions.builder().maxAge(8).color(0.41f + r.nextFloat() * 0.2f, 0.0f, 0.019f + r.nextFloat() * 0.2f)
                .alpha(0.0f, r.nextFloat(), r.nextFloat(), r.nextFloat(), 0.0f).grid(16).particles(72 + r.nextInt(4), 1, 1).scale(2.0f + r.nextFloat() * 4.0f)
                .loop(false).slowDown(0.9).gravity(0).rotation(r.nextFloat(), 0.0f), r.nextInt(4));
    }

    @Override
    public void onCast(Entity caster) {
        caster.level().playSound(null, caster.blockPosition().above(), SoundEvents.ELDER_GUARDIAN_CURSE, SoundSource.PLAYERS, 0.15f, 1.0f + caster.level().getRandom().nextFloat() / 2.0f);
    }
}
