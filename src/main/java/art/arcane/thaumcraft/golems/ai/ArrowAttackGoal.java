package art.arcane.thaumcraft.golems.ai;

import art.arcane.thaumcraft.golems.GolemEntity;
import java.util.EnumSet;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.ai.goal.Goal;

/** TC6 AIArrowAttack: ranged attack that keeps moving towards the target until it has a clear shot. */
public class ArrowAttackGoal extends Goal {
    private final GolemEntity host;
    private int rangedAttackTime = -1, seeTime;
    private final double speed;
    private final int attackIntervalMin, maxRangedAttackTime;
    private final float attackRadius, maxAttackDistance;
    public ArrowAttackGoal(GolemEntity host, double speed, int min, int max, float radius) {
        this.host = host; this.speed = speed; attackIntervalMin = min; maxRangedAttackTime = max; attackRadius = radius; maxAttackDistance = radius * radius;
        setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }
    @Override public boolean requiresUpdateEveryTick() { return true; }
    @Override public boolean canUse() { return host.getTarget() != null; }
    @Override public boolean canContinueToUse() { return canUse() || !host.getNavigation().isDone(); }
    @Override public void stop() { seeTime = 0; rangedAttackTime = -1; }
    @Override public void tick() {
        var t = host.getTarget();
        if (t == null) return;
        double d0 = host.distanceToSqr(t.getX(), t.getBoundingBox().minY, t.getZ());
        boolean flag = host.getSensing().hasLineOfSight(t);
        seeTime = flag ? seeTime + 1 : 0;
        if (d0 <= maxAttackDistance && seeTime >= 20) host.getNavigation().stop();
        else host.getNavigation().moveTo(t, speed);
        host.getLookControl().setLookAt(t, 10.0f, 30.0f);
        if (--rangedAttackTime == 0) {
            if (d0 > maxAttackDistance || !flag) return;
            float f = (float) Math.sqrt(d0) / attackRadius;
            host.performRangedAttack(t, Mth.clamp(f, 0.1f, 1.0f));
            rangedAttackTime = Mth.floor(f * (maxRangedAttackTime - attackIntervalMin) + attackIntervalMin);
        } else if (rangedAttackTime < 0) {
            float f2 = (float) Math.sqrt(d0) / attackRadius;
            rangedAttackTime = Mth.floor(f2 * (maxRangedAttackTime - attackIntervalMin) + attackIntervalMin);
        }
    }
}
