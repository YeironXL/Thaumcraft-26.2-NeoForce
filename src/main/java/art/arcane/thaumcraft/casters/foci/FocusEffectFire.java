package art.arcane.thaumcraft.casters.foci;

import art.arcane.thaumcraft.casters.*;
import art.arcane.thaumcraft.fx.GenericParticleOptions;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;

public final class FocusEffectFire extends FocusEffect {
    @Override public String getResearch() { return "BASEAUROMANCY"; }
    @Override public String getKey() { return "thaumcraft.FIRE"; }
    @Override public String getAspect() { return "ignis"; }
    @Override public int getComplexity() { return getSettingValue("duration") + getSettingValue("power") * 2; }
    @Override public float getDamageForDisplay(float finalPower) { return (3 + getSettingValue("power")) * finalPower; }

    @Override
    public boolean execute(HitResult target, Trajectory trajectory, float finalPower, int num) {
        Level world = getPackage().world;
        var hv = target.getLocation();
        FocusFx.impact(world, hv.x, hv.y, hv.z, getKey());
        if (!(target instanceof EntityHitResult eh)) {
            if (target instanceof BlockHitResult bh && bh.getType() == HitResult.Type.BLOCK && getSettingValue("duration") > 0) {
                BlockPos pos = bh.getBlockPos().relative(bh.getDirection());
                if (world.isEmptyBlock(pos) && world.getRandom().nextFloat() < finalPower) {
                    world.playSound(null, pos, SoundEvents.FLINTANDSTEEL_USE, SoundSource.BLOCKS, 1.0f, world.getRandom().nextFloat() * 0.4f + 0.8f);
                    world.setBlock(pos, Blocks.FIRE.defaultBlockState(), 11);
                    return true;
                }
            }
            return false;
        }
        Entity e = eh.getEntity();
        if (e.fireImmune()) return false;
        float fire = 1 + getSettingValue("duration") * getSettingValue("duration");
        float damage = getDamageForDisplay(finalPower);
        fire *= finalPower;
        if (world instanceof ServerLevel sl) {
            var src = new DamageSource(sl.registryAccess().lookupOrThrow(Registries.DAMAGE_TYPE).getOrThrow(DamageTypes.FIREBALL), e, getPackage().getCaster());
            e.hurtServer(sl, src, damage);
        }
        if (fire > 0) e.igniteForSeconds(Math.round(fire));
        return true;
    }

    @Override public NodeSetting[] createSettings() {
        return new NodeSetting[] {new NodeSetting("power", "focus.common.power", new NodeSetting.NodeSettingIntRange(1, 5)),
                new NodeSetting("duration", "focus.fire.burn", new NodeSetting.NodeSettingIntRange(0, 5))};
    }

    @Override
    public void renderParticleFX(Level world, double x, double y, double z, double mx, double my, double mz) {
        FociFx.gen(x, y, z, mx, my, mz, GenericParticleOptions.builder().gravity(-0.2f).maxAge(10).alpha(0.7f).particles(640, 10, 1).slowDown(0.75)
                .scale((float) (1.5 + world.getRandom().nextGaussian() * 0.2)), 0);
    }

    @Override
    public void onCast(Entity caster) {
        caster.level().playSound(null, caster.blockPosition().above(), SoundEvents.FIRECHARGE_USE, SoundSource.PLAYERS, 1.0f, 1.0f + (float) (caster.level().getRandom().nextGaussian() * 0.05));
    }
}
