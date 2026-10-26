package art.arcane.thaumcraft.golems.ai;

import art.arcane.thaumcraft.entity.construct.OwnedConstruct;
import java.util.EnumSet;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.target.TargetGoal;
import net.minecraft.world.entity.ai.targeting.TargetingConditions;

/** TC6 AIOwnerHurtByTarget: attack whatever attacked the owner. */
public class OwnerHurtByTargetGoal extends TargetGoal {
    private final OwnedConstruct construct;
    private LivingEntity attacker;
    private int timestamp;
    public OwnerHurtByTargetGoal(OwnedConstruct c) { super(c, false); construct = c; setFlags(EnumSet.of(Flag.TARGET)); }
    @Override public boolean canUse() {
        if (!construct.isOwned()) return false;
        LivingEntity o = construct.getOwnerEntity();
        if (o == null) return false;
        attacker = o.getLastHurtByMob();
        return o.getLastHurtByMobTimestamp() != timestamp && canAttack(attacker, TargetingConditions.DEFAULT);
    }
    @Override public void start() {
        mob.setTarget(attacker);
        LivingEntity o = construct.getOwnerEntity();
        if (o != null) timestamp = o.getLastHurtByMobTimestamp();
        super.start();
    }
}
