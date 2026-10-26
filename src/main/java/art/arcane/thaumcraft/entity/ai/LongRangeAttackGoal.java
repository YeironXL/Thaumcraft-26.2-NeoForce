package art.arcane.thaumcraft.entity.ai;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.RangedAttackGoal;
import net.minecraft.world.entity.monster.RangedAttackMob;

/** TC6 AILongRangeAttack: a ranged attack that only fires when the target is at least {@code min} blocks away. */
public class LongRangeAttackGoal extends RangedAttackGoal {
    private final Mob wielder;
    private final double minDistance;

    public LongRangeAttackGoal(RangedAttackMob mob, double min, double speed, int minInterval, int maxInterval, float radius) {
        super(mob, speed, minInterval, maxInterval, radius);
        this.wielder = (Mob) mob;
        this.minDistance = min;
    }

    @Override
    public boolean canUse() {
        boolean ex = super.canUse();
        if (ex) {
            LivingEntity t = wielder.getTarget();
            if (t == null) return false;
            if (!t.isAlive()) { wielder.setTarget(null); return false; }
            double ra = wielder.distanceToSqr(t.getX(), t.getBoundingBox().minY, t.getZ());
            if (ra < minDistance * minDistance) return false;
        }
        return ex;
    }
}
