package art.arcane.thaumcraft.casters.foci;

import art.arcane.thaumcraft.casters.*;
import art.arcane.thaumcraft.fx.GenericParticleOptions;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;

import java.util.ArrayList;
import java.util.List;

/**
 * TC4 Wand Focus: Shock, recreated as a TC6 focus effect. TC4: a target-seeking lightning attack with considerable
 * damage that no longer transforms mobs the way real lightning does. Port: lightning damage to the struck entity,
 * then (setting "chain", invented, inspired by the TC4 "chain lightning" upgrade players remember) it arcs to up to
 * N further living things within 6 blocks, each jump at 75 % of the previous damage. No lightning bolt entity is
 * spawned, so creepers/pigs/villagers are never transformed.
 */
public final class FocusEffectShock extends FocusEffect {
    public static final double CHAIN_RANGE = 6.0;
    @Override public String getResearch() { return "FOCUSSHOCK"; }
    @Override public String getKey() { return "thaumcraft.SHOCK"; }
    @Override public String getAspect() { return "potentia"; }
    @Override public int getComplexity() { return getSettingValue("power") * 2 + getSettingValue("chain") * 3; }
    @Override public float getDamageForDisplay(float finalPower) { return (3 + getSettingValue("power")) * finalPower; }

    @Override
    public boolean execute(HitResult target, Trajectory trajectory, float finalPower, int num) {
        Level world = getPackage().world;
        var hv = target.getLocation();
        FocusFx.impact(world, hv.x, hv.y, hv.z, getKey());
        if (target instanceof EntityHitResult eh && world instanceof ServerLevel sl) {
            shock(sl, eh.getEntity(), getPackage().getCaster(), getDamageForDisplay(finalPower), getSettingValue("chain"));
            return true;
        }
        return false;
    }

    /** Hits {@code first}, then chains to the nearest unhit living entities. Returns every entity struck. */
    public static List<Entity> shock(ServerLevel sl, Entity first, Entity caster, float damage, int chain) {
        List<Entity> hit = new ArrayList<>();
        Entity cur = first;
        float dmg = damage;
        for (int i = 0; i <= chain && cur != null; i++) {
            cur.hurtServer(sl, sl.damageSources().lightningBolt(), dmg);
            hit.add(cur);
            sl.playSound(null, cur.getX(), cur.getY(), cur.getZ(), SoundEvents.LIGHTNING_BOLT_IMPACT, SoundSource.PLAYERS, 0.6f, 1.6f + sl.getRandom().nextFloat() * 0.3f);
            if (i == chain) break;
            final Entity from = cur;
            Entity next = null;
            double best = Double.MAX_VALUE;
            for (Entity e : sl.getEntities(from, new AABB(from.position(), from.position()).inflate(CHAIN_RANGE),
                    e -> e instanceof LivingEntity le && le.isAlive() && e != caster && !hit.contains(e))) {
                double d = e.distanceToSqr(from);
                if (d < best && d <= CHAIN_RANGE * CHAIN_RANGE) { best = d; next = e; }
            }
            if (next != null) FocusFx.zap(sl, from.position().add(0, from.getBbHeight() / 2, 0), next.position().add(0, next.getBbHeight() / 2, 0), 0x9FD8FF, 0.5f);
            cur = next;
            dmg *= 0.75f;
        }
        return hit;
    }

    @Override public NodeSetting[] createSettings() {
        return new NodeSetting[] {new NodeSetting("power", "focus.common.power", new NodeSetting.NodeSettingIntRange(1, 5)),
                new NodeSetting("chain", "focus.shock.chain", new NodeSetting.NodeSettingIntRange(0, 3))};
    }

    @Override
    public void renderParticleFX(Level world, double x, double y, double z, double mx, double my, double mz) {
        var r = world.getRandom();
        FociFx.gen(x, y, z, mx, my, mz, GenericParticleOptions.builder().maxAge(4 + r.nextInt(4)).alpha(0.9f, 0.0f).grid(64)
                .particles(320, 8, 1).color(0.6f, 0.85f, 1.0f).scale(0.6f + r.nextFloat() * 0.4f, 0.1f), 0);
    }

    @Override
    public void onCast(Entity caster) {
        caster.level().playSound(null, caster.blockPosition().above(), SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.PLAYERS, 0.15f, 2.0f);
    }
}
