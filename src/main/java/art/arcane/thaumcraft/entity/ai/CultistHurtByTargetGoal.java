package art.arcane.thaumcraft.entity.ai;

import art.arcane.thaumcraft.entity.monster.CultistEntity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.phys.AABB;

/** TC6 AICultistHurtByTarget: revenge targeting that calls every nearby cultist (any kind) for help. */
public class CultistHurtByTargetGoal extends HurtByTargetGoal {
    public CultistHurtByTargetGoal(PathfinderMob owner, boolean callsHelp) {
        super(owner);
        if (callsHelp) setAlertOthers();
    }

    @Override
    protected void alertOthers() {
        double d0 = getFollowDistance();
        LivingEntity revenge = mob.getLastHurtByMob();
        for (CultistEntity other : mob.level().getEntitiesOfClass(CultistEntity.class,
                new AABB(mob.getX(), mob.getY(), mob.getZ(), mob.getX() + 1.0, mob.getY() + 1.0, mob.getZ() + 1.0).inflate(d0, 10.0, d0))) {
            if (mob != other && other.getTarget() == null && !other.isAlliedTo(revenge)) other.setTarget(revenge);
        }
    }
}
