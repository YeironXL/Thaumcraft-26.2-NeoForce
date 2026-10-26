package art.arcane.thaumcraft.golems.ai;

import art.arcane.thaumcraft.entity.construct.OwnedConstruct;
import java.util.EnumSet;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.target.TargetGoal;
import net.minecraft.world.entity.ai.targeting.TargetingConditions;

/** TC6 AIOwnerHurtTarget: attack whatever the owner attacked. */
public class OwnerHurtTargetGoal extends TargetGoal {
    private final OwnedConstruct construct;
    private LivingEntity target;
    private int timestamp;
    public OwnerHurtTargetGoal(OwnedConstruct c) { super(c, false); construct = c; setFlags(EnumSet.of(Flag.TARGET)); }
    @Override public boolean canUse() {
        if (!construct.isOwned()) return false;
        LivingEntity o = construct.getOwnerEntity();
        if (o == null) return false;
        target = o.getLastHurtMob();
        return o.getLastHurtMobTimestamp() != timestamp && canAttack(target, TargetingConditions.DEFAULT);
    }
    @Override public void start() {
        mob.setTarget(target);
        LivingEntity o = construct.getOwnerEntity();
        if (o != null) timestamp = o.getLastHurtMobTimestamp();
        super.start();
    }
}
