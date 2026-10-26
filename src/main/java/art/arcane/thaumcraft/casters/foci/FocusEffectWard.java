package art.arcane.thaumcraft.casters.foci;

import art.arcane.thaumcraft.casters.*;
import art.arcane.thaumcraft.casters.ward.WardManager;
import art.arcane.thaumcraft.fx.GenericParticleOptions;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

/**
 * TC4 Focus of Warding re-made as a TC6 focus effect. Hitting a block wards it for the caster (unbreakable and
 * unchangeable for everyone else, immune to explosions and pistons); the owner hitting a warded block unwards it.
 * Entities are not affected. Cost follows TC4's cheap per-block upkeep: fixed complexity 4, no settings.
 */
public final class FocusEffectWard extends FocusEffect {
    @Override public String getResearch() { return "FOCUSWARD"; }
    @Override public String getKey() { return "thaumcraft.WARD"; }
    @Override public String getAspect() { return "praemunio"; }
    @Override public int getComplexity() { return 4; }

    @Override
    public boolean execute(HitResult target, Trajectory trajectory, float finalPower, int num) {
        Level world = getPackage().world;
        var hv = target.getLocation();
        FocusFx.impact(world, hv.x, hv.y, hv.z, getKey());
        if (!(target instanceof BlockHitResult bh) || bh.getType() != HitResult.Type.BLOCK || world.isClientSide()) return false;
        var caster = getPackage().getCaster();
        if (caster == null) return false;
        return toggle(world, bh.getBlockPos(), caster.getUUID());
    }

    /** Ward an unwarded block, or unward one owned by {@code who}. Returns whether anything changed. */
    public static boolean toggle(Level world, net.minecraft.core.BlockPos pos, java.util.UUID who) {
        var owner = WardManager.owner(world, pos);
        boolean changed;
        if (owner == null) changed = WardManager.ward(world, pos, who);
        else changed = owner.equals(who) && WardManager.unward(world, pos);
        if (changed) world.playSound(null, pos, owner == null ? SoundEvents.AMETHYST_BLOCK_CHIME : SoundEvents.AMETHYST_BLOCK_BREAK, SoundSource.BLOCKS, 1.0f, owner == null ? 1.2f : 0.8f);
        return changed;
    }

    @Override
    public void onCast(Entity caster) {
        caster.level().playSound(null, caster.blockPosition().above(), SoundEvents.BEACON_POWER_SELECT, SoundSource.PLAYERS, 0.4f, 1.6f);
    }

    @Override
    public void renderParticleFX(Level world, double x, double y, double z, double vx, double vy, double vz) {
        var r = world.getRandom();
        FociFx.gen(x, y, z, vx, vy, vz, GenericParticleOptions.builder().maxAge(10 + r.nextInt(6)).color(0.3f, 0.45f + r.nextFloat() * 0.2f, 1.0f)
                .alpha(0.0f, 0.8f, 0.8f, 0.0f).grid(64).particles(0, 1, 1).scale(0.6f + r.nextFloat() * 0.3f, 0.1f).slowDown(0.85), 0);
    }
}
